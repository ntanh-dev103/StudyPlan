package com.studyplan.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class ScheduleDAO {

    private final DatabaseHelper dbHelper;
    private final Context context;

    public ScheduleDAO(Context context) {
        this.context = context.getApplicationContext();
        dbHelper = DatabaseHelper.getInstance(this.context);
    }

    /**
     * Insert a new schedule item. Returns the new row ID.
     */
    public long insert(ScheduleItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = buildContentValues(item);
        long id = db.insert(DatabaseHelper.TABLE_SCHEDULE, null, cv);
        item.setId((int) id);
        if (id > 0) {
            FirestoreHelper.getInstance(context).syncScheduleItemToCloud(item);
        }
        return id;
    }

    /**
     * Update an existing schedule item.
     */
    public int update(ScheduleItem item) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = buildContentValues(item);
        int rows = db.update(DatabaseHelper.TABLE_SCHEDULE, cv,
                DatabaseHelper.COL_SCHED_ID + " = ?",
                new String[]{String.valueOf(item.getId())});
        if (rows > 0) {
            FirestoreHelper.getInstance(context).syncScheduleItemToCloud(item);
        }
        return rows;
    }

    /**
     * Delete a schedule item by ID.
     */
    public int delete(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        int rows = db.delete(DatabaseHelper.TABLE_SCHEDULE,
                DatabaseHelper.COL_SCHED_ID + " = ?",
                new String[]{String.valueOf(id)});
        if (rows > 0) {
            FirestoreHelper.getInstance(context).deleteScheduleItemFromCloud(id);
        }
        return rows;
    }

    /**
     * Get all schedule items ordered by day then start time.
     */
    public List<ScheduleItem> getAll() {
        List<ScheduleItem> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_SCHEDULE,
                null, null, null, null, null,
                "CASE " + DatabaseHelper.COL_SCHED_DAY +
                        " WHEN 'T2' THEN 1 WHEN 'T3' THEN 2 WHEN 'T4' THEN 3" +
                        " WHEN 'T5' THEN 4 WHEN 'T6' THEN 5 WHEN 'T7' THEN 6" +
                        " WHEN 'CN' THEN 7 ELSE 8 END, " +
                        DatabaseHelper.COL_SCHED_START + " ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToScheduleItem(cursor));
            }
            cursor.close();
        }
        return list;
    }

    /**
     * Get schedule items for a specific day of week ("T2", "T3", ... "CN").
     */
    public List<ScheduleItem> getByDay(String dayOfWeek) {
        List<ScheduleItem> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_SCHEDULE,
                null,
                DatabaseHelper.COL_SCHED_DAY + " = ?",
                new String[]{dayOfWeek},
                null, null,
                DatabaseHelper.COL_SCHED_START + " ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToScheduleItem(cursor));
            }
            cursor.close();
        }
        return list;
    }

    /**
     * Get schedule items for weekdays (T2-T6) – "theo tuần".
     */
    public List<ScheduleItem> getWeekSchedule() {
        List<ScheduleItem> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_SCHEDULE,
                null,
                DatabaseHelper.COL_SCHED_DAY + " IN ('T2','T3','T4','T5','T6')",
                null, null, null,
                "CASE " + DatabaseHelper.COL_SCHED_DAY +
                        " WHEN 'T2' THEN 1 WHEN 'T3' THEN 2 WHEN 'T4' THEN 3" +
                        " WHEN 'T5' THEN 4 WHEN 'T6' THEN 5 ELSE 6 END, " +
                        DatabaseHelper.COL_SCHED_START + " ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToScheduleItem(cursor));
            }
            cursor.close();
        }
        return list;
    }

    /**
     * Get schedule items for today (Home screen).
     */
    public List<ScheduleItem> getTodaySchedule() {
        // Determine the current day of week
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int dayIndex = cal.get(java.util.Calendar.DAY_OF_WEEK);
        String dayStr;
        switch (dayIndex) {
            case java.util.Calendar.MONDAY: dayStr = "T2"; break;
            case java.util.Calendar.TUESDAY: dayStr = "T3"; break;
            case java.util.Calendar.WEDNESDAY: dayStr = "T4"; break;
            case java.util.Calendar.THURSDAY: dayStr = "T5"; break;
            case java.util.Calendar.FRIDAY: dayStr = "T6"; break;
            case java.util.Calendar.SATURDAY: dayStr = "T7"; break;
            default: dayStr = "CN"; break;
        }
        return getByDay(dayStr);
    }

    /**
     * Get today's day label (e.g. "T5" for Thursday).
     */
    public String getTodayDayLabel() {
        java.util.Calendar cal = java.util.Calendar.getInstance();
        int dayIndex = cal.get(java.util.Calendar.DAY_OF_WEEK);
        switch (dayIndex) {
            case java.util.Calendar.MONDAY: return "T2";
            case java.util.Calendar.TUESDAY: return "T3";
            case java.util.Calendar.WEDNESDAY: return "T4";
            case java.util.Calendar.THURSDAY: return "T5";
            case java.util.Calendar.FRIDAY: return "T6";
            case java.util.Calendar.SATURDAY: return "T7";
            default: return "CN";
        }
    }

    /**
     * Get total count of schedule items.
     */
    public int getCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_SCHEDULE, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    /**
     * Get count by day.
     */
    public int getCountByDay(String dayOfWeek) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_SCHEDULE +
                        " WHERE " + DatabaseHelper.COL_SCHED_DAY + " = ?",
                new String[]{dayOfWeek});
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    /**
     * Search schedule by subject name.
     */
    public List<ScheduleItem> search(String query) {
        List<ScheduleItem> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String likeQuery = "%" + query + "%";
        Cursor cursor = db.query(DatabaseHelper.TABLE_SCHEDULE,
                null,
                DatabaseHelper.COL_SCHED_SUBJECT + " LIKE ? OR " +
                        DatabaseHelper.COL_SCHED_ROOM + " LIKE ? OR " +
                        DatabaseHelper.COL_SCHED_TEACHER + " LIKE ?",
                new String[]{likeQuery, likeQuery, likeQuery},
                null, null,
                DatabaseHelper.COL_SCHED_START + " ASC");

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToScheduleItem(cursor));
            }
            cursor.close();
        }
        return list;
    }

    /**
     * Combined search + day filter.
     * filter = "all" → no day constraint (keyword-only).
     * filter = "today" → resolve current day and filter.
     * filter = "week" → T2-T6 only.
     * filter = "T2".."CN" → specific day.
     */
    public List<ScheduleItem> searchWithFilter(String query, String filter) {
        List<ScheduleItem> list = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        boolean hasQuery = query != null && !query.isEmpty();
        String likeQuery = hasQuery ? "%" + query + "%" : null;

        // Build keyword part
        String keywordWhere = hasQuery
                ? "(" + DatabaseHelper.COL_SCHED_SUBJECT + " LIKE ? OR " +
                  DatabaseHelper.COL_SCHED_ROOM + " LIKE ? OR " +
                  DatabaseHelper.COL_SCHED_TEACHER + " LIKE ?)"
                : null;

        // Build day part
        String dayWhere = null;
        if (filter != null) {
            switch (filter) {
                case "today":
                    String today = getTodayDayLabel();
                    dayWhere = DatabaseHelper.COL_SCHED_DAY + " = '" + today + "'";
                    break;
                case "week":
                    dayWhere = DatabaseHelper.COL_SCHED_DAY + " IN ('T2','T3','T4','T5','T6')";
                    break;
                case "all":
                    dayWhere = null;
                    break;
                default:
                    // Specific day labels: T2, T3, T4, T5, T6, T7, CN
                    dayWhere = DatabaseHelper.COL_SCHED_DAY + " = '" + filter + "'";
                    break;
            }
        }

        // Combine
        String selection = null;
        String[] selectionArgs = null;
        if (keywordWhere != null && dayWhere != null) {
            selection = keywordWhere + " AND " + dayWhere;
            selectionArgs = new String[]{likeQuery, likeQuery, likeQuery};
        } else if (keywordWhere != null) {
            selection = keywordWhere;
            selectionArgs = new String[]{likeQuery, likeQuery, likeQuery};
        } else if (dayWhere != null) {
            selection = dayWhere;
        }

        String orderBy = "CASE " + DatabaseHelper.COL_SCHED_DAY +
                " WHEN 'T2' THEN 1 WHEN 'T3' THEN 2 WHEN 'T4' THEN 3" +
                " WHEN 'T5' THEN 4 WHEN 'T6' THEN 5 WHEN 'T7' THEN 6" +
                " WHEN 'CN' THEN 7 ELSE 8 END, " + DatabaseHelper.COL_SCHED_START + " ASC";

        Cursor cursor = db.query(DatabaseHelper.TABLE_SCHEDULE,
                null, selection, selectionArgs, null, null, orderBy);

        if (cursor != null) {
            while (cursor.moveToNext()) {
                list.add(cursorToScheduleItem(cursor));
            }
            cursor.close();
        }
        return list;
    }


    // ─── Helper ─────────────────────────────────────────────────

    private ContentValues buildContentValues(ScheduleItem item) {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_SCHED_SUBJECT, item.getSubjectName());
        cv.put(DatabaseHelper.COL_SCHED_START, item.getStartTime());
        cv.put(DatabaseHelper.COL_SCHED_END, item.getEndTime());
        cv.put(DatabaseHelper.COL_SCHED_ROOM, item.getRoom());
        cv.put(DatabaseHelper.COL_SCHED_TEACHER, item.getTeacher());
        cv.put(DatabaseHelper.COL_SCHED_STATUS, item.getStatus());
        cv.put(DatabaseHelper.COL_SCHED_COLOR, item.getColorTag());
        cv.put(DatabaseHelper.COL_SCHED_DAY, item.getDayOfWeek());
        return cv;
    }

    private ScheduleItem cursorToScheduleItem(Cursor cursor) {
        return new ScheduleItem(
                cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCHED_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCHED_SUBJECT)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCHED_START)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCHED_END)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCHED_ROOM)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCHED_TEACHER)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCHED_STATUS)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCHED_COLOR)),
                cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCHED_DAY))
        );
    }
}
