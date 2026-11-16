<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="activeMenu" value="student"/>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>학생 목록 | 도서 대출 관리</title>
<link rel="stylesheet" href="${ctx}/css/style.css">
<script src="${ctx}/js/app.js" defer></script>
</head>
<body>
<%@ include file="/WEB-INF/include/header.jspf" %>
<main class="container">
<div class="page-heading"><div><h1 class="page-title">학생 목록</h1><p class="page-description">등록된 학생 정보를 확인하고 검색하거나 관리합니다.</p></div></div>

<c:choose>
<c:when test="${param.msg == 'saved'}"><p class="message message-success">학생이 등록되었습니다.</p></c:when>
<c:when test="${param.msg == 'updated'}"><p class="message message-success">학생 정보가 수정되었습니다.</p></c:when>
<c:when test="${param.msg == 'deleted'}"><p class="message message-success">학생이 삭제되었습니다.</p></c:when>
</c:choose>
<c:if test="${not empty error}"><p class="message message-error"><c:out value="${error}"/></p></c:if>

<form class="search-form" action="${ctx}/student/list" method="get" data-url="/student/list">
<label class="sr-only" for="keyword">학번 또는 학생 이름 검색</label>
<input class="search-input" id="keyword" name="keyword" type="search" placeholder="학번 또는 학생 이름 검색" value="<c:out value="${param.keyword}"/>">
<button class="button button-primary" type="submit">검색</button>
<a class="button button-primary" href="${ctx}/student/form">학생 등록</a>
</form>

<section class="panel"><div class="panel-header"><h2 class="panel-title">학생 목록</h2></div><div class="table-wrap"><table class="data-table">
<thead><tr><th>학번</th><th>이름</th><th class="col-optional">연락처</th><th>상태</th><th>관리</th></tr></thead>
<tbody>
<c:forEach items="${students}" var="student">
<tr>
<td><c:out value="${student.studentNo}"/></td>
<td><c:out value="${student.studentName}"/></td>
<td class="col-optional"><c:out value="${empty student.phone ? '-' : student.phone}"/></td>
<%-- 상태 우선순위: 이용 안 함 > 대출 정지 > 연체 중 > 이용 가능 (contract §8-2) --%>
<td><c:choose>
<c:when test="${student.useYn == 'N'}"><span class="badge badge-secondary">이용 안 함</span></c:when>
<c:when test="${student.penalized}"><span class="badge badge-danger" title="<c:out value="${student.penaltyEndDate}"/>부터 대출 가능">대출 정지</span></c:when>
<c:when test="${student.overdueCount > 0}"><span class="badge badge-warning">연체 중</span></c:when>
<c:otherwise><span class="badge badge-success">이용 가능</span></c:otherwise>
</c:choose><span class="loan-count">대출 중 <c:out value="${student.loanCount}"/>/<c:out value="${maxLoanCount}"/></span></td>
<td><a class="button button-small button-secondary" href="${ctx}/student/form?id=<c:out value="${student.studentId}"/>">수정</a>
<form style="display:inline" action="${ctx}/student/delete" method="post" data-url="/student/delete"><input type="hidden" name="studentId" value="<c:out value="${student.studentId}"/>"><button class="button button-small button-danger" type="submit" data-confirm="이 학생을 삭제할까요?">삭제</button></form></td>
</tr>
</c:forEach>
<c:if test="${empty students}">
<tr><td colspan="5" class="empty-state"><c:choose><c:when test="${not empty param.keyword}">검색 결과가 없습니다.</c:when><c:otherwise>등록된 학생이 없습니다.</c:otherwise></c:choose></td></tr>
</c:if>
</tbody></table></div></section>
</main>
</body>
</html>
