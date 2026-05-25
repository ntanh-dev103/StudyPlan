package com.studyplan.app;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to synchronize local SQLite database with Cloud Firestore.
 * Handles cloud sync in the background when active.
 */
public class FirestoreHelper {

    private static final String TAG = "FirestoreHelper";
    private static final String PREF_NAME = "StudyPlanPrefs";
    private static FirestoreHelper instance;
    private final Context context;
    private final FirebaseFirestore db;

    private FirestoreHelper(Context context) {
        this.context = context.getApplicationContext();
        this.db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirestoreHelper getInstance(Context context) {
        if (instance == null) {
            instance = new FirestoreHelper(context);
        }
        return instance;
    }

    // ─── Shared Preference Helpers ──────────────────────────────

    private String getLoggedInUserEmail() {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        return prefs.getString("user_email", "").toLowerCase().trim();
    }

    // ─── Users Sync ─────────────────────────────────────────────

    public void syncUserToCloud(String fullname, String email, String hashedPassword) {
        if (email == null || email.isEmpty()) return;

        Map<String, Object> userData = new HashMap<>();
        userData.put("fullname", fullname);
        userData.put("email", email.toLowerCase().trim());
        userData.put("password", hashedPassword);
        userData.put("created_at", String.valueOf(System.currentTimeMillis()));

        db.collection("users")
                .document(email.toLowerCase().trim())
                .set(userData)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User profile synced to cloud successfully"))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync user profile", e));
    }

    // ─── Subjects Sync ──────────────────────────────────────────

    public void syncSubjectToCloud(Subject subject) {
        String email = getLoggedInUserEmail();
        if (email.isEmpty()) return;

        Map<String, Object> data = new HashMap<>();
        data.put("id", subject.getId());
        data.put("name", subject.getName());
        data.put("teacher", subject.getTeacher());
        data.put("credits", subject.getCredits());
        data.put("semester", subject.getSemester());
        data.put("type", subject.getType());
        data.put("color_tag", subject.getColorTag());

        db.collection("users")
                .document(email)
                .collection("subjects")
                .document(String.valueOf(subject.getId()))
                .set(data)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Subject synced: " + subject.getName()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync subject", e));
    }

    public void deleteSubjectFromCloud(int subjectId) {
        String email = getLoggedInUserEmail();
        if (email.isEmpty()) return;

        db.collection("users")
                .document(email)
                .collection("subjects")
                .document(String.valueOf(subjectId))
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Subject deleted from cloud: " + subjectId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete subject from cloud", e));
    }

    // ─── Assignments Sync ───────────────────────────────────────

    public void syncAssignmentToCloud(Assignment assignment) {
        String email = getLoggedInUserEmail();
        if (email.isEmpty()) return;

        Map<String, Object> data = new HashMap<>();
        data.put("id", assignment.getId());
        data.put("title", assignment.getTitle());
        data.put("subject_name", assignment.getSubjectName());
        data.put("deadline", assignment.getDeadline());
        data.put("priority", assignment.getPriority());
        data.put("status", assignment.getStatus());
        data.put("is_done", assignment.isDone());

        db.collection("users")
                .document(email)
                .collection("assignments")
                .document(String.valueOf(assignment.getId()))
                .set(data)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Assignment synced: " + assignment.getTitle()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync assignment", e));
    }

    public void deleteAssignmentFromCloud(int assignmentId) {
        String email = getLoggedInUserEmail();
        if (email.isEmpty()) return;

        db.collection("users")
                .document(email)
                .collection("assignments")
                .document(String.valueOf(assignmentId))
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Assignment deleted from cloud: " + assignmentId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete assignment from cloud", e));
    }

    // ─── Schedule Items Sync ────────────────────────────────────

    public void syncScheduleItemToCloud(ScheduleItem item) {
        String email = getLoggedInUserEmail();
        if (email.isEmpty()) return;

        Map<String, Object> data = new HashMap<>();
        data.put("id", item.getId());
        data.put("subject_name", item.getSubjectName());
        data.put("start_time", item.getStartTime());
        data.put("end_time", item.getEndTime());
        data.put("room", item.getRoom());
        data.put("teacher", item.getTeacher());
        data.put("status", item.getStatus());
        data.put("color_tag", item.getColorTag());
        data.put("day_of_week", item.getDayOfWeek());

        db.collection("users")
                .document(email)
                .collection("schedule_items")
                .document(String.valueOf(item.getId()))
                .set(data)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Schedule item synced: " + item.getSubjectName()))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to sync schedule item", e));
    }

    public void deleteScheduleItemFromCloud(int itemId) {
        String email = getLoggedInUserEmail();
        if (email.isEmpty()) return;

        db.collection("users")
                .document(email)
                .collection("schedule_items")
                .document(String.valueOf(itemId))
                .delete()
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Schedule item deleted from cloud: " + itemId))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to delete schedule item from cloud", e));
    }

    // ─── End-to-End Core Sync Methods ────────────────────────────

    /**
     * Push all local SQLite data for the logged-in user up to Cloud Firestore.
     */
    public void pushAllLocalDataToCloud() {
        String email = getLoggedInUserEmail();
        if (email.isEmpty()) {
            Log.w(TAG, "Cannot sync: No user email logged in");
            return;
        }

        DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        SQLiteDatabase sqliteDb = dbHelper.getReadableDatabase();

        // 1. Sync Subjects
        Cursor subjectCursor = sqliteDb.query(DatabaseHelper.TABLE_SUBJECTS, null, null, null, null, null, null);
        if (subjectCursor != null) {
            while (subjectCursor.moveToNext()) {
                Subject s = new Subject(
                        subjectCursor.getInt(subjectCursor.getColumnIndexOrThrow(DatabaseHelper.COL_SUBJECT_ID)),
                        subjectCursor.getString(subjectCursor.getColumnIndexOrThrow(DatabaseHelper.COL_SUBJECT_NAME)),
                        subjectCursor.getString(subjectCursor.getColumnIndexOrThrow(DatabaseHelper.COL_SUBJECT_TEACHER)),
                        subjectCursor.getInt(subjectCursor.getColumnIndexOrThrow(DatabaseHelper.COL_SUBJECT_CREDITS)),
                        subjectCursor.getString(subjectCursor.getColumnIndexOrThrow(DatabaseHelper.COL_SUBJECT_SEMESTER)),
                        subjectCursor.getString(subjectCursor.getColumnIndexOrThrow(DatabaseHelper.COL_SUBJECT_TYPE)),
                        subjectCursor.getString(subjectCursor.getColumnIndexOrThrow(DatabaseHelper.COL_SUBJECT_COLOR))
                );
                syncSubjectToCloud(s);
            }
            subjectCursor.close();
        }

        // 2. Sync Assignments
        Cursor assignCursor = sqliteDb.query(DatabaseHelper.TABLE_ASSIGNMENTS, null, null, null, null, null, null);
        if (assignCursor != null) {
            while (assignCursor.moveToNext()) {
                Assignment a = new Assignment(
                        assignCursor.getInt(assignCursor.getColumnIndexOrThrow(DatabaseHelper.COL_ASSIGN_ID)),
                        assignCursor.getString(assignCursor.getColumnIndexOrThrow(DatabaseHelper.COL_ASSIGN_TITLE)),
                        assignCursor.getString(assignCursor.getColumnIndexOrThrow(DatabaseHelper.COL_ASSIGN_SUBJECT)),
                        assignCursor.getString(assignCursor.getColumnIndexOrThrow(DatabaseHelper.COL_ASSIGN_DEADLINE)),
                        assignCursor.getString(assignCursor.getColumnIndexOrThrow(DatabaseHelper.COL_ASSIGN_PRIORITY)),
                        assignCursor.getString(assignCursor.getColumnIndexOrThrow(DatabaseHelper.COL_ASSIGN_STATUS)),
                        assignCursor.getInt(assignCursor.getColumnIndexOrThrow(DatabaseHelper.COL_ASSIGN_IS_DONE)) == 1
                );
                syncAssignmentToCloud(a);
            }
            assignCursor.close();
        }

        // 3. Sync Schedule Items
        Cursor schedCursor = sqliteDb.query(DatabaseHelper.TABLE_SCHEDULE, null, null, null, null, null, null);
        if (schedCursor != null) {
            while (schedCursor.moveToNext()) {
                ScheduleItem item = new ScheduleItem(
                        schedCursor.getInt(schedCursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCHED_ID)),
                        schedCursor.getString(schedCursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCHED_SUBJECT)),
                        schedCursor.getString(schedCursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCHED_START)),
                        schedCursor.getString(schedCursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCHED_END)),
                        schedCursor.getString(schedCursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCHED_ROOM)),
                        schedCursor.getString(schedCursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCHED_TEACHER)),
                        schedCursor.getString(schedCursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCHED_STATUS)),
                        schedCursor.getString(schedCursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCHED_COLOR)),
                        schedCursor.getString(schedCursor.getColumnIndexOrThrow(DatabaseHelper.COL_SCHED_DAY))
                );
                syncScheduleItemToCloud(item);
            }
            schedCursor.close();
        }
        Log.d(TAG, "Triggered full local-to-cloud push sync background tasks.");
    }

    /**
     * Pull all data from Cloud Firestore for the logged-in user and write it locally.
     * Overwrites or merges matching rows by original IDs.
     */
    public void pullAllCloudDataToLocal(final Runnable onComplete) {
        final String email = getLoggedInUserEmail();
        if (email.isEmpty()) {
            if (onComplete != null) onComplete.run();
            return;
        }

        final DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
        final SQLiteDatabase sqliteDb = dbHelper.getWritableDatabase();

        // 1. Pull Subjects
        db.collection("users").document(email).collection("subjects")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    sqliteDb.beginTransaction();
                    try {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            ContentValues cv = new ContentValues();
                            cv.put(DatabaseHelper.COL_SUBJECT_ID, doc.getLong("id").intValue());
                            cv.put(DatabaseHelper.COL_SUBJECT_NAME, doc.getString("name"));
                            cv.put(DatabaseHelper.COL_SUBJECT_TEACHER, doc.getString("teacher"));
                            cv.put(DatabaseHelper.COL_SUBJECT_CREDITS, doc.getLong("credits").intValue());
                            cv.put(DatabaseHelper.COL_SUBJECT_SEMESTER, doc.getString("semester"));
                            cv.put(DatabaseHelper.COL_SUBJECT_TYPE, doc.getString("type"));
                            cv.put(DatabaseHelper.COL_SUBJECT_COLOR, doc.getString("color_tag"));

                            sqliteDb.insertWithOnConflict(DatabaseHelper.TABLE_SUBJECTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                        }
                        sqliteDb.setTransactionSuccessful();
                        Log.d(TAG, "Pulled subjects from cloud and synced locally.");
                    } catch (Exception e) {
                        Log.e(TAG, "Error saving cloud subjects", e);
                    } finally {
                        sqliteDb.endTransaction();
                    }

                    // 2. Pull Assignments
                    db.collection("users").document(email).collection("assignments")
                            .get()
                            .addOnSuccessListener(queryDocumentSnapshots2 -> {
                                sqliteDb.beginTransaction();
                                try {
                                    for (DocumentSnapshot doc : queryDocumentSnapshots2.getDocuments()) {
                                        ContentValues cv = new ContentValues();
                                        cv.put(DatabaseHelper.COL_ASSIGN_ID, doc.getLong("id").intValue());
                                        cv.put(DatabaseHelper.COL_ASSIGN_TITLE, doc.getString("title"));
                                        cv.put(DatabaseHelper.COL_ASSIGN_SUBJECT, doc.getString("subject_name"));
                                        cv.put(DatabaseHelper.COL_ASSIGN_DEADLINE, doc.getString("deadline"));
                                        cv.put(DatabaseHelper.COL_ASSIGN_PRIORITY, doc.getString("priority"));
                                        cv.put(DatabaseHelper.COL_ASSIGN_STATUS, doc.getString("status"));
                                        cv.put(DatabaseHelper.COL_ASSIGN_IS_DONE, doc.getBoolean("is_done") ? 1 : 0);

                                        sqliteDb.insertWithOnConflict(DatabaseHelper.TABLE_ASSIGNMENTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                                    }
                                    sqliteDb.setTransactionSuccessful();
                                    Log.d(TAG, "Pulled assignments from cloud and synced locally.");
                                } catch (Exception e) {
                                    Log.e(TAG, "Error saving cloud assignments", e);
                                } finally {
                                    sqliteDb.endTransaction();
                                }

                                // 3. Pull Schedule Items
                                db.collection("users").document(email).collection("schedule_items")
                                        .get()
                                        .addOnSuccessListener(queryDocumentSnapshots3 -> {
                                            sqliteDb.beginTransaction();
                                            try {
                                                for (DocumentSnapshot doc : queryDocumentSnapshots3.getDocuments()) {
                                                    ContentValues cv = new ContentValues();
                                                    cv.put(DatabaseHelper.COL_SCHED_ID, doc.getLong("id").intValue());
                                                    cv.put(DatabaseHelper.COL_SCHED_SUBJECT, doc.getString("subject_name"));
                                                    cv.put(DatabaseHelper.COL_SCHED_START, doc.getString("start_time"));
                                                    cv.put(DatabaseHelper.COL_SCHED_END, doc.getString("end_time"));
                                                    cv.put(DatabaseHelper.COL_SCHED_ROOM, doc.getString("room"));
                                                    cv.put(DatabaseHelper.COL_SCHED_TEACHER, doc.getString("teacher"));
                                                    cv.put(DatabaseHelper.COL_SCHED_STATUS, doc.getString("status"));
                                                    cv.put(DatabaseHelper.COL_SCHED_COLOR, doc.getString("color_tag"));
                                                    cv.put(DatabaseHelper.COL_SCHED_DAY, doc.getString("day_of_week"));

                                                    sqliteDb.insertWithOnConflict(DatabaseHelper.TABLE_SCHEDULE, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
                                                }
                                                sqliteDb.setTransactionSuccessful();
                                                Log.d(TAG, "Pulled schedule items from cloud and synced locally.");
                                            } catch (Exception e) {
                                                Log.e(TAG, "Error saving cloud schedules", e);
                                            } finally {
                                                sqliteDb.endTransaction();
                                            }

                                            // Complete callback
                                            if (onComplete != null) {
                                                onComplete.run();
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.e(TAG, "Failed to pull schedules", e);
                                            if (onComplete != null) onComplete.run();
                                        });
                                })
                            .addOnFailureListener(e -> {
                                Log.e(TAG, "Failed to pull assignments", e);
                                if (onComplete != null) onComplete.run();
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to pull subjects", e);
                    if (onComplete != null) onComplete.run();
                });
    }
}
