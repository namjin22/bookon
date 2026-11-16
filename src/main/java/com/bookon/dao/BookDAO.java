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
    // 새 도서를 등록합니다. 초기 대출 가능 권수는 보유 권수와 같습니다.
    public void insert(Book book) throws SQLException {
        String sql = "INSERT INTO TBL_BOOK "
                + "(BOOK_ID, TITLE, AUTHOR, PUBLISHER, TOTAL_QTY, AVAIL_QTY, USE_YN, REG_DATE) "
                + "VALUES (SEQ_BOOK.NEXTVAL, ?, ?, ?, ?, ?, 'Y', SYSDATE)";
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, book.getTitle());
            statement.setString(2, book.getAuthor());
            if (book.getPublisher() == null || book.getPublisher().trim().isEmpty()) {
                statement.setNull(3, Types.VARCHAR);
            } else {
                statement.setString(3, book.getPublisher());
            }
            statement.setInt(4, book.getTotalQty());
            statement.setInt(5, book.getTotalQty());
            if (statement.executeUpdate() != 1) {
                throw new SQLException("도서 등록 건수가 1이 아닙니다.");
            }
        }
    }

    // 사용 중인 도서만 최신 등록순으로 가져옵니다. 같은 시각이면 ID가 큰 것이 먼저입니다.
    public List<Book> findAll() throws SQLException {
        String sql = "SELECT BOOK_ID, TITLE, AUTHOR, PUBLISHER, TOTAL_QTY, AVAIL_QTY, USE_YN, REG_DATE "
                + "FROM TBL_BOOK WHERE USE_YN = ? ORDER BY REG_DATE DESC, BOOK_ID DESC";
        List<Book> books = new ArrayList<>();
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, "Y");
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Book book = new Book();
                    book.setBookId(result.getInt("BOOK_ID"));
                    book.setTitle(result.getString("TITLE"));
                    book.setAuthor(result.getString("AUTHOR"));
                    book.setPublisher(result.getString("PUBLISHER"));
                    book.setTotalQty(result.getInt("TOTAL_QTY"));
                    book.setAvailQty(result.getInt("AVAIL_QTY"));
                    book.setUseYn(result.getString("USE_YN"));
                    book.setRegDate(result.getDate("REG_DATE"));
                    books.add(book);
                }
            }
        }
        return books;
    }
}
