package com.flashlight;

import android.hardware.Sensor;

public class SensorItem {
    private final int id;
    private final String name;
    private final int sensorType; // -1 for GPS
    private Sensor sensor;
    private String valueText;

    public SensorItem(int id, String name, int sensorType) {
        this.id = id;
        this.name = name;
        this.sensorType = sensorType;
        this.valueText = "Показания: Датчик отсутствует на устройстве";
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getSensorType() {
        return sensorType;
    }

    public Sensor getSensor() {
        return sensor;
    }

    public void setSensor(Sensor sensor) {
        this.sensor = sensor;
    }

    public String getValueText() {
        return valueText;
    }

    public void setValueText(String valueText) {
        this.valueText = valueText;
    }
}