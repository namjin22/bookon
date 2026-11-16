package com.bookon.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import com.bookon.model.Student;
import com.bookon.util.DBUtil;

public class StudentDAO {
    // 학번 UNIQUE 제약(UQ_STUDENT_NO) 위반은 ORA-00001로 올라옵니다.
    private static final int ORACLE_UNIQUE_VIOLATION = 1;

    // 목록·수정 화면용 조회입니다. 대출 정지 여부와 미반납·연체 권수는 SQL에서 계산해 모델에 채웁니다.
    // 대출 정지: PENALTY_END_DATE가 오늘보다 뒤이면 정지 중입니다 (오늘이면 이미 대출할 수 있습니다).
    private static final String SELECT_STUDENT = """
            SELECT s.STUDENT_ID, s.STUDENT_NO, s.STUDENT_NAME, s.PHONE, s.USE_YN, s.REG_DATE,
                   s.PENALTY_END_DATE,
                   CASE WHEN s.PENALTY_END_DATE > TRUNC(SYSDATE) THEN 1 ELSE 0 END AS PENALIZED,
                   (SELECT COUNT(*) FROM TBL_LOAN l
                    WHERE l.STUDENT_ID = s.STUDENT_ID AND l.RETURN_YN = 'N') AS LOAN_COUNT,
                   (SELECT COUNT(*) FROM TBL_LOAN l
                    WHERE l.STUDENT_ID = s.STUDENT_ID AND l.RETURN_YN = 'N'
                      AND l.DUE_DATE < TRUNC(SYSDATE)) AS OVERDUE_COUNT
            FROM TBL_STUDENT s
            """;

    // 새 학생을 등록합니다. 학번이 이미 있으면 SQLException이 발생하며 isDuplicateStudentNo로 구분합니다.
    public void insert(Student student) throws SQLException {
        String sql = "INSERT INTO TBL_STUDENT "
                + "(STUDENT_ID, STUDENT_NO, STUDENT_NAME, PHONE, USE_YN, REG_DATE) "
                + "VALUES (SEQ_STUDENT.NEXTVAL, ?, ?, ?, 'Y', SYSDATE)";
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, student.getStudentNo());
            statement.setString(2, student.getStudentName());
            setPhone(statement, 3, student.getPhone());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("학생 등록 건수가 1이 아닙니다.");
            }
        }
    }

    // 사용 중인 학생만 최신 등록순으로 가져옵니다. 같은 시각이면 ID가 큰 것이 먼저입니다.
    public List<Student> findAll() throws SQLException {
        return search("");
    }

    // 키워드는 이름(대소문자 무시) 또는 학번에 포함되는지 검사합니다.
    public List<Student> search(String keyword) throws SQLException {
        boolean hasKeyword = !keyword.isEmpty();
        String sql = SELECT_STUDENT + "WHERE s.USE_YN = 'Y' "
                + (hasKeyword ? "AND (INSTR(LOWER(s.STUDENT_NAME), LOWER(?)) > 0 OR INSTR(s.STUDENT_NO, ?) > 0) " : "")
                + "ORDER BY s.REG_DATE DESC, s.STUDENT_ID DESC";
        List<Student> students = new ArrayList<>();
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (hasKeyword) {
                statement.setString(1, keyword);
                statement.setString(2, keyword);
            }
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    students.add(readStudent(result));
                }
            }
        }
        return students;
    }

    // 사용 중인 학생 한 명을 가져옵니다. 없거나 삭제된 학생이면 null입니다.
    public Student findById(int id) throws SQLException {
        String sql = SELECT_STUDENT + "WHERE s.STUDENT_ID = ? AND s.USE_YN = 'Y'";
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? readStudent(result) : null;
            }
        }
    }

    // 학번·이름·연락처만 수정합니다. 대출 정지일(PENALTY_END_DATE)은 반납 처리에서만 바뀌므로 여기서 건드리지 않습니다.
    // 사용 중인 학생이 아니면 false입니다. 학번 중복은 SQLException입니다.
    public boolean update(Student student) throws SQLException {
        String sql = "UPDATE TBL_STUDENT SET STUDENT_NO = ?, STUDENT_NAME = ?, PHONE = ? "
                + "WHERE STUDENT_ID = ? AND USE_YN = 'Y'";
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, student.getStudentNo());
            statement.setString(2, student.getStudentName());
            setPhone(statement, 3, student.getPhone());
            statement.setInt(4, student.getStudentId());
            return statement.executeUpdate() == 1;
        }
    }

    // 미반납 대출이 없는 학생만 논리 삭제(USE_YN='N')합니다. 확인과 변경을 한 문장으로 처리합니다.
    public boolean deactivate(int id) throws SQLException {
        String sql = """
                UPDATE TBL_STUDENT SET USE_YN = 'N'
                WHERE STUDENT_ID = ? AND USE_YN = 'Y'
                  AND NOT EXISTS (SELECT 1 FROM TBL_LOAN WHERE STUDENT_ID = ? AND RETURN_YN = 'N')
                """;
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.setInt(2, id);
            return statement.executeUpdate() == 1;
        }
    }

    // 학생의 미반납 대출 건수를 셉니다. 삭제가 막힌 이유를 안내할 때 씁니다.
    public int countUnreturnedLoans(int studentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM TBL_LOAN WHERE STUDENT_ID = ? AND RETURN_YN = 'N'";
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        }
    }

    // 학번 중복(UQ_STUDENT_NO) 때문에 실패한 예외인지 판별합니다.
    public static boolean isDuplicateStudentNo(SQLException e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && sql.getErrorCode() == ORACLE_UNIQUE_VIOLATION
                    && String.valueOf(sql.getMessage()).contains("UQ_STUDENT_NO")) {
                return true;
            }
        }
        return false;
    }

    // 연락처가 비어 있으면 NULL로 저장합니다.
    private void setPhone(PreparedStatement statement, int index, String phone) throws SQLException {
        if (phone == null || phone.trim().isEmpty()) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, phone);
        }
    }

    private Student readStudent(ResultSet result) throws SQLException {
        Student student = new Student();
        student.setStudentId(result.getInt("STUDENT_ID"));
        student.setStudentNo(result.getString("STUDENT_NO"));
        student.setStudentName(result.getString("STUDENT_NAME"));
        student.setPhone(result.getString("PHONE"));
        student.setUseYn(result.getString("USE_YN"));
        student.setRegDate(result.getDate("REG_DATE"));
        student.setPenaltyEndDate(result.getDate("PENALTY_END_DATE"));
        student.setPenalized(result.getInt("PENALIZED") == 1);
        student.setLoanCount(result.getInt("LOAN_COUNT"));
        student.setOverdueCount(result.getInt("OVERDUE_COUNT"));
        return student;
    }
}
