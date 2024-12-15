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

public class SelectAllergiesActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private boolean isReturningFromEdit = false; // 수정 여부 확인 플래그

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_allergies);

        CheckBox checkBoxPeanut = findViewById(R.id.checkBox_peanut);
        CheckBox checkBoxDairy = findViewById(R.id.checkBox_dairy);
        CheckBox checkBoxGluten = findViewById(R.id.checkBox_gluten);
        Button saveAllergyButton = findViewById(R.id.saveAllergyButton);

        // SharedPreferences 초기화
        sharedPreferences = getSharedPreferences("NutritionApp", Context.MODE_PRIVATE);

        // 저장된 알레르기 데이터 로드 및 체크박스 상태 복원
        Set<String> savedAllergies = sharedPreferences.getStringSet("selectedAllergies", new HashSet<>());
        if (savedAllergies.contains("땅콩")) checkBoxPeanut.setChecked(true);
        if (savedAllergies.contains("우유")) checkBoxDairy.setChecked(true);
        if (savedAllergies.contains("글루틴")) checkBoxGluten.setChecked(true);

        // 수정 여부 확인 (인텐트로 전달받은 값)
        isReturningFromEdit = getIntent().getBooleanExtra("isReturningFromEdit", false);

        // 기기별 고유 ID 생성 (UUID 사용)
        String userId = getOrCreateUserId();

        // 저장 버튼 클릭 이벤트
        saveAllergyButton.setOnClickListener(v -> {
            List<String> selectedAllergies = new ArrayList<>();
            if (checkBoxPeanut.isChecked()) selectedAllergies.add("땅콩");
            if (checkBoxDairy.isChecked()) selectedAllergies.add("우유");
            if (checkBoxGluten.isChecked()) selectedAllergies.add("글루틴");

            if (selectedAllergies.isEmpty()) {
                Toast.makeText(this, "최소한 하나의 알레르기를 선택하세요!", Toast.LENGTH_SHORT).show();
            } else {
                // SharedPreferences에 알레르기 데이터 저장
                saveAllergiesToPreferences(selectedAllergies);

                // 다음 화면으로 이동
                if (isReturningFromEdit) {
                    // 수정 후 돌아온 경우 BarcodeActivity로 이동
                    Intent intent = new Intent(SelectAllergiesActivity.this, ScanBarcodeActivity.class);
                    startActivity(intent);
                } else {
                    // 처음 저장한 경우 NutritionInfoActivity로 이동
                    Intent intent = new Intent(SelectAllergiesActivity.this, SelectNutritionInfoActivity.class);
                    intent.putStringArrayListExtra("selectedAllergies", (ArrayList<String>) selectedAllergies);
                    startActivity(intent);
                }

                finish(); // 현재 액티비티 종료
            }
        });
    }

    // SharedPreferences에 UUID 저장 및 불러오기
    private String getOrCreateUserId() {
        String userId = sharedPreferences.getString("userId", null);

        if (userId == null) {
            // UUID 생성
            userId = UUID.randomUUID().toString();

            // SharedPreferences에 저장
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("userId", userId);
            editor.apply();
        }

        return userId;
    }

    // 알레르기 데이터를 SharedPreferences에 저장
    private void saveAllergiesToPreferences(List<String> selectedAllergies) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putStringSet("selectedAllergies", new HashSet<>(selectedAllergies));
        editor.apply();
    }
}
