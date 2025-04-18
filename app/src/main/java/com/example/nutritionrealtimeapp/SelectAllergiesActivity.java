package com.example.nutritionrealtimeapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SelectAllergiesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_allergies);

        SharedPreferences sharedPreferences = getSharedPreferences("NutritionApp", MODE_PRIVATE);
        boolean isFirstLaunch = sharedPreferences.getBoolean("isFirstLaunch", true);

        CheckBox checkBoxPeanut = findViewById(R.id.checkBox_peanut);
        CheckBox checkBoxDairy = findViewById(R.id.checkBox_dairy);
        CheckBox checkBoxGluten = findViewById(R.id.checkBox_gluten);
        Button saveAllergyButton = findViewById(R.id.saveAllergyButton);

        boolean savedPeanut = sharedPreferences.getBoolean("peanut", false);
        boolean savedDairy = sharedPreferences.getBoolean("dairy", false);
        boolean savedGluten = sharedPreferences.getBoolean("gluten", false);

        checkBoxPeanut.setChecked(savedPeanut);
        checkBoxDairy.setChecked(savedDairy);
        checkBoxGluten.setChecked(savedGluten);

        saveAllergyButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putBoolean("peanut", checkBoxPeanut.isChecked());
            editor.putBoolean("dairy", checkBoxDairy.isChecked());
            editor.putBoolean("gluten", checkBoxGluten.isChecked());
            editor.apply(); // 저장

            Toast.makeText(this, "알러지 정보가 저장되었습니다", Toast.LENGTH_SHORT).show();

            if (isFirstLaunch) {

                Intent intent = new Intent(this, SelectNutritionInfoActivity.class);
                startActivity(intent);
                finish();
            }else {
                Intent intent = new Intent(this, ScanBarcodeActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }
}
