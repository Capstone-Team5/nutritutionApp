package com.example.nutritionrealtimeapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;


public class SelectModeActivity extends AppCompatActivity {
    private CheckBox checkVoice; // 음성 모드
    private CheckBox checkBraille;  // 점자 모드
    private CheckBox checkVisual; // 시각화 모드
    private Button saveAllergyButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_mode);

        // 체크박스 찾기
        checkVoice = findViewById(R.id.voice_mode);
        checkBraille = findViewById(R.id.braille_mode);
        checkVisual = findViewById(R.id.visual_mode);
        saveAllergyButton = findViewById(R.id.saveAllergyButton);

        SharedPreferences sharedPreferences = getSharedPreferences("NutritionApp", MODE_PRIVATE);
        boolean isFirstLaunch = sharedPreferences.getBoolean("isFirstLaunch", true);


        boolean isVoiceMode = sharedPreferences.getBoolean("voiceMode", false);
        boolean isBrailleMode = sharedPreferences.getBoolean("brailleMode", false);
        boolean isVisualMode = sharedPreferences.getBoolean("visualMode", false);

        checkVoice.setChecked(isVoiceMode);
        checkBraille.setChecked(isBrailleMode);
        checkVisual.setChecked(isVisualMode);


        // 하나만 체크되도록 리스너 설정  -> 중복 선택 허용
        /*
        checkVoice.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkBraille.setChecked(false);
                checkVisual.setChecked(false);
            }
        });

        checkBraille.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkVoice.setChecked(false);
                checkVisual.setChecked(false);
            }
        });

        checkVisual.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                checkVoice.setChecked(false);
                checkBraille.setChecked(false);
            }
        });
        */
        // 버튼 클릭 시 저장
        saveAllergyButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();

            // 선택된 모드 저장
            editor.putBoolean("voiceMode", checkVoice.isChecked());
            editor.putBoolean("brailleMode", checkBraille.isChecked());
            editor.putBoolean("visualMode", checkVisual.isChecked());

            editor.apply();

            Toast.makeText(this, "모드가 저장되었습니다", Toast.LENGTH_SHORT).show();

            if(isFirstLaunch) {
                editor = sharedPreferences.edit();
                editor.putBoolean("isFirstLaunch", false);
                editor.apply();
                Intent intent = new Intent(this, ScanBarcodeActivity.class);
                startActivity(intent);
                finish();
            }
            else {
                Intent intent = new Intent(this, ScanBarcodeActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
