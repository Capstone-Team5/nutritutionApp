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
        CheckBox checkBoxDietaryFiber = findViewById(R.id.checkBox_dietaryFiber);
        CheckBox checkBoxCalcium = findViewById(R.id.checkBox_calcium);
        CheckBox checkBoxSodium = findViewById(R.id.checkBox_sodium);
        CheckBox checkBoxVitaminA = findViewById(R.id.checkBox_vitaminA);
        CheckBox checkBoxVitaminC = findViewById(R.id.checkBox_vitaminC);
        CheckBox checkBoxVitaminD = findViewById(R.id.checkBox_vitaminD);
        CheckBox checkBoxCholesterol = findViewById(R.id.checkBox_cholesterol);
        CheckBox checkBoxSaturatedFA = findViewById(R.id.checkBox_saturatedFattyAcids);
        CheckBox checkBoxTransFA = findViewById(R.id.checkBox_transFattyAcids);
        CheckBox checkBoxFructose = findViewById(R.id.checkBox_fructose);
        CheckBox checkBoxLactose = findViewById(R.id.checkBox_lactose);
        CheckBox checkBoxCaffeine = findViewById(R.id.checkBox_caffeine);
        CheckBox checkBoxMagnesium = findViewById(R.id.checkBox_magnesium);
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
            if (savedNutritionInfoSet.contains("식이섬유")) checkBoxDietaryFiber.setChecked(true);
            if (savedNutritionInfoSet.contains("칼슘")) checkBoxCalcium.setChecked(true);
            if (savedNutritionInfoSet.contains("나트륨")) checkBoxSodium.setChecked(true);
            if (savedNutritionInfoSet.contains("비타민 A")) checkBoxVitaminA.setChecked(true);
            if (savedNutritionInfoSet.contains("비타민 C")) checkBoxVitaminC.setChecked(true);
            if (savedNutritionInfoSet.contains("비타민 D")) checkBoxVitaminD.setChecked(true);
            if (savedNutritionInfoSet.contains("콜레스테롤")) checkBoxCholesterol.setChecked(true);
            if (savedNutritionInfoSet.contains("포화지방산")) checkBoxSaturatedFA.setChecked(true);
            if (savedNutritionInfoSet.contains("트랜스지방산")) checkBoxTransFA.setChecked(true);
            if (savedNutritionInfoSet.contains("과당")) checkBoxFructose.setChecked(true);
            if (savedNutritionInfoSet.contains("유당")) checkBoxLactose.setChecked(true);
            if (savedNutritionInfoSet.contains("카페인")) checkBoxCaffeine.setChecked(true);
            if (savedNutritionInfoSet.contains("마그네슘")) checkBoxMagnesium.setChecked(true);
            if (savedNutritionInfoSet.contains("원산지")) checkBoxCountry.setChecked(true);
        }

        // 저장 버튼 클릭 이벤트
        saveButton.setOnClickListener(v -> {
            List<String> selectedNutritionInfo = new ArrayList<>();
            if (checkBoxCalories.isChecked()) selectedNutritionInfo.add("칼로리");
            if (checkBoxProtein.isChecked()) selectedNutritionInfo.add("단백질");
            if (checkBoxFat.isChecked()) selectedNutritionInfo.add("지방");
            if (checkBoxCarbs.isChecked()) selectedNutritionInfo.add("탄수화물");
            if (checkBoxDietaryFiber.isChecked()) selectedNutritionInfo.add("식이섬유");
            if (checkBoxCalcium.isChecked()) selectedNutritionInfo.add("칼슘");
            if (checkBoxSodium.isChecked()) selectedNutritionInfo.add("나트륨");
            if (checkBoxVitaminA.isChecked()) selectedNutritionInfo.add("비타민 A");
            if (checkBoxVitaminC.isChecked()) selectedNutritionInfo.add("비타민 C");
            if (checkBoxVitaminD.isChecked()) selectedNutritionInfo.add("비타민 D");
            if (checkBoxCholesterol.isChecked()) selectedNutritionInfo.add("콜레스테롤");
            if (checkBoxSaturatedFA.isChecked()) selectedNutritionInfo.add("불포화지방산");
            if (checkBoxTransFA.isChecked()) selectedNutritionInfo.add("트랜스지방산");
            if (checkBoxFructose.isChecked()) selectedNutritionInfo.add("과당");
            if (checkBoxLactose.isChecked()) selectedNutritionInfo.add("유당");
            if (checkBoxCaffeine.isChecked()) selectedNutritionInfo.add("마페인");
            if (checkBoxMagnesium.isChecked()) selectedNutritionInfo.add("마그네슘");
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
