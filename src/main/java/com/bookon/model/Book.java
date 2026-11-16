package com.bookon.model;

import java.sql.Date;

public class Book {
    private int bookId;
    private String title;
    private String author;
    private String publisher;
    private int totalQty;
    private int availQty;
    private String useYn;
    private Date regDate;

    // bookId 값을 읽습니다.
    public int getBookId() {
        return bookId;
    }

    // bookId 값을 저장합니다.
    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    // title 값을 읽습니다.
    public String getTitle() {
        return title;
    }

    // title 값을 저장합니다.
    public void setTitle(String title) {
        this.title = title;
    }

    // author 값을 읽습니다.
    public String getAuthor() {
        return author;
    }

    // author 값을 저장합니다.
    public void setAuthor(String author) {
        this.author = author;
    }

    // publisher 값을 읽습니다.
    public String getPublisher() {
        return publisher;
    }

    // publisher 값을 저장합니다.
    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    // totalQty 값을 읽습니다.
    public int getTotalQty() {
        return totalQty;
    }

    // totalQty 값을 저장합니다.
    public void setTotalQty(int totalQty) {
        this.totalQty = totalQty;
    }

    // availQty 값을 읽습니다.
    public int getAvailQty() {
        return availQty;
    }

    // availQty 값을 저장합니다.
    public void setAvailQty(int availQty) {
        this.availQty = availQty;
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
