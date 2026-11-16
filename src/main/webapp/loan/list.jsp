<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="activeMenu" value="loan"/>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>대출 목록 | 도서 대출 관리</title>
<link rel="stylesheet" href="${ctx}/css/style.css">
<script src="${ctx}/js/app.js" defer></script>
</head>
<body>
<%@ include file="/WEB-INF/include/header.jspf" %>
<main class="container">
<div class="page-heading"><div><h1 class="page-title">대출 목록</h1><p class="page-description">대출을 등록하고 기존 대출 내역과 반납 상태를 확인합니다.</p></div></div>

<c:choose>
<c:when test="${param.msg == 'borrowed'}"><p class="message message-success">대출이 등록되었습니다.</p></c:when>
<c:when test="${param.msg == 'returned'}"><p class="message message-success">반납 처리되었습니다.</p></c:when>
<c:when test="${param.msg == 'alreadyReturned'}"><p class="message message-info">이미 반납 처리된 대출입니다.</p></c:when>
</c:choose>
<c:if test="${not empty error}"><p class="message message-error"><c:out value="${error}"/></p></c:if>

<form class="search-form" action="${ctx}/loan/list" method="get" data-url="/loan/list">
<label class="sr-only" for="keyword">도서명 또는 학생 이름 검색</label>
<input class="search-input" id="keyword" name="keyword" type="search" placeholder="도서명 또는 학생 이름 검색" value="<c:out value="${param.keyword}"/>">
<label class="sr-only" for="status">대출 상태</label>
<select class="form-select" id="status" name="status">
<option value="ALL">전체 상태</option>
<option value="UNRETURNED"${param.status == 'UNRETURNED' ? ' selected' : ''}>대출 중</option>
<option value="OVERDUE"${param.status == 'OVERDUE' ? ' selected' : ''}>연체</option>
<option value="RETURNED"${param.status == 'RETURNED' ? ' selected' : ''}>반납 완료</option>
</select>
<button class="button button-primary" type="submit">검색</button>
</form>

<section class="panel"><div class="panel-header"><h2 class="panel-title">도서 대출 등록</h2></div><div class="panel-content">
<form action="${ctx}/loan/borrow" method="post" data-url="/loan/borrow" data-disable-submit novalidate>
<div class="form-group${not empty errors.studentId ? ' invalid' : ''}"><label for="studentId">학생 <span>*</span></label><select class="form-select" id="studentId" name="studentId" data-validate="required" required aria-describedby="studentId-error">
<option value="">학생을 선택하세요</option>
<c:forEach items="${activeStudents}" var="s"><option value="<c:out value="${s.studentId}"/>"${param.studentId == s.studentId ? ' selected' : ''}><c:out value="${s.studentNo}"/> <c:out value="${s.studentName}"/></option></c:forEach>
</select><span class="form-error" id="studentId-error"><c:out value="${errors.studentId}"/></span></div>

<div class="form-group${not empty errors.bookId ? ' invalid' : ''}"><label for="bookId">도서 <span>*</span></label><select class="form-select" id="bookId" name="bookId" data-validate="required" required aria-describedby="bookId-error">
<option value="">도서를 선택하세요</option>
<c:forEach items="${availableBooks}" var="b"><option value="<c:out value="${b.bookId}"/>"${param.bookId == b.bookId ? ' selected' : ''}><c:out value="${b.title}"/> (대출 가능 <c:out value="${b.availQty}"/>권)</option></c:forEach>
</select><span class="form-error" id="bookId-error"><c:out value="${errors.bookId}"/></span></div>

<div class="form-group${not empty errors.dueDate ? ' invalid' : ''}"><label for="dueDate">반납 예정일 <span>*</span></label><input class="form-input" type="date" id="dueDate" name="dueDate" data-validate="required" required aria-describedby="dueDate-error" value="<c:out value="${param.dueDate}"/>"><span class="form-error" id="dueDate-error"><c:out value="${errors.dueDate}"/></span></div>

<div class="form-actions"><button class="button button-primary" type="submit">대출 등록</button></div>
</form></div></section>

<section class="panel"><div class="panel-header"><h2 class="panel-title">대출 내역</h2></div><div class="table-wrap"><table class="data-table">
<thead><tr><th>도서명</th><th>학생 이름</th><th class="col-optional">대출일</th><th>반납 예정일</th><th>상태</th><th>관리</th></tr></thead>
<tbody>
<c:forEach items="${loans}" var="loan">
<tr>
<td><c:out value="${loan.bookTitle}"/></td>
<td><c:out value="${loan.studentName}"/></td>
<td class="col-optional"><c:out value="${loan.loanDate}"/></td>
<td><c:out value="${loan.dueDate}"/></td>
<td><c:choose>
<c:when test="${loan.returnYn == 'Y'}"><span class="badge badge-secondary">반납 완료</span></c:when>
<c:when test="${loan.overdue}"><span class="badge badge-danger">연체</span></c:when>
<c:otherwise><span class="badge badge-warning">대출 중</span></c:otherwise>
</c:choose></td>
<td><c:choose>
<c:when test="${loan.returnYn == 'Y'}"><span class="text-muted">처리 완료</span></c:when>
<c:otherwise><form action="${ctx}/loan/return" method="post" data-url="/loan/return"><input type="hidden" name="loanId" value="<c:out value="${loan.loanId}"/>"><button class="button button-small button-primary" type="submit" data-confirm="이 도서를 반납 처리할까요?">반납 처리</button></form></c:otherwise>
</c:choose></td>
</tr>
</c:forEach>
<c:if test="${empty loans}">
<tr><td colspan="6" class="empty-state"><c:choose><c:when test="${not empty param.keyword or (not empty param.status and param.status != 'ALL')}">검색 결과가 없습니다.</c:when><c:otherwise>대출 내역이 없습니다.</c:otherwise></c:choose></td></tr>
</c:if>
</tbody></table></div></section>
</main>
</body>
</html>
