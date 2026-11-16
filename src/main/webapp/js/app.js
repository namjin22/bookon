document.addEventListener("DOMContentLoaded", function () {
    setupDueDateMin();
    setupConfirmButtons();
    setupFormValidation();
});
function getToday() {
    const today = new Date(Date.now() - new Date().getTimezoneOffset()*60000).toISOString().slice(0,10);
    return today;
}
function setupDueDateMin() {
    document.querySelectorAll('input[name="dueDate"]').forEach(function (field) { field.min = getToday(); });
}
function setupConfirmButtons() {
    document.querySelectorAll("[data-confirm]").forEach(function (element) {
        element.addEventListener("click", function (event) {
            if (!confirm(element.dataset.confirm)) event.preventDefault();
        });
    });
}
function setupFormValidation() {
    document.querySelectorAll("form[data-disable-submit]").forEach(function (form) {
        form.addEventListener("submit", function (event) {
            if (!validateForm(form)) { event.preventDefault(); return; }
            form.querySelectorAll('button[type="submit"],input[type="submit"]').forEach(function (button) { button.disabled = true; });
        });
        form.querySelectorAll("[data-validate]").forEach(function (field) {
            field.addEventListener("input", function () { validateField(field); });
            field.addEventListener("change", function () { validateField(field); });
        });
    });
}
function validateForm(form) {
    let valid = true, firstInvalid = null;
    form.querySelectorAll("[data-validate]").forEach(function (field) {
        if (!validateField(field)) { valid = false; if (!firstInvalid) firstInvalid = field; }
    });
    if (firstInvalid) firstInvalid.focus();
    return valid;
}
function validateField(field) {
    const type = field.dataset.validate, value = field.value.trim();
    let message = "";
    if (type === "required" && value === "") message = "필수 입력 항목입니다.";
    else if (type === "quantity") {
        const n = Number(value);
        if (value === "" || !Number.isInteger(n) || n < 1 || n > 999) message = "수량은 1~999 사이의 정수로 입력해 주세요.";
    } else if (type === "kdc" && !/^\d{3}$/.test(value)) message = "분류번호는 숫자 3자리로 입력해 주세요.";
    else if (type === "studentNo" && !/^\d{4,10}$/.test(value)) message = "학번은 숫자 4~10자리로 입력해 주세요.";
    else if (type === "phone" && value !== "") {
        if (!/^(01[016789]-\d{3,4}-\d{4}|0\d{1,2}-\d{3,4}-\d{4})$/.test(value)) message = "연락처를 010-1234-5678 형식으로 입력해 주세요.";
    }
    if (!message && field.name === "dueDate" && value !== "" && value < getToday()) message = "반납 예정일은 오늘 이후여야 합니다.";
    const group = field.closest(".form-group"), error = document.getElementById(field.id + "-error");
    if (message) { field.setAttribute("aria-invalid","true"); if(group)group.classList.add("invalid"); if(error)error.textContent=message; }
    else { field.removeAttribute("aria-invalid"); if(group)group.classList.remove("invalid"); if(error)error.textContent=""; }
    return message === "";
}
