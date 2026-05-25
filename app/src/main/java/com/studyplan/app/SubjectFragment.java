package com.studyplan.app;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class SubjectFragment extends Fragment implements SubjectAdapter.OnSubjectActionListener {

    private RecyclerView rvSubjects;
    private SubjectAdapter adapter;
    private SubjectDAO subjectDAO;
    private TextView tvSubjectCount, tvCreditCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_subject, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        subjectDAO = new SubjectDAO(requireContext());

        rvSubjects = view.findViewById(R.id.rv_subjects);
        tvSubjectCount = view.findViewById(R.id.tv_subject_count);
        tvCreditCount = view.findViewById(R.id.tv_credit_count);
        EditText etSearch = view.findViewById(R.id.et_search);
        FloatingActionButton fabAdd = view.findViewById(R.id.fab_add_subject);

        List<Subject> subjectList = subjectDAO.getAll();
        adapter = new SubjectAdapter(subjectList, this);
        rvSubjects.setLayoutManager(new LinearLayoutManager(getContext()));
        rvSubjects.setAdapter(adapter);

        updateStats();

        if (etSearch != null) {
            etSearch.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    String query = s.toString().trim();
                    adapter.setData(query.isEmpty() ? subjectDAO.getAll() : subjectDAO.search(query));
                }
                @Override public void afterTextChanged(Editable s) {}
            });
        }

        if (fabAdd != null) fabAdd.setOnClickListener(v -> showAddSubjectDialog());
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshFromDB();
    }

    private void updateStats() {
        if (tvSubjectCount != null) tvSubjectCount.setText(String.valueOf(subjectDAO.getCount()));
        if (tvCreditCount != null) tvCreditCount.setText(String.valueOf(subjectDAO.getTotalCredits()));
    }

    private void refreshFromDB() {
        adapter.setData(subjectDAO.getAll());
        updateStats();
    }

    // ─── Adapter Callbacks ───────────────────────────────────────

    @Override
    public void onEdit(Subject subject, int position) {
        showEditSubjectDialog(subject);
    }

    @Override
    public void onDelete(Subject subject, int position) {
        new AlertDialog.Builder(requireContext(), R.style.RoundedDialog)
                .setTitle(getString(R.string.dialog_delete_subject))
                .setMessage(getString(R.string.dialog_delete_confirm) + "\n\n" + subject.getName())
                .setPositiveButton(getString(R.string.btn_delete), (dialog, which) -> {
                    subjectDAO.delete(subject.getId());
                    refreshFromDB();
                    Toast.makeText(getContext(), getString(R.string.toast_deleted), Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .show();
    }

    // ─── Add Subject Dialog ───────────────────────────────────────
    // Dialog KHÔNG tự đóng nếu validate fail (override positive button sau show()).

    private void showAddSubjectDialog() {
        View dv = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_subject, null);
        EditText etName = dv.findViewById(R.id.et_subject_name);
        EditText etTeacher = dv.findViewById(R.id.et_teacher);
        EditText etCredits = dv.findViewById(R.id.et_credits);
        EditText etSemester = dv.findViewById(R.id.et_semester);
        TextView chipPractice = dv.findViewById(R.id.chip_practice);
        TextView chipTheory = dv.findViewById(R.id.chip_theory);

        final String[] selectedType = {"Thực hành"};

        // Highlight mặc định
        if (chipPractice != null) {
            chipPractice.setBackgroundResource(R.drawable.bg_chip_green);
            chipPractice.setTextColor(color(R.color.chip_green_text));
        }

        setupTypeChips(chipPractice, chipTheory, selectedType);

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.RoundedDialog)
                .setTitle(getString(R.string.dialog_add_subject))
                .setView(dv)
                .setPositiveButton(getString(R.string.btn_add), null) // null → override sau
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .create();

        dialog.show();

        // Override positive — dialog KHÔNG tự đóng khi validate fail
        Button btnOk = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        btnOk.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String teacher = etTeacher.getText().toString().trim();
            String creditsStr = etCredits.getText().toString().trim();
            String semester = etSemester.getText().toString().trim();

            // Validate tên môn học
            if (name.isEmpty()) {
                Toast.makeText(getContext(), "⚠ Vui lòng nhập tên môn học", Toast.LENGTH_SHORT).show();
                etName.requestFocus();
                return; // Dialog KHÔNG đóng
            }

            // Validate tín chỉ — bọc parseInt trong try-catch
            int credits = 3;
            if (!creditsStr.isEmpty()) {
                try {
                    credits = Integer.parseInt(creditsStr);
                    if (credits <= 0) {
                        Toast.makeText(getContext(), "⚠ Số tín chỉ phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                        etCredits.requestFocus();
                        return; // Dialog KHÔNG đóng
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(),
                            "⚠ Số tín chỉ không hợp lệ — hãy nhập số nguyên (vd: 3)",
                            Toast.LENGTH_SHORT).show();
                    etCredits.requestFocus();
                    return; // Dialog KHÔNG đóng
                }
            }

            if (semester.isEmpty()) semester = "HK1";
            if (teacher.isEmpty()) teacher = "Chưa phân công";

            String colorTag = selectedType[0].equals("Thực hành") ? "green" : "blue";
            Subject newSubject = new Subject(0, name, teacher, credits, semester, selectedType[0], colorTag);
            subjectDAO.insert(newSubject);
            refreshFromDB();
            Toast.makeText(getContext(), getString(R.string.toast_added), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    // ─── Edit Subject Dialog ──────────────────────────────────────
    // Cũng không tự đóng khi validate fail.

    private void showEditSubjectDialog(Subject subject) {
        View dv = LayoutInflater.from(getContext()).inflate(R.layout.dialog_add_subject, null);
        EditText etName = dv.findViewById(R.id.et_subject_name);
        EditText etTeacher = dv.findViewById(R.id.et_teacher);
        EditText etCredits = dv.findViewById(R.id.et_credits);
        EditText etSemester = dv.findViewById(R.id.et_semester);
        TextView chipPractice = dv.findViewById(R.id.chip_practice);
        TextView chipTheory = dv.findViewById(R.id.chip_theory);

        // Pre-fill
        etName.setText(subject.getName());
        etTeacher.setText(subject.getTeacher());
        etCredits.setText(String.valueOf(subject.getCredits()));
        etSemester.setText(subject.getSemester());

        final String[] selectedType = {subject.getType()};

        // Highlight theo type hiện tại
        if ("Lý thuyết".equals(subject.getType())) {
            if (chipTheory != null) {
                chipTheory.setBackgroundResource(R.drawable.bg_chip_blue);
                chipTheory.setTextColor(color(R.color.chip_blue_text));
            }
            if (chipPractice != null) {
                chipPractice.setBackgroundResource(R.drawable.bg_chip_unselected);
                chipPractice.setTextColor(color(R.color.text_secondary));
            }
        } else {
            if (chipPractice != null) {
                chipPractice.setBackgroundResource(R.drawable.bg_chip_green);
                chipPractice.setTextColor(color(R.color.chip_green_text));
            }
            if (chipTheory != null) {
                chipTheory.setBackgroundResource(R.drawable.bg_chip_unselected);
                chipTheory.setTextColor(color(R.color.text_secondary));
            }
        }

        setupTypeChips(chipPractice, chipTheory, selectedType);

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.RoundedDialog)
                .setTitle(getString(R.string.dialog_edit_subject))
                .setView(dv)
                .setPositiveButton(getString(R.string.btn_save), null) // null → override sau
                .setNegativeButton(getString(R.string.btn_cancel), null)
                .create();

        dialog.show();

        Button btnOk = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        btnOk.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String teacher = etTeacher.getText().toString().trim();
            String creditsStr = etCredits.getText().toString().trim();
            String semester = etSemester.getText().toString().trim();

            // Validate tên môn học
            if (name.isEmpty()) {
                Toast.makeText(getContext(), "⚠ Vui lòng nhập tên môn học", Toast.LENGTH_SHORT).show();
                etName.requestFocus();
                return; // Dialog KHÔNG đóng
            }

            // Validate tín chỉ
            int credits = subject.getCredits();
            if (!creditsStr.isEmpty()) {
                try {
                    credits = Integer.parseInt(creditsStr);
                    if (credits <= 0) {
                        Toast.makeText(getContext(), "⚠ Số tín chỉ phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                        etCredits.requestFocus();
                        return; // Dialog KHÔNG đóng
                    }
                } catch (NumberFormatException e) {
                    Toast.makeText(getContext(),
                            "⚠ Số tín chỉ không hợp lệ — hãy nhập số nguyên (vd: 3)",
                            Toast.LENGTH_SHORT).show();
                    etCredits.requestFocus();
                    return; // Dialog KHÔNG đóng
                }
            }

            String colorTag = selectedType[0].equals("Thực hành") ? "green" : "blue";
            Subject updated = new Subject(
                    subject.getId(), name,
                    teacher.isEmpty() ? subject.getTeacher() : teacher,
                    credits,
                    semester.isEmpty() ? subject.getSemester() : semester,
                    selectedType[0], colorTag);

            subjectDAO.update(updated);
            refreshFromDB();
            Toast.makeText(getContext(), getString(R.string.toast_saved), Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private void setupTypeChips(TextView chipPractice, TextView chipTheory, String[] selectedType) {
        if (chipPractice != null) {
            chipPractice.setOnClickListener(v -> {
                selectedType[0] = "Thực hành";
                chipPractice.setBackgroundResource(R.drawable.bg_chip_green);
                chipPractice.setTextColor(color(R.color.chip_green_text));
                if (chipTheory != null) {
                    chipTheory.setBackgroundResource(R.drawable.bg_chip_unselected);
                    chipTheory.setTextColor(color(R.color.text_secondary));
                }
            });
        }
        if (chipTheory != null) {
            chipTheory.setOnClickListener(v -> {
                selectedType[0] = "Lý thuyết";
                chipTheory.setBackgroundResource(R.drawable.bg_chip_blue);
                chipTheory.setTextColor(color(R.color.chip_blue_text));
                if (chipPractice != null) {
                    chipPractice.setBackgroundResource(R.drawable.bg_chip_unselected);
                    chipPractice.setTextColor(color(R.color.text_secondary));
                }
            });
        }
    }

    private int color(int colorRes) {
        return requireContext().getResources().getColor(colorRes);
    }
}
