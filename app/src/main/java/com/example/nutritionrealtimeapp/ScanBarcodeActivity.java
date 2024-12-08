package com.example.nutritionrealtimeapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ScanBarcodeActivity extends AppCompatActivity {

    private List<String> selectedNutritionInfo;
    private List<String> selectedAllergies;
    private FirebaseFirestore firestore;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_barcode);

        firestore = FirebaseFirestore.getInstance();
        sharedPreferences = getSharedPreferences("NutritionApp", MODE_PRIVATE);

        TextView nutritionInfoTextView = findViewById(R.id.nutritionInfoTextView);
        TextView allergyInfoTextView = findViewById(R.id.allergyInfoTextView);
        Button scanButton = findViewById(R.id.scanButton);
        Button editButton = findViewById(R.id.editButton);
        Button editAllergyButton = findViewById(R.id.editAllergyButton);

        // SharedPreferences에서 데이터 로드
        selectedNutritionInfo = new ArrayList<>(sharedPreferences.getStringSet("selectedNutritionInfo", new HashSet<>()));
        selectedAllergies = new ArrayList<>(sharedPreferences.getStringSet("selectedAllergies", new HashSet<>()));

        // 영양정보와 알레르기 정보를 화면에 표시
        if (!selectedNutritionInfo.isEmpty()) {
            nutritionInfoTextView.setText("선택한 영양정보: " + String.join(", ", selectedNutritionInfo));
        } else {
            nutritionInfoTextView.setText("선택된 영양정보가 없습니다.");
        }

        if (!selectedAllergies.isEmpty()) {
            allergyInfoTextView.setText("선택한 알레르기: " + String.join(", ", selectedAllergies));
        } else {
            allergyInfoTextView.setText("선택된 알레르기가 없습니다.");
        }

        // 바코드 스캔 버튼 클릭 이벤트
        scanButton.setOnClickListener(v -> startBarcodeScanner());

        // 영양정보 수정 버튼
        editButton.setOnClickListener(v -> {
            Intent intent = new Intent(ScanBarcodeActivity.this, SelectNutritionInfoActivity.class);
            startActivity(intent);
            finish();
        });

        // 알레르기 수정 버튼
        editAllergyButton.setOnClickListener(v -> {
            Intent intent = new Intent(ScanBarcodeActivity.this, SelectAllergiesActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void startBarcodeScanner() {
        // 바코드 스캐너 설정
        GmsBarcodeScannerOptions options = new GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(
                        Barcode.FORMAT_QR_CODE,
                        Barcode.FORMAT_EAN_13
                )
                .build();

        GmsBarcodeScanner scanner = GmsBarcodeScanning.getClient(this, options);

        scanner.startScan()
                .addOnSuccessListener(barcode -> {
                    String rawValue = barcode.getRawValue();

                    // 알레르기 데이터 확인 및 알림
                    if (rawValue != null && selectedAllergies != null) {
                        for (String allergy : selectedAllergies) {
                            if (rawValue.contains(allergy)) {
                                Toast.makeText(this, "주의: " + allergy + "가 포함되었습니다!", Toast.LENGTH_LONG).show();
                                break;
                            }
                        }
                    }

                    saveToFirestore(rawValue); // Firestore에 저장
                })
                .addOnCanceledListener(() -> {
                    Toast.makeText(this, "스캔이 취소되었습니다.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "스캔 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveToFirestore(String barcodeValue) {
        if (selectedNutritionInfo == null || selectedNutritionInfo.isEmpty()) {
            Toast.makeText(this, "선택된 영양정보가 없습니다!", Toast.LENGTH_SHORT).show();
            return;
        }

        NutritionData data = new NutritionData(barcodeValue, selectedNutritionInfo);

        firestore.collection("NutritionData")
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "데이터 저장 성공: " + documentReference.getId(), Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(ScanBarcodeActivity.this, DisplayNutritionActivity.class);
                    intent.putExtra("barcodeValue", barcodeValue);
                    intent.putStringArrayListExtra("selectedNutritionInfo", (ArrayList<String>) selectedNutritionInfo);
                    startActivity(intent);
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "데이터 저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
