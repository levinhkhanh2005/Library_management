package com.example.model;

import java.util.Objects;

/** Khối ngành độc lập. Một khối ngành có thể chứa nhiều thể loại sách. */
public class Major {
    private int id;
    private String name;
    private String description;
    private String createdAt;

    public Major() {}

    public Major(int id, String name, String description, String createdAt) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
    }

    public Major(int id, String name) {
        this(id, name, "", null);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    @Override public String toString() { return name == null ? "" : name; }
    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Major)) return false;
        Major m = (Major)o;
        return id == m.id || (name != null && name.equalsIgnoreCase(m.name));
    }
    @Override public int hashCode() { return Objects.hash(id, name == null ? null : name.toLowerCase()); }
}
