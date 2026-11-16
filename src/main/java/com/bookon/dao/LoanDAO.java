package com.bookon.dao;

import com.bookon.model.Book;
import com.bookon.model.Loan;
import com.bookon.model.Student;
import com.bookon.util.DBUtil;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LoanDAO {
    public static final int MAX_LOAN_COUNT = 3;
    public enum Status { ALL, UNRETURNED, OVERDUE, RETURNED }
    public enum BorrowResult { BORROWED, INACTIVE_STUDENT, PENALIZED, OVERDUE, LIMIT_REACHED, UNAVAILABLE }
    public enum ReturnStatus { RETURNED, ALREADY_RETURNED, FAILED }
    public record ReturnResult(ReturnStatus status, int lateDays) {}

    // Keep these four statements in sync with sql/03_loan_return.sql.
    private static final String DECREMENT_STOCK = """
            UPDATE TBL_BOOK SET AVAIL_QTY = AVAIL_QTY - 1
            WHERE BOOK_ID = ? AND USE_YN = 'Y' AND AVAIL_QTY > 0
            """;
    private static final String INSERT_LOAN = """
            INSERT INTO TBL_LOAN (LOAN_ID, STUDENT_ID, BOOK_ID, LOAN_DATE, DUE_DATE, RETURN_YN)
            SELECT SEQ_LOAN.NEXTVAL, s.STUDENT_ID, ?, TRUNC(SYSDATE), ?, 'N'
            FROM TBL_STUDENT s
            WHERE s.STUDENT_ID = ? AND s.USE_YN = 'Y'
            """;
    private static final String MARK_RETURNED = """
            UPDATE TBL_LOAN SET RETURN_YN = 'Y', RETURN_DATE = TRUNC(SYSDATE)
            WHERE LOAN_ID = ? AND RETURN_YN = 'N'
            """;
    private static final String INCREMENT_STOCK = """
            UPDATE TBL_BOOK SET AVAIL_QTY = AVAIL_QTY + 1
            WHERE BOOK_ID = (SELECT BOOK_ID FROM TBL_LOAN WHERE LOAN_ID = ?)
            """;

    public BorrowResult borrow(int studentId, String bookId, Date dueDate) throws SQLException {
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            boolean committed = false;
            try {
                if (!lockStudent(connection, studentId)) return BorrowResult.INACTIVE_STUDENT;
                Student student = readBorrowStatus(connection, studentId);
                if (!"Y".equals(student.getUseYn())) return BorrowResult.INACTIVE_STUDENT;
                if (student.isPenalized()) return BorrowResult.PENALIZED;
                if (student.getOverdueCount() > 0) return BorrowResult.OVERDUE;
                if (student.getLoanCount() >= MAX_LOAN_COUNT) return BorrowResult.LIMIT_REACHED;
                try (PreparedStatement stock = connection.prepareStatement(DECREMENT_STOCK)) {
                    stock.setString(1, bookId);
                    if (stock.executeUpdate() != 1) return BorrowResult.UNAVAILABLE;
                }
                try (PreparedStatement statement = connection.prepareStatement(INSERT_LOAN)) {
                    statement.setString(1, bookId);
                    statement.setDate(2, dueDate);
                    statement.setInt(3, studentId);
                    if (statement.executeUpdate() != 1) return BorrowResult.INACTIVE_STUDENT;
                }
                connection.commit();
                committed = true;
                return BorrowResult.BORROWED;
            } finally {
                finishTransaction(connection, committed);
            }
        }
    }

    public ReturnResult returnLoan(int loanId) throws SQLException {
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            boolean committed = false;
            try {
                int studentId = findLoanStudent(connection, loanId);
                if (studentId == 0) return new ReturnResult(ReturnStatus.ALREADY_RETURNED, 0);
                // Borrow and return both take the student lock before modifying loan or stock.
                if (!lockStudent(connection, studentId)) return new ReturnResult(ReturnStatus.FAILED, 0);
                int updated = updateById(connection, MARK_RETURNED, loanId);
                if (updated == 0) return new ReturnResult(ReturnStatus.ALREADY_RETURNED, 0);
                if (updated != 1 || updateById(connection, INCREMENT_STOCK, loanId) != 1) {
                    return new ReturnResult(ReturnStatus.FAILED, 0);
                }
                int days = findLateDays(connection, loanId);
                if (days > 0 && !extendPenalty(connection, studentId, days)) {
                    return new ReturnResult(ReturnStatus.FAILED, 0);
                }
                connection.commit();
                committed = true;
                return new ReturnResult(ReturnStatus.RETURNED, days);
            } finally {
                finishTransaction(connection, committed);
            }
        }
    }

    private boolean lockStudent(Connection connection, int studentId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT STUDENT_ID FROM TBL_STUDENT WHERE STUDENT_ID = ? FOR UPDATE")) {
            statement.setInt(1, studentId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    private static final String STUDENT_STATUS = """
            SELECT s.STUDENT_ID, s.STUDENT_NO, s.STUDENT_NAME, s.USE_YN, s.PENALTY_END_DATE,
                   CASE WHEN s.PENALTY_END_DATE > TRUNC(SYSDATE) THEN 1 ELSE 0 END AS PENALIZED,
                   (SELECT COUNT(*) FROM TBL_LOAN l
                    WHERE l.STUDENT_ID = s.STUDENT_ID AND l.RETURN_YN = 'N') AS LOAN_COUNT,
                   (SELECT COUNT(*) FROM TBL_LOAN l
                    WHERE l.STUDENT_ID = s.STUDENT_ID AND l.RETURN_YN = 'N'
                      AND l.DUE_DATE < TRUNC(SYSDATE)) AS OVERDUE_COUNT
            FROM TBL_STUDENT s
            """;

    private Student readBorrowStatus(Connection connection, int studentId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(STUDENT_STATUS + " WHERE s.STUDENT_ID = ?")) {
            statement.setInt(1, studentId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) throw new SQLException("잠근 학생 정보를 찾을 수 없습니다.");
                return readStudentStatus(rows);
            }
        }
    }

    private Student readStudentStatus(ResultSet rows) throws SQLException {
        Student student = new Student();
        student.setStudentId(rows.getInt("STUDENT_ID"));
        student.setStudentNo(rows.getString("STUDENT_NO"));
        student.setStudentName(rows.getString("STUDENT_NAME"));
        student.setUseYn(rows.getString("USE_YN"));
        student.setPenaltyEndDate(rows.getDate("PENALTY_END_DATE"));
        student.setPenalized(rows.getInt("PENALIZED") == 1);
        student.setLoanCount(rows.getInt("LOAN_COUNT"));
        student.setOverdueCount(rows.getInt("OVERDUE_COUNT"));
        return student;
    }

    private int findLoanStudent(Connection connection, int loanId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT STUDENT_ID FROM TBL_LOAN WHERE LOAN_ID = ?")) {
            statement.setInt(1, loanId);
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next() ? rows.getInt(1) : 0;
            }
        }
    }

    private int findLateDays(Connection connection, int loanId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT GREATEST(0, TRUNC(RETURN_DATE) - TRUNC(DUE_DATE)) FROM TBL_LOAN WHERE LOAN_ID = ?")) {
            statement.setInt(1, loanId);
            try (ResultSet rows = statement.executeQuery()) {
                if (!rows.next()) throw new SQLException("반납된 대출 정보를 찾을 수 없습니다.");
                return rows.getInt(1);
            }
        }
    }

    private boolean extendPenalty(Connection connection, int studentId, int days) throws SQLException {
        String sql = """
                UPDATE TBL_STUDENT
                SET PENALTY_END_DATE = GREATEST(NVL(PENALTY_END_DATE, TRUNC(SYSDATE)), TRUNC(SYSDATE)) + ?
                WHERE STUDENT_ID = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, days);
            statement.setInt(2, studentId);
            return statement.executeUpdate() == 1;
        }
    }

    private int updateById(Connection connection, String sql, int id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate();
        }
    }

    private void finishTransaction(Connection connection, boolean committed) throws SQLException {
        // Do not turn auto-commit on if rollback fails. JDBC could commit pending changes.
        if (!committed) connection.rollback();
        connection.setAutoCommit(true);
    }

    public List<Loan> findLoans(String keyword, Status status) throws SQLException {
        String condition = switch (status) {
            case ALL -> "";
            case UNRETURNED -> " AND l.RETURN_YN = 'N'";
            case OVERDUE -> " AND l.RETURN_YN = 'N' AND l.DUE_DATE < TRUNC(SYSDATE)";
            case RETURNED -> " AND l.RETURN_YN = 'Y'";
        };
        String sql = """
                SELECT l.LOAN_ID, l.STUDENT_ID, l.BOOK_ID, l.LOAN_DATE, l.DUE_DATE,
                       l.RETURN_YN, l.RETURN_DATE, s.STUDENT_NAME, s.STUDENT_NO, b.TITLE,
                       CASE WHEN l.RETURN_YN = 'N' AND l.DUE_DATE < TRUNC(SYSDATE)
                            THEN 1 ELSE 0 END AS OVERDUE
                FROM TBL_LOAN l
                JOIN TBL_STUDENT s ON s.STUDENT_ID = l.STUDENT_ID
                JOIN TBL_BOOK b ON b.BOOK_ID = l.BOOK_ID
                WHERE 1 = 1
                """ + condition
                + (keyword.isEmpty() ? "" : """
                     AND (INSTR(LOWER(s.STUDENT_NAME), LOWER(?)) > 0
                          OR INSTR(LOWER(s.STUDENT_NO), LOWER(?)) > 0
                          OR INSTR(LOWER(b.TITLE), LOWER(?)) > 0)
                    """)
                + " ORDER BY l.LOAN_DATE DESC, l.LOAN_ID DESC";
        List<Loan> loans = new ArrayList<>();
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (!keyword.isEmpty()) {
                for (int i = 1; i <= 3; i++) statement.setString(i, keyword);
            }
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    Loan loan = new Loan();
                    loan.setLoanId(rows.getInt("LOAN_ID"));
                    loan.setStudentId(rows.getInt("STUDENT_ID"));
                    loan.setBookId(rows.getString("BOOK_ID"));
                    loan.setLoanDate(rows.getDate("LOAN_DATE"));
                    loan.setDueDate(rows.getDate("DUE_DATE"));
                    loan.setReturnYn(rows.getString("RETURN_YN"));
                    loan.setReturnDate(rows.getDate("RETURN_DATE"));
                    loan.setStudentName(rows.getString("STUDENT_NAME"));
                    loan.setStudentNo(rows.getString("STUDENT_NO"));
                    loan.setBookTitle(rows.getString("TITLE"));
                    loan.setOverdue(rows.getInt("OVERDUE") == 1);
                    loans.add(loan);
                }
            }
        }
        return loans;
    }

    public List<Student> findActiveStudents() throws SQLException {
        String sql = STUDENT_STATUS + " WHERE s.USE_YN = 'Y' ORDER BY s.STUDENT_NO";
        List<Student> students = new ArrayList<>();
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                students.add(readStudentStatus(rows));
            }
        }
        return students;
    }

    public List<Book> findAvailableBooks() throws SQLException {
        String sql = """
                SELECT BOOK_ID, TITLE, AVAIL_QTY, TOTAL_QTY FROM TBL_BOOK
                WHERE USE_YN = 'Y' AND AVAIL_QTY > 0 ORDER BY TITLE, BOOK_ID
                """;
        List<Book> books = new ArrayList<>();
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                Book book = new Book();
                book.setBookId(rows.getString("BOOK_ID"));
                book.setTitle(rows.getString("TITLE"));
                book.setAvailQty(rows.getInt("AVAIL_QTY"));
                book.setTotalQty(rows.getInt("TOTAL_QTY"));
                books.add(book);
            }
        }
        return books;
    }
}
