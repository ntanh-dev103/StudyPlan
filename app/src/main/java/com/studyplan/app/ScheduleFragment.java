package com.studyplan.app;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class ScheduleFragment extends Fragment implements ScheduleAdapter.OnScheduleActionListener {

    private RecyclerView rvTimeline;
    private ScheduleAdapter adapter;
    private ScheduleDAO scheduleDAO;
    private SubjectDAO subjectDAO;
    private TextView tvScheduleTitle, tvScheduleCount;

    // Day filter chips
    private TextView chipAll, chipToday, chipWeek, chipT2, chipT3, chipT4, chipT5, chipT6;
    private TextView currentActiveChip;
    private String currentDayFilter = "all";
    private String currentSearch = "";

    // Day spinner data
    private static final String[] DAYS_KEY = {"T2", "T3", "T4", "T5", "T6", "T7", "CN"};
    private static final String[] DAYS_LABEL = {
            "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy", "Chủ Nhật"
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_schedule, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        scheduleDAO = new ScheduleDAO(requireContext());
        subjectDAO = new SubjectDAO(requireContext());

        rvTimeline = view.findViewById(R.id.rv_timeline);
        tvScheduleTitle = view.findViewById(R.id.tv_schedule_title);
        tvScheduleCount = view.findViewById(R.id.tv_schedule_count);

        FloatingActionButton fabAdd = view.findViewById(R.id.fab_add_schedule);
        View btnAddToolbar = view.findViewById(R.id.btn_add_schedule);

        // Day filter chips
        chipAll = view.findViewById(R.id.chip_day_all);
        chipToday = view.findViewById(R.id.chip_day_today);
        chipWeek = view.findViewById(R.id.chip_day_week);
        chipT2 = view.findViewById(R.id.chip_day_t2);
        chipT3 = view.findViewById(R.id.chip_day_t3);
        chipT4 = view.findViewById(R.id.chip_day_t4);
        chipT5 = view.findViewById(R.id.chip_day_t5);
        chipT6 = view.findViewById(R.id.chip_day_t6);
        currentActiveChip = chipAll;

        // RecyclerView
        List<ScheduleItem> items = scheduleDAO.getAll();
        adapter = new ScheduleAdapter(items, this);
        rvTimeline.setLayoutManager(new LinearLayoutManager(getContext()));
        rvTimeline.setAdapter(adapter);

        updateCountBadge(items.size());
        setupDayFilters();
        setupSemesterChips(view);

        // Search
        EditText etSearch = view.findViewById(R.id.et_search_schedule);
        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    currentSearch = s.toString().trim();
                    refreshFromDB();
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        // FAB + toolbar button
        View.OnClickListener addListener = v -> showAddScheduleDialog();
        if (fabAdd != null) fabAdd.setOnClickListener(addListener);
        if (btnAddToolbar != null) btnAddToolbar.setOnClickListener(addListener);

        View btnManual = view.findViewById(R.id.btn_manual_schedule);
        if (btnManual != null) btnManual.setOnClickListener(v -> showAddScheduleDialog());

        View btnAuto = view.findViewById(R.id.btn_auto_schedule);
        if (btnAuto != null) btnAuto.setOnClickListener(v ->
                Toast.makeText(getContext(), getString(R.string.toast_schedule_auto), Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshFromDB();
    }

    // ─── Day Filters ─────────────────────────────────────────────

    private void setupDayFilters() {
        if (chipAll != null) chipAll.setOnClickListener(v -> filterByDay(chipAll, "all"));
        if (chipToday != null) chipToday.setOnClickListener(v -> filterByDay(chipToday, "today"));
        if (chipWeek != null) chipWeek.setOnClickListener(v -> filterByDay(chipWeek, "week"));
        if (chipT2 != null) chipT2.setOnClickListener(v -> filterByDay(chipT2, "T2"));
        if (chipT3 != null) chipT3.setOnClickListener(v -> filterByDay(chipT3, "T3"));
        if (chipT4 != null) chipT4.setOnClickListener(v -> filterByDay(chipT4, "T4"));
        if (chipT5 != null) chipT5.setOnClickListener(v -> filterByDay(chipT5, "T5"));
        if (chipT6 != null) chipT6.setOnClickListener(v -> filterByDay(chipT6, "T6"));
    }

    private void filterByDay(TextView chip, String filter) {
        if (currentActiveChip != null) {
            currentActiveChip.setBackgroundResource(R.drawable.bg_chip_unselected);
            currentActiveChip.setTextColor(color(R.color.sp_text_secondary));
        }
        if (chip != null) {
            chip.setBackgroundResource(R.drawable.bg_chip_orange_selected);
            chip.setTextColor(color(R.color.white));
        }
        currentActiveChip = chip;
        currentDayFilter = filter;

        List<ScheduleItem> data = scheduleDAO.searchWithFilter(currentSearch, filter);
        adapter.updateItems(data);
        updateCountBadge(data.size());

        if (tvScheduleTitle != null) {
            switch (filter) {
                case "today": tvScheduleTitle.setText("📅 Lịch hôm nay (" + scheduleDAO.getTodayDayLabel() + ")"); break;
                case "week": tvScheduleTitle.setText("📋 Lịch trong tuần"); break;
                case "all": tvScheduleTitle.setText(getString(R.string.schedule_day_title)); break;
                default: tvScheduleTitle.setText("📅 Lịch " + filter); break;
            }
        }
    }

    private void refreshFromDB() {
        filterByDay(currentActiveChip, currentDayFilter);
    }

    private void updateCountBadge(int count) {
        if (tvScheduleCount != null) tvScheduleCount.setText(String.valueOf(count));
    }

    // ─── Semester Chips ──────────────────────────────────────────

    private void setupSemesterChips(View view) {
        TextView c1 = view.findViewById(R.id.chip_sem_1);
        TextView c2 = view.findViewById(R.id.chip_sem_2);
        TextView c3 = view.findViewById(R.id.chip_sem_3);
        if (c1 == null || c2 == null || c3 == null) return;

        View.OnClickListener l = v -> {
            resetSemChip(c1); resetSemChip(c2); resetSemChip(c3);
            TextView clicked = (TextView) v;
            clicked.setBackgroundResource(R.drawable.bg_tab_purple_selected);
            clicked.setTextColor(color(R.color.white));
        };
        c1.setOnClickListener(l);
        c2.setOnClickListener(l);
        c3.setOnClickListener(l);
    }

    private void resetSemChip(TextView c) {
        if (c == null) return;
        c.setBackgroundResource(R.drawable.bg_tab_unselected);
        c.setTextColor(color(R.color.sp_text_secondary));
    }

    // ─── Adapter Callbacks ───────────────────────────────────────

    @Override
    public void onEdit(ScheduleItem item, int position) {
        showEditScheduleDialog(item);
    }

    @Override
    public void onDelete(ScheduleItem item, int position) {
        new AlertDialog.Builder(requireContext(), R.style.RoundedDialog)
                .setTitle("Xóa lịch học")
                .setMessage("Bạn có chắc muốn xóa?\n\n" + item.getSubjectName() +
                        " (" + item.getDayOfWeek() + " " + item.getStartTime() + " - " + item.getEndTime() + ")")
                .setPositiveButton(getString(R.string.btn_delete), (d, w) -> {
                    scheduleDAO.delete(item.getId());
                    refreshFromDB();
                    Toast.makeText(getContext(), getString(R.string.toast_deleted), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    // ─── Add Schedule Dialog ──────────────────────────────────────

    private void showAddScheduleDialog() {
        View dv = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_schedule, null);
        bindAndShowScheduleDialog(dv, null);
    }

    // ─── Edit Schedule Dialog ─────────────────────────────────────

    private void showEditScheduleDialog(ScheduleItem item) {
        View dv = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_schedule, null);
        bindAndShowScheduleDialog(dv, item);
    }

    /**
     * Dùng chung cho Add và Edit.
     * item == null → chế độ Add; item != null → chế độ Edit.
     * Validate sai → dialog KHÔNG đóng (override btn positive sau show()).
     * Giờ kết thúc phải > giờ bắt đầu.
     * Nếu thiếu dữ liệu → hiện lỗi, không crash.
     */
    private void bindAndShowScheduleDialog(View dv, @Nullable ScheduleItem item) {
        Spinner spinnerSubject = dv.findViewById(R.id.spinner_schedule_subject);
        Spinner spinnerDay = dv.findViewById(R.id.spinner_day);
        TextView tvStart = dv.findViewById(R.id.tv_start_time);
        TextView tvEnd = dv.findViewById(R.id.tv_end_time);
        EditText etRoom = dv.findViewById(R.id.et_room);
        EditText etTeacher = dv.findViewById(R.id.et_teacher);
        TextView tvErrSubject = dv.findViewById(R.id.tv_error_subject_sched);
        TextView tvErrTime = dv.findViewById(R.id.tv_error_time);

        // Subject spinner
        List<Subject> subjects = subjectDAO.getAll();
        List<String> subjectNames = new ArrayList<>();
        if (subjects.isEmpty()) subjectNames.add("Chưa có môn — hãy thêm môn trước");
        else for (Subject s : subjects) subjectNames.add(s.getName());
        ArrayAdapter<String> subAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, subjectNames);
        subAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(subAdapter);

        // Day spinner
        ArrayAdapter<String> dayAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, DAYS_LABEL);
        dayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDay.setAdapter(dayAdapter);

        // State holders
        final String[] startTime = {""};
        final String[] endTime = {""};

        if (item != null) {
            // Edit: pre-fill
            int preSubIdx = 0;
            for (int i = 0; i < subjects.size(); i++) {
                if (subjects.get(i).getName().equals(item.getSubjectName())) { preSubIdx = i; break; }
            }
            spinnerSubject.setSelection(preSubIdx);
            spinnerDay.setSelection(dayIndex(item.getDayOfWeek()));
            startTime[0] = item.getStartTime();
            endTime[0] = item.getEndTime();
            if (!startTime[0].isEmpty()) tvStart.setText("⏰ " + startTime[0]);
            if (!endTime[0].isEmpty()) tvEnd.setText("⏰ " + endTime[0]);
            if (etRoom != null) etRoom.setText(item.getRoom());
            if (etTeacher != null) etTeacher.setText(item.getTeacher());
        } else {
            // Add: default today
            spinnerDay.setSelection(dayIndex(scheduleDAO.getTodayDayLabel()));
        }

        // TimePicker
        setupTimePicker(tvStart, startTime, tvErrTime, "Giờ bắt đầu");
        setupTimePicker(tvEnd, endTime, tvErrTime, "Giờ kết thúc");

        String dialogTitle = item == null
                ? getString(R.string.dialog_add_schedule)
                : "Sửa lịch học";
        String btnLabel = item == null
                ? getString(R.string.btn_add)
                : getString(R.string.btn_save);

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.RoundedDialog)
                .setTitle(dialogTitle)
                .setView(dv)
                .setPositiveButton(btnLabel, null) // null → override sau
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .create();

        dialog.show();

        // Override positive: dialog KHÔNG đóng nếu validate fail
        Button btnOk = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        btnOk.setOnClickListener(v -> {
            boolean valid = true;

            // Validate môn học
            if (subjects.isEmpty()) {
                if (tvErrSubject != null) { tvErrSubject.setVisibility(View.VISIBLE); }
                valid = false;
            } else if (tvErrSubject != null) tvErrSubject.setVisibility(View.GONE);

            // Validate giờ
            if (startTime[0].isEmpty() || endTime[0].isEmpty()) {
                if (tvErrTime != null) {
                    tvErrTime.setText("⚠ Vui lòng chọn giờ bắt đầu và kết thúc");
                    tvErrTime.setVisibility(View.VISIBLE);
                }
                valid = false;
            } else if (!isEndAfterStart(startTime[0], endTime[0])) {
                if (tvErrTime != null) {
                    tvErrTime.setText("⚠ Giờ kết thúc phải sau giờ bắt đầu");
                    tvErrTime.setVisibility(View.VISIBLE);
                }
                valid = false;
            } else if (tvErrTime != null) tvErrTime.setVisibility(View.GONE);

            if (!valid) return; // Dialog KHÔNG tự đóng

            String selectedSubject = subjects.isEmpty() ?
                    (item != null ? item.getSubjectName() : "Chưa xác định") :
                    subjects.get(Math.max(0, spinnerSubject.getSelectedItemPosition())).getName();
            String selectedDay = DAYS_KEY[spinnerDay.getSelectedItemPosition()];
            String room = etRoom != null ? etRoom.getText().toString().trim() : "";
            String teacher = etTeacher != null ? etTeacher.getText().toString().trim() : "";
            if (room.isEmpty()) room = item != null ? item.getRoom() : "Chưa xếp";
            if (teacher.isEmpty()) teacher = item != null ? item.getTeacher() : "Chưa phân công";

            String colorTag = getSubjectColor(subjects, selectedSubject);

            if (item == null) {
                // Add
                ScheduleItem newItem = new ScheduleItem(0, selectedSubject,
                        startTime[0], endTime[0], room, teacher, "upcoming", colorTag, selectedDay);
                scheduleDAO.insert(newItem);
                Toast.makeText(getContext(), getString(R.string.toast_added), Toast.LENGTH_SHORT).show();
            } else {
                // Edit
                ScheduleItem updated = new ScheduleItem(item.getId(), selectedSubject,
                        startTime[0], endTime[0], room, teacher,
                        item.getStatus(), colorTag, selectedDay);
                scheduleDAO.update(updated);
                Toast.makeText(getContext(), getString(R.string.toast_saved), Toast.LENGTH_SHORT).show();
            }
            refreshFromDB();
            dialog.dismiss();
        });
    }

    // ─── Helpers ─────────────────────────────────────────────────

    /**
     * Gắn TimePickerDialog vào TextView chỉ đọc.
     * Sau khi chọn: hiển thị "⏰ HH:mm" và ẩn error.
     */
    private void setupTimePicker(TextView tv, String[] holder, TextView tvError, String title) {
        if (tv == null) return;
        tv.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new TimePickerDialog(requireContext(), (view1, hour, minute) -> {
                String fmt = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                holder[0] = fmt;
                tv.setText("⏰ " + fmt);
                if (tvError != null) tvError.setVisibility(View.GONE);
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true)
            {{ setTitle(title); }}
            .show();
        });
    }

    /**
     * Kiểm tra giờ kết thúc > giờ bắt đầu (format HH:mm).
     */
    private boolean isEndAfterStart(String start, String end) {
        try {
            String[] sp = start.split(":");
            String[] ep = end.split(":");
            int sm = Integer.parseInt(sp[0]) * 60 + Integer.parseInt(sp[1]);
            int em = Integer.parseInt(ep[0]) * 60 + Integer.parseInt(ep[1]);
            return em > sm;
        } catch (Exception e) {
            return true; // Nếu parse lỗi thì không block
        }
    }

    private int dayIndex(String day) {
        if (day == null) return 0;
        for (int i = 0; i < DAYS_KEY.length; i++) {
            if (DAYS_KEY[i].equals(day)) return i;
        }
        return 0;
    }

    private String getSubjectColor(List<Subject> subjects, String name) {
        for (Subject s : subjects) {
            if (s.getName().equals(name)) return s.getColorTag();
        }
        return "purple";
    }

    private int color(int colorRes) {
        return requireContext().getResources().getColor(colorRes);
    }
}
