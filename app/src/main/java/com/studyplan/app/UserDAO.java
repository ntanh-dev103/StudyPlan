package com.studyplan.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Data Access Object for user authentication operations.
 * Handles registration, login verification, and user profile queries.
 * Supports both email/password and Google Sign-In authentication.
 * Passwords are hashed with SHA-256 before storage.
 */
public class UserDAO {

    private static final String TAG = "UserDAO";
    private final DatabaseHelper dbHelper;
    private final Context context;

    public UserDAO(Context context) {
        this.context = context.getApplicationContext();
        dbHelper = DatabaseHelper.getInstance(this.context);
    }

    // ─── Email/Password Registration ────────────────────────────

    /**
     * Register a new user with email/password. Returns the new user ID, or -1 if email already exists.
     */
    public long register(String fullname, String email, String password) {
        // Check if email already exists
        if (isEmailExists(email)) {
            return -1;
        }

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_USER_FULLNAME, fullname);
        cv.put(DatabaseHelper.COL_USER_EMAIL, email.toLowerCase().trim());
        cv.put(DatabaseHelper.COL_USER_PASSWORD, hashPassword(password));
        cv.put(DatabaseHelper.COL_USER_LOGIN_METHOD, "email");

        long id = db.insert(DatabaseHelper.TABLE_USERS, null, cv);
        if (id > 0) {
            // Synchronize new user to Firebase Firestore
            try {
                FirestoreHelper.getInstance(context).syncUserToCloud(fullname, email, hashPassword(password));
            } catch (Exception e) {
                Log.w(TAG, "Failed to sync user to cloud (Firebase may not be configured)", e);
            }
        }
        return id;
    }

    // ─── Google Sign-In Registration ────────────────────────────

    /**
     * Register or retrieve a Google Sign-In user.
     * Saves Google account info (googleId, photoUrl) into the local SQLite database.
     *
     * Flow:
     * 1. If email already exists → update Google fields and return existing user ID (account linking)
     * 2. If email is new → create a new user with Google info, no password required
     *
     * @param fullname  Display name from Google account
     * @param email     Email from Google account
     * @param googleId  Google unique user ID (sub claim from ID token)
     * @param photoUrl  Profile photo URL from Google account (nullable)
     * @return The user ID (existing or newly created), or -1 on error
     */
    public long registerOrGetGoogleUser(String fullname, String email, String googleId, String photoUrl) {
        String normalizedEmail = email.toLowerCase().trim();

        // Check if user already exists
        if (isEmailExists(normalizedEmail)) {
            // Update existing user with Google info (account linking)
            updateGoogleInfo(normalizedEmail, googleId, photoUrl, fullname);
            Log.i(TAG, "Google user linked with existing account: " + normalizedEmail);
            return getUserIdByEmail(normalizedEmail);
        }

        // Create new user with Google auth (no password needed)
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_USER_FULLNAME, fullname);
        cv.put(DatabaseHelper.COL_USER_EMAIL, normalizedEmail);
        cv.put(DatabaseHelper.COL_USER_PASSWORD, "GOOGLE_AUTH"); // Marker: no real password
        cv.put(DatabaseHelper.COL_USER_GOOGLE_ID, googleId != null ? googleId : "");
        cv.put(DatabaseHelper.COL_USER_PHOTO_URL, photoUrl != null ? photoUrl : "");
        cv.put(DatabaseHelper.COL_USER_LOGIN_METHOD, "google");

        long id = db.insert(DatabaseHelper.TABLE_USERS, null, cv);
        if (id > 0) {
            Log.i(TAG, "New Google user created: " + normalizedEmail + " (ID=" + id + ")");
            // Synchronize new Google user to Firebase Firestore
            try {
                FirestoreHelper.getInstance(context).syncUserToCloud(fullname, normalizedEmail, "GOOGLE_AUTH");
            } catch (Exception e) {
                Log.w(TAG, "Failed to sync Google user to cloud", e);
            }
        } else {
            Log.e(TAG, "Failed to insert Google user: " + normalizedEmail);
        }
        return id;
    }

    /**
     * Backward-compatible overload (without googleId / photoUrl).
     * Used when Google Sign-In fails to provide full info (fallback dialog).
     */
    public long registerOrGetGoogleUser(String fullname, String email) {
        return registerOrGetGoogleUser(fullname, email, null, null);
    }

    /**
     * Update existing user with Google account info.
     * This links a local email account with a Google account.
     */
    private void updateGoogleInfo(String email, String googleId, String photoUrl, String fullname) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();

        if (googleId != null && !googleId.isEmpty()) {
            cv.put(DatabaseHelper.COL_USER_GOOGLE_ID, googleId);
        }
        if (photoUrl != null && !photoUrl.isEmpty()) {
            cv.put(DatabaseHelper.COL_USER_PHOTO_URL, photoUrl);
        }
        // Update fullname if the current one is empty/default
        String currentName = getFullnameByEmail(email);
        if (currentName == null || currentName.isEmpty() || currentName.equals("Người dùng Google")) {
            cv.put(DatabaseHelper.COL_USER_FULLNAME, fullname);
        }
        // Mark as google login method (or google_linked if was email before)
        String currentMethod = getLoginMethodByEmail(email);
        if ("email".equals(currentMethod)) {
            cv.put(DatabaseHelper.COL_USER_LOGIN_METHOD, "google_linked");
        } else {
            cv.put(DatabaseHelper.COL_USER_LOGIN_METHOD, "google");
        }

        if (cv.size() > 0) {
            int rows = db.update(DatabaseHelper.TABLE_USERS, cv,
                    DatabaseHelper.COL_USER_EMAIL + " = ?",
                    new String[]{email.toLowerCase().trim()});
            Log.d(TAG, "Updated Google info for " + email + ": " + rows + " row(s)");
        }
    }

    // ─── Login ──────────────────────────────────────────────────

    /**
     * Authenticate a user with email and password.
     * Returns true if credentials match a record in the database.
     * Note: Google-only users (password = "GOOGLE_AUTH") cannot login via email/password.
     */
    public boolean authenticate(String email, String password) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String hashedPassword = hashPassword(password);

        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COL_USER_ID},
                DatabaseHelper.COL_USER_EMAIL + " = ? AND " +
                        DatabaseHelper.COL_USER_PASSWORD + " = ? AND " +
                        DatabaseHelper.COL_USER_PASSWORD + " != 'GOOGLE_AUTH'",
                new String[]{email.toLowerCase().trim(), hashedPassword},
                null, null, null);

        boolean authenticated = false;
        if (cursor != null) {
            authenticated = cursor.getCount() > 0;
            cursor.close();
        }
        return authenticated;
    }

    /**
     * Check if a user is a Google-only account (no password set).
     * If true, suggest them to login via Google instead.
     */
    public boolean isGoogleOnlyAccount(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COL_USER_PASSWORD},
                DatabaseHelper.COL_USER_EMAIL + " = ?",
                new String[]{email.toLowerCase().trim()},
                null, null, null);

        boolean isGoogleOnly = false;
        if (cursor != null && cursor.moveToFirst()) {
            String pwd = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_PASSWORD));
            isGoogleOnly = "GOOGLE_AUTH".equals(pwd);
            cursor.close();
        }
        return isGoogleOnly;
    }

    // ─── Query ──────────────────────────────────────────────────

    /**
     * Check if an email is already registered.
     */
    public boolean isEmailExists(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COL_USER_ID},
                DatabaseHelper.COL_USER_EMAIL + " = ?",
                new String[]{email.toLowerCase().trim()},
                null, null, null);

        boolean exists = false;
        if (cursor != null) {
            exists = cursor.getCount() > 0;
            cursor.close();
        }
        return exists;
    }

    /**
     * Get user's full name by email.
     */
    public String getFullnameByEmail(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COL_USER_FULLNAME},
                DatabaseHelper.COL_USER_EMAIL + " = ?",
                new String[]{email.toLowerCase().trim()},
                null, null, null);

        String fullname = null;
        if (cursor != null && cursor.moveToFirst()) {
            fullname = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_FULLNAME));
            cursor.close();
        }
        return fullname;
    }

    /**
     * Get user ID by email.
     */
    public int getUserIdByEmail(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COL_USER_ID},
                DatabaseHelper.COL_USER_EMAIL + " = ?",
                new String[]{email.toLowerCase().trim()},
                null, null, null);

        int userId = -1;
        if (cursor != null && cursor.moveToFirst()) {
            userId = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COL_USER_ID));
            cursor.close();
        }
        return userId;
    }

    /**
     * Get login method for a user (email, google, google_linked, google_firebase).
     */
    public String getLoginMethodByEmail(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COL_USER_LOGIN_METHOD},
                DatabaseHelper.COL_USER_EMAIL + " = ?",
                new String[]{email.toLowerCase().trim()},
                null, null, null);

        String method = "email"; // default
        if (cursor != null && cursor.moveToFirst()) {
            int colIdx = cursor.getColumnIndex(DatabaseHelper.COL_USER_LOGIN_METHOD);
            if (colIdx >= 0) {
                String val = cursor.getString(colIdx);
                if (val != null) method = val;
            }
            cursor.close();
        }
        return method;
    }

    /**
     * Get Google photo URL for a user.
     */
    public String getPhotoUrlByEmail(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COL_USER_PHOTO_URL},
                DatabaseHelper.COL_USER_EMAIL + " = ?",
                new String[]{email.toLowerCase().trim()},
                null, null, null);

        String photoUrl = null;
        if (cursor != null && cursor.moveToFirst()) {
            int colIdx = cursor.getColumnIndex(DatabaseHelper.COL_USER_PHOTO_URL);
            if (colIdx >= 0) {
                photoUrl = cursor.getString(colIdx);
            }
            cursor.close();
        }
        return photoUrl;
    }

    /**
     * Get Google ID for a user.
     */
    public String getGoogleIdByEmail(String email) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(
                DatabaseHelper.TABLE_USERS,
                new String[]{DatabaseHelper.COL_USER_GOOGLE_ID},
                DatabaseHelper.COL_USER_EMAIL + " = ?",
                new String[]{email.toLowerCase().trim()},
                null, null, null);

        String googleId = null;
        if (cursor != null && cursor.moveToFirst()) {
            int colIdx = cursor.getColumnIndex(DatabaseHelper.COL_USER_GOOGLE_ID);
            if (colIdx >= 0) {
                googleId = cursor.getString(colIdx);
            }
            cursor.close();
        }
        return googleId;
    }

    /**
     * Get total number of registered users.
     */
    public int getUserCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_USERS, null);
        int count = 0;
        if (cursor != null && cursor.moveToFirst()) {
            count = cursor.getInt(0);
            cursor.close();
        }
        return count;
    }

    /**
     * Update user's display name.
     */
    public boolean updateFullname(String email, String newFullname) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_USER_FULLNAME, newFullname);
        int rows = db.update(DatabaseHelper.TABLE_USERS, cv,
                DatabaseHelper.COL_USER_EMAIL + " = ?",
                new String[]{email.toLowerCase().trim()});
        return rows > 0;
    }

    /**
     * Update user's photo URL.
     */
    public boolean updatePhotoUrl(String email, String photoUrl) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COL_USER_PHOTO_URL, photoUrl != null ? photoUrl : "");
        int rows = db.update(DatabaseHelper.TABLE_USERS, cv,
                DatabaseHelper.COL_USER_EMAIL + " = ?",
                new String[]{email.toLowerCase().trim()});
        return rows > 0;
    }

    // ─── Password Hashing ───────────────────────────────────────

    /**
     * Hash a password using SHA-256.
     * This ensures passwords are NEVER stored as plain text in the database.
     */
    private String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            // Fallback: return password as-is (should never happen, SHA-256 always available)
            return password;
        }
    }
}
