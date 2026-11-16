<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<c:set var="ctx" value="${pageContext.request.contextPath}"/>
<c:set var="activeMenu" value="student"/>
<%-- studentId가 있으면 수정 모드 --%>
<c:set var="editMode" value="${student.studentId > 0}"/>
<c:set var="pageName" value="${editMode ? '학생 수정' : '학생 등록'}"/>
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
<div class="page-heading"><div><h1 class="page-title"><c:out value="${pageName}"/></h1><p class="page-description"><c:out value="${editMode ? '등록된 학생 정보를 수정합니다.' : '도서 대출을 이용할 학생 정보를 입력합니다.'}"/></p></div></div>

<c:if test="${not empty error}"><p class="message message-error"><c:out value="${error}"/></p></c:if>

<section class="panel"><div class="panel-header"><h2 class="panel-title">학생 정보</h2></div><div class="panel-content">
<form action="${ctx}/student/save" method="post" data-url="/student/save" data-disable-submit novalidate>
<c:if test="${editMode}"><input type="hidden" name="studentId" value="<c:out value="${student.studentId}"/>"></c:if>

<div class="form-group${not empty errors.studentNo ? ' invalid' : ''}"><label for="studentNo">학번 <span>*</span></label><input class="form-input" id="studentNo" name="studentNo" maxlength="10" placeholder="숫자 4~10자리" data-validate="studentNo" required aria-describedby="studentNo-error"${not empty errors.studentNo ? ' aria-invalid="true"' : ''} value="<c:out value="${student.studentNo}"/>"><span class="form-error" id="studentNo-error"><c:out value="${errors.studentNo}"/></span></div>

<div class="form-group${not empty errors.studentName ? ' invalid' : ''}"><label for="studentName">이름 <span>*</span></label><input class="form-input" id="studentName" name="studentName" maxlength="30" placeholder="학생 이름" data-validate="required" required aria-describedby="studentName-error"${not empty errors.studentName ? ' aria-invalid="true"' : ''} value="<c:out value="${student.studentName}"/>"><span class="form-error" id="studentName-error"><c:out value="${errors.studentName}"/></span></div>

<div class="form-group${not empty errors.phone ? ' invalid' : ''}"><label for="phone">연락처</label><input class="form-input" id="phone" name="phone" type="tel" maxlength="13" placeholder="010-1234-5678 (선택)" data-validate="phone" aria-describedby="phone-error"${not empty errors.phone ? ' aria-invalid="true"' : ''} value="<c:out value="${student.phone}"/>"><span class="form-error" id="phone-error"><c:out value="${errors.phone}"/></span></div>

<div class="form-actions"><a class="button button-secondary" href="${ctx}/student/list">취소</a><button class="button button-primary" type="submit"><c:out value="${editMode ? '수정 저장' : '학생 등록'}"/></button></div>
</form></div></section>
</main>
</body>
</html>
