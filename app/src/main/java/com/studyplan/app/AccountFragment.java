package com.studyplan.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;

public class AccountFragment extends Fragment {

    private static final String PREF_NAME = "StudyPlanPrefs";

    private SubjectDAO subjectDAO;
    private ScheduleDAO scheduleDAO;
    private AssignmentDAO assignmentDAO;

    // Profile card views
    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private TextView tvProfileRole;
    private TextView tvAvatarAccount;   // id: tv_avatar_account

    // Stats views
    private TextView tvStatSubCount;    // id: tv_stat_sub_count
    private TextView tvStatSchedCount;  // id: tv_stat_sched_count
    private TextView tvStatAssignCount; // id: tv_stat_assign_count

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_account, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        subjectDAO = new SubjectDAO(requireContext());
        scheduleDAO = new ScheduleDAO(requireContext());
        assignmentDAO = new AssignmentDAO(requireContext());

        // Bind views
        tvProfileName = view.findViewById(R.id.tv_profile_name);
        tvProfileEmail = view.findViewById(R.id.tv_profile_email);
        tvProfileRole = view.findViewById(R.id.tv_profile_role);
        tvAvatarAccount = view.findViewById(R.id.tv_avatar_account);
        tvStatSubCount = view.findViewById(R.id.tv_stat_sub_count);
        tvStatSchedCount = view.findViewById(R.id.tv_stat_sched_count);
        tvStatAssignCount = view.findViewById(R.id.tv_stat_assign_count);

        SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE);

        // ── Load profile từ SharedPreferences ────────────────────
        String userName = prefs.getString("user_name", "Người dùng");
        String userEmail = prefs.getString("user_email", "");
        String loginMethod = prefs.getString("login_method", "email");

        if (tvProfileName != null) tvProfileName.setText(userName);
        if (tvProfileEmail != null) tvProfileEmail.setText(userEmail.isEmpty() ? "Chưa có email" : userEmail);

        // Hiển thị phương thức đăng nhập ở role
        if (tvProfileRole != null) {
            String currentRole = prefs.getString("user_role", "");
            if (currentRole.isEmpty()) {
                if (loginMethod.contains("google")) {
                    tvProfileRole.setText("🔗 Đăng nhập bằng Google");
                } else {
                    tvProfileRole.setText(getString(R.string.profile_role));
                }
            } else {
                tvProfileRole.setText(currentRole);
            }
        }

        // Avatar: chữ cái đầu tên cuối (tên Việt)
        updateAvatarLetter(tvAvatarAccount, userName);

        // ── Load stats từ DAO ─────────────────────────────────────
        refreshStats();

        // ── Edit profile: mở dialog_edit_profile.xml ─────────────
        View btnEdit = view.findViewById(R.id.btn_edit_profile);
        if (btnEdit != null) {
            btnEdit.setOnClickListener(v -> showEditProfileDialog(prefs));
        }
        // Click avatar cũng mở dialog
        if (tvAvatarAccount != null) {
            tvAvatarAccount.setOnClickListener(v -> showEditProfileDialog(prefs));
        }

        // ── Menu items ────────────────────────────────────────────
        bindMenu(view, R.id.menu_subject, () -> switchTab(R.id.nav_subject));
        bindMenu(view, R.id.menu_schedule, () -> switchTab(R.id.nav_schedule));
        bindMenu(view, R.id.menu_assignment, () -> switchTab(R.id.nav_assignment));

        bindMenu(view, R.id.menu_settings, () ->
                showInfoDialog("⚙️ Cài đặt", "Tính năng đang phát triển.\nSẽ có trong phiên bản tiếp theo!"));

        bindMenu(view, R.id.menu_notification, () -> {
            int late = assignmentDAO.getCountByStatus("late");
            if (late > 0) {
                showInfoDialog("⚠️ Thông báo",
                        "Bạn có " + late + " bài tập đã trễ deadline!\n\nHãy vào tab Bài Tập để kiểm tra.");
            } else {
                showInfoDialog("🔔 Thông báo", "Không có thông báo mới.\nTất cả bài tập đang trong hạn ✅");
            }
        });

        bindMenu(view, R.id.menu_about, () -> {
            String name = prefs.getString("user_name", "N/A");
            String email = prefs.getString("user_email", "N/A");
            String method = prefs.getString("login_method", "email");
            String methodLabel;
            switch (method) {
                case "google_firebase": methodLabel = "Google (Firebase)"; break;
                case "google_local": methodLabel = "Google (Local)"; break;
                case "google_linked": methodLabel = "Email + Google"; break;
                default: methodLabel = "Email/Mật khẩu"; break;
            }
            showInfoDialog("Về StudyPlan",
                    "StudyPlan v1.0\nỨng dụng quản lý học tập thông minh\n\n" +
                    "👤 Tài khoản: " + name + "\n📧 Email: " + email + "\n" +
                    "🔑 Phương thức: " + methodLabel + "\n\n" +
                    "📚 Tổng môn học: " + subjectDAO.getCount() + "\n" +
                    "📅 Tổng lịch học: " + scheduleDAO.getCount() + "\n" +
                    "📝 Tổng bài tập: " + assignmentDAO.getCount() + "\n\n" +
                    "💾 Database: SQLite local\n🔒 Bảo mật: SHA-256");
        });

        // ── Logout — có confirm dialog ────────────────────────────
        View btnLogout = view.findViewById(R.id.btn_logout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> showLogoutConfirmDialog(prefs));
        }
    }

    // ─── onResume: reload stats mỗi khi quay lại tab ─────────────

    @Override
    public void onResume() {
        super.onResume();
        refreshStats();
        // Reload tên nếu đã đổi trong session khác
        if (getView() != null) {
            SharedPreferences prefs = requireContext().getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE);
            String userName = prefs.getString("user_name", "");
            if (tvProfileName != null) tvProfileName.setText(userName.isEmpty() ? "Người dùng" : userName);
            if (tvProfileEmail != null) {
                String email = prefs.getString("user_email", "");
                tvProfileEmail.setText(email.isEmpty() ? "Chưa có email" : email);
            }
            updateAvatarLetter(tvAvatarAccount, userName);
        }
    }

    // ─── refreshStats ─────────────────────────────────────────────
    // Stats môn/lịch/bài tập lấy từ DAO — không hardcode.

    private void refreshStats() {
        if (tvStatSubCount != null) tvStatSubCount.setText(String.valueOf(subjectDAO.getCount()));
        if (tvStatSchedCount != null) tvStatSchedCount.setText(String.valueOf(scheduleDAO.getCount()));
        if (tvStatAssignCount != null) tvStatAssignCount.setText(String.valueOf(assignmentDAO.getCount()));
    }

    // ─── Edit Profile Dialog ──────────────────────────────────────
    // Dùng dialog_edit_profile.xml riêng (KHÔNG dùng dialog_add_schedule.xml).

    private void showEditProfileDialog(SharedPreferences prefs) {
        View dv = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);

        EditText etName = dv.findViewById(R.id.et_profile_name);
        EditText etRole = dv.findViewById(R.id.et_profile_role);
        TextView tvAvatarPreview = dv.findViewById(R.id.tv_avatar_preview);
        TextView tvErrorName = dv.findViewById(R.id.tv_error_profile_name);

        // Pre-fill
        String currentName = tvProfileName != null ? tvProfileName.getText().toString() : "";
        String currentRole = tvProfileRole != null ? tvProfileRole.getText().toString() : "";
        etName.setText(currentName);
        etRole.setText(currentRole.equals(getString(R.string.profile_role)) ? "" : currentRole);
        updateAvatarLetter(tvAvatarPreview, currentName);

        // Live preview avatar khi gõ tên
        etName.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateAvatarLetter(tvAvatarPreview, s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.RoundedDialog)
                .setTitle(getString(R.string.dialog_edit_profile))
                .setView(dv)
                .setPositiveButton(getString(R.string.btn_save), null) // null → override sau
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .create();

        dialog.show();

        // Override positive — validate fail thì dialog KHÔNG đóng
        Button btnSave = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String role = etRole.getText().toString().trim();

            if (name.isEmpty()) {
                if (tvErrorName != null) tvErrorName.setVisibility(View.VISIBLE);
                return;
            }
            if (tvErrorName != null) tvErrorName.setVisibility(View.GONE);

            // Update profile card
            if (tvProfileName != null) tvProfileName.setText(name);
            if (tvProfileRole != null) {
                tvProfileRole.setText(role.isEmpty() ? getString(R.string.profile_role) : role);
            }
            updateAvatarLetter(tvAvatarAccount, name);

            // Lưu vào SharedPreferences
            prefs.edit().putString("user_name", name).apply();

            Toast.makeText(getContext(), getString(R.string.toast_saved), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    // ─── Logout Confirm Dialog ────────────────────────────────────
    // Có confirm dialog; sau logout xóa session và quay về LoginActivity.

    private void showLogoutConfirmDialog(SharedPreferences prefs) {
        new AlertDialog.Builder(requireContext(), R.style.RoundedDialog)
                .setTitle(getString(R.string.dialog_logout_title))
                .setMessage(getString(R.string.dialog_logout_message))
                .setPositiveButton(getString(R.string.menu_logout), (d, w) -> doLogout(prefs))
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    private void doLogout(SharedPreferences prefs) {
        // 1. Sign out Firebase Auth
        try {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut();
        } catch (Exception ignored) {}

        // 2. Sign out Google Sign-In client (để lần sau hiện account picker)
        try {
            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(
                    GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .requestProfile()
                    .build();
            GoogleSignInClient googleClient = GoogleSignIn.getClient(requireContext(), gso);
            googleClient.signOut();
        } catch (Exception ignored) {}

        // 3. Xóa session SharedPreferences
        prefs.edit().clear().apply();

        // 4. Quay về LoginActivity
        if (getActivity() != null) {
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            getActivity().finish();
        }
    }

    // ─── Helpers ─────────────────────────────────────────────────

    /**
     * Hiển thị chữ cái đầu tên cuối (chuẩn tên Việt).
     */
    private void updateAvatarLetter(TextView tvAvatar, String name) {
        if (tvAvatar == null) return;
        if (name == null || name.trim().isEmpty()) {
            tvAvatar.setText("?");
            return;
        }
        String[] parts = name.trim().split("\\s+");
        String letter = parts[parts.length - 1].substring(0, 1).toUpperCase();
        tvAvatar.setText(letter);
    }

    private void bindMenu(View root, int viewId, Runnable action) {
        View v = root.findViewById(viewId);
        if (v != null) v.setOnClickListener(btn -> action.run());
    }

    private void switchTab(int tabId) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).switchToTab(tabId);
        }
    }

    private void showInfoDialog(String title, String message) {
        new AlertDialog.Builder(requireContext(), R.style.RoundedDialog)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("Đã hiểu", null)
                .show();
    }
}
