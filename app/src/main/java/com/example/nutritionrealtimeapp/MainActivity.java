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

        SharedPreferences sharedPreferences = getSharedPreferences("NutritionApp", MODE_PRIVATE);
        boolean isFirstLaunch = sharedPreferences.getBoolean("isFirstLaunch", true);

        if (!isFirstLaunch) {
            Intent intent = new Intent(this, ScanBarcodeActivity.class);
            startActivity(intent);
            finish(); // MainActivity 종료
        } else {
            // 앱 처음 실행시
            Button startButton = findViewById(R.id.startButton);
            startButton.setOnClickListener(v -> {
                // SelectAllergiesActivity로 이동
                Intent intent = new Intent(this, SelectAllergiesActivity.class);
                startActivity(intent);
            });
        }
    }
}
