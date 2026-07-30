package com.novanest.model;

import jakarta.persistence.*;

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

	// Default Constructor
	public User() {
	}

	// Parameterized Constructor
	public User(Integer id, String username, String password, String gender, String email, String phone, Role role) {

		this.id = id;
		this.username = username;
		this.password = password;
		this.gender = gender;
		this.email = email;
		this.phone = phone;
		this.role = role;
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
}