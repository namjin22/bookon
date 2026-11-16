package com.bookon.servlet;

import com.bookon.dao.LoanDAO;
import com.bookon.util.DBUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet("/main")
public class MainServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private static final String COUNTS = """
            SELECT (SELECT NVL(SUM(TOTAL_QTY), 0) FROM TBL_BOOK WHERE USE_YN = 'Y') AS TOTAL_BOOKS,
                   (SELECT NVL(SUM(AVAIL_QTY), 0) FROM TBL_BOOK WHERE USE_YN = 'Y') AS AVAILABLE_BOOKS,
                   (SELECT COUNT(*) FROM TBL_LOAN WHERE RETURN_YN = 'N') AS LOANED_BOOKS,
                   (SELECT COUNT(*) FROM TBL_LOAN
                    WHERE RETURN_YN = 'N' AND DUE_DATE < TRUNC(SYSDATE)) AS OVERDUE_BOOKS
            FROM DUAL
            """;
    private final LoanDAO loanDAO = new LoanDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement(COUNTS);
             ResultSet rows = statement.executeQuery()) {
            if (rows.next()) {
                request.setAttribute("totalBookCount", rows.getLong("TOTAL_BOOKS"));
                request.setAttribute("availableBookCount", rows.getLong("AVAILABLE_BOOKS"));
                request.setAttribute("loanedCount", rows.getLong("LOANED_BOOKS"));
                request.setAttribute("overdueCount", rows.getLong("OVERDUE_BOOKS"));
            }
            request.setAttribute("recentLoans", loanDAO.findRecentLoans());
        } catch (SQLException e) {
            getServletContext().log("대시보드 조회 실패", e);
            request.setAttribute("error", "도서 대출 요약을 불러오지 못했습니다.");
        }
        request.getRequestDispatcher("/main.jsp").forward(request, response);
    }
}
