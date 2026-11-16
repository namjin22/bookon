<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="activeMenu" value="book"/>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>도서 목록 | 도서 대출 관리</title>
<link rel="stylesheet" href="${ctx}/css/style.css">
<script src="${ctx}/js/app.js" defer></script>
</head>
<body>
<%@ include file="/WEB-INF/include/header.jspf" %>
<main class="container">
<div class="page-heading"><div><h1 class="page-title">도서 목록</h1><p class="page-description">등록된 도서를 확인하고 검색하거나 관리합니다.</p></div></div>

<c:choose>
<c:when test="${param.msg == 'saved'}"><p class="message message-success">도서가 등록되었습니다.</p></c:when>
<c:when test="${param.msg == 'updated'}"><p class="message message-success">도서 정보가 수정되었습니다.</p></c:when>
<c:when test="${param.msg == 'deleted'}"><p class="message message-success">도서가 삭제되었습니다.</p></c:when>
</c:choose>
<c:if test="${not empty error}"><p class="message message-error"><c:out value="${error}"/></p></c:if>

<form class="search-form" action="${ctx}/book/list" method="get" data-url="/book/list">
<label class="sr-only" for="keyword">도서명 검색</label>
<input class="search-input" id="keyword" name="keyword" type="search" placeholder="도서명 검색" value="<c:out value="${param.keyword}"/>">
<button class="button button-primary" type="submit">검색</button>
<a class="button button-primary" href="${ctx}/book/form">도서 등록</a>
</form>

<section class="panel"><div class="panel-header"><h2 class="panel-title">도서 목록</h2></div><div class="table-wrap"><table class="data-table">
<thead><tr><th>도서 코드</th><th>도서명</th><th class="col-optional">저자</th><th class="col-optional">출판사</th><th>대출 가능 / 전체</th><th>상태</th><th>관리</th></tr></thead>
<tbody>
<c:forEach items="${books}" var="book">
<tr>
<td class="code-cell"><c:out value="${book.bookId}"/></td>
<td><c:out value="${book.title}"/></td>
<td class="col-optional"><c:out value="${book.author}"/></td>
<td class="col-optional"><c:out value="${empty book.publisher ? '-' : book.publisher}"/></td>
<td><c:out value="${book.availQty}"/> / <c:out value="${book.totalQty}"/></td>
<td><c:choose><c:when test="${book.availQty > 0}"><span class="badge badge-success">대출 가능</span></c:when><c:otherwise><span class="badge badge-warning">대출 중</span></c:otherwise></c:choose></td>
<td><a class="button button-small button-secondary" href="${ctx}/book/form?id=<c:out value="${book.bookId}"/>">수정</a>
<form style="display:inline" action="${ctx}/book/delete" method="post" data-url="/book/delete"><input type="hidden" name="bookId" value="<c:out value="${book.bookId}"/>"><button class="button button-small button-danger" type="submit" data-confirm="이 도서를 삭제할까요?">삭제</button></form></td>
</tr>
</c:forEach>
<c:if test="${empty books}">
<tr><td colspan="7" class="empty-state"><c:choose><c:when test="${not empty param.keyword}">검색 결과가 없습니다.</c:when><c:otherwise>등록된 도서가 없습니다.</c:otherwise></c:choose></td></tr>
</c:if>
</tbody></table></div></section>
</main>
</body>
</html>
