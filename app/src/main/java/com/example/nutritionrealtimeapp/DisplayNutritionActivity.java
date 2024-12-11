package com.example.nutritionrealtimeapp;

import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class DisplayNutritionActivity extends AppCompatActivity {

    private TextToSpeech tts; // Text-to-Speech 객체
    private String apiKey = "YOUR_API_KEY"; // API 키

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_nutrition);

        TextView barcodeTextView = findViewById(R.id.barcodeTextView);
        TextView nutritionInfoTextView = findViewById(R.id.nutritionInfoTextView);
        Button speechButton = findViewById(R.id.speechButton);

        // Retrieve data from the previous activity
        String barcodeValue = getIntent().getStringExtra("barcodeValue");

        // Display barcode information
        barcodeTextView.setText("바코드 번호: " + (barcodeValue != null ? barcodeValue : "없음"));

        // Initialize TextToSpeech
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.KOREAN);
            } else {
                Toast.makeText(this, "TextToSpeech 초기화 실패", Toast.LENGTH_SHORT).show();
            }
        });

        // Fetch and display nutrition info
        if (barcodeValue != null) {
            fetchNutritionInfo(barcodeValue, nutritionInfoTextView);
        } else {
            nutritionInfoTextView.setText("바코드 값이 없습니다.");
        }

        // Button to read text via speech
        speechButton.setOnClickListener(v -> {
            String text = nutritionInfoTextView.getText().toString();
            if (!text.isEmpty()) {
                speakText(text);
            } else {
                Toast.makeText(this, "읽을 텍스트가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchNutritionInfo(String barcode, TextView nutritionInfoTextView) {
        new Thread(() -> {
            try {
                String urlString = "http://openapi.foodsafetykorea.go.kr/api/"
                        + apiKey
                        + "/C005/json/1/5/BAR_CD=" + barcode;

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

                // Parse and display the JSON response
                runOnUiThread(() -> parseAndDisplayNutritionInfo(response.toString(), nutritionInfoTextView));

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "API 호출 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
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

    private void speakText(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        // Release TextToSpeech resources
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }
}
