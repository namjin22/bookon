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
    public enum Status { ALL, UNRETURNED, OVERDUE, RETURNED }
    public enum ReturnResult { RETURNED, ALREADY_RETURNED, FAILED }

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

    public boolean borrow(int studentId, int bookId, Date dueDate) throws SQLException {
        try (Connection connection = DBUtil.getConnection()) {
            connection.setAutoCommit(false);
            boolean committed = false;
            try {
                if (updateById(connection, DECREMENT_STOCK, bookId) != 1) return false;
                try (PreparedStatement statement = connection.prepareStatement(INSERT_LOAN)) {
                    statement.setInt(1, bookId);
                    statement.setDate(2, dueDate);
                    statement.setInt(3, studentId);
                    if (statement.executeUpdate() != 1) return false;
                }
                connection.commit();
                committed = true;
                return true;
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
                int updated = updateById(connection, MARK_RETURNED, loanId);
                if (updated == 0) return ReturnResult.ALREADY_RETURNED;
                if (updated != 1 || updateById(connection, INCREMENT_STOCK, loanId) != 1) {
                    return ReturnResult.FAILED;
                }
                connection.commit();
                committed = true;
                return ReturnResult.RETURNED;
            } finally {
                finishTransaction(connection, committed);
            }
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
                    loan.setBookId(rows.getInt("BOOK_ID"));
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
        String sql = """
                SELECT STUDENT_ID, STUDENT_NO, STUDENT_NAME FROM TBL_STUDENT
                WHERE USE_YN = 'Y' ORDER BY STUDENT_NO
                """;
        List<Student> students = new ArrayList<>();
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                Student student = new Student();
                student.setStudentId(rows.getInt("STUDENT_ID"));
                student.setStudentNo(rows.getString("STUDENT_NO"));
                student.setStudentName(rows.getString("STUDENT_NAME"));
                students.add(student);
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
                book.setBookId(rows.getInt("BOOK_ID"));
                book.setTitle(rows.getString("TITLE"));
                book.setAvailQty(rows.getInt("AVAIL_QTY"));
                book.setTotalQty(rows.getInt("TOTAL_QTY"));
                books.add(book);
            }
        }
        return books;
    }
}
