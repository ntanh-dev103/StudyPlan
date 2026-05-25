package com.studyplan.app;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HomeFragment extends Fragment {

    private AssignmentDAO assignmentDAO;
    private ScheduleDAO scheduleDAO;

    // Greeting card views
    private TextView tvTodayDate;
    private TextView tvGreetingHello;
    private TextView tvAvatarHome;
    private TextView tvStatTotal;
    private TextView tvStatDone;
    private TextView tvStatInProgress;
    private TextView tvStatNotStarted;
    private TextView tvChipDone;
    private TextView tvChipInProgress;
    private TextView tvChipNotStarted;

    // List containers
    private LinearLayout llTodaySchedules;
    private LinearLayout llUpcomingDeadlines;

    // Empty states
    private TextView tvEmptySchedule;
    private TextView tvEmptyDeadline;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Init DAOs
        assignmentDAO = new AssignmentDAO(requireContext());
        scheduleDAO = new ScheduleDAO(requireContext());

        // Bind views
        tvTodayDate = view.findViewById(R.id.tv_today_date);
        tvGreetingHello = view.findViewById(R.id.tv_greeting_hello);
        tvAvatarHome = view.findViewById(R.id.tv_avatar_home);
        tvStatTotal = view.findViewById(R.id.tv_stat_total);
        tvStatDone = view.findViewById(R.id.tv_stat_done);
        tvStatInProgress = view.findViewById(R.id.tv_stat_in_progress);
        tvStatNotStarted = view.findViewById(R.id.tv_stat_not_started);
        tvChipDone = view.findViewById(R.id.tv_chip_done);
        tvChipInProgress = view.findViewById(R.id.tv_chip_in_progress);
        tvChipNotStarted = view.findViewById(R.id.tv_chip_not_started);
        llTodaySchedules = view.findViewById(R.id.ll_today_schedules);
        llUpcomingDeadlines = view.findViewById(R.id.ll_upcoming_deadlines);
        tvEmptySchedule = view.findViewById(R.id.tv_empty_schedule);
        tvEmptyDeadline = view.findViewById(R.id.tv_empty_deadline);

        // "Xem tất cả" → switch tab
        View btnViewAllSchedule = view.findViewById(R.id.btn_view_all_schedule);
        if (btnViewAllSchedule != null) {
            btnViewAllSchedule.setOnClickListener(v -> switchTab(R.id.nav_schedule));
        }
        View btnViewAllDeadline = view.findViewById(R.id.btn_view_all_deadline);
        if (btnViewAllDeadline != null) {
            btnViewAllDeadline.setOnClickListener(v -> switchTab(R.id.nav_assignment));
        }

        // Quick Actions
        bindQuickAction(view, R.id.quick_action_subject, R.id.nav_subject);
        bindQuickAction(view, R.id.quick_action_schedule, R.id.nav_schedule);
        bindQuickAction(view, R.id.quick_action_assignment, R.id.nav_assignment);
        bindQuickAction(view, R.id.quick_action_account, R.id.nav_account);

        // Notification bell
        View btnNotif = view.findViewById(R.id.btn_notification);
        if (btnNotif != null) {
            btnNotif.setOnClickListener(v -> {
                int late = assignmentDAO.getCountByStatus("late");
                if (late > 0) {
                    Toast.makeText(getContext(),
                            "⚠ Có " + late + " bài tập trễ deadline!", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(getContext(), "Không có thông báo mới ✅", Toast.LENGTH_SHORT).show();
                }
            });
        }

        loadAll();
    }

    /**
     * onResume: reload dữ liệu mỗi khi quay lại Home từ tab khác.
     */
    @Override
    public void onResume() {
        super.onResume();
        loadAll();
    }

    // ─── Load All ────────────────────────────────────────────────

    private void loadAll() {
        loadUserInfo();
        loadTaskStats();
        loadTodaySchedules();
        loadUpcomingDeadlines();
    }

    // ─── 1. loadUserInfo ─────────────────────────────────────────
    // Lấy tên user từ SharedPreferences, hiển thị lời chào, avatar chữ cái đầu, ngày thật.

    private void loadUserInfo() {
        SharedPreferences prefs = requireContext()
                .getSharedPreferences("StudyPlanPrefs", android.content.Context.MODE_PRIVATE);
        String userName = prefs.getString("user_name", "");

        // Ngày thật bằng Calendar
        if (tvTodayDate != null) {
            String[] vietDays = {"CHỦ NHẬT", "THỨ HAI", "THỨ BA", "THỨ TƯ",
                    "THỨ NĂM", "THỨ SÁU", "THỨ BẢY"};
            Calendar cal = Calendar.getInstance();
            String dayName = vietDays[cal.get(Calendar.DAY_OF_WEEK) - 1];
            String dateStr = new SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(new Date());
            tvTodayDate.setText(dayName + ", " + dateStr);
        }

        // Lời chào
        if (tvGreetingHello != null) {
            tvGreetingHello.setText(userName.isEmpty()
                    ? "Xin chào! 👋"
                    : "Xin chào,\n" + userName + " 👋");
        }

        // Avatar: chữ cái đầu từ phần tên cuối cùng (tên Việt)
        if (tvAvatarHome != null) {
            if (!userName.isEmpty()) {
                String[] parts = userName.trim().split("\\s+");
                String letter = parts[parts.length - 1].substring(0, 1).toUpperCase();
                tvAvatarHome.setText(letter);
            } else {
                tvAvatarHome.setText("?");
            }
        }
    }

    // ─── 2. loadTaskStats ────────────────────────────────────────
    // Lấy thống kê bài tập từ AssignmentDAO.

    private void loadTaskStats() {
        int total = assignmentDAO.getCount();
        int done = assignmentDAO.getCountByStatus("done");
        int inProgress = assignmentDAO.getCountByStatus("in_progress");
        int notStarted = assignmentDAO.getCountByStatus("not_started");

        // tv_stat_total: dạng "done/total" hiển thị trên badge
        if (tvStatTotal != null) tvStatTotal.setText(done + "/" + total);

        // Hidden stat TextViews (dùng nội bộ hoặc để test)
        if (tvStatDone != null) tvStatDone.setText(String.valueOf(done));
        if (tvStatInProgress != null) tvStatInProgress.setText(String.valueOf(inProgress));
        if (tvStatNotStarted != null) tvStatNotStarted.setText(String.valueOf(notStarted));

        // Chip labels hiển thị trực tiếp
        if (tvChipDone != null) tvChipDone.setText("● Hoàn thành: " + done);
        if (tvChipInProgress != null) tvChipInProgress.setText("● Đang làm: " + inProgress);
        if (tvChipNotStarted != null) tvChipNotStarted.setText("● Chưa làm: " + notStarted);
    }

    // ─── 3. loadTodaySchedules ───────────────────────────────────
    // Lấy lịch hôm nay từ ScheduleDAO.getTodaySchedule().

    private void loadTodaySchedules() {
        if (llTodaySchedules == null) return;
        llTodaySchedules.removeAllViews();

        List<ScheduleItem> list = scheduleDAO.getTodaySchedule();

        if (list.isEmpty()) {
            if (tvEmptySchedule != null) tvEmptySchedule.setVisibility(View.VISIBLE);
            return;
        }
        if (tvEmptySchedule != null) tvEmptySchedule.setVisibility(View.GONE);

        for (ScheduleItem item : list) {
            View card = buildScheduleCard(item);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dp(8));
            card.setLayoutParams(lp);
            llTodaySchedules.addView(card);
        }
    }

    // ─── 4. loadUpcomingDeadlines ────────────────────────────────
    // Lấy deadline sắp tới từ AssignmentDAO.getUpcomingDeadlines().

    private void loadUpcomingDeadlines() {
        if (llUpcomingDeadlines == null) return;
        llUpcomingDeadlines.removeAllViews();

        List<Assignment> list = assignmentDAO.getUpcomingDeadlines();

        if (list.isEmpty()) {
            if (tvEmptyDeadline != null) tvEmptyDeadline.setVisibility(View.VISIBLE);
            return;
        }
        if (tvEmptyDeadline != null) tvEmptyDeadline.setVisibility(View.GONE);

        for (Assignment a : list) {
            View card = buildDeadlineCard(a);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.setMargins(0, 0, 0, dp(8));
            card.setLayoutParams(lp);
            llUpcomingDeadlines.addView(card);
        }
    }

    // ─── Card Builders ───────────────────────────────────────────

    private View buildScheduleCard(ScheduleItem item) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackground(requireContext().getResources().getDrawable(R.drawable.bg_card_elevated, null));
        card.setElevation(dp(3));
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setGravity(Gravity.CENTER_VERTICAL);

        // Accent bar
        View accent = new View(requireContext());
        int color = colorFromTag(item.getColorTag());
        accent.setBackgroundColor(color);
        LinearLayout.LayoutParams accentLp = new LinearLayout.LayoutParams(dp(4), dp(48));
        accentLp.setMarginEnd(dp(12));
        accent.setLayoutParams(accentLp);
        card.addView(accent);

        // Info column
        LinearLayout col = new LinearLayout(requireContext());
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvName = new TextView(requireContext());
        tvName.setText(item.getSubjectName());
        tvName.setTextColor(color(R.color.sp_text_primary));
        tvName.setTextSize(15f);
        tvName.setTypeface(null, android.graphics.Typeface.BOLD);
        col.addView(tvName);

        TextView tvRoom = new TextView(requireContext());
        tvRoom.setText("📍 " + item.getRoom() + "  •  " + item.getTeacher());
        tvRoom.setTextColor(color(R.color.sp_text_secondary));
        tvRoom.setTextSize(12f);
        LinearLayout.LayoutParams rlp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        rlp.topMargin = dp(4);
        tvRoom.setLayoutParams(rlp);
        col.addView(tvRoom);
        card.addView(col);

        // Time
        TextView tvTime = new TextView(requireContext());
        tvTime.setText(item.getStartTime() + "\n" + item.getEndTime());
        tvTime.setTextColor(color);
        tvTime.setTextSize(13f);
        tvTime.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTime.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);
        card.addView(tvTime);

        return card;
    }

    private View buildDeadlineCard(Assignment a) {
        LinearLayout card = new LinearLayout(requireContext());
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setBackground(requireContext().getResources().getDrawable(R.drawable.bg_card_elevated, null));
        card.setElevation(dp(3));
        card.setPadding(dp(14), dp(14), dp(14), dp(14));
        card.setGravity(Gravity.CENTER_VERTICAL);

        // Accent
        View accent = new View(requireContext());
        int statusClr = statusColor(a.getStatus());
        accent.setBackgroundColor(statusClr);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(dp(4), dp(48));
        alp.setMarginEnd(dp(12));
        accent.setLayoutParams(alp);
        card.addView(accent);

        // Info column
        LinearLayout col = new LinearLayout(requireContext());
        col.setOrientation(LinearLayout.VERTICAL);
        col.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView tvTitle = new TextView(requireContext());
        tvTitle.setText(a.getTitle());
        tvTitle.setTextColor(color(R.color.sp_text_primary));
        tvTitle.setTextSize(15f);
        tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        col.addView(tvTitle);

        TextView tvSub = new TextView(requireContext());
        tvSub.setText("📚 " + a.getSubjectName());
        tvSub.setTextColor(color(R.color.sp_text_secondary));
        tvSub.setTextSize(12f);
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.topMargin = dp(4);
        tvSub.setLayoutParams(slp);
        col.addView(tvSub);
        card.addView(col);

        // Right: deadline + priority
        LinearLayout right = new LinearLayout(requireContext());
        right.setOrientation(LinearLayout.VERTICAL);
        right.setGravity(Gravity.END | Gravity.CENTER_VERTICAL);

        TextView tvDl = new TextView(requireContext());
        tvDl.setText(a.getDeadline());
        tvDl.setTextColor(statusClr);
        tvDl.setTextSize(11f);
        tvDl.setTypeface(null, android.graphics.Typeface.BOLD);
        tvDl.setGravity(Gravity.END);
        right.addView(tvDl);

        TextView tvPrio = new TextView(requireContext());
        tvPrio.setText(prioEmoji(a.getPriority()) + " " + a.getPriority());
        tvPrio.setTextColor(color(R.color.sp_text_secondary));
        tvPrio.setTextSize(11f);
        tvPrio.setGravity(Gravity.END);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        plp.topMargin = dp(4);
        tvPrio.setLayoutParams(plp);
        right.addView(tvPrio);

        card.addView(right);
        return card;
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private void bindQuickAction(View root, int viewId, int tabId) {
        View v = root.findViewById(viewId);
        if (v != null) v.setOnClickListener(btn -> switchTab(tabId));
    }

    private void switchTab(int tabId) {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).switchToTab(tabId);
        }
    }

    private int colorFromTag(String tag) {
        if (tag == null) return color(R.color.module_schedule);
        switch (tag) {
            case "blue": return color(R.color.sp_primary);
            case "green": return color(R.color.module_subject);
            case "orange": return color(R.color.module_assignment);
            case "red": return Color.parseColor("#F44336");
            default: return color(R.color.module_schedule);
        }
    }

    private int statusColor(String status) {
        if (status == null) return color(R.color.status_not_started);
        switch (status) {
            case "done": return color(R.color.status_done);
            case "in_progress": return color(R.color.status_in_progress);
            case "late": return color(R.color.status_late);
            default: return color(R.color.status_not_started);
        }
    }

    private String prioEmoji(String p) {
        if (p == null) return "⚪";
        switch (p) {
            case "Cao": return "🔴";
            case "Thấp": return "🟢";
            default: return "🟠";
        }
    }

    private int color(int colorRes) {
        return requireContext().getResources().getColor(colorRes);
    }

    private int dp(int dp) {
        return Math.round(dp * requireContext().getResources().getDisplayMetrics().density);
    }
}
