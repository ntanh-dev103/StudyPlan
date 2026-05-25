package com.studyplan.app;

import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SubjectAdapter extends RecyclerView.Adapter<SubjectAdapter.ViewHolder> {

    public interface OnSubjectActionListener {
        void onEdit(Subject subject, int position);
        void onDelete(Subject subject, int position);
    }

    private List<Subject> subjects;
    private List<Subject> allSubjects;
    private OnSubjectActionListener listener;

    public SubjectAdapter(List<Subject> subjects, OnSubjectActionListener listener) {
        this.subjects = new ArrayList<>(subjects);
        this.allSubjects = new ArrayList<>(subjects);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_subject, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Subject subject = subjects.get(position);

        holder.tvName.setText(subject.getName());
        holder.tvInfo.setText(subject.getTeacher() + " · " + subject.getCredits() + " tín chỉ · " + subject.getSemester());
        holder.tvType.setText(subject.getType());

        // Set color based on colorTag
        int colorRes;
        int chipBgRes;
        int chipTextColor;
        switch (subject.getColorTag()) {
            case "blue":
                colorRes = R.color.blue_primary;
                chipBgRes = R.drawable.bg_chip_blue;
                chipTextColor = R.color.chip_blue_text;
                break;
            case "orange":
                colorRes = R.color.orange_primary;
                chipBgRes = R.drawable.bg_chip_orange;
                chipTextColor = R.color.chip_orange_text;
                break;
            case "purple":
                colorRes = R.color.purple_primary;
                chipBgRes = R.drawable.bg_chip_purple;
                chipTextColor = R.color.chip_purple_text;
                break;
            case "red":
                colorRes = R.color.status_late;
                chipBgRes = R.drawable.bg_chip_red;
                chipTextColor = R.color.chip_red_text;
                break;
            default: // green
                colorRes = R.color.green_primary;
                chipBgRes = R.drawable.bg_chip_green;
                chipTextColor = R.color.chip_green_text;
                break;
        }

        holder.colorBar.setBackgroundColor(holder.itemView.getContext().getResources().getColor(colorRes));
        holder.tvType.setBackgroundResource(chipBgRes);
        holder.tvType.setTextColor(holder.itemView.getContext().getResources().getColor(chipTextColor));
        holder.btnEdit.setImageTintList(ColorStateList.valueOf(
                holder.itemView.getContext().getResources().getColor(colorRes)));

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEdit(subject, holder.getAdapterPosition());
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(subject, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return subjects.size();
    }

    public void filter(String query) {
        subjects.clear();
        if (query.isEmpty()) {
            subjects.addAll(allSubjects);
        } else {
            String lowerQuery = query.toLowerCase();
            for (Subject s : allSubjects) {
                if (s.getName().toLowerCase().contains(lowerQuery) ||
                    s.getTeacher().toLowerCase().contains(lowerQuery)) {
                    subjects.add(s);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void addSubject(Subject subject) {
        allSubjects.add(subject);
        subjects.add(subject);
        notifyItemInserted(subjects.size() - 1);
    }

    public void updateSubject(int position, Subject subject) {
        if (position >= 0 && position < subjects.size()) {
            Subject old = subjects.get(position);
            int allPos = allSubjects.indexOf(old);
            subjects.set(position, subject);
            if (allPos >= 0) allSubjects.set(allPos, subject);
            notifyItemChanged(position);
        }
    }

    public void removeSubject(int position) {
        if (position >= 0 && position < subjects.size()) {
            Subject removed = subjects.remove(position);
            allSubjects.remove(removed);
            notifyItemRemoved(position);
        }
    }

    /**
     * Replace all data with a fresh list from the database.
     */
    public void setData(List<Subject> newData) {
        allSubjects.clear();
        allSubjects.addAll(newData);
        subjects.clear();
        subjects.addAll(newData);
        notifyDataSetChanged();
    }

    public List<Subject> getAllSubjects() {
        return allSubjects;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        View colorBar;
        TextView tvName, tvInfo, tvType;
        ImageView btnEdit, btnDelete;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            colorBar = itemView.findViewById(R.id.view_color_bar);
            tvName = itemView.findViewById(R.id.tv_subject_name);
            tvInfo = itemView.findViewById(R.id.tv_subject_info);
            tvType = itemView.findViewById(R.id.tv_subject_type);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
