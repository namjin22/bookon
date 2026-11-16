package com.bookon.servlet;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import com.bookon.dao.LoanDAO;
import com.bookon.dao.StudentDAO;
import com.bookon.model.Student;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@WebServlet({"/student/list", "/student/form", "/student/save", "/student/delete"})
public class StudentServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private final StudentDAO studentDAO = new StudentDAO();

    // 목록과 등록·수정 화면을 엽니다. 저장·삭제 URL은 POST로만 사용할 수 있습니다.
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        switch (request.getServletPath()) {
        case "/student/list":
            showList(request, response);
            break;
        case "/student/form":
            showForm(request, response);
            break;
        default:
            response.setHeader("Allow", "POST");
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    // 저장·삭제 요청만 처리합니다. 다른 URL로 POST하면 405를 돌려줍니다.
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        switch (request.getServletPath()) {
        case "/student/save" -> save(request, response);
        case "/student/delete" -> delete(request, response);
        default -> {
            response.setHeader("Allow", "GET");
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
        }
    }

    // DB 오류는 로그에 기록하고, 화면에는 안전한 안내 문구만 전달합니다.
    // maxLoanCount는 화면에서 "미반납 n / 최대 권수"를 보여 줄 때 씁니다. 숫자는 LoanDAO 한 곳에만 둡니다.
    private void showList(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setAttribute("maxLoanCount", LoanDAO.MAX_LOAN_COUNT);
        try {
            request.setAttribute("students", studentDAO.search(value(request, "keyword")));
        } catch (SQLException e) {
            getServletContext().log("학생 목록 조회 실패", e);
            request.setAttribute("students", Collections.emptyList());
            if (request.getAttribute("error") == null) {
                request.setAttribute("error", "학생 목록을 불러오지 못했습니다. 잠시 후 다시 시도하세요.");
            }
        }
        request.getRequestDispatcher("/student/list.jsp").forward(request, response);
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        if (request.getParameter("id") != null) {
            try {
                int id = parseId(value(request, "id"));
                Student student = id > 0 ? studentDAO.findById(id) : null;
                if (student == null) {
                    request.setAttribute("error", "수정할 학생을 찾을 수 없습니다.");
                    showList(request, response);
                    return;
                }
                request.setAttribute("student", student);
            } catch (SQLException e) {
                getServletContext().log("학생 조회 실패", e);
                request.setAttribute("error", "학생 정보를 불러오지 못했습니다.");
                showList(request, response);
                return;
            }
        }
        request.getRequestDispatcher("/student/form.jsp").forward(request, response);
    }

    // 미반납 대출이 있으면 삭제하지 않고 이유를 안내합니다. 없으면 USE_YN='N'으로 바꿉니다.
    private void delete(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int id = parseId(value(request, "studentId"));
        try {
            if (id > 0 && studentDAO.deactivate(id)) {
                redirect(request, response, "deleted");
                return;
            }
            if (id <= 0 || studentDAO.findById(id) == null) {
                request.setAttribute("error", "학생을 찾을 수 없거나 이미 삭제되었습니다.");
            } else {
                request.setAttribute("error", "미반납 대출이 있는 학생은 삭제할 수 없습니다. 반납 처리 후 다시 시도하세요.");
            }
        } catch (SQLException e) {
            getServletContext().log("학생 삭제 실패", e);
            request.setAttribute("error", "학생을 삭제하지 못했습니다.");
        }
        showList(request, response);
    }

    // 입력값을 검증하고 저장합니다. 성공 후 redirect하여 새로고침 재등록을 막습니다.
    private void save(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Student student = new Student();
        String studentId = value(request, "studentId");
        boolean editing = !studentId.isEmpty();
        if (editing) student.setStudentId(parseId(studentId));
        student.setStudentNo(value(request, "studentNo"));
        student.setStudentName(value(request, "studentName"));
        student.setPhone(value(request, "phone"));
        Map<String, String> errors = new LinkedHashMap<>();
        validateStudentNo(errors, student.getStudentNo());
        validateText(errors, "studentName", student.getStudentName(), "이름", 30, true);
        validatePhone(errors, student.getPhone());
        request.setAttribute("student", student);
        request.setAttribute("errors", errors);
        if (editing && student.getStudentId() == 0) {
            request.setAttribute("error", "올바른 학생 ID가 필요합니다.");
            showList(request, response);
            return;
        }
        if (!errors.isEmpty()) {
            request.getRequestDispatcher("/student/form.jsp").forward(request, response);
            return;
        }
        try {
            if (editing && !studentDAO.update(student)) {
                request.setAttribute("error", "학생이 없거나 이미 삭제되었습니다.");
                request.getRequestDispatcher("/student/form.jsp").forward(request, response);
                return;
            }
            if (!editing) studentDAO.insert(student);
        } catch (SQLException e) {
            if (StudentDAO.isDuplicateStudentNo(e)) {
                errors.put("studentNo", "이미 있는 학번");
            } else {
                getServletContext().log("학생 저장 실패", e);
                request.setAttribute("error", "학생을 저장하지 못했습니다. 입력 내용과 DB 연결 상태를 확인하세요.");
            }
            request.getRequestDispatcher("/student/form.jsp").forward(request, response);
            return;
        }
        redirect(request, response, editing ? "updated" : "saved");
    }

    private void redirect(HttpServletRequest request, HttpServletResponse response, String message)
            throws IOException {
        response.sendRedirect(response.encodeRedirectURL(request.getContextPath() + "/student/list?msg=" + message));
    }

    private int parseId(String value) {
        return value.matches("[1-9][0-9]{0,5}") ? Integer.parseInt(value) : 0;
    }

    // 파라미터가 없으면 빈 문자열을 반환하고, 앞뒤 공백은 제거합니다.
    private String value(HttpServletRequest request, String name) {
        String input = request.getParameter(name);
        return input == null ? "" : input.trim();
    }

    // 학번은 필수이며 숫자 4~10자리만 허용합니다.
    private void validateStudentNo(Map<String, String> errors, String studentNo) {
        if (studentNo.isEmpty()) {
            errors.put("studentNo", "학번을 입력하세요.");
        } else if (!studentNo.matches("[0-9]{4,10}")) {
            errors.put("studentNo", "학번은 숫자 4~10자리로 입력하세요.");
        }
    }

    // 연락처는 선택 입력입니다. 입력했다면 13자 이하이고 010-1234-5678 형식이어야 합니다.
    private void validatePhone(Map<String, String> errors, String phone) {
        if (phone.isEmpty()) return;
        if (length(phone) > 13) {
            errors.put("phone", "연락처는 13자 이하로 입력하세요.");
        } else if (!phone.matches("01[016789]-[0-9]{3,4}-[0-9]{4}")) {
            errors.put("phone", "연락처는 010-1234-5678 형식으로 입력하세요.");
        }
    }

    // 필수 입력과 글자 수 제한을 서버에서 검사합니다.
    private void validateText(Map<String, String> errors, String field, String input,
            String label, int max, boolean required) {
        if (required && input.isEmpty()) {
            errors.put(field, label + "을(를) 입력하세요.");
        } else if (length(input) > max) {
            errors.put(field, label + "은(는) " + max + "자 이하로 입력하세요.");
        }
    }

    // 글자 수는 DB(CHAR 기준)와 같게 코드 포인트로 셉니다.
    private int length(String input) {
        return input.codePointCount(0, input.length());
    }
}
