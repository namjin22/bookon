package com.bookon.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.bookon.dao.BookDAO;
import com.bookon.model.Book;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet({"/book/list", "/book/form", "/book/save", "/book/delete"})
public class BookServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final BookDAO bookDAO = new BookDAO();

    // 목록과 등록 화면을 엽니다. 저장 URL은 POST로만 사용할 수 있습니다.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        switch (request.getServletPath()) {
        case "/book/list":
            showList(request, response);
            break;
        case "/book/form":
            showForm(request, response);
            break;
        default:
            response.setHeader("Allow", "POST");
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    // 저장 요청만 처리합니다. 다른 URL로 POST하면 405를 돌려줍니다.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        switch (request.getServletPath()) {
        case "/book/save" -> save(request, response);
        case "/book/delete" -> delete(request, response);
        default -> {
            response.setHeader("Allow", "GET");
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
        }
    }

    // DB 오류는 로그에 기록하고, 화면에는 안전한 안내 문구만 전달합니다.
    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            request.setAttribute("books", bookDAO.search(value(request, "keyword")));
        } catch (SQLException e) {
            getServletContext().log("도서 목록 조회 실패", e);
            request.setAttribute("books", Collections.emptyList());
            if (request.getAttribute("error") == null) {
                request.setAttribute("error", "도서 목록을 불러오지 못했습니다. 잠시 후 다시 시도하세요.");
            }
        }
        request.getRequestDispatcher("/book/list.jsp").forward(request, response);
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (request.getParameter("id") != null) {
            try {
                String id = parseId(request.getParameter("id"));
                Book book = id != null ? bookDAO.findById(id) : null;
                if (book == null) {
                    request.setAttribute("error", "수정할 도서를 찾을 수 없습니다.");
                    showList(request, response);
                    return;
                }
                request.setAttribute("book", book);
            } catch (SQLException e) {
                getServletContext().log("도서 조회 실패", e);
                request.setAttribute("error", "도서 정보를 불러오지 못했습니다.");
                showList(request, response);
                return;
            }
        }
        request.getRequestDispatcher("/book/form.jsp").forward(request, response);
    }

    private void delete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String id = parseId(request.getParameter("bookId"));
        try {
            if (id != null && bookDAO.deactivate(id)) {
                redirect(request, response, "deleted");
                return;
            }
            request.setAttribute("error", "도서가 없거나 대출 중인 도서라 삭제할 수 없습니다.");
        } catch (SQLException e) {
            getServletContext().log("도서 삭제 실패", e);
            request.setAttribute("error", "도서를 삭제하지 못했습니다.");
        }
        showList(request, response);
    }

    // 입력값을 검증하고 저장합니다. 성공 후 redirect하여 새로고침 재등록을 막습니다.
    private void save(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Book book = new Book();
        String bookId = request.getParameter("bookId");
        boolean editing = bookId != null;
        if (editing) book.setBookId(parseId(bookId));
        book.setTitle(value(request, "title"));
        book.setAuthor(value(request, "author"));
        book.setPublisher(value(request, "publisher"));
        String quantity = value(request, "totalQty");
        Map<String, String> errors = new LinkedHashMap<>();
        String kdc = request.getParameter("kdc");
        request.setAttribute("kdc", kdc);
        if (!editing && (kdc == null || !kdc.matches("[0-9]{3}"))) {
            errors.put("kdc", "분류번호는 숫자 3자리로 입력하세요.");
        }
        validateText(errors, "title", book.getTitle(), "제목", 300, true);
        validateText(errors, "author", book.getAuthor(), "저자", 100, true);
        validateText(errors, "publisher", book.getPublisher(), "출판사", 100, false);
        if (!quantity.matches("[0-9]{1,3}")) {
            errors.put("totalQty", "보유 권수는 1~999 정수로 입력하세요.");
        } else {
            int totalQty = Integer.parseInt(quantity);
            book.setTotalQty(totalQty);
            if (totalQty < 1) {
                errors.put("totalQty", "보유 권수는 1~999 정수로 입력하세요.");
            }
        }
        // 숫자 필드에는 'abc' 등을 담을 수 없으므로 원문을 별도 속성으로 보존합니다.
        request.setAttribute("totalQtyInput", request.getParameter("totalQty"));
        request.setAttribute("book", book);
        request.setAttribute("errors", errors);
        if (editing && book.getBookId() == null) {
            request.setAttribute("error", "올바른 도서 ID가 필요합니다.");
            showList(request, response);
            return;
        }
        if (!errors.isEmpty()) {
            request.getRequestDispatcher("/book/form.jsp").forward(request, response);
            return;
        }
        try {
            if (editing && !bookDAO.update(book)) {
                request.setAttribute("error", "도서가 없거나 전체 수량이 현재 대출 수량보다 적습니다.");
                request.getRequestDispatcher("/book/form.jsp").forward(request, response);
                return;
            }
            if (!editing) book.setBookId(bookDAO.insert(book, kdc));
        } catch (BookDAO.CodeExhaustedException e) {
            errors.put("kdc", "이 분류의 도서 일련번호가 모두 사용되었습니다.");
            request.getRequestDispatcher("/book/form.jsp").forward(request, response);
            return;
        } catch (SQLException e) {
            getServletContext().log("도서 저장 실패", e);
            request.setAttribute("error", "도서를 저장하지 못했습니다. 입력 내용과 DB 연결 상태를 확인하세요.");
            request.getRequestDispatcher("/book/form.jsp").forward(request, response);
            return;
        }
        redirect(request, response, editing ? "updated" : "saved");
    }

    private void redirect(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + "/book/list?msg=" + message));
    }

    private String parseId(String value) {
        return value != null && value.matches("[0-9]{3}-[0-9]{3}") ? value : null;
    }

    // 파라미터가 없으면 빈 문자열을 반환하고, 앞뒤 공백은 제거합니다.
    private String value(HttpServletRequest request, String name) {
        String input = request.getParameter(name);
        return input == null ? "" : input.trim();
    }

    // 필수 입력과 글자 수 제한을 서버에서 검사합니다.
    private void validateText(Map<String, String> errors, String field, String input,
            String label, int max, boolean required) {
        if (required && input.isEmpty()) {
            errors.put(field, label + "을(를) 입력하세요.");
        } else if (input.length() > max) {
            errors.put(field, label + "은(는) " + max + "자 이하로 입력하세요.");
        }
    }
}
