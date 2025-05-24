package com.example.nutritionrealtimeapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashSet;
import java.util.Set;

public class SelectAllergiesActivity extends AppCompatActivity {
    private static final String PREF_NAME = "NutritionApp";
    private static final String KEY_SELECTED_ALLERGIES = "selectedAllergies";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_allergies);

        SharedPreferences sharedPreferences = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isFirstLaunch = sharedPreferences.getBoolean("isFirstLaunch", true);

        CheckBox checkBoxPeanut = findViewById(R.id.checkBox_peanut);
        CheckBox checkBoxDairy = findViewById(R.id.checkBox_dairy);
        CheckBox checkBoxGluten = findViewById(R.id.checkBox_gluten);
        Button saveAllergyButton = findViewById(R.id.saveAllergyButton);

        // 저장된 알레르기 목록 불러오기
        Set<String> savedAllergies = sharedPreferences.getStringSet(KEY_SELECTED_ALLERGIES, new HashSet<>());

        // 체크박스 상태 설정
        checkBoxPeanut.setChecked(savedAllergies.contains("땅콩"));
        checkBoxDairy.setChecked(savedAllergies.contains("유제품"));
        checkBoxGluten.setChecked(savedAllergies.contains("글루텐"));

        saveAllergyButton.setOnClickListener(v -> {
            Set<String> selectedAllergies = new HashSet<>();
            if (checkBoxPeanut.isChecked()) selectedAllergies.add("땅콩");
            if (checkBoxDairy.isChecked()) selectedAllergies.add("유제품");
            if (checkBoxGluten.isChecked()) selectedAllergies.add("글루텐");

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putStringSet(KEY_SELECTED_ALLERGIES, selectedAllergies);
            editor.apply();

            Toast.makeText(this, "알러지 정보가 저장되었습니다", Toast.LENGTH_SHORT).show();

            if (isFirstLaunch) {
                Intent intent = new Intent(this, SelectNutritionInfoActivity.class);
                startActivity(intent);
                finish();
            } else {
                Intent intent = new Intent(this, ScanBarcodeActivity.class);
                startActivity(intent);
                finish();
            }
        });
    }
}
