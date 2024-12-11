package com.example.nutritionrealtimeapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import android.speech.tts.TextToSpeech;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Locale;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


public class ScanBarcodeActivity extends AppCompatActivity {

    private List<String> selectedNutritionInfo;
    private List<String> selectedAllergies;
    private FirebaseFirestore firestore;
    private SharedPreferences sharedPreferences;
    private TextToSpeech textToSpeech;
    private String apiKey = "/0ec81814ab4442ed9dd6"; // 식품 정보 API 키
    private String nutritionApiKey = "zblpVQX%2B75IpUWic%2BfeIY7TaV1DCNu8qOPWmVR2AUqYKrsB%2BNM6wYv1pjWczB0%2FK2TNlTq%2FOmaZ67dSEImlQeQ%3D%3D"; // 영양 정보 API 키

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

        textToSpeech = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.setLanguage(Locale.getDefault()); // 디바이스의 기본 언어로 설정
            }
        });

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
            intent.putExtra("isReturningFromEdit", true);
            startActivity(intent);
            finish();
        });
    }
    private void startBarcodeScanner() {
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
                    if (rawValue != null) {
                        // 1. 바코드 값으로 첫 번째 API 호출
                        String foodInfo = fetchFoodInfo(rawValue);

                        if (foodInfo != null) {
                            // 2. 첫 번째 API 응답에서 식품 이름 추출
                            String foodName = extractFoodNameFromResponse(foodInfo);
                            if (foodName != null) {
                                // 3. 식품 이름으로 두 번째 API 호출
                                String nutritionInfo = fetchNutritionInfo(foodName);

                                if (nutritionInfo != null) {
                                    // 4. 두 번째 API 응답 데이터를 화면에 표시
                                    TextView nutritionInfoTextView = findViewById(R.id.nutritionInfoTextView);
                                    parseAndDisplayNutritionInfo(nutritionInfo, nutritionInfoTextView);
                                } else {
                                    Toast.makeText(this, "영양 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(this, "식품 이름을 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "식품 정보를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show();
                        }
                    }
                })
                .addOnCanceledListener(() -> {
                    Toast.makeText(this, "스캔이 취소되었습니다.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "스캔 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void checkAllergies(String recognizedText) {
        if (recognizedText != null && selectedAllergies != null) {
            for (String allergy : selectedAllergies) {
                if (recognizedText.contains(allergy)) {
                    String alertMessage = "주의: " + allergy + "가 포함되었습니다!";

                    // TTS로 알림
                    if (textToSpeech != null) {
                        textToSpeech.speak(alertMessage, TextToSpeech.QUEUE_FLUSH, null, null);
                    }

                    // 토스트 메시지 추가
                    Toast.makeText(this, alertMessage, Toast.LENGTH_LONG).show();

                    // 점자(진동) 알림 추가
                    Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        vibrator.vibrate(500); // 500ms 동안 진동
                    }
                    break;
                }
            }
        }
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
    private String fetchFoodInfo(String barcode) {
        try {
            String urlString = "http://openapi.foodsafetykorea.go.kr/api/" + apiKey +
                    "/C005/json/1/5/BAR_CD=" + URLEncoder.encode(barcode, "UTF-8");

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String extractFoodNameFromResponse(String jsonResponse) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONObject body = jsonObject.getJSONObject("body");
            JSONArray items = body.getJSONArray("items");

            if (items.length() > 0) {
                JSONObject item = items.getJSONObject(0);
                return item.getString("DESC_KOR"); // 식품 이름 필드
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String fetchNutritionInfo(String foodName) {
        try {
            // URL 생성: foodName을 기반으로 영양 정보 가져오기
            String urlString = "https://apis.data.go.kr/1471000/FoodNtrCpntDbInfo01/getFoodNtrCpntDbInq01?" +
                    "serviceKey=" + URLEncoder.encode(nutritionApiKey, "UTF-8") +
                    "&desc_kor=" + URLEncoder.encode(foodName, "UTF-8");

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return response.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    private void parseAndDisplayNutritionInfo(String jsonResponse, TextView nutritionInfoTextView) {
        try {
            JSONObject jsonObject = new JSONObject(jsonResponse);
            JSONObject body = jsonObject.getJSONObject("body");
            JSONArray items = body.getJSONArray("items");

            StringBuilder nutritionInfoBuilder = new StringBuilder();

            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);

                String foodName = item.getString("DESC_KOR");
                String calorie = item.optString("NUTR_CONT1", "N/A");
                String protein = item.optString("NUTR_CONT2", "N/A");
                String fat = item.optString("NUTR_CONT3", "N/A");
                String carbs = item.optString("NUTR_CONT4", "N/A");

                nutritionInfoBuilder.append("음식 이름: ").append(foodName).append("\n")
                        .append("칼로리: ").append(calorie).append("\n")
                        .append("단백질: ").append(protein).append("\n")
                        .append("지방: ").append(fat).append("\n")
                        .append("탄수화물: ").append(carbs).append("\n\n");
            }

            // Display the parsed nutrition info
            nutritionInfoTextView.setText(nutritionInfoBuilder.toString());
        } catch (Exception e) {
            Toast.makeText(this, "JSON 파싱 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void filterAndDisplayNutritionInfo(List<String> nutritionInfo) {
        TextView nutritionInfoTextView = findViewById(R.id.nutritionInfoTextView);

        // 값이 있는 항목만 필터링
        StringBuilder displayedInfo = new StringBuilder();
        for (String info : nutritionInfo) {
            if (info != null && !info.isEmpty() && !info.equals("없음")) { // 값이 없거나 기본값인 "없음" 제외
                displayedInfo.append(info).append("\n");
            }
        }

        // 필터링된 데이터 표시
        if (displayedInfo.length() > 0) {
            nutritionInfoTextView.setText(displayedInfo.toString());
        } else {
            nutritionInfoTextView.setText("저장된 영양 정보가 없습니다.");
        }
    }

    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
    }
}
