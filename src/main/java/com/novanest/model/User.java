package com.novanest.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "user_id")
	private Integer id;

	@Column(nullable = false, unique = true, length = 50)
	private String username;

	@Column(nullable = false)
	private String password;

	@Column(nullable = false, length = 10)
	private String gender;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(nullable = false, unique = true, length = 10)
	private String phone;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Role role;

	@Column(length = 50)
	private String status = "ACTIVE";

	@Column(name = "profile_image", length = 512)
	private String profileImage;

	@Column(name = "full_name", length = 100)
	private String fullName;


	// New Address Fields
	@Column(name = "house_no", length = 100)
	private String houseNo;

	@Column(length = 100)
	private String street;

	@Column(length = 100)
	private String area;

	@Column(length = 100)
	private String city;

	@Column(length = 100)
	private String district;

	@Column(length = 100)
	private String state;

	@Column(length = 100)
	private String country;

	@Column(length = 50)
	private String pincode;


	// Default Constructor
	public User() {
	}

	// Getters
	public Integer getId() {
		return id;
	}

	public String getUsername() {
		return username;
	}

	public String getPassword() {
		return password;
	}

	public String getGender() {
		return gender;
	}

	public String getEmail() {
		return email;
	}

	public String getPhone() {
		return phone;
	}

	public Role getRole() {
		return role;
	}

	public String getStatus() {
		return status;
	}

	public String getProfileImage() {
		return profileImage;
	}

	public String getFullName() {
		return fullName;
	}


	public String getHouseNo() {
		return houseNo;
	}

	public String getStreet() {
		return street;
	}

	public String getArea() {
		return area;
	}

	public String getCity() {
		return city;
	}

	public String getDistrict() {
		return district;
	}

	public String getState() {
		return state;
	}

	public String getCountry() {
		return country;
	}

	public String getPincode() {
		return pincode;
	}


	// Setters
	public void setId(Integer id) {
		this.id = id;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public void setProfileImage(String profileImage) {
		this.profileImage = profileImage;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}


	public void setHouseNo(String houseNo) {
		this.houseNo = houseNo;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public void setArea(String area) {
		this.area = area;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public void setDistrict(String district) {
		this.district = district;
	}

	public void setState(String state) {
		this.state = state;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public void setPincode(String pincode) {
		this.pincode = pincode;
	}

}