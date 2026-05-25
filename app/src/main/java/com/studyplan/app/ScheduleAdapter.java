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

public class ScheduleAdapter extends RecyclerView.Adapter<ScheduleAdapter.ViewHolder> {

    public interface OnScheduleActionListener {
        void onEdit(ScheduleItem item, int position);
        void onDelete(ScheduleItem item, int position);
    }

    private List<ScheduleItem> items;
    private OnScheduleActionListener listener;

    public ScheduleAdapter(List<ScheduleItem> items, OnScheduleActionListener listener) {
        this.items = new ArrayList<>(items);
        this.listener = listener;
    }

    public ScheduleAdapter(List<ScheduleItem> items) {
        this.items = new ArrayList<>(items);
        this.listener = null;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_timeline, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ScheduleItem item = items.get(position);

        holder.tvStartTime.setText(item.getStartTime());
        holder.tvEndTime.setText(item.getEndTime());
        holder.tvSubjectName.setText(item.getSubjectName());

        // Show day + room + teacher
        String dayLabel = item.getDayOfWeek() != null ? item.getDayOfWeek() + " · " : "";
        holder.tvDetails.setText(dayLabel + "📍 " + item.getRoom() + "  👤 " + item.getTeacher());

        // Color based on colorTag
        int accentColor;
        int lightColor;
        switch (item.getColorTag()) {
            case "blue":
                accentColor = R.color.blue_primary;
                lightColor = R.color.blue_light;
                break;
            case "green":
                accentColor = R.color.green_primary;
                lightColor = R.color.green_light;
                break;
            case "orange":
                accentColor = R.color.orange_primary;
                lightColor = R.color.orange_light;
                break;
            default: // purple
                accentColor = R.color.purple_primary;
                lightColor = R.color.purple_light;
                break;
        }

        int color = holder.itemView.getContext().getResources().getColor(accentColor);
        int light = holder.itemView.getContext().getResources().getColor(lightColor);

        holder.tvStartTime.setTextColor(color);
        holder.viewTimeLine.setBackgroundColor(light);
        holder.viewDivider.setBackgroundColor(color);

        // Status badge
        String statusText;
        int statusBg;
        int statusTextColor;
        switch (item.getStatus()) {
            case "done":
                statusText = "Đã xong";
                statusBg = R.drawable.bg_badge_green;
                statusTextColor = R.color.chip_green_text;
                break;
            case "in_progress":
                statusText = "Đang học";
                statusBg = R.drawable.bg_badge_blue;
                statusTextColor = R.color.chip_blue_text;
                break;
            default:
                statusText = "Sắp tới";
                statusBg = R.drawable.bg_badge_orange;
                statusTextColor = R.color.chip_orange_text;
                break;
        }
        holder.tvStatus.setText(statusText);
        holder.tvStatus.setBackgroundResource(statusBg);
        holder.tvStatus.setTextColor(holder.itemView.getContext().getResources().getColor(statusTextColor));

        // Long press to edit/delete
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) {
                listener.onDelete(item, holder.getAdapterPosition());
            }
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onEdit(item, holder.getAdapterPosition());
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void updateItems(List<ScheduleItem> newItems) {
        items.clear();
        items.addAll(newItems);
        notifyDataSetChanged();
    }

    public void addItem(ScheduleItem item) {
        items.add(item);
        notifyItemInserted(items.size() - 1);
    }

    public ScheduleItem getItem(int position) {
        if (position >= 0 && position < items.size()) {
            return items.get(position);
        }
        return null;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvStartTime, tvEndTime, tvSubjectName, tvDetails, tvStatus;
        View viewTimeLine, viewDivider;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvStartTime = itemView.findViewById(R.id.tv_start_time);
            tvEndTime = itemView.findViewById(R.id.tv_end_time);
            tvSubjectName = itemView.findViewById(R.id.tv_subject_name);
            tvDetails = itemView.findViewById(R.id.tv_details);
            tvStatus = itemView.findViewById(R.id.tv_status);
            viewTimeLine = itemView.findViewById(R.id.view_time_line);
            viewDivider = itemView.findViewById(R.id.view_divider);
        }
    }
}
