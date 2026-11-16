<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="activeMenu" value="home"/>
<%-- 요청 언어와 관계없이 1,248처럼 쉼표를 찍기 위해 로케일 고정 --%>
<fmt:setLocale value="ko_KR"/>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>대시보드 | 도서 대출 관리</title>
<link rel="stylesheet" href="${ctx}/css/style.css">
<script src="${ctx}/js/app.js" defer></script>
</head>
<body>
<%@ include file="/WEB-INF/include/header.jspf" %>
<main class="container">
<div class="page-heading"><div><h1 class="page-title">대시보드</h1><p class="page-description">도서와 학생, 대출 현황을 한눈에 확인합니다.</p></div></div>

<c:if test="${not empty error}"><p class="message message-error"><c:out value="${error}"/></p></c:if>

<div class="card-grid">
<div class="card"><div class="stat-label">전체 도서</div><div class="stat-value"><fmt:formatNumber value="${empty totalBookCount ? 0 : totalBookCount}"/><span class="stat-unit">권</span></div></div>
<div class="card"><div class="stat-label">대출 가능</div><div class="stat-value"><fmt:formatNumber value="${empty availableBookCount ? 0 : availableBookCount}"/><span class="stat-unit">권</span></div></div>
<div class="card"><div class="stat-label">대출 중</div><div class="stat-value"><fmt:formatNumber value="${empty loanedCount ? 0 : loanedCount}"/><span class="stat-unit">권</span></div></div>
<div class="card"><div class="stat-label">연체 도서</div><div class="stat-value"><fmt:formatNumber value="${empty overdueCount ? 0 : overdueCount}"/><span class="stat-unit">권</span></div></div>
</div>

<section class="panel"><div class="panel-header"><h2 class="panel-title">최근 대출 내역</h2></div><div class="table-wrap"><table class="data-table">
<thead><tr><th>도서명</th><th>학생 이름</th><th>대출일</th><th>상태</th></tr></thead>
<tbody>
<c:forEach items="${recentLoans}" var="loan">
<tr>
<td><c:out value="${loan.bookTitle}"/></td>
<td><c:out value="${loan.studentName}"/></td>
<td><c:out value="${loan.loanDate}"/></td>
<td><c:choose>
<c:when test="${loan.returnYn == 'Y'}"><span class="badge badge-secondary">반납 완료</span></c:when>
<c:when test="${loan.overdue}"><span class="badge badge-danger">연체</span></c:when>
<c:otherwise><span class="badge badge-warning">대출 중</span></c:otherwise>
</c:choose></td>
</tr>
</c:forEach>
<c:if test="${empty recentLoans}">
<tr><td colspan="4" class="empty-state">최근 대출 내역이 없습니다.</td></tr>
</c:if>
</tbody></table></div></section>

<section class="panel"><div class="panel-header"><h2 class="panel-title">바로가기</h2></div><div class="panel-content quick-links"><a class="button button-primary" href="${ctx}/book/list">도서 관리</a><a class="button button-primary" href="${ctx}/student/list">학생 관리</a><a class="button button-primary" href="${ctx}/loan/list">대출 관리</a></div></section>
</main>
</body>
</html>
