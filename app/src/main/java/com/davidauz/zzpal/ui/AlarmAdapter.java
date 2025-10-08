package com.davidauz.zzpal.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.davidauz.zzpal.R;
import com.davidauz.zzpal.entity.Alarm;

public class AlarmAdapter extends ListAdapter<Alarm, AlarmAdapter.AlarmViewHolder> {

    private final OnToggleListener onToggleListener;
    private final OnDeleteListener onDeleteListener;

    public interface OnToggleListener {
        void onToggle(Alarm alarm, boolean enabled);
    }

    public interface OnDeleteListener {
        void onDelete(Alarm alarm);
    }

    public AlarmAdapter(OnToggleListener onToggle, OnDeleteListener onDelete) {
        super(new DiffUtil.ItemCallback<Alarm>() {
            @Override
            public boolean areItemsTheSame(@NonNull Alarm oldItem, @NonNull Alarm newItem) {
                return oldItem.getId() == newItem.getId();
            }

            @Override
            public boolean areContentsTheSame(@NonNull Alarm oldItem, @NonNull Alarm newItem) {
                return oldItem.sameas(newItem);
            }
        });
        this.onToggleListener = onToggle;
        this.onDeleteListener = onDelete;
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alarm, parent, false);
        return new AlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        Alarm alarm = getItem(position);
        holder.bind(alarm, onToggleListener, onDeleteListener);
    }

    static class AlarmViewHolder extends RecyclerView.ViewHolder {
        private final TextView labelView;
        private final Switch toggleSwitch;
        private final ImageButton deleteButton;

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            labelView = itemView.findViewById(R.id.alarm_label);
            toggleSwitch = itemView.findViewById(R.id.alarm_toggle);
            deleteButton = itemView.findViewById(R.id.delete_button);
        }

        public void bind(Alarm alrm, OnToggleListener toggleListener, OnDeleteListener deleteListener) {
            labelView.setText(alrm.getTypeDescr()+" - "+alrm.hours+"h:"+ alrm.minutes+"m, "+alrm.durationSeconds+"s.");
            toggleSwitch.setChecked(alrm.enabled);

            toggleSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                toggleListener.onToggle(alrm, isChecked);
            });

            deleteButton.setOnClickListener(v -> {
                deleteListener.onDelete(alrm);
            });
        }
    }
}
