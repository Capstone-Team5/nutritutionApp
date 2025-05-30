package com.example.nutritionrealtimeapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class DisplayNutritionActivity extends AppCompatActivity {

    private static final String TAG = "DisplayNutritionActivity";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private InputStream inputStream;
    private OutputStream outputStream;
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_nutrition);

        LinearLayout fontControlLayout = findViewById(R.id.fontControlLayout);

        TextView barcodeTextView = findViewById(R.id.barcodeTextView);
        TextView nutritionInfoTextView = findViewById(R.id.nutritionInfoTextView);
        Button speechButton = findViewById(R.id.speechButton);
        Button brailleButton = findViewById(R.id.brailleButton);
        Button btnIncrease = findViewById(R.id.btnIncrease);
        Button btnDecrease = findViewById(R.id.btnDecrease);

        String barcode = getIntent().getStringExtra("barcode");
        String foodName = getIntent().getStringExtra("foodName");
        String nutritionInfo = getIntent().getStringExtra("nutritionInfo");

        //중복 선택 수정
        // [MODIFIED] SharedPreferences에서 모드 값들 가져오기
        SharedPreferences sharedPreferences = getSharedPreferences("NutritionApp", MODE_PRIVATE);
        boolean isVisualMode = sharedPreferences.getBoolean("visualMode", false);
        boolean isVoiceMode = sharedPreferences.getBoolean("voiceMode", false);
        boolean isBrailleMode = sharedPreferences.getBoolean("brailleMode", false);

        barcodeTextView.setText(barcode != null && !barcode.isEmpty() ? "바코드: " + barcode : "바코드 정보가 없습니다");
        nutritionInfoTextView.setText(foodName != null && !foodName.isEmpty() ? "식품명: " + foodName : "식품명이 없습니다");
        if (nutritionInfo != null && !nutritionInfo.isEmpty()) {
            nutritionInfoTextView.append("\n" + nutritionInfo);
        }

// [수정] 모드별 UI 요소 표시 제어
        fontControlLayout.setVisibility(isVisualMode ? View.VISIBLE : View.GONE);
        speechButton.setVisibility(isVoiceMode ? View.VISIBLE : View.GONE);
        brailleButton.setVisibility(isBrailleMode ? View.VISIBLE : View.GONE);

// [수정] 시각화 모드에 한해서 텍스트 확대/축소 기능 활성화
        if (isVisualMode) {
            nutritionInfoTextView.setTextSize(28);
            nutritionInfoTextView.setMovementMethod(new ScrollingMovementMethod());

            btnIncrease.setOnClickListener(v -> {
                float currentSize = nutritionInfoTextView.getTextSize() / getResources().getDisplayMetrics().scaledDensity;
                nutritionInfoTextView.setTextSize(currentSize + 2);
            });

            btnDecrease.setOnClickListener(v -> {
                float currentSize = nutritionInfoTextView.getTextSize() / getResources().getDisplayMetrics().scaledDensity;
                if (currentSize > 12) nutritionInfoTextView.setTextSize(currentSize - 2);
            });
        }

/*
        //SharedPreferecnes에서 visualMode 조회
        // visualMode 값 확인
        SharedPreferences sharedPreferences = getSharedPreferences("NutritionApp", MODE_PRIVATE);
        boolean isVisualMode = sharedPreferences.getBoolean("visualMode", false);
        //boolean isVisualMode = getSharedPreferences("NutritionApp", MODE_PRIVATE)
                                //.getBoolean("visualMode", false);
        
        barcodeTextView.setText(barcode != null && !barcode.isEmpty() ? "바코드: " + barcode : "바코드 정보가 없습니다");
        nutritionInfoTextView.setText(foodName != null && !foodName.isEmpty() ? "식품명: " + foodName : "식품명이 없습니다");
        if (nutritionInfo != null && !nutritionInfo.isEmpty()) {
            nutritionInfoTextView.append("\n" + nutritionInfo);
        }

        if(isVisualMode) {
            // 초기 텍스트 크기 24sp 설정
            nutritionInfoTextView.setTextSize(28);
            nutritionInfoTextView.setMovementMethod(new ScrollingMovementMethod());

            // 글씨 크기 증가 버튼
            btnIncrease.setOnClickListener(v -> {
                float currentSize = nutritionInfoTextView.getTextSize() / getResources().getDisplayMetrics().scaledDensity;
                nutritionInfoTextView.setTextSize(currentSize + 2);
            });

            // 글씨 크기 감소 버튼
            btnDecrease.setOnClickListener(v -> {
                float currentSize = nutritionInfoTextView.getTextSize() / getResources().getDisplayMetrics().scaledDensity;
                if (currentSize > 12) nutritionInfoTextView.setTextSize(currentSize - 2);
            });
        } else {
            fontControlLayout.setVisibility(View.GONE);
        }
*/
        //tts 초기화
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.KOREAN);
            } else {
                Toast.makeText(this, "Text-to-Speech 초기화 실패", Toast.LENGTH_SHORT).show();
            }
        });

        //음성 버튼 클릭
        speechButton.setOnClickListener(v -> {
            String text = nutritionInfoTextView.getText().toString();
            if (!text.isEmpty()) {
                speakText(text);
            } else {
                Toast.makeText(this, "읽을 텍스트가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        //점자 버튼 클릭
        brailleButton.setOnClickListener(v -> {
            String textToSend = nutritionInfoTextView.getText().toString().replaceAll(":", "");
            if (!textToSend.isEmpty()) {
                checkBluetoothSupportAndEnable(convertToHexWithKoreanSplit(textToSend));
            } else {
                Toast.makeText(this, "전송할 데이터가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "이 기기는 블루투스를 지원하지 않습니다.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void checkBluetoothSupportAndEnable(String dataToSend) {
        if (bluetoothAdapter == null) return;


        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            if (bluetoothSocket != null && bluetoothSocket.isConnected()) {
                sendData(dataToSend);
                receiveData();
            } else {
                selectBluetoothDevice(dataToSend);
            }
        }
    }

    private void selectBluetoothDevice(String dataToSend) {
        List<BluetoothDevice> devices = new ArrayList<>(bluetoothAdapter.getBondedDevices());
        if (devices.isEmpty()) {
            Toast.makeText(this, "페어링된 블루투스 디바이스가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("블루투스 디바이스 선택");

        List<String> deviceNames = new ArrayList<>();
        for (BluetoothDevice device : devices) {
            deviceNames.add(device.getName());
        }
        deviceNames.add("취소");

        builder.setItems(deviceNames.toArray(new String[0]), (dialog, which) -> {
            if (which == devices.size()) {
                dialog.dismiss();
            } else {
                connectDevice(devices.get(which), dataToSend);
            }
        });

        builder.setCancelable(false);
        builder.show();
    }

    private void connectDevice(BluetoothDevice device, String dataToSend) {
        new Thread(() -> {
            try {
                if (bluetoothSocket == null || !bluetoothSocket.isConnected()) {
                    bluetoothSocket = device.createRfcommSocketToServiceRecord(BT_UUID);
                    bluetoothAdapter.cancelDiscovery();
                    bluetoothSocket.connect();
                    outputStream = bluetoothSocket.getOutputStream();
                    inputStream = bluetoothSocket.getInputStream();

                    runOnUiThread(() -> Toast.makeText(this, "블루투스 연결 성공", Toast.LENGTH_SHORT).show());
                }

                sendData(dataToSend);
                receiveData();

            } catch (IOException e) {
                Log.e(TAG, "Bluetooth connection failed: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(this, "블루투스 연결 실패", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void receiveData() {
        new Thread(() -> {
            try {
                byte[] buffer = new byte[1024];
                int bytes;

                if (inputStream != null) {
                    while ((bytes = inputStream.read(buffer)) != -1) {
                        String receivedMessage = new String(buffer, 0, bytes);
                        Log.d(TAG, "Received data: " + receivedMessage);

                        runOnUiThread(() -> Toast.makeText(this, "수신된 데이터: " + receivedMessage, Toast.LENGTH_SHORT).show());
                    }
                }
            } catch (IOException e) {
                Log.e(TAG, "Data receive failed: " + e.getMessage());
            }
        }).start();
    }
    private String convertToHexWithKoreanSplit(String text) {
        StringBuilder hexString = new StringBuilder();
        String[] chosung = {"ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"};
        String[] jungsung = {"ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅘ", "ㅙ", "ㅚ", "ㅛ", "ㅜ", "ㅝ", "ㅞ", "ㅟ", "ㅠ", "ㅡ", "ㅢ", "ㅣ"};
        String[] jongsung = {"", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ", "ㄻ", "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"};

        for (char c : text.toCharArray()) {
            if (c >= 0xAC00 && c <= 0xD7AF) { // 한글 범위
                int unicode = c - 0xAC00;
                int cho = unicode / (21 * 28); // 초성
                int jung = (unicode % (21 * 28)) / 28; // 중성
                int jong = unicode % 28; // 종성

                // 초성 처리
                hexString.append("0x");
                hexString.append(String.format("%04X", (int) chosung[cho].charAt(0)));

                // 중성 처리
                hexString.append("0x");
                hexString.append(String.format("%04X", (int) jungsung[jung].charAt(0)));

                // 종성 처리: 종성 값에 0x1000을 더함
                if (jong != 0) {
                    int jongValue = jongsung[jong].charAt(0) + 0x1000; // 종성값에 0x1000 추가
                    hexString.append("0x");
                    hexString.append(String.format("%04X", jongValue));
                }
            } else {
                // 한글이 아니면 일반 문자 처리
                hexString.append("0x");
                hexString.append(String.format("%04X", (int) c));
            }
        }
        return hexString.toString();
    }

    private void sendData(String data) {
        Log.d(TAG, "Sending data: " + data);
        try {
            String formattedData = data + "\n"; // 줄바꿈 추가로 데이터 경계 명확히
            outputStream.write(formattedData.getBytes());
            runOnUiThread(() -> Toast.makeText(this, "데이터 전송 성공: " + formattedData, Toast.LENGTH_SHORT).show());
        } catch (IOException e) {
            Log.e(TAG, "Data send failed: " + e.getMessage());
            runOnUiThread(() -> Toast.makeText(this, "데이터 전송 실패", Toast.LENGTH_SHORT).show());
        }
    }

    private void speakText(String text) {
        if (tts != null) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

}
