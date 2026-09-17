package com.flashlight;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.nothing.ketchum.Common;
import com.nothing.ketchum.Glyph;
import com.nothing.ketchum.GlyphException;
import com.nothing.ketchum.GlyphFrame;
import com.nothing.ketchum.GlyphManager;
import android.content.ComponentName;
import android.util.Log;

import com.flashlight.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private CameraManager cameraManager;
    private String cameraId;
    private boolean isFlashOn = false;

    private GlyphManager glyphManager;
    private GlyphManager.Callback glyphCallback;
    private boolean glyphsOn = false;

    private Button buttonFlashlight;
    private Button buttonGlyph;
    private TextView errorText;
    private GlyphFrame.Builder glyphFrameBuilder; // Сохраняем билдер, когда сервис готов
    private boolean isGlyphServiceReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);

        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Инициализация камеры
        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            // Берём камеру с вспышкой
            for (String id : cameraManager.getCameraIdList()) {
                Boolean hasFlash = cameraManager.getCameraCharacteristics(id)
                        .get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
                if (hasFlash != null && hasFlash) {
                    cameraId = id;
                    break;
                }
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

        // Кнопка
        buttonFlashlight = binding.buttonFlashlight;
        buttonFlashlight.setOnClickListener(v -> toggleFlashlight());

        if (Common.is24111())
        {
            initGlyph();

            buttonGlyph = binding.buttonGlyph;
            buttonGlyph.setOnClickListener(v -> toggleGlyphs());}
        else {
            binding.buttonGlyph.setVisibility(View.GONE);
            binding.buttonGlyph.setEnabled(false);
        }

    }

    private void initGlyph() {
        glyphManager = GlyphManager.getInstance(getApplicationContext());
        glyphManager.init(new GlyphManager.Callback() {
            @Override
            public void onServiceConnected(ComponentName name) {
                // Сервис подключился — теперь билдер доступен
                if (Common.is24111()) { // Для Phone (3a) — is24111() или is23111()
                    glyphManager.register(Glyph.DEVICE_24111);
                }
                try {
                    glyphManager.openSession();
                    // Сохраняем билдер один раз, когда сервис готов
                    glyphFrameBuilder = glyphManager.getGlyphFrameBuilder();
                    isGlyphServiceReady = true;
                    Log.d("Glyph", "Сервис глифов готов");
                } catch (GlyphException e) {
                    Log.e("Glyph", "Ошибка сессии: " + e.getMessage());
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                isGlyphServiceReady = false;
                glyphFrameBuilder = null;
                try {
                    glyphManager.closeSession();
                } catch (GlyphException e){
                    Log.e("Glyph", "Ошибка сессии: " + e.getMessage());
                }
            }
        });
    }

    private void toggleGlyphs() {
        if (!isGlyphServiceReady || glyphFrameBuilder == null || glyphManager == null) {
            Toast.makeText(this, "Сервис Glyph еще не готов", Toast.LENGTH_SHORT).show();
            errorText.setText("Сервис Glyph еще не готов");
            return;
        }

        try {
            if (!glyphsOn) {
                // Включаем глифы на всех каналах
                GlyphFrame frame = glyphFrameBuilder
                        .buildChannelA()
                        .buildChannelB()
                        .buildChannelC()
                        .build();

                glyphManager.toggle(frame);
                glyphsOn = true;

                buttonGlyph.setBackgroundTintList(ColorStateList.valueOf(Color.YELLOW));
            } else {
                // Выключаем глифы
                glyphManager.turnOff();
                glyphsOn = false;

                int defaultColor = ContextCompat.getColor(this, R.color.white);
                buttonGlyph.setBackgroundTintList(ColorStateList.valueOf(defaultColor));
            }
        } catch (Exception e) {
            Log.e("Glyph", "Ошибка: " + e.getMessage());
        }
    }

    private void toggleFlashlight() {
        if (cameraId == null) {
            Toast.makeText(this, "Фонарик недоступен на этом устройстве",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            isFlashOn = !isFlashOn;
            cameraManager.setTorchMode(cameraId, isFlashOn);

            if (isFlashOn) {
                // Включён — делаем кнопку жёлтой, текст меняем
                buttonFlashlight.setBackgroundColor(Color.YELLOW);
                buttonFlashlight.setText("Выключить");
            } else {
                // Выключен — возвращаем обычный вид
                int disable_color = ContextCompat.getColor(this, R.color.white);
                buttonFlashlight.setBackgroundColor(disable_color);
                buttonFlashlight.setText("Фонарик");
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка доступа к камере",
                    Toast.LENGTH_SHORT).show();
        }
    }
}