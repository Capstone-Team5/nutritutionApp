package com.example.nutritionrealtimeapp;

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
        Button saveButton = findViewById(R.id.saveButton);

        // SharedPreferences 초기화
        sharedPreferences = getSharedPreferences("NutritionApp", Context.MODE_PRIVATE);

        // 저장된 값이 있으면 체크박스 상태 복원
        Set<String> savedNutritionInfoSet = sharedPreferences.getStringSet("selectedNutritionInfo", null);
        if (savedNutritionInfoSet != null) {
            if (savedNutritionInfoSet.contains("칼로리")) checkBoxCalories.setChecked(true);
            if (savedNutritionInfoSet.contains("단백질")) checkBoxProtein.setChecked(true);
            if (savedNutritionInfoSet.contains("지방")) checkBoxFat.setChecked(true);
            if (savedNutritionInfoSet.contains("탄수화물")) checkBoxCarbs.setChecked(true);
        }

        // 저장 버튼 클릭 이벤트
        saveButton.setOnClickListener(v -> {
            List<String> selectedNutritionInfo = new ArrayList<>();
            if (checkBoxCalories.isChecked()) selectedNutritionInfo.add("칼로리");
            if (checkBoxProtein.isChecked()) selectedNutritionInfo.add("단백질");
            if (checkBoxFat.isChecked()) selectedNutritionInfo.add("지방");
            if (checkBoxCarbs.isChecked()) selectedNutritionInfo.add("탄수화물");

            if (selectedNutritionInfo.isEmpty()) {
                Toast.makeText(this, "최소한 하나의 영양정보를 선택하세요!", Toast.LENGTH_SHORT).show();
            } else {
                // SharedPreferences에 선택한 데이터 저장
                saveNutritionInfoToPreferences(selectedNutritionInfo);

                // ScanBarcodeActivity로 이동
                Intent intent = new Intent(SelectNutritionInfoActivity.this, ScanBarcodeActivity.class);
                intent.putStringArrayListExtra("selectedNutritionInfo", (ArrayList<String>) selectedNutritionInfo);
                startActivity(intent);
                finish(); // 현재 액티비티 종료
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
