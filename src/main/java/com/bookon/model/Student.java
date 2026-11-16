package com.bookon.model;

import java.sql.Date;

public class Student {
    private int studentId;
    private String studentNo;
    private String studentName;
    private String phone;
    private String useYn;
    private Date regDate;

    // studentId 값을 읽습니다.
    public int getStudentId() {
        return studentId;
    }

    // studentId 값을 저장합니다.
    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    // studentNo 값을 읽습니다.
    public String getStudentNo() {
        return studentNo;
    }

    // studentNo 값을 저장합니다.
    public void setStudentNo(String studentNo) {
        this.studentNo = studentNo;
    }

    // studentName 값을 읽습니다.
    public String getStudentName() {
        return studentName;
    }

    // studentName 값을 저장합니다.
    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    // phone 값을 읽습니다.
    public String getPhone() {
        return phone;
    }

    // phone 값을 저장합니다.
    public void setPhone(String phone) {
        this.phone = phone;
    }

    // useYn 값을 읽습니다.
    public String getUseYn() {
        return useYn;
    }

    // useYn 값을 저장합니다.
    public void setUseYn(String useYn) {
        this.useYn = useYn;
    }

    // regDate 값을 읽습니다.
    public Date getRegDate() {
        return regDate;
    }

    // regDate 값을 저장합니다.
    public void setRegDate(Date regDate) {
        this.regDate = regDate;
    }
}
