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
        CheckBox checkBoxEgg = findViewById(R.id.checkBox_egg);
        CheckBox checkBoxWalnuts = findViewById(R.id.checkBox_walnuts);
        CheckBox checkBoxAlmond = findViewById(R.id.checkBox_almond);
        CheckBox checkBoxShrimp = findViewById(R.id.checkBox_shrimp);
        CheckBox checkBoxCrab = findViewById(R.id.checkBox_crab);
        CheckBox checkBoxSesameSeeds = findViewById(R.id.checkBox_sesameSeeds);
        CheckBox checkBoxGarlic = findViewById(R.id.checkBox_garlic);
        CheckBox checkBoxPeach = findViewById(R.id.checkBox_peach);
        CheckBox checkBoxKiwi = findViewById(R.id.checkBox_kiwi);
        CheckBox checkBoxApple = findViewById(R.id.checkBox_apple);

        Button saveAllergyButton = findViewById(R.id.saveAllergyButton);

        // 저장된 알레르기 목록 불러오기
        Set<String> savedAllergies = sharedPreferences.getStringSet(KEY_SELECTED_ALLERGIES, new HashSet<>());

        // 체크박스 상태 설정
        checkBoxPeanut.setChecked(savedAllergies.contains("땅콩"));
        checkBoxDairy.setChecked(savedAllergies.contains("유제품"));
        checkBoxGluten.setChecked(savedAllergies.contains("글루텐"));
        checkBoxEgg.setChecked(savedAllergies.contains("달걀"));
        checkBoxWalnuts.setChecked(savedAllergies.contains("호두"));
        checkBoxAlmond.setChecked(savedAllergies.contains("아몬드"));
        checkBoxShrimp.setChecked(savedAllergies.contains("새우"));
        checkBoxCrab.setChecked(savedAllergies.contains("게"));
        checkBoxSesameSeeds.setChecked(savedAllergies.contains("참깨"));
        checkBoxGarlic.setChecked(savedAllergies.contains("마늘"));
        checkBoxPeach.setChecked(savedAllergies.contains("복숭아"));
        checkBoxKiwi.setChecked(savedAllergies.contains("키위"));
        checkBoxApple.setChecked(savedAllergies.contains("사과"));

        saveAllergyButton.setOnClickListener(v -> {
            Set<String> selectedAllergies = new HashSet<>();
            if (checkBoxPeanut.isChecked()) selectedAllergies.add("땅콩");
            if (checkBoxDairy.isChecked()) selectedAllergies.add("유제품");
            if (checkBoxGluten.isChecked()) selectedAllergies.add("글루텐");
            if (checkBoxEgg.isChecked()) selectedAllergies.add("달걀");
            if (checkBoxWalnuts.isChecked()) selectedAllergies.add("호두");
            if (checkBoxAlmond.isChecked()) selectedAllergies.add("아몬드");
            if (checkBoxShrimp.isChecked()) selectedAllergies.add("새우");
            if (checkBoxCrab.isChecked()) selectedAllergies.add("게");
            if (checkBoxSesameSeeds.isChecked()) selectedAllergies.add("참깨");
            if (checkBoxGarlic.isChecked()) selectedAllergies.add("마늘");
            if (checkBoxPeach.isChecked()) selectedAllergies.add("복숭아");
            if (checkBoxKiwi.isChecked()) selectedAllergies.add("키위");
            if (checkBoxApple.isChecked()) selectedAllergies.add("사과");


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
