package com.example.nutritionrealtimeapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.OutputStream;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class DisplayNutritionActivity extends AppCompatActivity {

    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private TextToSpeech tts; // Text-to-Speech 객체

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_nutrition);

        TextView barcodeTextView = findViewById(R.id.barcodeTextView);
        TextView nutritionInfoTextView = findViewById(R.id.nutritionInfoTextView);
        Button speechButton = findViewById(R.id.speechButton);

        // Retrieve data from the previous activity
        String foodName = getIntent().getStringExtra("foodName");
        String nutritionInfo = getIntent().getStringExtra("nutritionInfo");
        // Display nutrition information
        if (nutritionInfo != null && !nutritionInfo.isEmpty()) {
            nutritionInfoTextView.setText(nutritionInfo);
        } else {
            nutritionInfoTextView.setText("영양 정보가 없습니다.");
        }

        // Initialize TextToSpeech
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.KOREAN);
            } else {
                Toast.makeText(this, "TextToSpeech 초기화 실패", Toast.LENGTH_SHORT).show();
            }
        });

        // Button to read text via speech
        speechButton.setOnClickListener(v -> {
            String text = nutritionInfoTextView.getText().toString();
            if (!text.isEmpty()) {
                speakText(text);
            } else {
                Toast.makeText(this, "읽을 텍스트가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
//        // Button to send braille data
//        brailleButton.setOnClickListener(v -> sendToBraille(selectedNutritionInfo));

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
//    private void sendKoreanHexData(String koreanText) {
//        try {
//            // Bluetooth 활성화 확인
//            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
//                Toast.makeText(this, "블루투스를 활성화해주세요.", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            // Arduino Bluetooth 장치 연결 (Bluetooth 주소 변경 필요)
//            BluetoothDevice device = bluetoothAdapter.getRemoteDevice("00:00:00:00:00:00"); // 교체: Arduino Bluetooth 주소
//            bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
//            bluetoothSocket.connect();
//            outputStream = bluetoothSocket.getOutputStream();
//
//            // 한글 문자열을 자모 단위로 변환하여 전송
//            if (koreanText != null) {
//                String hexData = convertToHexForKorean(koreanText);  // 한글을 16진수로 변환
//                byte[] hexBytes = hexStringToByteArray(hexData);  // 16진수 문자열을 바이트 배열로 변환
//                outputStream.write(hexBytes);  // 바이트 배열로 전송
//                outputStream.write("\n".getBytes());  // 구분자 추가 (필요시)
//                Toast.makeText(this, "16진수 데이터 전송 완료!", Toast.LENGTH_SHORT).show();
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            Toast.makeText(this, "데이터 전송 실패", Toast.LENGTH_SHORT).show();
//        } finally {
//            // 자원 정리
//            try {
//                if (outputStream != null) outputStream.close();
//                if (bluetoothSocket != null) bluetoothSocket.close();
//            } catch (Exception ignored) {}
//        }
//    }
//
//    // 한글 자모를 16진수로 변환하는 함수
//    private String convertToHexForKorean(String text) {
//        StringBuilder hexString = new StringBuilder();
//        for (char c : text.toCharArray()) {
//            if (isKoreanCharacter(c)) {
//                // 각 자모를 16진수로 변환
//                String hex = String.format("%04X", (int) c);  // 한글 자모의 16진수 표현
//                hexString.append(hex);
//            } else {
//                // 한글이 아닌 경우는 그대로 처리
//                hexString.append(String.format("%02X", (int) c));
//            }
//        }
//        return hexString.toString();
//    }
//
//    // 한글 문자인지 확인하는 함수
//    private boolean isKoreanCharacter(char c) {
//        return (c >= 0xAC00 && c <= 0xD7AF);  // 한글 음절 범위 (유니코드)
//    }
//
//    // 16진수 문자열을 바이트 배열로 변환하는 함수
//    private byte[] hexStringToByteArray(String hexString) {
//        int length = hexString.length();
//        byte[] byteArray = new byte[length / 2];
//        for (int i = 0; i < length; i += 2) {
//            byteArray[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
//                    + Character.digit(hexString.charAt(i + 1), 16));
//        }
//        return byteArray;
//    }

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
