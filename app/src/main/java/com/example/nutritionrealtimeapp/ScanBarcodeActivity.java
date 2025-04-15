package com.example.nutritionrealtimeapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.util.Xml;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

//import com.google.firebase.firestore.FirebaseFirestore;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanner;
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions;
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class ScanBarcodeActivity extends AppCompatActivity {

    private List<String> selectedNutritionInfo;
    private List<String> selectedAllergies;
    //  private FirebaseFirestore firestore;
    private SharedPreferences sharedPreferences;
    private TextToSpeech textToSpeech;
    private String nutritionApiKey = "zblpVQX%2B75IpUWic%2BfeIY7TaV1DCNu8qOPWmVR2AUqYKrsB%2BNM6wYv1pjWczB0%2FK2TNlTq%2FOmaZ67dSEImlQeQ%3D%3D"; // 영양 정보 API 키
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_barcode);

        sharedPreferences = getSharedPreferences("NutritionApp", MODE_PRIVATE);

        TextView nutritionInfoTextView = findViewById(R.id.nutritionInfoTextView);
        TextView allergyInfoTextView = findViewById(R.id.allergyInfoTextView);
        Button scanButton = findViewById(R.id.scanButton);
        Button nutritionEditButton = findViewById(R.id.nutritionEditButton);
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
        nutritionEditButton.setOnClickListener(v -> {
            Intent intent = new Intent(ScanBarcodeActivity.this, SelectNutritionInfoActivity.class);
            startActivity(intent);
            finish();
        });

        // 알레르기 수정 버튼
        editAllergyButton.setOnClickListener(v -> {
            Toast.makeText(this, "알레르기 수정 버튼 선택", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(ScanBarcodeActivity.this, SelectAllergiesActivity.class);
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
                        executorService.execute(new FetchFoodRunnable(rawValue));
                    }
                })
                .addOnCanceledListener(() -> {
                    Toast.makeText(this, "스캔이 취소되었습니다.", Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "스캔 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * XML 응답에서 여러 개의 식품 이름을 추출하는 메서드
     */
    private String extractFoodNameFromResponse(String jsonResponse) {
        try {
            // JSON 파싱 시작
            JSONObject rootObject = new JSONObject(jsonResponse);

            // "C005" 객체 확인
            if (!rootObject.has("C005")) {
                Log.e("JSON_PARSING_ERROR", "C005 object not found.");
                return null; // "C005"가 없으면 null 반환
            }
            JSONObject c005Object = rootObject.getJSONObject("C005");

            // "row" 배열 확인
            if (!c005Object.has("row")) {
                Log.e("JSON_PARSING_ERROR", "Row array is not found in C005.");
                return null; // "row" 배열이 없으면 null 반환
            }
            JSONArray rowArray = c005Object.getJSONArray("row");

            if (rowArray.length() == 0) {
                Log.e("JSON_PARSING_ERROR", "Row array is empty.");
                return null; // "row" 배열이 비어 있으면 null 반환
            }

            // 첫 번째 아이템의 "PRDLST_NM" 추출
            JSONObject firstItem = rowArray.getJSONObject(0);
            if (!firstItem.has("PRDLST_NM")) {
                Log.e("JSON_PARSING_ERROR", "PRDLST_NM key not found in first item.");
                return null; // "PRDLST_NM" 키가 없으면 null 반환
            }
            String foodName = firstItem.getString("PRDLST_NM");

            if (foodName.isEmpty()) {
                Log.e("JSON_PARSING_ERROR", "PRDLST_NM is empty.");
                return null; // "PRDLST_NM" 값이 비어 있으면 null 반환
            }

            Log.d("EXTRACTED_FOOD_NAME", "Food name: " + foodName);
            return foodName;

        } catch (Exception e) {
            Log.e("JSON_PARSING_ERROR", "Error parsing JSON response: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private String extractActualFoodName(String fullName) {
        if (fullName.contains("_")) {
            String[] parts = fullName.split("_");
            return parts[parts.length - 1]; // 마지막 부분 반환
        }
        return fullName; // 언더스코어가 없으면 원래 값 반환
    }
    /**
     * 결과 객체를 정의하여 성공과 오류를 명확히 구분
     */
    private class FetchResult {
        String foodName;
        String nutritionInfo;
        String errorMessage;

        FetchResult(String foodName, String nutritionInfo) {
            this.foodName = foodName;
            this.nutritionInfo = nutritionInfo;
        }

        FetchResult(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }

    /**
     * 바코드로부터 식품 정보를 가져오고, 영양 정보를 Fetch하는 Runnable 클래스
     */
    private class FetchFoodRunnable implements Runnable {
        private String barcode;

        FetchFoodRunnable(String barcode) {
            this.barcode = barcode;
        }


        @Override
        public void run() {
            FetchResult result;
            try {
                // 1. 바코드로 식품 정보를 가져오기
                String foodInfo = fetchFoodInfo(barcode);
                if (foodInfo == null || foodInfo.isEmpty()) {
                    result = new FetchResult("식품 정보를 가져올 수 없습니다.");
                } else {
                    // 2. XML 응답에서 첫 번째 식품 이름 추출
                    String foodName = extractFoodNameFromResponse(foodInfo);
                    if (foodName == null || foodName.isEmpty()) {
                        result = new FetchResult("식품 이름을 가져올 수 없습니다.");
                    } else {
                        // 3. 식품 이름으로 영양 정보 가져오기
                        String nutritionInfo = fetchNutritionInfo(foodName);
                        if (nutritionInfo == null || nutritionInfo.isEmpty()) {
                            result = new FetchResult("영양 정보를 가져올 수 없습니다.");
                        } else {
                            // 성공적인 결과를 생성
                            result = new FetchResult(foodName, nutritionInfo);
                        }
                    }
                }
            } catch (Exception e) {
                Log.e("FetchFoodRunnable", "데이터 가져오기 중 오류 발생: ", e);
                result = new FetchResult("데이터 가져오기 중 오류가 발생했습니다.");
            }

            // FetchResult를 메인 스레드에서 처리
            final FetchResult finalResult = result;
            mainHandler.post(() -> {
                if (finalResult.errorMessage != null) {
                    Toast.makeText(ScanBarcodeActivity.this, finalResult.errorMessage, Toast.LENGTH_SHORT).show();
                } else {
                    Intent intent = new Intent(ScanBarcodeActivity.this, DisplayNutritionActivity.class);
                    intent.putExtra("foodName", finalResult.foodName);
                    intent.putExtra("barcode",barcode);
                    intent.putExtra("nutritionInfo", finalResult.nutritionInfo);
                    startActivity(intent);
                }
            });
        }
    }

    /**
     * 바코드로부터 식품 정보를 Fetch하는 메서드
     */
    private String fetchFoodInfo(String barcode) {
        try {
            // URL 직접 생성
            String baseUrl = "https://openapi.foodsafetykorea.go.kr/api";
            String apiKey = "0ec81814ab4442ed9dd6"; // API Key
            String serviceId = "C005";
            String dataType = "json"; // JSON 형식 요청
            int startIndex = 1;
            int endIndex = 5;

            // URL 문자열 생성
            String urlString = String.format(
                    "%s/%s/%s/%s/%d/%d/BAR_CD=%s",
                    baseUrl, apiKey, serviceId, dataType, startIndex, endIndex, barcode
            );

            Log.d("FETCH_URL", "Generated URL: " + urlString);

            // HTTP 요청
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setUseCaches(false); // 캐시 비활성화

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e("HTTP_ERROR", "HTTP error code: " + responseCode);
                return null;
            }

            // 응답 읽기
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            Log.d("FETCH_RESPONSE", "Response: " + response.toString());
            return response.toString();

        } catch (Exception e) {
            Log.e("FETCH_ERROR", "Error fetching food info: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 식품 이름을 기반으로 영양 정보를 Fetch하는 메서드
     */
    private String fetchNutritionInfo(String foodName) {
        try {
            // URL 직접 생성
            String baseUrl = "https://apis.data.go.kr/1471000/FoodNtrCpntDbInfo01/getFoodNtrCpntDbInq01";
            String foodNameWithoutSpaces = foodName.replace(" ", ""); // 띄어쓰기 제거
            String encodedFoodName = URLEncoder.encode(foodNameWithoutSpaces, "UTF-8");
            String dataType = "xml";
            int pageNo = 1;
            int numOfRows = 10;

            // URL 문자열 생성
            String urlString = String.format(
                    "%s?serviceKey=%s&FOOD_NM_KR=%s&type=%s&pageNo=%d&numOfRows=%d",
                    baseUrl, nutritionApiKey, encodedFoodName, dataType, pageNo, numOfRows
            );

            Log.d("FETCH_URL", "Generated URL: " + urlString); // URL 디버깅 로그

            // HTTP 요청
            String xmlResponse = makeHttpRequest(urlString);

            if (xmlResponse == null || xmlResponse.isEmpty()) {
                Log.e("FETCH_ERROR", "No response from API.");
                return "API 응답이 없습니다.";
            }

            // XML 응답 파싱
            return parseNutritionResponse(xmlResponse);

        } catch (Exception e) {
            Log.e("FETCH_ERROR", "Error fetching nutrition info: " + e.getMessage());
            e.printStackTrace();
            return "영양 정보를 가져오는 동안 오류가 발생했습니다.";
        }
    }
    /**
     * HTTP GET 요청을 수행하고 응답을 반환하는 메서드
     */
    private String makeHttpRequest(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                Log.e("HTTP_REQUEST_ERROR", "HTTP error code: " + responseCode);
                return null;
            }

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            Log.d("MAKE_HTTP_REQUEST", "Response: " + response.toString());
            return response.toString();
        } catch (Exception e) {
            Log.e("MAKE_HTTP_REQUEST_ERROR", "Error making HTTP request: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 영양 정보 XML 응답을 파싱하여 문자열로 반환하는 메서드
     */
    private String parseNutritionResponse(String xmlResponse) {
        StringBuilder nutritionInfo = new StringBuilder();
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new StringReader(xmlResponse));
            int eventType = parser.getEventType();

            String tagName = null;
            String foodName = null;
            String calories = null;
            String protein = null;
            String fat = null;
            String carbohydrates = null;
            boolean isFirstItemProcessed = false; // 첫 번째 아이템 처리 여부 확인 변수

            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        tagName = parser.getName();
                        break;

                    case XmlPullParser.TEXT:
                        String text = parser.getText().trim();
                        if (!text.isEmpty() && !isFirstItemProcessed) {
                            try {
                                if ("FOOD_NM_KR".equals(tagName)) {
                                    foodName = extractActualFoodName(text);
                                } else if ("AMT_NUM1".equals(tagName) && selectedNutritionInfo.contains("칼로리")) {
                                    calories = parseToIntString(text) + " kcal";
                                } else if ("AMT_NUM2".equals(tagName) && selectedNutritionInfo.contains("탄수화물")) {
                                    carbohydrates = parseToIntString(text) + " g";
                                } else if ("AMT_NUM3".equals(tagName) && selectedNutritionInfo.contains("단백질")) {
                                    protein = parseToIntString(text) + " g";
                                } else if ("AMT_NUM4".equals(tagName) && selectedNutritionInfo.contains("지방")) {
                                    fat = parseToIntString(text) + " g";
                                }
                            } catch (NumberFormatException e) {
                                Log.e("XmlParser", "Error parsing value: " + text, e);
                            }
                        }
                        tagName = null; // 한 번 사용 후 초기화
                        break;

                    case XmlPullParser.END_TAG:
                        if ("item".equals(parser.getName()) && !isFirstItemProcessed) {
                            isFirstItemProcessed = true;

                            // SharedPreferences에 저장된 영양정보만 표시
                            if (selectedNutritionInfo.contains("칼로리") && calories != null) {
                                nutritionInfo.append("칼로리: ").append(calories).append("\n");
                            }
                            if (selectedNutritionInfo.contains("탄수화물") && carbohydrates != null) {
                                nutritionInfo.append("탄수화물: ").append(carbohydrates).append("\n");
                            }
                            if (selectedNutritionInfo.contains("단백질") && protein != null) {
                                nutritionInfo.append("단백질: ").append(protein).append("\n");
                            }
                            if (selectedNutritionInfo.contains("지방") && fat != null) {
                                nutritionInfo.append("지방: ").append(fat).append("\n");
                            }

                            // 다음 아이템을 위해 값 초기화
                            foodName = calories = protein = fat = carbohydrates = null;
                        }
                        break;
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            Log.e("PARSING_ERROR", "Error parsing XML response: " + e.getMessage(), e);
            return "XML 데이터를 파싱하는 동안 오류가 발생했습니다.";
        }
        return nutritionInfo.toString();
    }

    /**
     * 문자열을 정수로 변환하고 실패 시 기본값 반환
     */
    private String parseToIntString(String value) {
        try {
            return String.valueOf((int) Float.parseFloat(value));
        } catch (NumberFormatException e) {
            Log.e("PARSING_ERROR", "Invalid number format for value: " + value, e);
            return "0"; // 기본값 반환
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