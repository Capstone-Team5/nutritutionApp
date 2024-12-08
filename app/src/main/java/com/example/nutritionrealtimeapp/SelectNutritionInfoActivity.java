package com.example.nutritionrealtimeapp;

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

public class SelectNutritionInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_nutrition_info);

        CheckBox checkBoxCalories = findViewById(R.id.checkBox_calories);
        CheckBox checkBoxProtein = findViewById(R.id.checkBox_protein);
        CheckBox checkBoxFat = findViewById(R.id.checkBox_fat);
        CheckBox checkBoxCarbs = findViewById(R.id.checkBox_carbs);
        Button saveButton = findViewById(R.id.saveButton);

        // SharedPreferences에서 이전 선택값 가져오기
        SharedPreferences sharedPreferences = getSharedPreferences("NutritionApp", MODE_PRIVATE);
        Set<String> savedNutritionInfoSet = sharedPreferences.getStringSet("selectedNutritionInfo", null);

        // 저장된 값이 있으면 체크박스에 반영
        if (savedNutritionInfoSet != null) {
            if (savedNutritionInfoSet.contains("칼로리")) checkBoxCalories.setChecked(true);
            if (savedNutritionInfoSet.contains("단백질")) checkBoxProtein.setChecked(true);
            if (savedNutritionInfoSet.contains("지방")) checkBoxFat.setChecked(true);
            if (savedNutritionInfoSet.contains("탄수화물")) checkBoxCarbs.setChecked(true);
        }

        // 저장 버튼 클릭 이벤트
        saveButton.setOnClickListener(v -> {
            Set<String> selectedNutritionInfoSet = new HashSet<>();
            if (checkBoxCalories.isChecked()) selectedNutritionInfoSet.add("칼로리");
            if (checkBoxProtein.isChecked()) selectedNutritionInfoSet.add("단백질");
            if (checkBoxFat.isChecked()) selectedNutritionInfoSet.add("지방");
            if (checkBoxCarbs.isChecked()) selectedNutritionInfoSet.add("탄수화물");

            if (selectedNutritionInfoSet.isEmpty()) {
                Toast.makeText(this, "최소한 하나의 영양정보를 선택하세요!", Toast.LENGTH_SHORT).show();
            } else {
                // SharedPreferences에 선택한 데이터 저장
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putStringSet("selectedNutritionInfo", selectedNutritionInfoSet);
                editor.apply();

                // 선택한 데이터를 ScanBarcodeActivity로 전달하고 이동
                Intent intent = new Intent(this, ScanBarcodeActivity.class);
                intent.putStringArrayListExtra("selectedNutritionInfo", new ArrayList<>(selectedNutritionInfoSet));
                startActivity(intent);
                finish(); // 현재 액티비티 종료
            }
        });
    }
}
