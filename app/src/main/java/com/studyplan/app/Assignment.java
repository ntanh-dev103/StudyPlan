package com.studyplan.app;

public class Assignment {
    private int id;
    private String title;
    private String subjectName;
    private String deadline;
    private String priority; // "Cao", "Trung bình", "Thấp"
    private String status;   // "late", "in_progress", "done", "not_started"
    private boolean isDone;

    public Assignment(int id, String title, String subjectName, String deadline,
                      String priority, String status, boolean isDone) {
        this.id = id;
        this.title = title;
        this.subjectName = subjectName;
        this.deadline = deadline;
        this.priority = priority;
        this.status = status;
        this.isDone = isDone;
    }

    // Getters
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getSubjectName() { return subjectName; }
    public String getDeadline() { return deadline; }
    public String getPriority() { return priority; }
    public String getStatus() { return status; }
    public boolean isDone() { return isDone; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public void setDeadline(String deadline) { this.deadline = deadline; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setStatus(String status) { this.status = status; }
    public void setDone(boolean done) { isDone = done; }
}
