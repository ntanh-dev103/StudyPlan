package com.studyplan.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "studyplan.db";
    private static final int DATABASE_VERSION = 4;

    // ─── Table: users ───────────────────────────────────────────
    public static final String TABLE_USERS = "users";
    public static final String COL_USER_ID = "id";
    public static final String COL_USER_FULLNAME = "fullname";
    public static final String COL_USER_EMAIL = "email";
    public static final String COL_USER_PASSWORD = "password";
    public static final String COL_USER_GOOGLE_ID = "google_id";
    public static final String COL_USER_PHOTO_URL = "photo_url";
    public static final String COL_USER_LOGIN_METHOD = "login_method";
    public static final String COL_USER_CREATED_AT = "created_at";

    // ─── Table: subjects ────────────────────────────────────────
    public static final String TABLE_SUBJECTS = "subjects";
    public static final String COL_SUBJECT_ID = "id";
    public static final String COL_SUBJECT_NAME = "name";
    public static final String COL_SUBJECT_TEACHER = "teacher";
    public static final String COL_SUBJECT_CREDITS = "credits";
    public static final String COL_SUBJECT_SEMESTER = "semester";
    public static final String COL_SUBJECT_TYPE = "type";
    public static final String COL_SUBJECT_COLOR = "color_tag";

    // ─── Table: assignments ─────────────────────────────────────
    public static final String TABLE_ASSIGNMENTS = "assignments";
    public static final String COL_ASSIGN_ID = "id";
    public static final String COL_ASSIGN_TITLE = "title";
    public static final String COL_ASSIGN_SUBJECT = "subject_name";
    public static final String COL_ASSIGN_DEADLINE = "deadline";
    public static final String COL_ASSIGN_PRIORITY = "priority";
    public static final String COL_ASSIGN_STATUS = "status";
    public static final String COL_ASSIGN_IS_DONE = "is_done";

    // ─── Table: schedule_items ──────────────────────────────────
    public static final String TABLE_SCHEDULE = "schedule_items";
    public static final String COL_SCHED_ID = "id";
    public static final String COL_SCHED_SUBJECT = "subject_name";
    public static final String COL_SCHED_START = "start_time";
    public static final String COL_SCHED_END = "end_time";
    public static final String COL_SCHED_ROOM = "room";
    public static final String COL_SCHED_TEACHER = "teacher";
    public static final String COL_SCHED_STATUS = "status";
    public static final String COL_SCHED_COLOR = "color_tag";
    public static final String COL_SCHED_DAY = "day_of_week";

    // ─── SQL: Create tables ─────────────────────────────────────

    private static final String CREATE_TABLE_USERS =
            "CREATE TABLE " + TABLE_USERS + " (" +
                    COL_USER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_USER_FULLNAME + " TEXT NOT NULL, " +
                    COL_USER_EMAIL + " TEXT NOT NULL UNIQUE, " +
                    COL_USER_PASSWORD + " TEXT, " +
                    COL_USER_GOOGLE_ID + " TEXT, " +
                    COL_USER_PHOTO_URL + " TEXT, " +
                    COL_USER_LOGIN_METHOD + " TEXT DEFAULT 'email', " +
                    COL_USER_CREATED_AT + " TEXT DEFAULT CURRENT_TIMESTAMP" +
                    ");";

    private static final String CREATE_TABLE_SUBJECTS =
            "CREATE TABLE " + TABLE_SUBJECTS + " (" +
                    COL_SUBJECT_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_SUBJECT_NAME + " TEXT NOT NULL, " +
                    COL_SUBJECT_TEACHER + " TEXT, " +
                    COL_SUBJECT_CREDITS + " INTEGER DEFAULT 3, " +
                    COL_SUBJECT_SEMESTER + " TEXT, " +
                    COL_SUBJECT_TYPE + " TEXT, " +
                    COL_SUBJECT_COLOR + " TEXT DEFAULT 'green'" +
                    ");";

    private static final String CREATE_TABLE_ASSIGNMENTS =
            "CREATE TABLE " + TABLE_ASSIGNMENTS + " (" +
                    COL_ASSIGN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_ASSIGN_TITLE + " TEXT NOT NULL, " +
                    COL_ASSIGN_SUBJECT + " TEXT, " +
                    COL_ASSIGN_DEADLINE + " TEXT, " +
                    COL_ASSIGN_PRIORITY + " TEXT DEFAULT 'Trung bình', " +
                    COL_ASSIGN_STATUS + " TEXT DEFAULT 'not_started', " +
                    COL_ASSIGN_IS_DONE + " INTEGER DEFAULT 0" +
                    ");";

    private static final String CREATE_TABLE_SCHEDULE =
            "CREATE TABLE " + TABLE_SCHEDULE + " (" +
                    COL_SCHED_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COL_SCHED_SUBJECT + " TEXT NOT NULL, " +
                    COL_SCHED_START + " TEXT, " +
                    COL_SCHED_END + " TEXT, " +
                    COL_SCHED_ROOM + " TEXT, " +
                    COL_SCHED_TEACHER + " TEXT, " +
                    COL_SCHED_STATUS + " TEXT DEFAULT 'upcoming', " +
                    COL_SCHED_COLOR + " TEXT DEFAULT 'purple', " +
                    COL_SCHED_DAY + " TEXT DEFAULT 'T2'" +
                    ");";

    // Singleton
    private static DatabaseHelper instance;

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);
        db.execSQL(CREATE_TABLE_SUBJECTS);
        db.execSQL(CREATE_TABLE_ASSIGNMENTS);
        db.execSQL(CREATE_TABLE_SCHEDULE);

        // Seed sample data
        seedSubjects(db);
        seedAssignments(db);
        seedScheduleItems(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 4) {
            // Migration v3 → v4: Add Google Sign-In columns to users table
            try {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_USER_GOOGLE_ID + " TEXT");
            } catch (Exception ignored) {} // Column may already exist
            try {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_USER_PHOTO_URL + " TEXT");
            } catch (Exception ignored) {}
            try {
                db.execSQL("ALTER TABLE " + TABLE_USERS + " ADD COLUMN " + COL_USER_LOGIN_METHOD + " TEXT DEFAULT 'email'");
            } catch (Exception ignored) {}
        }
    }

    // ─── Seed data ──────────────────────────────────────────────

    private void seedSubjects(SQLiteDatabase db) {
        insertSubject(db, "Lập Trình Android", "ThS. Nguyễn Văn A", 3, "HK1", "Thực hành", "green");
        insertSubject(db, "Cấu Trúc Dữ Liệu", "TS. Trần Thị B", 3, "HK1", "Lý thuyết", "blue");
        insertSubject(db, "Toán Cao Cấp A1", "PGS. Lê Văn C", 3, "HK1", "Lý thuyết", "purple");
        insertSubject(db, "Mạng Máy Tính", "ThS. Phạm Văn D", 2, "HK2", "Thực hành", "orange");
        insertSubject(db, "Tiếng Anh CNTT", "ThS. Hoàng Thị E", 3, "HK2", "Lý thuyết", "red");
    }

    private void seedAssignments(SQLiteDatabase db) {
        insertAssignment(db, "Báo cáo Android", "Lập Trình Android", "20-05-2026 23:59", "Cao", "late", 0);
        insertAssignment(db, "Bài tập Chương 5", "Cấu Trúc Dữ Liệu", "22-05-2026 23:59", "Trung bình", "in_progress", 0);
        insertAssignment(db, "Đồ án nhóm", "Mạng Máy Tính", "25-05-2026 23:59", "Cao", "in_progress", 0);
        insertAssignment(db, "Bài tập lớn Giữa kỳ", "Cấu Trúc Dữ Liệu", "20-05-2026 23:59", "Cao", "late", 0);
        insertAssignment(db, "Thuyết trình Unit Test", "Lập Trình Android", "18-05-2026 23:59", "Thấp", "done", 1);
        insertAssignment(db, "Bài tập Chương 3", "Toán Cao Cấp A1", "15-05-2026 23:59", "Trung bình", "done", 1);
    }

    private void seedScheduleItems(SQLiteDatabase db) {
        // Thứ 2
        insertScheduleItem(db, "Lập Trình Android", "07:30", "09:00", "P.A101", "GV. Nguyễn Văn A", "upcoming", "purple", "T2");
        insertScheduleItem(db, "Toán Cao Cấp A1", "09:30", "11:00", "P.C301", "GV. Lê Văn C", "upcoming", "green", "T2");
        // Thứ 3
        insertScheduleItem(db, "Cấu Trúc Dữ Liệu", "07:30", "09:00", "P.B204", "GV. Trần Thị B", "upcoming", "blue", "T3");
        insertScheduleItem(db, "Tiếng Anh CNTT", "13:00", "14:30", "P.D102", "GV. Hoàng Thị E", "upcoming", "orange", "T3");
        // Thứ 4
        insertScheduleItem(db, "Lập Trình Android", "07:30", "09:00", "P.A101", "GV. Nguyễn Văn A", "upcoming", "purple", "T4");
        insertScheduleItem(db, "Mạng Máy Tính", "09:30", "11:00", "P.E205", "GV. Phạm Văn D", "upcoming", "orange", "T4");
        // Thứ 5
        insertScheduleItem(db, "Cấu Trúc Dữ Liệu", "07:30", "09:00", "P.B204", "GV. Trần Thị B", "upcoming", "blue", "T5");
        insertScheduleItem(db, "Toán Cao Cấp A1", "13:00", "14:30", "P.C301", "GV. Lê Văn C", "upcoming", "green", "T5");
        // Thứ 6
        insertScheduleItem(db, "Mạng Máy Tính", "07:30", "09:00", "P.E205", "GV. Phạm Văn D", "upcoming", "orange", "T6");
        insertScheduleItem(db, "Tiếng Anh CNTT", "09:30", "11:00", "P.D102", "GV. Hoàng Thị E", "upcoming", "blue", "T6");
    }

    // ─── Helper insert methods for seeding ──────────────────────

    private void insertSubject(SQLiteDatabase db, String name, String teacher,
                               int credits, String semester, String type, String color) {
        ContentValues cv = new ContentValues();
        cv.put(COL_SUBJECT_NAME, name);
        cv.put(COL_SUBJECT_TEACHER, teacher);
        cv.put(COL_SUBJECT_CREDITS, credits);
        cv.put(COL_SUBJECT_SEMESTER, semester);
        cv.put(COL_SUBJECT_TYPE, type);
        cv.put(COL_SUBJECT_COLOR, color);
        db.insert(TABLE_SUBJECTS, null, cv);
    }

    private void insertAssignment(SQLiteDatabase db, String title, String subject,
                                  String deadline, String priority, String status, int isDone) {
        ContentValues cv = new ContentValues();
        cv.put(COL_ASSIGN_TITLE, title);
        cv.put(COL_ASSIGN_SUBJECT, subject);
        cv.put(COL_ASSIGN_DEADLINE, deadline);
        cv.put(COL_ASSIGN_PRIORITY, priority);
        cv.put(COL_ASSIGN_STATUS, status);
        cv.put(COL_ASSIGN_IS_DONE, isDone);
        db.insert(TABLE_ASSIGNMENTS, null, cv);
    }

    private void insertScheduleItem(SQLiteDatabase db, String subject, String start,
                                    String end, String room, String teacher,
                                    String status, String color, String dayOfWeek) {
        ContentValues cv = new ContentValues();
        cv.put(COL_SCHED_SUBJECT, subject);
        cv.put(COL_SCHED_START, start);
        cv.put(COL_SCHED_END, end);
        cv.put(COL_SCHED_ROOM, room);
        cv.put(COL_SCHED_TEACHER, teacher);
        cv.put(COL_SCHED_STATUS, status);
        cv.put(COL_SCHED_COLOR, color);
        cv.put(COL_SCHED_DAY, dayOfWeek);
        db.insert(TABLE_SCHEDULE, null, cv);
    }
}
