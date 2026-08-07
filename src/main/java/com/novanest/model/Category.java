package com.novanest.model;

import jakarta.persistence.*;

@Entity
@Table(name = "categories")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Integer id;

    @Column(name = "category_name", nullable = false)
    private String categoryName;

    @Column(name = "category_image")
    private String categoryImage;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "banner_image")
    private String bannerImage;

    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "visibility")
    private Boolean visibility = true;

    public Category() {
    }

    public Category(Integer id, String categoryName) {
        this.id = id;
        this.categoryName = categoryName;
    }

    public Category(Integer id, String categoryName, String categoryImage, String description) {
        this.id = id;
        this.categoryName = categoryName;
        this.categoryImage = categoryImage;
        this.description = description;
    }

    public Category(Integer id, String categoryName, String categoryImage, String description, String bannerImage, Integer displayOrder, Boolean visibility) {
        this.id = id;
        this.categoryName = categoryName;
        this.categoryImage = categoryImage;
        this.description = description;
        this.bannerImage = bannerImage;
        this.displayOrder = displayOrder;
        this.visibility = visibility;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryImage() {
        return categoryImage;
    }

    public void setCategoryImage(String categoryImage) {
        this.categoryImage = categoryImage;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getBannerImage() {
        return bannerImage;
    }

    public void setBannerImage(String bannerImage) {
        this.bannerImage = bannerImage;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Boolean getVisibility() {
        return visibility;
    }

    public void setVisibility(Boolean visibility) {
        this.visibility = visibility;
    }
}
