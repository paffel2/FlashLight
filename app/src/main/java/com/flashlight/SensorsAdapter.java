package com.flashlight;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class SensorsAdapter extends RecyclerView.Adapter<SensorsAdapter.SensorViewHolder> {

    private final List<SensorItem> items;

    public SensorsAdapter(List<SensorItem> items) {
        this.items = items;
    }

    public void updateSensorValue(int sensorType, String formattedValues) {
        for (int i = 0; i < items.size(); i++) {
            SensorItem item = items.get(i);
            if (item.getSensorType() == sensorType) {
                item.setValueText("Показания: " + formattedValues);
                notifyItemChanged(i, "value_update");
                break;
            }
        }
    }

    public void updateGpsValue(String gpsText) {
        for (int i = 0; i < items.size(); i++) {
            SensorItem item = items.get(i);
            if (item.getSensorType() == -1) { // GPS item
                item.setValueText("Показания: " + gpsText);
                notifyItemChanged(i, "value_update");
                break;
            }
        }
    }

    @NonNull
    @Override
    public SensorViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_sensor, parent, false);
        return new SensorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SensorViewHolder holder, int position) {
        SensorItem item = items.get(position);
        holder.textName.setText(item.getName());
        holder.textValues.setText(item.getValueText());
    }

    @Override
    public void onBindViewHolder(@NonNull SensorViewHolder holder, int position, @NonNull List<Object> payloads) {
        if (!payloads.isEmpty()) {
            SensorItem item = items.get(position);
            holder.textValues.setText(item.getValueText());
        } else {
            super.onBindViewHolder(holder, position, payloads);
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class SensorViewHolder extends RecyclerView.ViewHolder {
        TextView textName;
        TextView textValues;

        public SensorViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_sensor_name);
            textValues = itemView.findViewById(R.id.text_sensor_values);
        }
    }
}