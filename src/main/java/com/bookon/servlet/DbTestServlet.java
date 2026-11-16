package com.bookon.servlet;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.bookon.util.DBUtil;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// 개발 확인용입니다. 최종 제출 전 이 클래스와 배포된 클래스 파일을 제거하세요.
@WebServlet("/dbtest")
public class DbTestServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // 연결과 간단한 조회를 확인하고 성공 또는 실패를 한 줄로 출력합니다.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/plain;charset=UTF-8");
        response.setHeader("Cache-Control", "no-store");
        try (Connection connection = DBUtil.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM DUAL");
             ResultSet result = statement.executeQuery()) {
            if (!result.next() || result.getInt(1) != 1) {
                throw new SQLException("DB 확인 쿼리 결과가 예상과 다릅니다.");
            }
            response.getWriter().println("DB 연결 성공");
        } catch (SQLException e) {
            getServletContext().log("DB 연결 확인 실패", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().println("DB 연결 실패: 서버 로그를 확인하세요.");
        }
    }
}
