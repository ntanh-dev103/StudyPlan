package com.studyplan.app;

import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AssignmentAdapter extends RecyclerView.Adapter<AssignmentAdapter.ViewHolder> {

    public interface OnAssignmentActionListener {
        void onToggleDone(Assignment assignment, int position);
        void onClick(Assignment assignment, int position);
    }

    private List<Assignment> assignments;
    private List<Assignment> allAssignments;
    private OnAssignmentActionListener listener;

    public AssignmentAdapter(List<Assignment> assignments, OnAssignmentActionListener listener) {
        this.assignments = new ArrayList<>(assignments);
        this.allAssignments = new ArrayList<>(assignments);
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_assignment, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Assignment assignment = assignments.get(position);

        holder.tvTitle.setText(assignment.getTitle());
        holder.tvSubject.setText("📚 " + assignment.getSubjectName());
        holder.tvDeadline.setText("📅 " + assignment.getDeadline());
        holder.tvPriority.setText(assignment.getPriority());

        // Priority colors
        int priorityColor;
        switch (assignment.getPriority()) {
            case "Cao":
                priorityColor = R.color.priority_high;
                break;
            case "Trung bình":
                priorityColor = R.color.priority_medium;
                break;
            default:
                priorityColor = R.color.priority_low;
                break;
        }
        holder.tvPriority.setTextColor(holder.itemView.getContext().getResources().getColor(priorityColor));
        holder.viewPriorityDot.setBackgroundTintList(ColorStateList.valueOf(
                holder.itemView.getContext().getResources().getColor(priorityColor)));

        // Status badge
        String statusText;
        int statusBg;
        int statusTextColor;
        switch (assignment.getStatus()) {
            case "late":
                statusText = "Trễ";
                statusBg = R.drawable.bg_badge_red;
                statusTextColor = R.color.chip_red_text;
                break;
            case "done":
                statusText = "Đã xong";
                statusBg = R.drawable.bg_badge_green;
                statusTextColor = R.color.chip_green_text;
                break;
            case "in_progress":
                statusText = "Đang làm";
                statusBg = R.drawable.bg_badge_blue;
                statusTextColor = R.color.chip_blue_text;
                break;
            default:
                statusText = "Chưa làm";
                statusBg = R.drawable.bg_badge_orange;
                statusTextColor = R.color.chip_orange_text;
                break;
        }
        holder.tvStatus.setText(statusText);
        holder.tvStatus.setBackgroundResource(statusBg);
        holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(statusTextColor));

        // Checkbox state
        if (assignment.isDone()) {
            holder.ivCheckbox.setImageResource(android.R.drawable.checkbox_on_background);
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            holder.tvTitle.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_hint));
        } else {
            holder.ivCheckbox.setImageResource(android.R.drawable.checkbox_off_background);
            holder.tvTitle.setPaintFlags(holder.tvTitle.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
            holder.tvTitle.setTextColor(holder.itemView.getContext().getResources().getColor(R.color.text_primary));
        }

        holder.ivCheckbox.setOnClickListener(v -> {
            if (listener != null) listener.onToggleDone(assignment, holder.getAdapterPosition());
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(assignment, holder.getAdapterPosition());
        });
    }

    @Override
    public int getItemCount() {
        return assignments.size();
    }

    public void filterByStatus(String status) {
        assignments.clear();
        if (status.equals("all")) {
            assignments.addAll(allAssignments);
        } else {
            for (Assignment a : allAssignments) {
                if (a.getStatus().equals(status)) {
                    assignments.add(a);
                }
            }
        }
        notifyDataSetChanged();
    }

    public void addAssignment(Assignment assignment) {
        allAssignments.add(0, assignment);
        assignments.add(0, assignment);
        notifyItemInserted(0);
    }

    public void toggleDone(int position) {
        if (position >= 0 && position < assignments.size()) {
            Assignment a = assignments.get(position);
            a.setDone(!a.isDone());
            if (a.isDone()) {
                a.setStatus("done");
            } else {
                a.setStatus("in_progress");
            }
            notifyItemChanged(position);
        }
    }

    public int getCountByStatus(String status) {
        int count = 0;
        for (Assignment a : allAssignments) {
            if (a.getStatus().equals(status)) count++;
        }
        return count;
    }

    public int getTotalCount() {
        return allAssignments.size();
    }

    /**
     * Replace all data with a fresh list from the database.
     */
    public void setData(List<Assignment> newData) {
        allAssignments.clear();
        allAssignments.addAll(newData);
        assignments.clear();
        assignments.addAll(newData);
        notifyDataSetChanged();
    }

    /**
     * Get assignment at visible position.
     */
    public Assignment getAssignment(int position) {
        if (position >= 0 && position < assignments.size()) {
            return assignments.get(position);
        }
        return null;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCheckbox;
        TextView tvTitle, tvSubject, tvDeadline, tvPriority, tvStatus;
        View viewPriorityDot;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCheckbox = itemView.findViewById(R.id.iv_checkbox);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvSubject = itemView.findViewById(R.id.tv_subject);
            tvDeadline = itemView.findViewById(R.id.tv_deadline);
            tvPriority = itemView.findViewById(R.id.tv_priority);
            tvStatus = itemView.findViewById(R.id.tv_status);
            viewPriorityDot = itemView.findViewById(R.id.view_priority_dot);
        }
    }
}
