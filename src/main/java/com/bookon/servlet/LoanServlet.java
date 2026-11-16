package com.bookon.servlet;

import com.bookon.dao.LoanDAO;
import com.bookon.dao.LoanDAO.Status;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@WebServlet({"/loan/list", "/loan/borrow", "/loan/return"})
public class LoanServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final LoanDAO loanDAO = new LoanDAO();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (!"/loan/list".equals(request.getServletPath())) {
            response.setHeader("Allow", "POST");
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        showList(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        switch (request.getServletPath()) {
            case "/loan/borrow" -> borrow(request, response);
            case "/loan/return" -> returnLoan(request, response);
            default -> {
                response.setHeader("Allow", "GET");
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        }
    }

    private void borrow(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int studentId = parseId(request.getParameter("studentId"), 6);
        int bookId = parseId(request.getParameter("bookId"), 6);
        String rawDate = request.getParameter("dueDate");
        if (studentId == 0 || bookId == 0 || rawDate == null || !rawDate.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
            showError(request, response, "학생, 도서, 반납 예정일을 확인하세요.");
            return;
        }
        LocalDate dueDate;
        try {
            dueDate = LocalDate.parse(rawDate);
        } catch (DateTimeParseException e) {
            showError(request, response, "올바른 반납 예정일을 입력하세요.");
            return;
        }
        if (dueDate.isBefore(LocalDate.now())) {
            showError(request, response, "반납 예정일은 오늘 이후여야 합니다.");
            return;
        }
        try {
            if (loanDAO.borrow(studentId, bookId, Date.valueOf(dueDate))) {
                redirect(request, response, "borrowed");
                return;
            }
        } catch (SQLException e) {
            getServletContext().log("대출 처리 실패", e);
        }
        showError(request, response, "대출할 수 없습니다. 학생 상태와 도서 재고를 확인하세요.");
    }

    private void returnLoan(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int loanId = parseId(request.getParameter("loanId"), 8);
        if (loanId == 0) {
            showError(request, response, "올바른 대출 번호를 입력하세요.");
            return;
        }
        try {
            switch (loanDAO.returnLoan(loanId)) {
                case RETURNED -> {
                    redirect(request, response, "returned");
                    return;
                }
                case ALREADY_RETURNED -> {
                    redirect(request, response, "alreadyReturned");
                    return;
                }
                case FAILED -> getServletContext().log("반납 재고 복구 실패: " + loanId);
            }
        } catch (SQLException e) {
            getServletContext().log("반납 처리 실패", e);
        }
        showError(request, response, "반납을 처리하지 못했습니다.");
    }

    private int parseId(String value, int maxDigits) {
        if (value == null || value.length() > maxDigits || !value.matches("[1-9][0-9]*")) return 0;
        return Integer.parseInt(value);
    }

    private void redirect(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + "/loan/list?msg=" + message));
    }

    private void showError(HttpServletRequest request, HttpServletResponse response, String message)
            throws ServletException, IOException {
        request.setAttribute("error", message);
        showList(request, response);
    }

    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String keyword = request.getParameter("keyword");
        String status = request.getParameter("status");
        Status filter = Status.ALL;
        if (status != null) {
            try {
                filter = Status.valueOf(status);
            } catch (IllegalArgumentException e) {
                if (request.getAttribute("error") == null) {
                    request.setAttribute("error", "올바른 대출 상태를 선택하세요.");
                }
            }
        }
        try {
            request.setAttribute("loans", loanDAO.findLoans(keyword == null ? "" : keyword.strip(), filter));
            request.setAttribute("activeStudents", loanDAO.findActiveStudents());
            request.setAttribute("availableBooks", loanDAO.findAvailableBooks());
        } catch (SQLException e) {
            getServletContext().log("대출 목록 조회 실패", e);
            request.setAttribute("loans", List.of());
            request.setAttribute("activeStudents", List.of());
            request.setAttribute("availableBooks", List.of());
            if (request.getAttribute("error") == null) {
                request.setAttribute("error", "대출 정보를 불러오지 못했습니다.");
            }
        }
        request.getRequestDispatcher("/loan/list.jsp").forward(request, response);
    }
}
