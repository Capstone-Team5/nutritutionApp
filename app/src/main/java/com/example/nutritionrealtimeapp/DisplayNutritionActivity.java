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

        // Display food name and barcode information
        barcodeTextView.setText("식품명: " + (foodName != null ? foodName : "없음"));

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
//
//    private void sendToBraille(List<String> nutritionInfo) {
//        try {
//            // Check if Bluetooth is available and enabled
//            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
//                Toast.makeText(this, "블루투스를 활성화해주세요.", Toast.LENGTH_SHORT).show();
//                return;
//            }
//
//            // Connect to the Arduino Bluetooth device
//            BluetoothDevice device = bluetoothAdapter.getRemoteDevice("00:00:00:00:00:00"); // Replace with your Arduino's Bluetooth address
//            bluetoothSocket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
//            bluetoothSocket.connect();
//            outputStream = bluetoothSocket.getOutputStream();
//
//            // Send nutrition information as braille data
//            if (nutritionInfo != null) {
//                for (String info : nutritionInfo) {
//                    outputStream.write((info + "\n").getBytes());
//                }
//                Toast.makeText(this, "점자 데이터 전송 완료!", Toast.LENGTH_SHORT).show();
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//            Toast.makeText(this, "점자 데이터 전송 실패", Toast.LENGTH_SHORT).show();
//        } finally {
//            // Clean up resources
//            try {
//                if (outputStream != null) outputStream.close();
//                if (bluetoothSocket != null) bluetoothSocket.close();
//            } catch (Exception ignored) {}
//        }
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
