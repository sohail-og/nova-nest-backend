package com.novanest.dto;

public class AddressDto {
    private String fullName;
    private String phone;
    private String email;
    private String houseNo;
    private String street;
    private String area;
    private String city;
    private String district;
    private String state;
    private String country;
    private String pincode;
    private Boolean saveAsDefault;

    public AddressDto() {}

    public AddressDto(String fullName, String phone, String email, String houseNo, String street,
                      String area, String city, String district, String state, String country, String pincode) {
        this.fullName = fullName;
        this.phone = phone;
        this.email = email;
        this.houseNo = houseNo;
        this.street = street;
        this.area = area;
        this.city = city;
        this.district = district;
        this.state = state;
        this.country = country;
        this.pincode = pincode;
    }

    // Getters and Setters
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getHouseNo() {
        return houseNo;
    }

    public void setHouseNo(String houseNo) {
        this.houseNo = houseNo;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
    }

    public String getArea() {
        return area;
    }

    public void setArea(String area) {
        this.area = area;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPincode() {
        return pincode;
    }

    public void setPincode(String pincode) {
        this.pincode = pincode;
    }

    public Boolean getSaveAsDefault() {
        return saveAsDefault;
    }

    public void setSaveAsDefault(Boolean saveAsDefault) {
        this.saveAsDefault = saveAsDefault;
    }
}
