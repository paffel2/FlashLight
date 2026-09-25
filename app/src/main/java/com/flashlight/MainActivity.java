package com.flashlight;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.flashlight.databinding.ActivityMainBinding;
import com.nothing.ketchum.Common;
import com.nothing.ketchum.Glyph;
import com.nothing.ketchum.GlyphException;
import com.nothing.ketchum.GlyphFrame;
import com.nothing.ketchum.GlyphManager;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "FlashlightApp";

    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashOn = false;

    private GlyphManager glyphManager;
    private GlyphFrame.Builder glyphFrameBuilder;
    private boolean isGlyphServiceReady = false;
    private boolean glyphsOn = false;

    private Button buttonFlashlight;
    private Button buttonGlyph;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        buttonFlashlight = binding.buttonFlashlight;
        buttonGlyph = binding.buttonGlyph;

        initCamera();

        buttonFlashlight.setOnClickListener(v -> toggleFlashlight());
        buttonGlyph.setOnClickListener(v -> toggleGlyphs());

        initGlyph();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_sensors) {
            Intent intent = new Intent(this, SensorsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initCamera() {
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (cameraManager != null) {
                for (String id : cameraManager.getCameraIdList()) {
                    CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(id);
                    Boolean hasFlash = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    if (hasFlash != null && hasFlash) {
                        cameraId = id;
                        break;
                    }
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Ошибка доступа к камере: " + e.getMessage());
        }
    }

    private boolean isNothingDevice() {
        String manufacturer = Build.MANUFACTURER != null ? Build.MANUFACTURER : "";
        String brand = Build.BRAND != null ? Build.BRAND : "";
        return manufacturer.equalsIgnoreCase("Nothing") ||
               brand.equalsIgnoreCase("Nothing") ||
               Common.is20111() || Common.is22111() || Common.is23111() ||
               Common.is24111() || Common.is23113() || Common.is23112() ||
               Common.is25111() || Common.is25111p() || Common.is25131();
    }

    private boolean registerGlyphDevice() {
        if (glyphManager == null) return false;

        Log.d(TAG, "Build.MODEL: " + Build.MODEL + ", Manufacturer: " + Build.MANUFACTURER);

        if (Common.is20111()) {
            return glyphManager.register(Glyph.DEVICE_20111);
        } else if (Common.is22111()) {
            return glyphManager.register(Glyph.DEVICE_22111);
        } else if (Common.is23111()) {
            return glyphManager.register(Glyph.DEVICE_23111);
        } else if (Common.is24111()) {
            return glyphManager.register(Glyph.DEVICE_24111);
        } else if (Common.is23113()) {
            return glyphManager.register(Glyph.DEVICE_23113);
        } else if (Common.is23112()) {
            return glyphManager.register(Glyph.DEVICE_23112);
        } else if (Common.is25111()) {
            return glyphManager.register(Glyph.DEVICE_25111);
        } else if (Common.is25111p()) {
            return glyphManager.register(Glyph.DEVICE_25111p);
        } else if (Common.is25131()) {
            return glyphManager.register(Glyph.DEVICE_25131);
        } else {
            return glyphManager.register();
        }
    }

    private void initGlyph() {
        if (!isNothingDevice()) {
            buttonGlyph.setVisibility(View.GONE);
            return;
        }

        buttonGlyph.setVisibility(View.VISIBLE);
        buttonGlyph.setText("Включить Glyph");

        glyphManager = GlyphManager.getInstance(getApplicationContext());
        if (glyphManager == null) {
            return;
        }

        glyphManager.init(new GlyphManager.Callback() {
            @Override
            public void onServiceConnected(ComponentName name) {
                boolean registered = registerGlyphDevice();
                Log.d(TAG, "Glyph service connected. Registered: " + registered);
                if (registered) {
                    try {
                        glyphManager.openSession();
                        glyphFrameBuilder = glyphManager.getGlyphFrameBuilder();
                        isGlyphServiceReady = true;
                        Log.d(TAG, "Glyph session opened successfully");
                    } catch (GlyphException e) {
                        Log.e(TAG, "Ошибка открытия сессии Glyph: " + e.getMessage());
                    }
                } else {
                    Log.e(TAG, "Не удалось зарегистрировать модель Glyph");
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                isGlyphServiceReady = false;
                glyphFrameBuilder = null;
                Log.d(TAG, "Glyph service disconnected");
            }
        });
    }

    private void toggleFlashlight() {
        if (cameraId == null) {
            Toast.makeText(this, "Фонарик недоступен на этом устройстве", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            isFlashOn = !isFlashOn;
            cameraManager.setTorchMode(cameraId, isFlashOn);

            if (isFlashOn) {
                buttonFlashlight.setBackgroundTintList(ColorStateList.valueOf(Color.YELLOW));
                buttonFlashlight.setTextColor(Color.BLACK);
                buttonFlashlight.setText("Выключить фонарик");
            } else {
                int defaultColor = ContextCompat.getColor(this, R.color.white);
                buttonFlashlight.setBackgroundTintList(ColorStateList.valueOf(defaultColor));
                buttonFlashlight.setTextColor(Color.BLACK);
                buttonFlashlight.setText("Включить фонарик");
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Ошибка переключения фонарика: " + e.getMessage());
            Toast.makeText(this, "Ошибка доступа к камере", Toast.LENGTH_SHORT).show();
        }
    }

    private void toggleGlyphs() {
        if (!isGlyphServiceReady || glyphFrameBuilder == null || glyphManager == null) {
            Toast.makeText(this, "Сервис Glyph еще не готов", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (!glyphsOn) {
                GlyphFrame frame = glyphFrameBuilder
                        .buildChannelA()
                        .buildChannelB()
                        .buildChannelC()
                        .buildChannelD()
                        .buildChannelE()
                        .build();

                glyphManager.toggle(frame);
                glyphsOn = true;

                buttonGlyph.setText("Выключить Glyph");
                buttonGlyph.setBackgroundTintList(ColorStateList.valueOf(Color.YELLOW));
                buttonGlyph.setTextColor(Color.BLACK);
            } else {
                glyphManager.turnOff();
                glyphsOn = false;

                buttonGlyph.setText("Включить Glyph");
                int defaultColor = ContextCompat.getColor(this, R.color.white);
                buttonGlyph.setBackgroundTintList(ColorStateList.valueOf(defaultColor));
                buttonGlyph.setTextColor(Color.BLACK);
            }
        } catch (Exception e) {
            Log.e(TAG, "Ошибка переключения Glyph: " + e.getMessage(), e);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (glyphsOn && glyphManager != null) {
            try {
                glyphManager.turnOff();
            } catch (Exception ignored) {}
        }
        if (glyphManager != null) {
            try {
                glyphManager.closeSession();
            } catch (Exception ignored) {}
            try {
                glyphManager.unInit();
            } catch (Exception ignored) {}
        }
    }
}