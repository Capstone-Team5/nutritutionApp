package com.example.nutritionrealtimeapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.UUID;

public class SelectAllergiesActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_allergies);

        CheckBox checkBoxPeanut = findViewById(R.id.checkBox_peanut);
        CheckBox checkBoxDairy = findViewById(R.id.checkBox_dairy);
        CheckBox checkBoxGluten = findViewById(R.id.checkBox_gluten);
        Button saveAllergyButton = findViewById(R.id.saveAllergyButton);

        // Firestore 인스턴스 가져오기
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // SharedPreferences 초기화
        sharedPreferences = getSharedPreferences("NutritionApp", Context.MODE_PRIVATE);

        // 저장된 알레르기 데이터 로드 및 체크박스 상태 복원
        Set<String> savedAllergies = sharedPreferences.getStringSet("selectedAllergies", new HashSet<>());
        if (savedAllergies.contains("땅콩")) checkBoxPeanut.setChecked(true);
        if (savedAllergies.contains("우유")) checkBoxDairy.setChecked(true);
        if (savedAllergies.contains("글루틴")) checkBoxGluten.setChecked(true);

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
                // Firestore에 저장할 데이터 준비
                HashMap<String, Object> allergyData = new HashMap<>();
                allergyData.put("allergies", selectedAllergies);

                // Firestore에 데이터 저장
                db.collection("Users").document(userId)
                        .set(allergyData)
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this, "알레르기 저장 완료!", Toast.LENGTH_SHORT).show();

                            // SharedPreferences에 알레르기 데이터 저장
                            saveAllergiesToPreferences(selectedAllergies);

                            // ScanBarcodeActivity로 이동
                            Intent intent = new Intent(SelectAllergiesActivity.this, ScanBarcodeActivity.class);
                            intent.putStringArrayListExtra("selectedAllergies", (ArrayList<String>) selectedAllergies);
                            startActivity(intent);
                            finish(); // 현재 액티비티 종료
                        })
                        .addOnFailureListener(e -> Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
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
