package com.studyplan.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class AssignmentDAO {

    private final DatabaseHelper dbHelper;
    private final Context context;

    public AssignmentDAO(Context context) {
        this.context = context.getApplicationContext();
        dbHelper = DatabaseHelper.getInstance(this.context);
    }

    /**
     * Get assignment by ID.
     */
    public Assignment getById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_ASSIGNMENTS,
                null,
                DatabaseHelper.COL_ASSIGN_ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null);

        Assignment assignment = null;
        if (cursor != null && cursor.moveToFirst()) {
            assignment = cursorToAssignment(cursor);
            cursor.close();
        }
        return assignment;
    }

    /**
     * Insert a new assignment. Returns the new row ID.
     */
    public long insert(Assignment assignment) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_ASSIGN_TITLE, assignment.getTitle());
        cv.put(DatabaseHelper.COL_ASSIGN_SUBJECT, assignment.getSubjectName());
        cv.put(DatabaseHelper.COL_ASSIGN_DEADLINE, assignment.getDeadline());
        cv.put(DatabaseHelper.COL_ASSIGN_PRIORITY, assignment.getPriority());
        cv.put(DatabaseHelper.COL_ASSIGN_STATUS, assignment.getStatus());
        cv.put(DatabaseHelper.COL_ASSIGN_IS_DONE, assignment.isDone() ? 1 : 0);
        long id = db.insert(DatabaseHelper.TABLE_ASSIGNMENTS, null, cv);
        assignment.setId((int) id);
        if (id > 0) {
            FirestoreHelper.getInstance(context).syncAssignmentToCloud(assignment);
        }
        return id;
    }

    /**
     * Update an existing assignment.
     */
    public int update(Assignment assignment) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_ASSIGN_TITLE, assignment.getTitle());
        cv.put(DatabaseHelper.COL_ASSIGN_SUBJECT, assignment.getSubjectName());
        cv.put(DatabaseHelper.COL_ASSIGN_DEADLINE, assignment.getDeadline());
        cv.put(DatabaseHelper.COL_ASSIGN_PRIORITY, assignment.getPriority());
        cv.put(DatabaseHelper.COL_ASSIGN_STATUS, assignment.getStatus());
        cv.put(DatabaseHelper.COL_ASSIGN_IS_DONE, assignment.isDone() ? 1 : 0);
        int rows = db.update(DatabaseHelper.TABLE_ASSIGNMENTS, cv,
                DatabaseHelper.COL_ASSIGN_ID + " = ?",
                new String[]{String.valueOf(assignment.getId())});
        if (rows > 0) {
            FirestoreHelper.getInstance(context).syncAssignmentToCloud(assignment);
        }
        return rows;
    }

    /**
     * Update only the done status and status field of an assignment.
     */
    public int updateDoneStatus(int id, boolean isDone, String status) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_ASSIGN_IS_DONE, isDone ? 1 : 0);
        cv.put(DatabaseHelper.COL_ASSIGN_STATUS, status);
        int rows = db.update(DatabaseHelper.TABLE_ASSIGNMENTS, cv,
                DatabaseHelper.COL_ASSIGN_ID + " = ?",
                new String[]{String.valueOf(id)});
        if (rows > 0) {
            Assignment a = getById(id);
            if (a != null) {
                FirestoreHelper.getInstance(context).syncAssignmentToCloud(a);
            }
        }
        return rows;
    }

    /**
     * Delete an assignment by ID.
     */
    public int delete(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete(DatabaseHelper.TABLE_ASSIGNMENTS,
                DatabaseHelper.COL_ASSIGN_ID + " = ?",
                new String[]{String.valueOf(id)});
        if (rows > 0) {
            FirestoreHelper.getInstance(context).deleteAssignmentFromCloud(id);
        }
        return rows;
    }

    /**
     * Get all assignments, ordered by ID descending (newest first).
     */
    public List<Assignment> getAll() {
        List<Assignment> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_ASSIGNMENTS,
                null, null, null, null, null,
                DatabaseHelper.COL_ASSIGN_ID + " DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToAssignment(cursor));
            }
            cursor.close();
        }
        return list;
    }

    /**
     * Get assignments filtered by status.
     */
    public List<Assignment> getByStatus(String status) {
        List<Assignment> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_ASSIGNMENTS,
                null,
                DatabaseHelper.COL_ASSIGN_STATUS + " = ?",
                new String[]{status},
                null, null,
                DatabaseHelper.COL_ASSIGN_ID + " DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToAssignment(cursor));
            }
            cursor.close();
        }
        return list;
    }

    /**
     * Get total count of all assignments.
     */
    public int getCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_ASSIGNMENTS, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    /**
     * Get count by status (e.g., "late", "done", "in_progress", "not_started").
     */
    public int getCountByStatus(String status) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_ASSIGNMENTS +
                        " WHERE " + DatabaseHelper.COL_ASSIGN_STATUS + " = ?",
                new String[]{status});
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    /**
     * Get assignments that are late or due today (for Home screen deadlines).
     */
    public List<Assignment> getUpcomingDeadlines() {
        List<Assignment> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_ASSIGNMENTS,
                null,
                DatabaseHelper.COL_ASSIGN_STATUS + " IN ('late', 'in_progress', 'not_started')",
                null, null, null,
                DatabaseHelper.COL_ASSIGN_ID + " ASC",
                "4"); // Limit to 4 items for home screen

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToAssignment(cursor));
            }
            cursor.close();
        }
        return list;
    }

    /**
     * Search assignments by title or subject name.
     */
    public List<Assignment> search(String query) {
        List<Assignment> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String likeQuery = "%" + query + "%";
        Cursor cursor = db.query(DatabaseHelper.TABLE_ASSIGNMENTS,
                null,
                DatabaseHelper.COL_ASSIGN_TITLE + " LIKE ? OR " +
                        DatabaseHelper.COL_ASSIGN_SUBJECT + " LIKE ?",
                new String[]{likeQuery, likeQuery},
                null, null,
                DatabaseHelper.COL_ASSIGN_ID + " DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToAssignment(cursor));
            }
            cursor.close();
        }
        return list;
    }

    /**
     * Combined search + filter: keyword matching with optional status constraint.
     * status = "all" means no status constraint (keyword-only search).
     */
    public List<Assignment> searchWithFilter(String query, String status) {
        List<Assignment> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        boolean hasQuery = query != null && !query.isEmpty();
        boolean hasFilter = status != null && !status.equals("all");

        String selection;
        String[] selectionArgs;

        if (hasQuery && hasFilter) {
            // Both keyword and status filter
            String likeQuery = "%" + query + "%";
            selection = "(" + DatabaseHelper.COL_ASSIGN_TITLE + " LIKE ? OR " +
                    DatabaseHelper.COL_ASSIGN_SUBJECT + " LIKE ?) AND " +
                    DatabaseHelper.COL_ASSIGN_STATUS + " = ?";
            selectionArgs = new String[]{likeQuery, likeQuery, status};
        } else if (hasQuery) {
            // Keyword only
            String likeQuery = "%" + query + "%";
            selection = DatabaseHelper.COL_ASSIGN_TITLE + " LIKE ? OR " +
                    DatabaseHelper.COL_ASSIGN_SUBJECT + " LIKE ?";
            selectionArgs = new String[]{likeQuery, likeQuery};
        } else if (hasFilter) {
            // Status filter only
            selection = DatabaseHelper.COL_ASSIGN_STATUS + " = ?";
            selectionArgs = new String[]{status};
        } else {
            // No filter at all → return all
            selection = null;
            selectionArgs = null;
        }

        Cursor cursor = db.query(DatabaseHelper.TABLE_ASSIGNMENTS,
                null, selection, selectionArgs, null, null,
                DatabaseHelper.COL_ASSIGN_ID + " DESC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToAssignment(cursor));
            }
            cursor.close();
        }
        return list;
    }

    // ─── Helper ─────────────────────────────────────────────────

    private Assignment cursorToAssignment(Cursor cursor) {
        return new Assignment(
                cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ASSIGN_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ASSIGN_TITLE)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ASSIGN_SUBJECT)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ASSIGN_DEADLINE)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ASSIGN_PRIORITY)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ASSIGN_STATUS)),
                cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_ASSIGN_IS_DONE)) == 1
        );
    }
}

