package com.grocery.model;

public class Customer {
    private int id;
    private String name;
    private String mobile;
    private String address;
    private String createdAt;

    public Customer() {}

    public Customer(int id, String name, String mobile, String address, String createdAt) {
        this.id = id;
        this.name = name;
        this.mobile = mobile;
        this.address = address;
        this.createdAt = createdAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMobile() { return mobile; }
    public void setMobile(String mobile) { this.mobile = mobile; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() { return name + (mobile != null && !mobile.isEmpty() ? " (" + mobile + ")" : ""); }
}
