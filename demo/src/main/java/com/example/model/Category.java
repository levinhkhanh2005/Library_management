package com.example.model;

import java.util.Objects;

/** Thể loại sách. Mỗi thể loại thuộc đúng một khối ngành; một khối ngành có nhiều thể loại. */
public class Category {
    private int id;
    private String name;
    private String description;
    private int majorId;
    private String major;
    private String createdAt;

    public Category() {}

    public Category(int id, String name, String description, int majorId, String major, String createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.majorId = majorId;
        this.major = major;
        this.createdAt = createdAt;
    }

    // Constructor tương thích code cũ.
    public Category(int id, String name, String description, String major, String createdAt) {
        this(id, name, description, 0, major, createdAt);
    }
    public Category(int id, String name, String description, String createdAt) {
        this(id, name, description, 0, null, createdAt);
    }
    public Category(int id, String name, String description) {
        this(id, name, description, 0, null, null);
    }
    public Category(String name, String description) {
        this(0, name, description, 0, null, null);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getMajorId() { return majorId; }
    public void setMajorId(int majorId) { this.majorId = majorId; }
    public String getMajor() { return major == null || major.isBlank() ? "Chưa phân khối ngành" : major; }
    public void setMajor(String major) { this.major = major; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Category)) return false;
        Category c = (Category)o;
        return id == c.id || (name != null && name.equalsIgnoreCase(c.name));
    }
    @Override public int hashCode() { return Objects.hash(id, name == null ? null : name.toLowerCase()); }
    @Override public String toString() { return name == null ? "" : name; }
}
