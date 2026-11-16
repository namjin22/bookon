package com.bookon.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import com.bookon.model.Book;
import com.bookon.util.DBUtil;

public class BookDAO {
    public String insert(Book book, String kdc) throws SQLException {
        String nextSerial = """
                SELECT NVL(MAX(TO_NUMBER(SUBSTR(BOOK_ID, 5))), 0) + 1
                FROM TBL_BOOK WHERE SUBSTR(BOOK_ID, 1, 3) = ?
                """;
        String insert = """
                INSERT INTO TBL_BOOK
                    (BOOK_ID, TITLE, AUTHOR, PUBLISHER, TOTAL_QTY, AVAIL_QTY, USE_YN, REG_DATE)
                VALUES (?, ?, ?, ?, ?, ?, 'Y', SYSDATE)
                """;
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement next = connection.prepareStatement(nextSerial);
             PreparedStatement save = connection.prepareStatement(insert)) {
            for (int retry = 0; ; retry++) {
                next.setString(1, kdc);
                int serial;
                try (ResultSet result = next.executeQuery()) {
                    result.next();
                    serial = result.getInt(1);
                }
                if (serial > 999) throw new CodeExhaustedException();
                String code = String.format(java.util.Locale.ROOT, "%s-%03d", kdc, serial);
                save.setString(1, code);
                save.setString(2, book.getTitle());
                save.setString(3, book.getAuthor());
                if (book.getPublisher() == null || book.getPublisher().isBlank()) save.setNull(4, Types.VARCHAR);
                else save.setString(4, book.getPublisher());
                save.setInt(5, book.getTotalQty());
                save.setInt(6, book.getTotalQty());
                try {
                    if (save.executeUpdate() != 1) throw new SQLException("도서 등록 건수가 1이 아닙니다.");
                    return code;
                } catch (SQLException e) {
                    if (e.getErrorCode() != 1 || retry == 3) throw e;
                }
            }
        }
    }

    public static class CodeExhaustedException extends SQLException {
        private static final long serialVersionUID = 1L;
    }

    // 사용 중인 도서만 최신 등록순으로 가져옵니다. 같은 시각이면 ID가 큰 것이 먼저입니다.
    public List<Book> findAll() throws SQLException {
        return search("");
    }

    public List<Book> search(String keyword) throws SQLException {
        String sql = "SELECT BOOK_ID, TITLE, AUTHOR, PUBLISHER, TOTAL_QTY, AVAIL_QTY, USE_YN, REG_DATE "
                + "FROM TBL_BOOK WHERE USE_YN = 'Y' "
                + (keyword.isEmpty() ? "" : "AND INSTR(LOWER(TITLE), LOWER(?)) > 0 ")
                + "ORDER BY REG_DATE DESC, BOOK_ID DESC";
        List<Book> books = new ArrayList<>();
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (!keyword.isEmpty()) statement.setString(1, keyword);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    books.add(readBook(result));
                }
            }
        }
        return books;
    }

    public Book findById(String id) throws SQLException {
        String sql = "SELECT * FROM TBL_BOOK WHERE BOOK_ID = ? AND USE_YN = 'Y'";
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? readBook(result) : null;
            }
        }
    }

    // 현재 대출 수량을 보존하고, 그보다 작은 전체 수량은 저장하지 않습니다.
    public boolean update(Book book) throws SQLException {
        String sql = """
                UPDATE TBL_BOOK SET TITLE = ?, AUTHOR = ?, PUBLISHER = ?,
                    TOTAL_QTY = ?, AVAIL_QTY = ? - (TOTAL_QTY - AVAIL_QTY)
                WHERE BOOK_ID = ? AND USE_YN = 'Y' AND TOTAL_QTY - AVAIL_QTY <= ?
                """;
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, book.getTitle());
            statement.setString(2, book.getAuthor());
            if (book.getPublisher() == null || book.getPublisher().isBlank()) statement.setNull(3, Types.VARCHAR);
            else statement.setString(3, book.getPublisher());
            statement.setInt(4, book.getTotalQty());
            statement.setInt(5, book.getTotalQty());
            statement.setString(6, book.getBookId());
            statement.setInt(7, book.getTotalQty());
            return statement.executeUpdate() == 1;
        }
    }

    public boolean deactivate(String id) throws SQLException {
        String sql = """
                UPDATE TBL_BOOK SET USE_YN = 'N'
                WHERE BOOK_ID = ? AND USE_YN = 'Y' AND AVAIL_QTY = TOTAL_QTY
                """;
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            return statement.executeUpdate() == 1;
        }
    }

    private Book readBook(ResultSet result) throws SQLException {
        Book book = new Book();
        book.setBookId(result.getString("BOOK_ID"));
        book.setTitle(result.getString("TITLE"));
        book.setAuthor(result.getString("AUTHOR"));
        book.setPublisher(result.getString("PUBLISHER"));
        book.setTotalQty(result.getInt("TOTAL_QTY"));
        book.setAvailQty(result.getInt("AVAIL_QTY"));
        book.setUseYn(result.getString("USE_YN"));
        book.setRegDate(result.getDate("REG_DATE"));
        return book;
    }
}
