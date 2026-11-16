package com.bookon.model;

import java.sql.Date;

public class Loan {
    private int loanId;
    private int studentId;
    private int bookId;
    private Date loanDate;
    private Date dueDate;
    private String returnYn;
    private Date returnDate;
    private String studentName;
    private String bookTitle;
    private String studentNo;
    private boolean overdue;

    // loanId 값을 읽습니다.
    public int getLoanId() {
        return loanId;
    }

    // loanId 값을 저장합니다.
    public void setLoanId(int loanId) {
        this.loanId = loanId;
    }

    // studentId 값을 읽습니다.
    public int getStudentId() {
        return studentId;
    }

    // studentId 값을 저장합니다.
    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    // bookId 값을 읽습니다.
    public int getBookId() {
        return bookId;
    }

    // bookId 값을 저장합니다.
    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    // loanDate 값을 읽습니다.
    public Date getLoanDate() {
        return loanDate;
    }

    // loanDate 값을 저장합니다.
    public void setLoanDate(Date loanDate) {
        this.loanDate = loanDate;
    }

    // dueDate 값을 읽습니다.
    public Date getDueDate() {
        return dueDate;
    }

    // dueDate 값을 저장합니다.
    public void setDueDate(Date dueDate) {
        this.dueDate = dueDate;
    }

    // returnYn 값을 읽습니다.
    public String getReturnYn() {
        return returnYn;
    }

    // returnYn 값을 저장합니다.
    public void setReturnYn(String returnYn) {
        this.returnYn = returnYn;
    }

    // returnDate 값을 읽습니다.
    public Date getReturnDate() {
        return returnDate;
    }

    // returnDate 값을 저장합니다.
    public void setReturnDate(Date returnDate) {
        this.returnDate = returnDate;
    }

    // studentName 값을 읽습니다.
    public String getStudentName() {
        return studentName;
    }

    // studentName 값을 저장합니다.
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    // bookTitle 값을 읽습니다.
    public String getBookTitle() {
        return bookTitle;
    }

    // bookTitle 값을 저장합니다.
    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    // studentNo 값을 읽습니다.
    public String getStudentNo() {
        return studentNo;
    }

    // studentNo 값을 저장합니다.
    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    // overdue 값을 읽습니다.
    public boolean isOverdue() {
        return overdue;
    }

    // overdue 값을 저장합니다.
    public void setOverdue(boolean overdue) {
        this.overdue = overdue;
    }
}
