package com.studyplan.app;

public class ScheduleItem {
    private int id;
    private String subjectName;
    private String startTime;
    private String endTime;
    private String room;
    private String teacher;
    private String status; // "done", "in_progress", "upcoming"
    private String colorTag; // "purple", "blue", "green", "orange"
    private String dayOfWeek; // "T2", "T3", "T4", "T5", "T6", "T7", "CN"

    public ScheduleItem(int id, String subjectName, String startTime, String endTime,
                        String room, String teacher, String status, String colorTag,
                        String dayOfWeek) {
        this.id = id;
        this.subjectName = subjectName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.room = room;
        this.teacher = teacher;
        this.status = status;
        this.colorTag = colorTag;
        this.dayOfWeek = dayOfWeek;
    }

    // Getters
    public int getId() { return id; }
    public String getSubjectName() { return subjectName; }
    public String getStartTime() { return startTime; }
    public String getEndTime() { return endTime; }
    public String getRoom() { return room; }
    public String getTeacher() { return teacher; }
    public String getStatus() { return status; }
    public String getColorTag() { return colorTag; }
    public String getDayOfWeek() { return dayOfWeek; }

    // Setters
    public void setId(int id) { this.id = id; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public void setRoom(String room) { this.room = room; }
    public void setTeacher(String teacher) { this.teacher = teacher; }
    public void setStatus(String status) { this.status = status; }
    public void setColorTag(String colorTag) { this.colorTag = colorTag; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }
}
