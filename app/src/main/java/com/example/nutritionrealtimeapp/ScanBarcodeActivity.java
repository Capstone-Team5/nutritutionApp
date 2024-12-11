package com.example.nutritionrealtimeapp;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Xml;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.example.nutritionrealtimeapp.DisplayNutritionActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import android.os.AsyncTask; // AsyncTask 클래스
import java.io.BufferedReader; // 네트워크 응답 읽기
import java.io.InputStreamReader; // 입력 스트림 처리
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection; // HTTP 요청 처리
import java.net.URL; // URL 처리
import java.net.URLEncoder;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import android.speech.tts.TextToSpeech;

import org.xmlpull.v1.XmlPullParser;

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
        Button scanAllergyButton = findViewById(R.id.scanAllergyButton);

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

        // 알레르기 스캔 버튼 클릭 이벤트
        scanAllergyButton.setOnClickListener(v -> {
            Intent intent = new Intent(ScanBarcodeActivity.this, ScanAllergyActivity.class);
            startActivity(intent);
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
                        Toast.makeText(this, "바코드 스캔 성공: " + rawValue, Toast.LENGTH_SHORT).show();

                        // 1. 바코드로 식품 정보를 가져옵니다.
                        new FetchFoodTask().execute(rawValue);
                    }
                })
                .addOnCanceledListener(() -> {
                    Toast.makeText(this, "스캔이 취소되었습니다.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "스캔 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private String extractFoodNameFromResponse(String xmlResponse) {
        try {
            // XML 파서 준비
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new StringReader(xmlResponse)); // XML 데이터 입력
            int eventType = parser.getEventType();

            String tagName = null;
            String foodName = null;

            // XML 문서를 순차적으로 읽기
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        tagName = parser.getName(); // 현재 태그 이름 가져오기
                        break;

                    case XmlPullParser.TEXT:
                        if ("PRDLST_NM".equals(tagName)) { // 원하는 태그인지 확인
                            foodName = parser.getText().trim(); // 태그 안의 텍스트 가져오기
                        }
                        break;

                    case XmlPullParser.END_TAG:
                        if ("row".equals(parser.getName()) && foodName != null) {
                            // 필요한 데이터를 찾으면 루프 종료
                            return foodName;
                        }
                        tagName = null;
                        break;
                }
                eventType = parser.next(); // 다음 이벤트로 이동
            }
        } catch (Exception e) {
            Log.e("XML_PARSING_ERROR", "Error parsing XML response: " + e.getMessage());
            e.printStackTrace();
        }
        return null; // 오류 발생 시 null 반환
    }

    private class FetchFoodTask extends AsyncTask<String, Void, String> {
        @Override
        protected String doInBackground(String... params) {
            String barcode = params[0];
            try {
                // Step 1: Fetch food information based on the barcode
                String foodInfo = fetchFoodInfo(barcode);
                if (foodInfo == null || foodInfo.isEmpty()) {
                    return "ERROR: 식품 정보를 가져올 수 없습니다.";
                }

                // Step 2: Extract food name from the fetched food information
                String foodName = extractFoodNameFromResponse(foodInfo);
                if (foodName == null || foodName.isEmpty()) {
                    return "ERROR: 식품 이름을 가져올 수 없습니다.";
                }

                // Step 3: Fetch nutrition information using the food name
                String nutritionInfo = fetchNutritionInfo(foodName);
                if (nutritionInfo == null || nutritionInfo.isEmpty()) {
                    return "ERROR: 영양 정보를 가져올 수 없습니다.";
                }

                // Return combined data: food name and nutrition info
                return "FOOD_NAME:" + foodName + "\n" + nutritionInfo;

            } catch (Exception e) {
                e.printStackTrace();
                return "ERROR: 데이터 가져오기 중 오류가 발생했습니다.";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null && !result.isEmpty()) {
                // Check if the result indicates an error
                if (result.startsWith("ERROR:")) {
                    Toast.makeText(ScanBarcodeActivity.this, result.replace("ERROR:", "").trim(), Toast.LENGTH_SHORT).show();
                } else if (result.startsWith("FOOD_NAME:")) {
                    // Extract food name and nutrition info from the result
                    String[] splitResult = result.split("\n", 2); // Split into food name and nutrition info
                    String foodName = splitResult[0].replace("FOOD_NAME:", "").trim();
                    String nutritionInfo = splitResult.length > 1 ? splitResult[1].trim() : "";

                    // Pass the data to the next activity
                    Intent intent = new Intent(ScanBarcodeActivity.this, DisplayNutritionActivity.class);
                    intent.putExtra("foodName", foodName);
                    intent.putExtra("nutritionInfo", nutritionInfo);
                    startActivity(intent); // Move to DisplayNutritionActivity
                } else {
                    Toast.makeText(ScanBarcodeActivity.this, "알 수 없는 응답 형식입니다.", Toast.LENGTH_SHORT).show();
                }
            } else {
                Toast.makeText(ScanBarcodeActivity.this, "API 응답을 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show();
            }
        }

        private String fetchFoodInfo(String barcode) {
            try {
                String baseUrl = "http://openapi.foodsafetykorea.go.kr/api";
                String apiKeyPath = "/0ec81814ab4442ed9dd6/C005/json/1/5/";

              
                String urlString = new StringBuilder()
                        .append(baseUrl)
                        .append(apiKeyPath)
                        .append("BAR_CD=")
                        .append(URLEncoder.encode(barcode, "UTF-8")) // 쿼리 매개변수 값 인코딩
                        .toString();

                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

                int responseCode = connection.getResponseCode();
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e("API_ERROR", "HTTP error code: " + responseCode);
                    return null;
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                Log.d("API_RESPONSE", "Response: " + response.toString());
                return response.toString();
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        private String fetchNutritionInfo(String foodName) {
            try {
                String baseUrl = "https://apis.data.go.kr/1471000/FoodNtrCpntDbInfo01/getFoodNtrCpntDbInq01";

                // URLBuilder 스타일로 URL 생성
                String urlString = new StringBuilder()
                        .append(baseUrl)
                        .append("?serviceKey=")
                        .append(URLEncoder.encode(nutritionApiKey, "UTF-8")) // API 키 인코딩
                        .append("&desc_kor=")
                        .append(URLEncoder.encode(foodName, "UTF-8")) // 음식 이름 인코딩
                        .append("&type=json&pageNo=1&numOfRows=10")
                        .toString();

                String jsonResponse = makeHttpRequest(urlString);
                return parseNutritionResponse(jsonResponse);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return null;
            }
        }

        private String makeHttpRequest(String urlString) {
            try {
                URL url = new URL(urlString);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");

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

        private String parseNutritionResponse(String xmlResponse) {
            StringBuilder nutritionInfo = new StringBuilder();
            try {
                // XML 파서 준비
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(new StringReader(xmlResponse));
                int eventType = parser.getEventType();

                String tagName = null;
                String foodName = null;
                String calories = null;
                String protein = null;
                String fat = null;

                while (eventType != XmlPullParser.END_DOCUMENT) {
                    switch (eventType) {
                        case XmlPullParser.START_TAG:
                            tagName = parser.getName();
                            break;

                        case XmlPullParser.TEXT:
                            String text = parser.getText().trim();
                            if (tagName != null && !text.isEmpty()) {
                                switch (tagName) {
                                    case "FOOD_NM_KR": // 식품명
                                        foodName = text;
                                        break;
                                    case "AMT_NUM1": // 칼로리
                                        calories = text + "kcal";
                                        break;
                                    case "AMT_NUM3": // 단백질
                                        protein = text + "g";
                                        break;
                                    case "AMT_NUM4": // 지방
                                        fat = text + "g";
                                        break;
                                }
                            }
                            break;

                        case XmlPullParser.END_TAG:
                            if (parser.getName().equals("item")) {
                                // 각 아이템의 정보를 문자열로 추가
                                nutritionInfo.append("식품명: ").append(foodName != null ? foodName : "N/A").append("\n");
                                nutritionInfo.append("칼로리: ").append(calories != null ? calories : "N/A").append("\n");
                                nutritionInfo.append("단백질: ").append(protein != null ? protein : "N/A").append("\n");
                                nutritionInfo.append("지방: ").append(fat != null ? fat : "N/A").append("\n\n");

                                // 다음 아이템을 위해 값 초기화
                                foodName = calories = protein = fat = null;
                            }
                            tagName = null;
                            break;
                    }
                    eventType = parser.next();
                }
            } catch (Exception e) {
                e.printStackTrace();
                return "XML 데이터를 파싱하는 동안 오류가 발생했습니다.";
            }
            return nutritionInfo.toString();
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