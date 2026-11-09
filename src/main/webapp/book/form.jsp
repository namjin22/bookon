<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="activeMenu" value="book"/>
<%-- bookId가 있으면 수정 모드 --%>
<c:set var="editMode" value="${book.bookId > 0}"/>
<c:set var="pageName" value="${editMode ? '도서 수정' : '도서 등록'}"/>
<%-- 전체 수량: 서버가 돌려준 원래 입력(totalQtyInput)을 우선 사용 --%>
<c:choose>
<c:when test="${not empty totalQtyInput}"><c:set var="qtyValue" value="${totalQtyInput}"/></c:when>
<c:when test="${book.totalQty > 0}"><c:set var="qtyValue" value="${book.totalQty}"/></c:when>
<c:otherwise><c:set var="qtyValue" value=""/></c:otherwise>
</c:choose>
<!DOCTYPE html>
<html lang="ko">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title><c:out value="${pageName}"/> | 도서 대출 관리</title>
<link rel="stylesheet" href="${ctx}/css/style.css">
<script src="${ctx}/js/app.js" defer></script>
</head>
<body>
<%@ include file="/WEB-INF/include/header.jspf" %>
<main class="container">
<div class="page-heading"><div><h1 class="page-title"><c:out value="${pageName}"/></h1><p class="page-description"><c:out value="${editMode ? '등록된 도서 정보를 수정합니다.' : '새로운 도서 정보를 입력합니다.'}"/></p></div></div>

<c:if test="${not empty error}"><p class="message message-error"><c:out value="${error}"/></p></c:if>

<section class="panel"><div class="panel-header"><h2 class="panel-title">도서 정보</h2></div><div class="panel-content">
<form action="${ctx}/book/save" method="post" data-url="/book/save" data-disable-submit novalidate>
<c:if test="${editMode}"><input type="hidden" name="bookId" value="<c:out value="${book.bookId}"/>"></c:if>

<div class="form-group${not empty errors.title ? ' invalid' : ''}"><label for="title">도서명 <span>*</span></label><input class="form-input" id="title" name="title" maxlength="300" placeholder="도서명을 입력하세요" data-validate="required" required aria-describedby="title-error"${not empty errors.title ? ' aria-invalid="true"' : ''} value="<c:out value="${book.title}"/>"><span class="form-error" id="title-error"><c:out value="${errors.title}"/></span></div>

<div class="form-group${not empty errors.author ? ' invalid' : ''}"><label for="author">저자 <span>*</span></label><input class="form-input" id="author" name="author" maxlength="100" placeholder="저자를 입력하세요" data-validate="required" required aria-describedby="author-error"${not empty errors.author ? ' aria-invalid="true"' : ''} value="<c:out value="${book.author}"/>"><span class="form-error" id="author-error"><c:out value="${errors.author}"/></span></div>

<div class="form-group${not empty errors.publisher ? ' invalid' : ''}"><label for="publisher">출판사</label><input class="form-input" id="publisher" name="publisher" maxlength="100" placeholder="출판사를 입력하세요 (선택)" aria-describedby="publisher-error"${not empty errors.publisher ? ' aria-invalid="true"' : ''} value="<c:out value="${book.publisher}"/>"><span class="form-error" id="publisher-error"><c:out value="${errors.publisher}"/></span></div>

<div class="form-group${not empty errors.totalQty ? ' invalid' : ''}"><label for="totalQty">전체 수량 <span>*</span></label><input class="form-input" id="totalQty" name="totalQty" type="number" min="1" max="999" placeholder="1~999" data-validate="quantity" required aria-describedby="totalQty-error"${not empty errors.totalQty ? ' aria-invalid="true"' : ''} value="<c:out value="${qtyValue}"/>"><span class="form-error" id="totalQty-error"><c:out value="${errors.totalQty}"/></span></div>

<div class="form-actions"><a class="button button-secondary" href="${ctx}/book/list">취소</a><button class="button button-primary" type="submit"><c:out value="${editMode ? '수정 저장' : '도서 등록'}"/></button></div>
</form></div></section>
</main>
</body>
</html>
