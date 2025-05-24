package com.example.nutritionrealtimeapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

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
            LinearLayout layout = findViewById(R.id.wholeLayout);

            // 화면 클릭시, 실행
            layout.setOnClickListener(v -> {
                Intent intent = new Intent(this, SelectAllergiesActivity.class);
                startActivity(intent);
            });
        }
    }
}
