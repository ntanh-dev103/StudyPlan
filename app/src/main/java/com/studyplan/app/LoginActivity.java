package com.studyplan.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final String PREF_NAME = "StudyPlanPrefs";
    private static final int RC_GOOGLE_SIGN_IN = 9001;

    // ─── Views ───────────────────────────────────────────────────
    private TextView tabLogin, tabRegister;
    private View indicatorLogin, indicatorRegister;
    private TextView btnLogin;
    private EditText etEmail, etPassword, etConfirmPassword, etFullname;
    private ImageView btnTogglePassword;
    private TextView tvErrorEmail, tvErrorPassword, tvErrorConfirm;
    private LinearLayout layoutFullname, layoutConfirmPassword;
    private TextView tvForgotPassword;
    private LinearLayout btnGoogle;
    private LinearLayout layoutDividerOr;
    private LinearLayout layoutGoogleLoading;

    // ─── Auth ────────────────────────────────────────────────────
    private boolean isLoginMode = true;
    private boolean isPasswordVisible = false;
    private UserDAO userDAO;
    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        userDAO = new UserDAO(this);

        // Auto-login nếu đã có session hợp lệ
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        if (prefs.getBoolean("is_logged_in", false)) {
            navigateToMain();
            return;
        }

        setContentView(R.layout.activity_login);
        bindViews();
        initGoogleSignIn();
        setupListeners();
        startFadeInAnimation();
    }

    // ─── Bind Views ──────────────────────────────────────────────

    private void bindViews() {
        tabLogin = findViewById(R.id.tab_login);
        tabRegister = findViewById(R.id.tab_register);
        indicatorLogin = findViewById(R.id.indicator_login);
        indicatorRegister = findViewById(R.id.indicator_register);
        btnLogin = findViewById(R.id.btn_login);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        etFullname = findViewById(R.id.et_fullname);
        btnTogglePassword = findViewById(R.id.btn_toggle_password);
        tvErrorEmail = findViewById(R.id.tv_error_email);
        tvErrorPassword = findViewById(R.id.tv_error_password);
        tvErrorConfirm = findViewById(R.id.tv_error_confirm);
        layoutFullname = findViewById(R.id.layout_fullname);
        layoutConfirmPassword = findViewById(R.id.layout_confirm_password);
        tvForgotPassword = findViewById(R.id.tv_forgot_password);
        btnGoogle = findViewById(R.id.btn_google);
        layoutDividerOr = findViewById(R.id.layout_divider_or);
        layoutGoogleLoading = findViewById(R.id.layout_google_loading);
    }

    // ─── Google Sign-In Init ─────────────────────────────────────
    /**
     * Khởi tạo GoogleSignInClient.
     * - Nếu Firebase đã liên kết (client ID thật): requestIdToken → Firebase auth thật.
     * - Nếu Firebase chưa liên kết (placeholder): requestEmail chỉ → fallback local account.
     * Nút Google LUÔN hiển thị trong cả 2 trường hợp.
     */
    private void initGoogleSignIn() {
        firebaseAuth = FirebaseAuth.getInstance();

        String clientId = getClientIdSafe();
        boolean isFirebaseConfigured = isValidClientId(clientId);

        GoogleSignInOptions.Builder gsoBuilder = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile();

        if (isFirebaseConfigured) {
            // Firebase đã cấu hình → request ID token để xác thực với Firebase Auth
            gsoBuilder.requestIdToken(clientId);
            Log.i(TAG, "Google Sign-In: Chế độ Firebase thật");
        } else {
            Log.i(TAG, "Google Sign-In: Chế độ demo (Firebase chưa liên kết)");
        }

        googleSignInClient = GoogleSignIn.getClient(this, gsoBuilder.build());

        // Nút Google LUÔN hiển thị — không ẩn
        if (btnGoogle != null) btnGoogle.setVisibility(View.VISIBLE);
        if (layoutDividerOr != null) layoutDividerOr.setVisibility(View.VISIBLE);
    }

    // ─── Setup Listeners ─────────────────────────────────────────

    private void setupListeners() {
        // Tab switching
        if (tabLogin != null) tabLogin.setOnClickListener(v -> switchToLoginMode());
        if (tabRegister != null) tabRegister.setOnClickListener(v -> switchToRegisterMode());

        // Toggle password visibility
        if (btnTogglePassword != null) {
            btnTogglePassword.setOnClickListener(v -> {
                isPasswordVisible = !isPasswordVisible;
                if (isPasswordVisible) {
                    etPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                    btnTogglePassword.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                } else {
                    etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    btnTogglePassword.setImageResource(android.R.drawable.ic_menu_view);
                }
                etPassword.setSelection(etPassword.getText().length());
            });
        }

        // Email/Password login button
        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                clearErrors();
                if (isLoginMode) handleEmailLogin();
                else handleEmailRegister();
            });
        }

        // Google Sign-In button
        if (btnGoogle != null) {
            btnGoogle.setOnClickListener(v -> handleGoogleSignIn());
        }

        // Forgot password
        if (tvForgotPassword != null) {
            tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    // ═══ GOOGLE SIGN-IN FLOW ═══════════════════════════════════════
    // ═══════════════════════════════════════════════════════════════

    /**
     * Bước 1: Mở Google account picker.
     * Kết quả trả về qua onActivityResult().
     */
    private void handleGoogleSignIn() {
        setGoogleLoading(true);
        // Sign out trước để luôn hiện account picker (không auto sign-in)
        googleSignInClient.signOut().addOnCompleteListener(task -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
        });
    }

    /**
     * Bước 2: Nhận kết quả từ Google account picker.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                // Google Sign-In thành công → xử lý account
                processGoogleAccount(account);
            } catch (ApiException e) {
                setGoogleLoading(false);
                handleGoogleSignInError(e);
            }
        }
    }

    /**
     * Bước 3: Xử lý GoogleSignInAccount nhận được.
     * Trích xuất đầy đủ thông tin: displayName, email, googleId, photoUrl, idToken
     * - Nếu có idToken → Firebase đã liên kết → dùng Firebase auth.
     * - Nếu không có idToken → Firebase chưa liên kết → tạo local account.
     *
     * Trong cả hai trường hợp, thông tin Google đều được lưu vào SQLite.
     */
    private void processGoogleAccount(GoogleSignInAccount account) {
        String displayName = account.getDisplayName();
        String email = account.getEmail();
        String idToken = account.getIdToken();
        String googleId = account.getId();  // Google unique user ID
        String photoUrl = (account.getPhotoUrl() != null) ? account.getPhotoUrl().toString() : "";

        if (displayName == null || displayName.isEmpty()) displayName = "Người dùng Google";
        if (email == null || email.isEmpty()) email = "";

        Log.i(TAG, "Google account: " + email
                + " | googleId: " + googleId
                + " | idToken: " + (idToken != null ? "có" : "không")
                + " | photoUrl: " + (photoUrl.isEmpty() ? "không" : "có"));

        if (idToken != null) {
            // Firebase đã liên kết → xác thực với Firebase Auth
            firebaseAuthWithGoogle(idToken, displayName, email, googleId, photoUrl);
        } else {
            // Firebase chưa liên kết → tạo/lấy local account từ thông tin Google
            createLocalGoogleAccount(displayName, email, googleId, photoUrl);
        }
    }

    /**
     * Bước 3a: Xác thực với Firebase Auth dùng Google credential.
     * Sau khi Firebase xác thực thành công, LƯU đầy đủ vào SQLite local.
     */
    private void firebaseAuthWithGoogle(String idToken, String displayName, String email,
                                         String googleId, String photoUrl) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    setGoogleLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

                        // Ưu tiên lấy thông tin từ Firebase user (có thể mới hơn)
                        String name = (firebaseUser != null && firebaseUser.getDisplayName() != null)
                                ? firebaseUser.getDisplayName() : displayName;
                        String mail = (firebaseUser != null && firebaseUser.getEmail() != null)
                                ? firebaseUser.getEmail() : email;
                        String gId = (firebaseUser != null && firebaseUser.getUid() != null)
                                ? firebaseUser.getUid() : googleId;
                        String photo = (firebaseUser != null && firebaseUser.getPhotoUrl() != null)
                                ? firebaseUser.getPhotoUrl().toString() : photoUrl;

                        // ★ LƯU VÀO SQLITE DATABASE ★
                        long userId = userDAO.registerOrGetGoogleUser(name, mail, gId, photo);
                        Log.i(TAG, "Firebase auth OK → SQLite user ID=" + userId);

                        // Lưu session vào SharedPreferences
                        saveSession(mail, name, "google_firebase", photo);

                        Toast.makeText(this,
                                "✅ Đăng nhập Google thành công!\nChào, " + name,
                                Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        String err = task.getException() != null ? task.getException().getMessage() : "Không rõ";
                        Log.e(TAG, "Firebase Auth failed: " + err);
                        // Firebase lỗi → fallback tạo local account
                        createLocalGoogleAccount(displayName, email, googleId, photoUrl);
                    }
                });
    }

    /**
     * Bước 3b: Fallback — tạo account local từ thông tin Google.
     * Dùng khi Firebase chưa được liên kết hoặc Firebase auth thất bại.
     * Vẫn lưu đầy đủ google_id, photo_url vào SQLite.
     */
    private void createLocalGoogleAccount(String displayName, String email,
                                           String googleId, String photoUrl) {
        setGoogleLoading(false);

        if (email == null || email.isEmpty()) {
            // Trường hợp hiếm: không lấy được email → hiện dialog nhập thủ công
            showGoogleFallbackDialog(displayName);
            return;
        }

        // ★ LƯU VÀO SQLITE DATABASE ★
        long userId = userDAO.registerOrGetGoogleUser(displayName, email, googleId, photoUrl);
        if (userId > 0) {
            Log.i(TAG, "Local Google account → SQLite user ID=" + userId);
            saveSession(email, displayName, "google_local", photoUrl);
            Toast.makeText(this,
                    "✅ Đăng nhập thành công!\nChào, " + displayName,
                    Toast.LENGTH_LONG).show();
            navigateToMain();
        } else {
            Toast.makeText(this, "Lỗi tạo tài khoản. Vui lòng thử lại.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Xử lý lỗi Google Sign-In.
     */
    private void handleGoogleSignInError(ApiException e) {
        int code = e.getStatusCode();
        Log.e(TAG, "Google Sign-In error code: " + code);

        switch (code) {
            case 4: // SIGN_IN_CANCELLED
                Toast.makeText(this, "Đã hủy đăng nhập Google", Toast.LENGTH_SHORT).show();
                break;
            case 7: // DEVELOPER_ERROR
                // SHA-1 hoặc Client ID chưa đăng ký trong Firebase
                showFirebaseNotLinkedDialog();
                break;
            case 12501: // Người dùng nhấn back
                Toast.makeText(this, "Đã hủy đăng nhập Google", Toast.LENGTH_SHORT).show();
                break;
            case 10: // DEVELOPER_ERROR (một số device dùng code 10)
                showFirebaseNotLinkedDialog();
                break;
            default:
                // Lỗi khác → hiện dialog fallback nhập thủ công
                Log.w(TAG, "Google Sign-In error: " + e.getMessage());
                showGoogleFallbackDialog("");
                break;
        }
    }

    /**
     * Dialog fallback khi Google Sign-In lỗi do Firebase chưa liên kết.
     * Cho phép user nhập tên + email Google thủ công để tạo local account.
     * Vẫn lưu vào SQLite bảng users.
     */
    private void showGoogleFallbackDialog(String prefillName) {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_google_fallback, null);
        EditText etName = dialogView.findViewById(R.id.et_google_name);
        EditText etEmailDialog = dialogView.findViewById(R.id.et_google_email);
        TextView tvErr = dialogView.findViewById(R.id.tv_google_error);

        if (prefillName != null && !prefillName.isEmpty()) {
            etName.setText(prefillName);
        }

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.RoundedDialog)
                .setTitle("🔐 Đăng nhập Google")
                .setView(dialogView)
                .setPositiveButton("Tiếp tục", null)
                .setNegativeButton("Hủy", null)
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String emailInput = etEmailDialog.getText().toString().trim();
            boolean valid = true;

            if (name.isEmpty()) {
                if (tvErr != null) { tvErr.setText("⚠ Vui lòng nhập tên"); tvErr.setVisibility(View.VISIBLE); }
                valid = false;
            } else if (!Patterns.EMAIL_ADDRESS.matcher(emailInput).matches()) {
                if (tvErr != null) { tvErr.setText("⚠ Email không hợp lệ"); tvErr.setVisibility(View.VISIBLE); }
                valid = false;
            }

            if (!valid) return;

            // ★ LƯU VÀO SQLITE DATABASE (fallback, no googleId/photoUrl) ★
            long userId = userDAO.registerOrGetGoogleUser(name, emailInput);
            if (userId > 0) {
                saveSession(emailInput, name, "google_local", "");
                dialog.dismiss();
                Toast.makeText(this, "✅ Đăng nhập Google thành công!\nChào, " + name, Toast.LENGTH_SHORT).show();
                navigateToMain();
            } else {
                if (tvErr != null) { tvErr.setText("⚠ Lỗi tạo tài khoản"); tvErr.setVisibility(View.VISIBLE); }
            }
        });
    }

    /**
     * Dialog thông báo Firebase chưa liên kết.
     */
    private void showFirebaseNotLinkedDialog() {
        new AlertDialog.Builder(this, R.style.RoundedDialog)
                .setTitle("⚙️ Firebase chưa liên kết")
                .setMessage(
                        "Google Sign-In cần Firebase được liên kết.\n\n" +
                        "Để sử dụng đầy đủ:\n" +
                        "1. Tạo Firebase project tại console.firebase.google.com\n" +
                        "2. Thêm app với package: com.studyplan.app\n" +
                        "3. Bật Google trong Authentication\n" +
                        "4. Thay google-services.json thật\n\n" +
                        "Hoặc dùng nút bên dưới để đăng nhập thủ công bằng Google account:"
                )
                .setPositiveButton("Nhập thông tin Google", (d, w) -> showGoogleFallbackDialog(""))
                .setNegativeButton("Đóng", null)
                .show();
    }

    // ═══════════════════════════════════════════════════════════════
    // ═══ EMAIL LOGIN / REGISTER ═══════════════════════════════════
    // ═══════════════════════════════════════════════════════════════

    private void handleEmailLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        boolean valid = true;

        if (TextUtils.isEmpty(email)) {
            showError(tvErrorEmail, getString(R.string.error_email_required));
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(tvErrorEmail, getString(R.string.error_email_invalid));
            valid = false;
        }

        if (TextUtils.isEmpty(password)) {
            showError(tvErrorPassword, getString(R.string.error_password_required));
            valid = false;
        } else if (password.length() < 6) {
            showError(tvErrorPassword, getString(R.string.error_password_short));
            valid = false;
        }

        if (!valid) return;

        if (userDAO.authenticate(email, password)) {
            String fullname = userDAO.getFullnameByEmail(email);
            String photoUrl = userDAO.getPhotoUrlByEmail(email);
            if (fullname == null || fullname.isEmpty()) fullname = "Người dùng";
            saveSession(email, fullname, "email", photoUrl != null ? photoUrl : "");
            Toast.makeText(this, "Chào mừng, " + fullname + "! 👋", Toast.LENGTH_SHORT).show();
            navigateToMain();
        } else {
            if (!userDAO.isEmailExists(email)) {
                showError(tvErrorEmail, "Email chưa được đăng ký");
            } else if (userDAO.isGoogleOnlyAccount(email)) {
                // Tài khoản Google không có mật khẩu → gợi ý đăng nhập bằng Google
                showError(tvErrorPassword,
                        "Tài khoản này dùng Google Sign-In. Hãy nhấn \"Tiếp tục với Google\".");
            } else {
                showError(tvErrorPassword, "Sai mật khẩu. Vui lòng thử lại.");
            }
        }
    }

    private void handleEmailRegister() {
        String fullname = etFullname.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();
        boolean valid = true;

        if (TextUtils.isEmpty(fullname)) {
            Toast.makeText(this, getString(R.string.error_name_required), Toast.LENGTH_SHORT).show();
            valid = false;
        }
        if (TextUtils.isEmpty(email)) {
            showError(tvErrorEmail, getString(R.string.error_email_required));
            valid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError(tvErrorEmail, getString(R.string.error_email_invalid));
            valid = false;
        }
        if (TextUtils.isEmpty(password)) {
            showError(tvErrorPassword, getString(R.string.error_password_required));
            valid = false;
        } else if (password.length() < 6) {
            showError(tvErrorPassword, getString(R.string.error_password_short));
            valid = false;
        }
        if (!TextUtils.isEmpty(password) && !password.equals(confirmPassword)) {
            showError(tvErrorConfirm, getString(R.string.error_password_mismatch));
            valid = false;
        }

        if (!valid) return;

        long userId = userDAO.register(fullname, email, password);
        if (userId > 0) {
            Toast.makeText(this,
                    "✅ Đăng ký thành công!\nEmail: " + email + "\n\nHãy đăng nhập!",
                    Toast.LENGTH_LONG).show();
            etFullname.setText("");
            etConfirmPassword.setText("");
            switchToLoginMode();
        } else {
            showError(tvErrorEmail, "Email này đã được đăng ký. Hãy dùng email khác.");
        }
    }

    // ─── Tab Switching ───────────────────────────────────────────

    private void switchToLoginMode() {
        isLoginMode = true;
        tabLogin.setTextColor(getResources().getColor(R.color.blue_primary));
        tabRegister.setTextColor(getResources().getColor(R.color.text_hint));
        indicatorLogin.setBackgroundColor(getResources().getColor(R.color.blue_primary));
        indicatorRegister.setBackgroundColor(getResources().getColor(R.color.divider));
        layoutFullname.setVisibility(View.GONE);
        layoutConfirmPassword.setVisibility(View.GONE);
        tvForgotPassword.setVisibility(View.VISIBLE);
        btnLogin.setText(getString(R.string.btn_login));
        clearErrors();
    }

    private void switchToRegisterMode() {
        isLoginMode = false;
        tabRegister.setTextColor(getResources().getColor(R.color.blue_primary));
        tabLogin.setTextColor(getResources().getColor(R.color.text_hint));
        indicatorRegister.setBackgroundColor(getResources().getColor(R.color.blue_primary));
        indicatorLogin.setBackgroundColor(getResources().getColor(R.color.divider));
        layoutFullname.setVisibility(View.VISIBLE);
        layoutConfirmPassword.setVisibility(View.VISIBLE);
        tvForgotPassword.setVisibility(View.GONE);
        btnLogin.setText(getString(R.string.btn_register));
        clearErrors();
    }

    // ─── Session ─────────────────────────────────────────────────

    /**
     * Lưu session đăng nhập vào SharedPreferences.
     * Bao gồm cả thông tin photo_url cho Google user.
     */
    private void saveSession(String email, String fullname, String method, String photoUrl) {
        SharedPreferences.Editor editor = getSharedPreferences(PREF_NAME, MODE_PRIVATE).edit();
        editor.putBoolean("is_logged_in", true);
        editor.putString("user_email", email);
        editor.putString("user_name", fullname);
        editor.putString("login_method", method);
        if (photoUrl != null && !photoUrl.isEmpty()) {
            editor.putString("user_photo_url", photoUrl);
        }
        editor.apply();
    }

    // Backward-compatible overload
    private void saveSession(String email, String fullname, String method) {
        saveSession(email, fullname, method, "");
    }

    private void navigateToMain() {
        startActivity(new Intent(this, MainActivity.class));
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    // ─── Forgot Password Dialog ───────────────────────────────────

    private void showForgotPasswordDialog() {
        new AlertDialog.Builder(this, R.style.RoundedDialog)
                .setTitle("Quên mật khẩu")
                .setMessage("Tính năng khôi phục mật khẩu qua email đang được phát triển.\n\n" +
                        "Vui lòng tạo tài khoản mới hoặc dùng đăng nhập Google.")
                .setPositiveButton("Đã hiểu", null)
                .show();
    }

    // ─── Helpers ─────────────────────────────────────────────────

    /** Kiểm tra client ID có phải thật không. */
    private boolean isValidClientId(String clientId) {
        return clientId != null
                && clientId.endsWith(".apps.googleusercontent.com")
                && !clientId.startsWith("YOUR_")
                && !clientId.contains("dummy")
                && !clientId.contains("placeholder")
                && clientId.length() > 40;
    }

    /** Lấy Web Client ID từ strings.xml an toàn (không crash nếu thiếu). */
    private String getClientIdSafe() {
        try {
            return getString(R.string.default_web_client_id);
        } catch (Exception e) {
            Log.w(TAG, "Không tìm thấy default_web_client_id");
            return "";
        }
    }

    /** Hiện/ẩn loading indicator cho Google Sign-In. */
    private void setGoogleLoading(boolean loading) {
        if (btnGoogle != null) btnGoogle.setAlpha(loading ? 0.6f : 1.0f);
        if (layoutGoogleLoading != null) {
            layoutGoogleLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        }
    }

    private void showError(TextView tv, String msg) {
        if (tv != null) { tv.setText(msg); tv.setVisibility(View.VISIBLE); }
    }

    private void clearErrors() {
        if (tvErrorEmail != null) tvErrorEmail.setVisibility(View.GONE);
        if (tvErrorPassword != null) tvErrorPassword.setVisibility(View.GONE);
        if (tvErrorConfirm != null) tvErrorConfirm.setVisibility(View.GONE);
    }

    private void startFadeInAnimation() {
        View preview = findViewById(R.id.login_preview);
        if (preview != null) {
            AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
            fadeIn.setDuration(700);
            preview.startAnimation(fadeIn);
        }
    }
}
