package com.studyplan.app;

import android.app.DatePickerDialog;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AssignmentFragment extends Fragment implements AssignmentAdapter.OnAssignmentActionListener {

    private RecyclerView rvAssignments;
    private AssignmentAdapter adapter;
    private AssignmentDAO assignmentDAO;
    private SubjectDAO subjectDAO;
    private TextView tvTotalCount, tvLateCount, tvDoneCount;

    // Filter chips
    private TextView chipAll, chipInProgress, chipDone, chipNotStarted, chipLate;
    private TextView currentActiveChip;
    private String currentFilter = "all";
    private String currentSearch = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_assignment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        assignmentDAO = new AssignmentDAO(requireContext());
        subjectDAO = new SubjectDAO(requireContext());

        rvAssignments = view.findViewById(R.id.rv_assignments);
        tvTotalCount = view.findViewById(R.id.tv_total_count);
        tvLateCount = view.findViewById(R.id.tv_late_count);
        tvDoneCount = view.findViewById(R.id.tv_done_count);

        chipAll = view.findViewById(R.id.chip_all);
        chipInProgress = view.findViewById(R.id.chip_in_progress);
        chipDone = view.findViewById(R.id.chip_done);
        chipNotStarted = view.findViewById(R.id.chip_not_started);
        chipLate = view.findViewById(R.id.chip_late);
        currentActiveChip = chipAll;

        EditText etSearch = view.findViewById(R.id.et_search_assignment);
        FloatingActionButton fabAdd = view.findViewById(R.id.fab_add_assignment);

        // RecyclerView
        List<Assignment> list = assignmentDAO.getAll();
        adapter = new AssignmentAdapter(list, this);
        rvAssignments.setLayoutManager(new LinearLayoutManager(getContext()));
        rvAssignments.setAdapter(adapter);

        updateStats();
        setupFilterChips();

        // Search
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

        if (fabAdd != null) fabAdd.setOnClickListener(v -> showAddAssignmentDialog());
    }

    // ─── onResume ────────────────────────────────────────────────

    @Override
    public void onResume() {
        super.onResume();
        // Kiểm tra và đánh dấu bài trễ hạn mỗi lần vào tab
        checkAndMarkLateAssignments();
        refreshFromDB();
    }

    // ─── checkAndMarkLateAssignments ─────────────────────────────
    /**
     * Duyệt tất cả bài tập chưa hoàn thành, nếu deadline đã qua
     * so với thời gian hiện tại → đổi status thành "late".
     * Tick hoàn thành sẽ cập nhật SQLite ngay qua updateDoneStatus().
     */
    private void checkAndMarkLateAssignments() {
        List<Assignment> all = assignmentDAO.getAll();
        long nowMs = System.currentTimeMillis();
        for (Assignment a : all) {
            if ("done".equals(a.getStatus()) || "late".equals(a.getStatus())) continue;
            if (a.getDeadline() == null || a.getDeadline().isEmpty()) continue;
            long deadlineMs = parseDeadlineMs(a.getDeadline());
            if (deadlineMs > 0 && deadlineMs < nowMs) {
                assignmentDAO.updateDoneStatus(a.getId(), false, "late");
            }
        }
    }

    /**
     * Parse deadline string sang milliseconds.
     * Hỗ trợ nhiều format: "dd-MM-yyyy HH:mm", "yyyy-MM-dd HH:mm", "dd/MM/yyyy HH:mm", v.v.
     */
    private long parseDeadlineMs(String deadline) {
        String[] fmts = {
            "dd-MM-yyyy HH:mm", "yyyy-MM-dd HH:mm",
            "dd/MM/yyyy HH:mm", "dd-MM-yyyy", "yyyy-MM-dd"
        };
        for (String f : fmts) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat(f, Locale.getDefault());
                sdf.setLenient(false);
                java.util.Date d = sdf.parse(deadline);
                if (d != null) return d.getTime();
            } catch (Exception ignored) {}
        }
        return -1;
    }

    // ─── Filter Chips ────────────────────────────────────────────

    private void setupFilterChips() {
        if (chipAll != null) chipAll.setOnClickListener(v -> applyFilter(chipAll, "all"));
        if (chipInProgress != null) chipInProgress.setOnClickListener(v -> applyFilter(chipInProgress, "in_progress"));
        if (chipDone != null) chipDone.setOnClickListener(v -> applyFilter(chipDone, "done"));
        if (chipNotStarted != null) chipNotStarted.setOnClickListener(v -> applyFilter(chipNotStarted, "not_started"));
        if (chipLate != null) chipLate.setOnClickListener(v -> applyFilter(chipLate, "late"));
    }

    private void applyFilter(TextView chip, String status) {
        if (currentActiveChip != null) {
            currentActiveChip.setBackgroundResource(R.drawable.bg_chip_unselected);
            currentActiveChip.setTextColor(color(R.color.sp_text_secondary));
        }
        chip.setBackgroundResource(R.drawable.bg_chip_orange_selected);
        chip.setTextColor(color(R.color.white));
        currentActiveChip = chip;
        currentFilter = status;
        refreshFromDB();
    }

    private void updateStats() {
        if (tvTotalCount != null) tvTotalCount.setText(String.valueOf(assignmentDAO.getCount()));
        if (tvLateCount != null) tvLateCount.setText(String.valueOf(assignmentDAO.getCountByStatus("late")));
        if (tvDoneCount != null) tvDoneCount.setText(String.valueOf(assignmentDAO.getCountByStatus("done")));
    }

    private void refreshFromDB() {
        List<Assignment> data = assignmentDAO.searchWithFilter(currentSearch, currentFilter);
        adapter.setData(data);
        updateStats();
    }

    // ─── Adapter Callbacks ───────────────────────────────────────

    @Override
    public void onToggleDone(Assignment assignment, int position) {
        boolean newDone = !assignment.isDone();
        // Tick hoàn thành → cập nhật SQLite ngay
        String newStatus = newDone ? "done" : "in_progress";
        assignmentDAO.updateDoneStatus(assignment.getId(), newDone, newStatus);
        refreshFromDB();
    }

    @Override
    public void onClick(Assignment assignment, int position) {
        showDetailDialog(assignment);
    }

    // ─── Detail Dialog ───────────────────────────────────────────

    private void showDetailDialog(Assignment a) {
        String statusLabel;
        switch (a.getStatus() != null ? a.getStatus() : "") {
            case "late": statusLabel = "🔴 Trễ hạn"; break;
            case "done": statusLabel = "✅ Hoàn thành"; break;
            case "in_progress": statusLabel = "🔵 Đang làm"; break;
            default: statusLabel = "⚪ Chưa làm"; break;
        }
        new AlertDialog.Builder(requireContext(), R.style.RoundedDialog)
                .setTitle(a.getTitle())
                .setMessage(
                        "📚 Môn: " + a.getSubjectName() + "\n" +
                        "📅 Deadline: " + a.getDeadline() + "\n" +
                        "⚡ Ưu tiên: " + a.getPriority() + "\n" +
                        "📋 Trạng thái: " + statusLabel)
                .setPositiveButton("✏️ Sửa", (d, w) -> showEditAssignmentDialog(a))
                .setNegativeButton("🗑 Xóa", (d, w) -> confirmDelete(a))
                .setNeutralButton("Đóng", null)
                .show();
    }

    private void confirmDelete(Assignment a) {
        new AlertDialog.Builder(requireContext(), R.style.RoundedDialog)
                .setTitle("Xóa bài tập")
                .setMessage("Bạn có chắc muốn xóa?\n\n" + a.getTitle())
                .setPositiveButton(getString(R.string.btn_delete), (d, w) -> {
                    assignmentDAO.delete(a.getId());
                    refreshFromDB();
                    Toast.makeText(getContext(), getString(R.string.toast_deleted), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    // ─── Add Assignment Dialog ────────────────────────────────────

    private void showAddAssignmentDialog() {
        View dv = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_assignment, null);

        EditText etTitle = dv.findViewById(R.id.et_assignment_title);
        Spinner spinnerSubject = dv.findViewById(R.id.spinner_assignment_subject);
        TextView tvDeadline = dv.findViewById(R.id.tv_deadline_display);
        TextView tvErrTitle = dv.findViewById(R.id.tv_error_title);
        TextView tvErrSubject = dv.findViewById(R.id.tv_error_subject);
        TextView tvErrDeadline = dv.findViewById(R.id.tv_error_deadline);

        // Priority chips
        TextView chipHigh = dv.findViewById(R.id.chip_priority_high);
        TextView chipMedium = dv.findViewById(R.id.chip_priority_medium);
        TextView chipLow = dv.findViewById(R.id.chip_priority_low);

        // Status chips
        TextView chipSNS = dv.findViewById(R.id.chip_status_not_started);
        TextView chipSIP = dv.findViewById(R.id.chip_status_in_progress);
        TextView chipSDone = dv.findViewById(R.id.chip_status_done);
        TextView chipSLate = dv.findViewById(R.id.chip_status_late);

        // State holders
        final String[] selPriority = {"Cao"};
        final String[] selStatus = {"not_started"};
        final String[] selDeadline = {""};
        final Calendar deadlineCal = Calendar.getInstance();

        // Setup subject spinner
        List<Subject> subjects = subjectDAO.getAll();
        List<String> subjectNames = buildSubjectNames(subjects);
        ArrayAdapter<String> subAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, subjectNames);
        subAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(subAdapter);

        // Priority chip click
        setupPriorityChips(chipHigh, chipMedium, chipLow, selPriority);
        highlightPriority("Cao", chipHigh, chipMedium, chipLow);

        // Status chip click
        setupStatusChips(chipSNS, chipSIP, chipSDone, chipSLate, selStatus);
        highlightStatus("not_started", chipSNS, chipSIP, chipSDone, chipSLate);

        // Deadline picker
        if (tvDeadline != null) {
            tvDeadline.setOnClickListener(v ->
                    openDateTimePicker(deadlineCal, selDeadline, tvDeadline, tvErrDeadline));
        }

        // Build dialog — setPositiveButton null để override sau
        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.RoundedDialog)
                .setTitle(getString(R.string.dialog_add_assignment))
                .setView(dv)
                .setPositiveButton(getString(R.string.btn_add), null)
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .create();

        dialog.show();

        // Override positive button: dialog KHÔNG đóng nếu validate fail
        Button btnOk = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        btnOk.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String deadline = selDeadline[0];
            boolean valid = true;

            if (title.isEmpty()) {
                if (tvErrTitle != null) tvErrTitle.setVisibility(View.VISIBLE);
                valid = false;
            } else if (tvErrTitle != null) tvErrTitle.setVisibility(View.GONE);

            if (subjects.isEmpty()) {
                if (tvErrSubject != null) tvErrSubject.setVisibility(View.VISIBLE);
                valid = false;
            } else if (tvErrSubject != null) tvErrSubject.setVisibility(View.GONE);

            if (deadline.isEmpty()) {
                if (tvErrDeadline != null) tvErrDeadline.setVisibility(View.VISIBLE);
                valid = false;
            } else if (tvErrDeadline != null) tvErrDeadline.setVisibility(View.GONE);

            if (!valid) return; // Dialog KHÔNG tự đóng

            String selectedSubject = subjects.isEmpty() ? "" :
                    subjects.get(Math.max(0, spinnerSubject.getSelectedItemPosition())).getName();

            Assignment a = new Assignment(0, title, selectedSubject, deadline,
                    selPriority[0], selStatus[0], "done".equals(selStatus[0]));
            assignmentDAO.insert(a);
            refreshFromDB();
            Toast.makeText(getContext(), getString(R.string.toast_added), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    // ─── Edit Assignment Dialog ───────────────────────────────────

    private void showEditAssignmentDialog(Assignment assignment) {
        View dv = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_assignment, null);

        EditText etTitle = dv.findViewById(R.id.et_assignment_title);
        Spinner spinnerSubject = dv.findViewById(R.id.spinner_assignment_subject);
        TextView tvDeadline = dv.findViewById(R.id.tv_deadline_display);
        TextView tvErrTitle = dv.findViewById(R.id.tv_error_title);
        TextView tvErrDeadline = dv.findViewById(R.id.tv_error_deadline);

        TextView chipHigh = dv.findViewById(R.id.chip_priority_high);
        TextView chipMedium = dv.findViewById(R.id.chip_priority_medium);
        TextView chipLow = dv.findViewById(R.id.chip_priority_low);

        TextView chipSNS = dv.findViewById(R.id.chip_status_not_started);
        TextView chipSIP = dv.findViewById(R.id.chip_status_in_progress);
        TextView chipSDone = dv.findViewById(R.id.chip_status_done);
        TextView chipSLate = dv.findViewById(R.id.chip_status_late);

        // Pre-fill
        etTitle.setText(assignment.getTitle());
        if (!assignment.getDeadline().isEmpty()) {
            tvDeadline.setText("📅 " + assignment.getDeadline());
        }

        // Subject spinner
        List<Subject> subjects = subjectDAO.getAll();
        List<String> subjectNames = buildSubjectNames(subjects);
        int preIdx = 0;
        for (int i = 0; i < subjects.size(); i++) {
            if (subjects.get(i).getName().equals(assignment.getSubjectName())) {
                preIdx = i; break;
            }
        }
        ArrayAdapter<String> subAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_item, subjectNames);
        subAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSubject.setAdapter(subAdapter);
        spinnerSubject.setSelection(preIdx);

        final String[] selPriority = {assignment.getPriority()};
        final String[] selStatus = {assignment.getStatus()};
        final String[] selDeadline = {assignment.getDeadline()};
        final Calendar deadlineCal = Calendar.getInstance();

        setupPriorityChips(chipHigh, chipMedium, chipLow, selPriority);
        highlightPriority(assignment.getPriority(), chipHigh, chipMedium, chipLow);
        setupStatusChips(chipSNS, chipSIP, chipSDone, chipSLate, selStatus);
        highlightStatus(assignment.getStatus(), chipSNS, chipSIP, chipSDone, chipSLate);

        if (tvDeadline != null) {
            tvDeadline.setOnClickListener(v ->
                    openDateTimePicker(deadlineCal, selDeadline, tvDeadline, tvErrDeadline));
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.RoundedDialog)
                .setTitle("Sửa bài tập")
                .setView(dv)
                .setPositiveButton(getString(R.string.btn_save), null)
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .create();

        dialog.show();

        Button btnOk = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        btnOk.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String deadline = selDeadline[0];
            boolean valid = true;

            if (title.isEmpty()) {
                if (tvErrTitle != null) tvErrTitle.setVisibility(View.VISIBLE);
                valid = false;
            } else if (tvErrTitle != null) tvErrTitle.setVisibility(View.GONE);

            if (deadline.isEmpty()) {
                if (tvErrDeadline != null) tvErrDeadline.setVisibility(View.VISIBLE);
                valid = false;
            } else if (tvErrDeadline != null) tvErrDeadline.setVisibility(View.GONE);

            if (!valid) return;

            String selectedSubject = subjects.isEmpty() ? assignment.getSubjectName() :
                    subjects.get(Math.max(0, spinnerSubject.getSelectedItemPosition())).getName();

            Assignment updated = new Assignment(
                    assignment.getId(), title, selectedSubject, deadline,
                    selPriority[0], selStatus[0], "done".equals(selStatus[0]));
            assignmentDAO.update(updated);
            refreshFromDB();
            Toast.makeText(getContext(), getString(R.string.toast_saved), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    // ─── Date+Time Picker ────────────────────────────────────────

    private void openDateTimePicker(Calendar cal, String[] selDeadline,
                                    TextView tvDisplay, TextView tvError) {
        DatePickerDialog datePicker = new DatePickerDialog(requireContext(),
                (view, year, month, day) -> {
                    cal.set(year, month, day);
                    TimePickerDialog timePicker = new TimePickerDialog(requireContext(),
                            (view2, hour, minute) -> {
                                cal.set(Calendar.HOUR_OF_DAY, hour);
                                cal.set(Calendar.MINUTE, minute);
                                String formatted = String.format(Locale.getDefault(),
                                        "%02d-%02d-%04d %02d:%02d",
                                        day, month + 1, year, hour, minute);
                                selDeadline[0] = formatted;
                                tvDisplay.setText("📅 " + formatted);
                                if (tvError != null) tvError.setVisibility(View.GONE);
                            },
                            cal.get(Calendar.HOUR_OF_DAY),
                            cal.get(Calendar.MINUTE), true);
                    timePicker.show();
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH));
        datePicker.show();
    }

    // ─── Priority Chip Helpers ───────────────────────────────────

    private void setupPriorityChips(TextView high, TextView medium, TextView low, String[] sel) {
        if (high != null) high.setOnClickListener(v -> {
            sel[0] = "Cao";
            highlightPriority("Cao", high, medium, low);
        });
        if (medium != null) medium.setOnClickListener(v -> {
            sel[0] = "Trung bình";
            highlightPriority("Trung bình", high, medium, low);
        });
        if (low != null) low.setOnClickListener(v -> {
            sel[0] = "Thấp";
            highlightPriority("Thấp", high, medium, low);
        });
    }

    private void highlightPriority(String p, TextView high, TextView medium, TextView low) {
        resetChip(high); resetChip(medium); resetChip(low);
        switch (p != null ? p : "") {
            case "Cao":
                if (high != null) { high.setBackgroundResource(R.drawable.bg_chip_red);
                    high.setTextColor(color(R.color.chip_red_text)); } break;
            case "Thấp":
                if (low != null) { low.setBackgroundResource(R.drawable.bg_chip_green);
                    low.setTextColor(color(R.color.chip_green_text)); } break;
            default:
                if (medium != null) { medium.setBackgroundResource(R.drawable.bg_chip_orange);
                    medium.setTextColor(color(R.color.chip_orange_text)); } break;
        }
    }

    // ─── Status Chip Helpers ─────────────────────────────────────

    private void setupStatusChips(TextView ns, TextView ip, TextView done, TextView late, String[] sel) {
        if (ns != null) ns.setOnClickListener(v -> { sel[0] = "not_started";
            highlightStatus("not_started", ns, ip, done, late); });
        if (ip != null) ip.setOnClickListener(v -> { sel[0] = "in_progress";
            highlightStatus("in_progress", ns, ip, done, late); });
        if (done != null) done.setOnClickListener(v -> { sel[0] = "done";
            highlightStatus("done", ns, ip, done, late); });
        if (late != null) late.setOnClickListener(v -> { sel[0] = "late";
            highlightStatus("late", ns, ip, done, late); });
    }

    private void highlightStatus(String status, TextView ns, TextView ip,
                                  TextView done, TextView late) {
        resetChip(ns); resetChip(ip); resetChip(done); resetChip(late);
        switch (status != null ? status : "not_started") {
            case "not_started":
                if (ns != null) { ns.setBackgroundResource(R.drawable.bg_chip_orange_selected);
                    ns.setTextColor(color(R.color.white)); } break;
            case "in_progress":
                if (ip != null) { ip.setBackgroundResource(R.drawable.bg_chip_blue);
                    ip.setTextColor(color(R.color.chip_blue_text)); } break;
            case "done":
                if (done != null) { done.setBackgroundResource(R.drawable.bg_chip_green);
                    done.setTextColor(color(R.color.chip_green_text)); } break;
            case "late":
                if (late != null) { late.setBackgroundResource(R.drawable.bg_chip_red);
                    late.setTextColor(color(R.color.chip_red_text)); } break;
        }
    }

    private void resetChip(TextView chip) {
        if (chip == null) return;
        chip.setBackgroundResource(R.drawable.bg_chip_unselected);
        chip.setTextColor(color(R.color.sp_text_secondary));
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private List<String> buildSubjectNames(List<Subject> subjects) {
        List<String> names = new ArrayList<>();
        if (subjects.isEmpty()) names.add("Chưa có môn — hãy thêm môn trước");
        else for (Subject s : subjects) names.add(s.getName());
        return names;
    }

    private int color(int colorRes) {
        return requireContext().getResources().getColor(colorRes);
    }
}
