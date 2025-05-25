package com.example.nutritionrealtimeapp;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class SelectNutritionInfoActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_nutrition_info);

        CheckBox checkBoxCalories = findViewById(R.id.checkBox_calories);
        CheckBox checkBoxProtein = findViewById(R.id.checkBox_protein);
        CheckBox checkBoxFat = findViewById(R.id.checkBox_fat);
        CheckBox checkBoxCarbs = findViewById(R.id.checkBox_carbs);
        CheckBox checkBoxPotassium = findViewById(R.id.checkBox_potassium);
        CheckBox checkBoxSodium = findViewById(R.id.checkBox_sodium);
        CheckBox checkBoxCholesterol = findViewById(R.id.checkBox_cholesterol);
        CheckBox checkBoxSaturatedFA = findViewById(R.id.checkBox_saturatedFattyAcids);
        CheckBox checkBoxCaffeine = findViewById(R.id.checkBox_caffeine);
        CheckBox checkBoxMass = findViewById(R.id.checkBox_mass);
        CheckBox checkBoxCountry = findViewById(R.id.checkBox_country);

        Button saveButton = findViewById(R.id.saveButton);

        sharedPreferences = getSharedPreferences("NutritionApp", Context.MODE_PRIVATE);
        boolean isFirstLaunch = sharedPreferences.getBoolean("isFirstLaunch", true);

        // 저장된 값이 있으면 체크박스 상태 복원
        Set<String> savedNutritionInfoSet = sharedPreferences.getStringSet("selectedNutritionInfo", null);
        if (savedNutritionInfoSet != null) {
            if (savedNutritionInfoSet.contains("칼로리")) checkBoxCalories.setChecked(true);
            if (savedNutritionInfoSet.contains("단백질")) checkBoxProtein.setChecked(true);
            if (savedNutritionInfoSet.contains("지방")) checkBoxFat.setChecked(true);
            if (savedNutritionInfoSet.contains("탄수화물")) checkBoxCarbs.setChecked(true);
            if (savedNutritionInfoSet.contains("칼륨")) checkBoxPotassium.setChecked(true);
            if (savedNutritionInfoSet.contains("나트륨")) checkBoxSodium.setChecked(true);
            if (savedNutritionInfoSet.contains("콜레스테롤")) checkBoxCholesterol.setChecked(true);
            if (savedNutritionInfoSet.contains("포화지방산")) checkBoxSaturatedFA.setChecked(true);
            if (savedNutritionInfoSet.contains("카페인")) checkBoxCaffeine.setChecked(true);
            if (savedNutritionInfoSet.contains("질량")) checkBoxMass.setChecked(true);
            if (savedNutritionInfoSet.contains("원산지")) checkBoxCountry.setChecked(true);
        }

        // 저장 버튼 클릭 이벤트
        saveButton.setOnClickListener(v -> {
            List<String> selectedNutritionInfo = new ArrayList<>();
            if (checkBoxCalories.isChecked()) selectedNutritionInfo.add("칼로리");
            if (checkBoxProtein.isChecked()) selectedNutritionInfo.add("단백질");
            if (checkBoxFat.isChecked()) selectedNutritionInfo.add("지방");
            if (checkBoxCarbs.isChecked()) selectedNutritionInfo.add("탄수화물");
            if (checkBoxPotassium.isChecked()) selectedNutritionInfo.add("칼륨");
            if (checkBoxSodium.isChecked()) selectedNutritionInfo.add("나트륨");
            if (checkBoxCholesterol.isChecked()) selectedNutritionInfo.add("콜레스테롤");
            if (checkBoxSaturatedFA.isChecked()) selectedNutritionInfo.add("포화지방산");
            if (checkBoxCaffeine.isChecked()) selectedNutritionInfo.add("카페인");
            if (checkBoxMass.isChecked()) selectedNutritionInfo.add("질량");
            if (checkBoxCountry.isChecked()) selectedNutritionInfo.add("원산지");

            if (selectedNutritionInfo.isEmpty()) {
                Toast.makeText(this, "최소한 하나의 영양정보를 선택하세요!", Toast.LENGTH_SHORT).show();
            } else {
                // SharedPreferences에 선택한 데이터 저장
                saveNutritionInfoToPreferences(selectedNutritionInfo);

                if(isFirstLaunch) {
                    Intent intent = new Intent(this, SelectModeActivity.class);
                    startActivity(intent);
                    finish();
                }
                else {
                    Intent intent = new Intent(this, ScanBarcodeActivity.class);
                    startActivity(intent);
                    finish();
                }
            }
        });
    }

    // SharedPreferences에 영양 정보를 저장
    private void saveNutritionInfoToPreferences(List<String> selectedNutritionInfo) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("selectedNutritionInfo", new HashSet<>(selectedNutritionInfo));
        editor.apply();
    }
}
