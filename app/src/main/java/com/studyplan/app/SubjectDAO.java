package com.studyplan.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class SubjectDAO {

    private final DatabaseHelper dbHelper;
    private final Context context;

    public SubjectDAO(Context context) {
        this.context = context.getApplicationContext();
        dbHelper = DatabaseHelper.getInstance(this.context);
    }

    /**
     * Insert a new subject. Returns the new row ID, or -1 on error.
     */
    public long insert(Subject subject) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_SUBJECT_NAME, subject.getName());
        cv.put(DatabaseHelper.COL_SUBJECT_TEACHER, subject.getTeacher());
        cv.put(DatabaseHelper.COL_SUBJECT_CREDITS, subject.getCredits());
        cv.put(DatabaseHelper.COL_SUBJECT_SEMESTER, subject.getSemester());
        cv.put(DatabaseHelper.COL_SUBJECT_TYPE, subject.getType());
        cv.put(DatabaseHelper.COL_SUBJECT_COLOR, subject.getColorTag());
        long id = db.insert(DatabaseHelper.TABLE_SUBJECTS, null, cv);
        subject.setId((int) id);
        if (id > 0) {
            FirestoreHelper.getInstance(context).syncSubjectToCloud(subject);
        }
        return id;
    }

    /**
     * Update an existing subject by ID.
     */
    public int update(Subject subject) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_SUBJECT_NAME, subject.getName());
        cv.put(DatabaseHelper.COL_SUBJECT_TEACHER, subject.getTeacher());
        cv.put(DatabaseHelper.COL_SUBJECT_CREDITS, subject.getCredits());
        cv.put(DatabaseHelper.COL_SUBJECT_SEMESTER, subject.getSemester());
        cv.put(DatabaseHelper.COL_SUBJECT_TYPE, subject.getType());
        cv.put(DatabaseHelper.COL_SUBJECT_COLOR, subject.getColorTag());
        int rows = db.update(DatabaseHelper.TABLE_SUBJECTS, cv,
                DatabaseHelper.COL_SUBJECT_ID + " = ?",
                new String[]{String.valueOf(subject.getId())});
        if (rows > 0) {
            FirestoreHelper.getInstance(context).syncSubjectToCloud(subject);
        }
        return rows;
    }

    /**
     * Delete a subject by ID.
     */
    public int delete(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete(DatabaseHelper.TABLE_SUBJECTS,
                DatabaseHelper.COL_SUBJECT_ID + " = ?",
                new String[]{String.valueOf(id)});
        if (rows > 0) {
            FirestoreHelper.getInstance(context).deleteSubjectFromCloud(id);
        }
        return rows;
    }

    /**
     * Get all subjects.
     */
    public List<Subject> getAll() {
        List<Subject> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_SUBJECTS,
                null, null, null, null, null,
                DatabaseHelper.COL_SUBJECT_ID + " ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToSubject(cursor));
            }
            cursor.close();
        }
        return list;
    }

    /**
     * Search subjects by name or teacher.
     */
    public List<Subject> search(String query) {
        List<Subject> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String selection = DatabaseHelper.COL_SUBJECT_NAME + " LIKE ? OR " +
                DatabaseHelper.COL_SUBJECT_TEACHER + " LIKE ?";
        String likeQuery = "%" + query + "%";
        Cursor cursor = db.query(DatabaseHelper.TABLE_SUBJECTS,
                null, selection, new String[]{likeQuery, likeQuery},
                null, null, null);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToSubject(cursor));
            }
            cursor.close();
        }
        return list;
    }

    /**
     * Get subject by ID.
     */
    public Subject getById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_SUBJECTS,
                null,
                DatabaseHelper.COL_SUBJECT_ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null);

        Subject subject = null;
        if (cursor != null && cursor.moveToFirst()) {
            subject = cursorToSubject(cursor);
            cursor.close();
        }
        return subject;
    }

    /**
     * Get total number of subjects.
     */
    public int getCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_SUBJECTS, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    /**
     * Get total credits across all subjects.
     */
    public int getTotalCredits() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COALESCE(SUM(" + DatabaseHelper.COL_SUBJECT_CREDITS + "), 0) FROM " +
                        DatabaseHelper.TABLE_SUBJECTS, null);
        int total = 0;
        if (cursor != null && cursor.moveToFirst()) {
            total = cursor.getInt(0);
            cursor.close();
        }
        return total;
    }

    /**
     * Get distinct semester count.
     */
    public int getSemesterCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(DISTINCT " + DatabaseHelper.COL_SUBJECT_SEMESTER + ") FROM " +
                        DatabaseHelper.TABLE_SUBJECTS, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    // ─── Helper ─────────────────────────────────────────────────

    private Subject cursorToSubject(Cursor cursor) {
        return new Subject(
                cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SUBJECT_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SUBJECT_NAME)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SUBJECT_TEACHER)),
                cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SUBJECT_CREDITS)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SUBJECT_SEMESTER)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SUBJECT_TYPE)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SUBJECT_COLOR))
        );
    }
}
