package com.studyplan.app;

public class Subject {
    private int id;
    private String name;
    private String teacher;
    private int credits;
    private String semester;
    private String type; // "Thực hành" or "Lý thuyết"
    private String colorTag; // "green", "blue", "orange", "purple", "red"

    public Subject(int id, String name, String teacher, int credits, String semester, String type, String colorTag) {
        this.id = id;
        this.name = name;
        this.teacher = teacher;
        this.credits = credits;
        this.semester = semester;
        this.type = type;
        this.colorTag = colorTag;
    }

    // Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public String getTeacher() { return teacher; }
    public int getCredits() { return credits; }
    public String getSemester() { return semester; }
    public String getType() { return type; }
    public String getColorTag() { return colorTag; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setTeacher(String teacher) { this.teacher = teacher; }
    public void setCredits(int credits) { this.credits = credits; }
    public void setSemester(String semester) { this.semester = semester; }
    public void setType(String type) { this.type = type; }
    public void setColorTag(String colorTag) { this.colorTag = colorTag; }
}
