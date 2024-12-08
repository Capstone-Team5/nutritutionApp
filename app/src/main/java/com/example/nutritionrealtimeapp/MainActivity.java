package com.example.nutritionrealtimeapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // SharedPreferences를 사용하여 저장된 영양정보 확인
        SharedPreferences sharedPreferences = getSharedPreferences("NutritionApp", MODE_PRIVATE);
        Set<String> savedNutritionInfoSet = sharedPreferences.getStringSet("selectedNutritionInfo", null);

        if (savedNutritionInfoSet != null) {
            // 저장된 값이 있으면 바로 ScanBarcodeActivity로 이동
            Intent intent = new Intent(this, ScanBarcodeActivity.class);

            // Set<String>을 ArrayList<String>로 변환
            ArrayList<String> nutritionInfoList = new ArrayList<>(savedNutritionInfoSet);

            // 데이터 전달
            intent.putStringArrayListExtra("selectedNutritionInfo", nutritionInfoList);

            startActivity(intent);
            finish(); // MainActivity 종료
        } else {
            // 저장된 값이 없으면 "시작하기" 버튼을 표시
            Button startButton = findViewById(R.id.startButton);
            startButton.setOnClickListener(v -> {
                // SelectAllergiesActivity로 이동
                Intent intent = new Intent(this, SelectAllergiesActivity.class);
                startActivity(intent);
            });
        }
    }
}
