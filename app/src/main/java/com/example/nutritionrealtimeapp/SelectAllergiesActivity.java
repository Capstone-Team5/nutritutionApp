package com.example.nutritionrealtimeapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

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
        EditText searchEditText = findViewById(R.id.searchEditText);
        ScrollView scrollView = findViewById(R.id.ScrollView);

        boolean savedPeanut = sharedPreferences.getBoolean("peanut", false);
        boolean savedDairy = sharedPreferences.getBoolean("dairy", false);
        boolean savedGluten = sharedPreferences.getBoolean("gluten", false);
        boolean savedEgg = sharedPreferences.getBoolean("egg", false);
        boolean savedWalnuts = sharedPreferences.getBoolean("walnuts", false);
        boolean savedAlmond = sharedPreferences.getBoolean("almond", false);
        boolean savedShrimp = sharedPreferences.getBoolean("shrimp", false);
        boolean savedCrab = sharedPreferences.getBoolean("crab", false);
        boolean savedSesameSeeds = sharedPreferences.getBoolean("sesameSeeds", false);
        boolean savedGarlic = sharedPreferences.getBoolean("garlic", false);
        boolean savedPeach = sharedPreferences.getBoolean("peach", false);
        boolean savedKiwi = sharedPreferences.getBoolean("kiwi", false);
        boolean savedApple = sharedPreferences.getBoolean("apple", false);

        checkBoxPeanut.setChecked(savedPeanut);
        checkBoxDairy.setChecked(savedDairy);
        checkBoxGluten.setChecked(savedGluten);
        checkBoxEgg.setChecked(savedEgg);
        checkBoxWalnuts.setChecked(savedWalnuts);
        checkBoxAlmond.setChecked(savedAlmond);
        checkBoxShrimp.setChecked(savedShrimp);
        checkBoxCrab.setChecked(savedCrab);
        checkBoxSesameSeeds.setChecked(savedSesameSeeds);
        checkBoxGarlic.setChecked(savedGarlic);
        checkBoxPeach.setChecked(savedPeach);
        checkBoxKiwi.setChecked(savedKiwi);
        checkBoxApple.setChecked(savedApple);

        List<CheckBox> checkBoxList = new ArrayList<>();
        checkBoxList.add(findViewById(R.id.checkBox_peanut));
        checkBoxList.add(findViewById(R.id.checkBox_dairy));
        checkBoxList.add(findViewById(R.id.checkBox_gluten));
        checkBoxList.add(findViewById(R.id.checkBox_egg));
        checkBoxList.add(findViewById(R.id.checkBox_walnuts));
        checkBoxList.add(findViewById(R.id.checkBox_almond));
        checkBoxList.add(findViewById(R.id.checkBox_shrimp));
        checkBoxList.add(findViewById(R.id.checkBox_crab));
        checkBoxList.add(findViewById(R.id.checkBox_sesameSeeds));
        checkBoxList.add(findViewById(R.id.checkBox_garlic));
        checkBoxList.add(findViewById(R.id.checkBox_peach));
        checkBoxList.add(findViewById(R.id.checkBox_kiwi));
        checkBoxList.add(findViewById(R.id.checkBox_apple));

        searchEditText.setOnEditorActionListener((v, actionId, event) -> {
            String query = searchEditText.getText().toString().trim();
            if (!query.isEmpty()) {
                boolean found = false;
                for (CheckBox cb : checkBoxList) {
                    if (cb.getText().toString().equals(query)) {
                        // 해당 체크박스의 위치로 스크롤
                        cb.requestFocus();
                        scrollView.post(() -> scrollView.smoothScrollTo(0, cb.getTop()));
                        // 포커스 강조(선택적으로)
                        cb.setBackgroundColor(0xFFCCE5FF); // 연한 파란색 배경
                        found = true;
                    } else {
                        cb.setBackgroundColor(0x00000000); // 배경 초기화
                    }
                }
                if (!found) {
                    Toast.makeText(this, "일치하는 알레르기가 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
            return true;
        });

        saveAllergyButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putBoolean("peanut", checkBoxPeanut.isChecked());
            editor.putBoolean("dairy", checkBoxDairy.isChecked());
            editor.putBoolean("gluten", checkBoxGluten.isChecked());
            editor.putBoolean("egg", checkBoxEgg.isChecked());
            editor.putBoolean("walnuts", checkBoxWalnuts.isChecked());
            editor.putBoolean("almond", checkBoxAlmond.isChecked());
            editor.putBoolean("shrimp", checkBoxShrimp.isChecked());
            editor.putBoolean("crab", checkBoxCrab.isChecked());
            editor.putBoolean("sesameSeeds", checkBoxSesameSeeds.isChecked());
            editor.putBoolean("garlic", checkBoxGarlic.isChecked());
            editor.putBoolean("peach", checkBoxPeach.isChecked());
            editor.putBoolean("kiwi", checkBoxKiwi.isChecked());
            editor.putBoolean("apple", checkBoxApple.isChecked());
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
