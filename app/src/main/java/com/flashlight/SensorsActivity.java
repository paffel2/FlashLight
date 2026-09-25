package com.flashlight;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.flashlight.databinding.ActivitySensorsBinding;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SensorsActivity extends AppCompatActivity implements SensorEventListener, LocationListener {

    private static final int PERMISSIONS_REQ_CODE = 1001;

    private SensorManager sensorManager;
    private LocationManager locationManager;

    private final List<SensorItem> sensorItems = new ArrayList<>();
    private SensorsAdapter adapter;
    private TriggerEventListener triggerEventListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        ActivitySensorsBinding binding = ActivitySensorsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            triggerEventListener = new TriggerEventListener() {
                @Override
                public void onTrigger(TriggerEvent event) {
                    if (event != null && event.sensor != null && adapter != null) {
                        int type = event.sensor.getType();
                        adapter.updateSensorValue(type, "Обнаружено движение!");
                        if (sensorManager != null) {
                            sensorManager.requestTriggerSensor(this, event.sensor);
                        }
                    }
                }
            };
        }

        initSensorList();

        adapter = new SensorsAdapter(sensorItems);
        binding.recyclerSensors.setAdapter(adapter);
        binding.textEmpty.setVisibility(View.GONE);

        checkPermissions();
    }

    private void initSensorList() {
        sensorItems.clear();

        // 1. Освещенность Ambient Light Sensor
        addSensorItem(1, "1. Освещенность (Ambient Light Sensor)", Sensor.TYPE_LIGHT);

        // 2. Наклон по осям Gyroscope
        addSensorItem(2, "2. Наклон по осям (Gyroscope)", Sensor.TYPE_GYROSCOPE);

        // 3. Количество шагов Pedometer
        addSensorItem(3, "3. Количество шагов (Pedometer)", Sensor.TYPE_STEP_COUNTER);

        // 4. Расстояние Proximity Sensor
        addSensorItem(4, "4. Расстояние (Proximity Sensor)", Sensor.TYPE_PROXIMITY);

        // 5. Магнитное поле Magnetic field
        addSensorItem(5, "5. Магнитное поле (Magnetic field)", Sensor.TYPE_MAGNETIC_FIELD);

        // 6. Поворот по осям Game rotation vector
        addSensorItem(6, "6. Поворот по осям (Game rotation vector)", Sensor.TYPE_GAME_ROTATION_VECTOR);

        // 7. Движение Motion detect
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            addSensorItem(7, "7. Движение (Motion detect)", Sensor.TYPE_MOTION_DETECT);
        } else {
            addSensorItem(7, "7. Движение (Motion detect)", Sensor.TYPE_SIGNIFICANT_MOTION);
        }

        // 8. Гравитация Gravity
        addSensorItem(8, "8. Гравитация (Gravity)", Sensor.TYPE_GRAVITY);

        // 9. Линейное ускорение Linear acceleration
        addSensorItem(9, "9. Линейное ускорение (Linear acceleration)", Sensor.TYPE_LINEAR_ACCELERATION);

        // 10. Абсолютное ускорение Accelerometer
        addSensorItem(10, "10. Абсолютное ускорение (Accelerometer)", Sensor.TYPE_ACCELEROMETER);

        // 11. Температура Temperature Sensor
        SensorItem tempItem = addSensorItem(11, "11. Температура (Temperature Sensor)", Sensor.TYPE_AMBIENT_TEMPERATURE);
        if (tempItem.getSensor() == null) {
            // Запасной вариант для более старых систем
            addSensorItemFallback(tempItem, Sensor.TYPE_TEMPERATURE);
        }

        // 12. Атмосферное давление Atmosphere Pressure
        addSensorItem(12, "12. Атмосферное давление (Atmosphere Pressure)", Sensor.TYPE_PRESSURE);

        // 13. Влажность Humidity Sensor
        addSensorItem(13, "13. Влажность (Humidity Sensor)", Sensor.TYPE_RELATIVE_HUMIDITY);

        // 14. Географическая позиция устройства по широте и долготе GPS
        SensorItem gpsItem = new SensorItem(14, "14. Географическая позиция (GPS)", -1);
        gpsItem.setValueText("Показания: Запрос GPS координат...");
        sensorItems.add(gpsItem);
    }

    private SensorItem addSensorItem(int id, String name, int sensorType) {
        SensorItem item = new SensorItem(id, name, sensorType);
        if (sensorManager != null) {
            Sensor sensor = sensorManager.getDefaultSensor(sensorType);
            if (sensor != null) {
                item.setSensor(sensor);
                item.setValueText("Показания: Ожидание данных...");
            }
        }
        sensorItems.add(item);
        return item;
    }

    private void addSensorItemFallback(SensorItem item, int fallbackType) {
        if (sensorManager != null) {
            Sensor sensor = sensorManager.getDefaultSensor(fallbackType);
            if (sensor != null) {
                item.setSensor(sensor);
                item.setValueText("Показания: Ожидание данных...");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sensorManager != null) {
            for (SensorItem item : sensorItems) {
                if (item.getSensor() != null) {
                    int type = item.getSensor().getType();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && (type == Sensor.TYPE_MOTION_DETECT || type == Sensor.TYPE_SIGNIFICANT_MOTION)) {
                        if (triggerEventListener != null) {
                            sensorManager.requestTriggerSensor(triggerEventListener, item.getSensor());
                        }
                    } else {
                        sensorManager.registerListener(this, item.getSensor(), SensorManager.SENSOR_DELAY_UI);
                    }
                }
            }
        }
        startLocationUpdates();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
            for (SensorItem item : sensorItems) {
                if (item.getSensor() != null) {
                    int type = item.getSensor().getType();
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && (type == Sensor.TYPE_MOTION_DETECT || type == Sensor.TYPE_SIGNIFICANT_MOTION)) {
                        if (triggerEventListener != null) {
                            sensorManager.cancelTriggerSensor(triggerEventListener, item.getSensor());
                        }
                    }
                }
            }
        }
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
    }

    private void checkPermissions() {
        List<String> needed = new ArrayList<>();
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            needed.add(Manifest.permission.ACCESS_FINE_LOCATION);
            needed.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
                needed.add(Manifest.permission.ACTIVITY_RECOGNITION);
            }
        }

        if (!needed.isEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toArray(new String[0]), PERMISSIONS_REQ_CODE);
        } else {
            startLocationUpdates();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQ_CODE) {
            startLocationUpdates();
        }
    }

    @SuppressWarnings("MissingPermission")
    private void startLocationUpdates() {
        if (locationManager == null) return;
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 1, this);
                    Location lastLoc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (lastLoc != null) {
                        onLocationChanged(lastLoc);
                    }
                } else if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 1, this);
                    Location lastLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
                    if (lastLoc != null) {
                        onLocationChanged(lastLoc);
                    }
                } else {
                    if (adapter != null) adapter.updateGpsValue("GPS выключен в настройках устройства");
                }
            }
        } catch (Exception e) {
            if (adapter != null) adapter.updateGpsValue("Ошибка доступа к GPS");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.sensor == null || adapter == null) return;

        float[] v = event.values;
        if (v == null || v.length == 0) return;

        int type = event.sensor.getType();
        String formattedText;
        switch (type) {
            case Sensor.TYPE_LIGHT:
                formattedText = String.format(Locale.US, "%.1f lx", v[0]);
                break;
            case Sensor.TYPE_GYROSCOPE:
                formattedText = v.length >= 3 ? String.format(Locale.US, "X: %.2f, Y: %.2f, Z: %.2f рад/с", v[0], v[1], v[2]) : String.format(Locale.US, "X: %.2f", v[0]);
                break;
            case Sensor.TYPE_STEP_COUNTER:
                formattedText = String.format(Locale.US, "%d шагов", (int) v[0]);
                break;
            case Sensor.TYPE_PROXIMITY:
                float maxRange = event.sensor.getMaximumRange();
                boolean isNear = v[0] < maxRange;
                formattedText = String.format(Locale.US, "%.1f см (%s)", v[0], isNear ? "Объект близко" : "Объект далеко");
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                formattedText = v.length >= 3 ? String.format(Locale.US, "X: %.1f, Y: %.1f, Z: %.1f мкТл", v[0], v[1], v[2]) : String.format(Locale.US, "X: %.1f", v[0]);
                break;
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
                formattedText = v.length >= 3 ? String.format(Locale.US, "X: %.2f, Y: %.2f, Z: %.2f", v[0], v[1], v[2]) : String.format(Locale.US, "X: %.2f", v[0]);
                break;
            case Sensor.TYPE_MOTION_DETECT:
            case Sensor.TYPE_SIGNIFICANT_MOTION:
                formattedText = v[0] > 0 ? "Обнаружено движение" : "Покой";
                break;
            case Sensor.TYPE_GRAVITY:
            case Sensor.TYPE_LINEAR_ACCELERATION:
            case Sensor.TYPE_ACCELEROMETER:
                formattedText = v.length >= 3 ? String.format(Locale.US, "X: %.2f, Y: %.2f, Z: %.2f м/с²", v[0], v[1], v[2]) : String.format(Locale.US, "X: %.2f", v[0]);
                break;
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
            case Sensor.TYPE_TEMPERATURE:
                formattedText = String.format(Locale.US, "%.1f °C", v[0]);
                break;
            case Sensor.TYPE_PRESSURE:
                formattedText = String.format(Locale.US, "%.1f гПа (hPa)", v[0]);
                break;
            case Sensor.TYPE_RELATIVE_HUMIDITY:
                formattedText = String.format(Locale.US, "%.1f %%", v[0]);
                break;
            default:
                formattedText = String.format(Locale.US, "%.2f", v[0]);
                break;
        }

        adapter.updateSensorValue(type, formattedText);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        if (adapter != null) {
            String gpsString = String.format(Locale.US, "Широта (Lat): %.5f\nДолгота (Lon): %.5f\nТочность: %.1f м",
                    location.getLatitude(), location.getLongitude(), location.getAccuracy());
            adapter.updateGpsValue(gpsString);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_sensors, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        if (item.getItemId() == R.id.action_flashlight) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}