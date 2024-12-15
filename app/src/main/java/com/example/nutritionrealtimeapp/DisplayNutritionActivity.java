package com.example.nutritionrealtimeapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.OutputStream;
import java.util.Locale;
import java.util.UUID;

public class DisplayNutritionActivity extends AppCompatActivity {

    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private TextToSpeech tts; // Text-to-Speech 객체

    private static final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_nutrition);

        TextView barcodeTextView = findViewById(R.id.barcodeTextView);
        TextView nutritionInfoTextView = findViewById(R.id.nutritionInfoTextView);
        Button speechButton = findViewById(R.id.speechButton);
        Button brailleButton = findViewById(R.id.brailleButton);

        // Retrieve data from the previous activity
        String barcode = getIntent().getStringExtra("barcode");
        String foodName = getIntent().getStringExtra("foodName");
        String nutritionInfo = getIntent().getStringExtra("nutritionInfo");

        // Display data
        if (barcode != null && !barcode.isEmpty()) {
            barcodeTextView.setText("바코드: " + barcode);
        } else {
            barcodeTextView.setText("바코드 정보가 없습니다");
        }

        if (foodName != null && !foodName.isEmpty()) {
            nutritionInfoTextView.setText("식품명: " + foodName);
        } else {
            nutritionInfoTextView.setText("식품명이 없습니다");
        }

        if (nutritionInfo != null && !nutritionInfo.isEmpty()) {
            nutritionInfoTextView.append("\n" + nutritionInfo);
        }

        // Initialize TextToSpeech
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.KOREAN);
            } else {
                Toast.makeText(this, "Text-to-Speech 초기화 실패", Toast.LENGTH_SHORT).show();
            }
        });

        // Read nutrition info aloud
        speechButton.setOnClickListener(v -> {
            String text = nutritionInfoTextView.getText().toString();
            if (!text.isEmpty()) {
                speakText(text);
            } else {
                Toast.makeText(this, "읽을 텍스트가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        // Send nutrition info via Bluetooth
        brailleButton.setOnClickListener(v -> {
            String textToSend = nutritionInfoTextView.getText().toString();
            if (!textToSend.isEmpty()) {
                connectToBluetoothDevice("HCHC-06_TEAM5");
                sendKoreanHexData(textToSend);
            } else {
                Toast.makeText(this, "전송할 데이터가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void connectToBluetoothDevice(String targetDeviceName) {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "블루투스를 활성화해주세요.", Toast.LENGTH_SHORT).show();
                return;
            }

            BluetoothDevice targetDevice = null;
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            for (BluetoothDevice device : bluetoothAdapter.getBondedDevices()) {
                if (device.getName().equalsIgnoreCase(targetDeviceName)) {
                    targetDevice = device;
                    break;
                }
            }

            if (targetDevice == null) {
                Toast.makeText(this, "블루투스 장치를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            bluetoothSocket = targetDevice.createRfcommSocketToServiceRecord(BT_UUID);
            bluetoothSocket.connect();
            outputStream = bluetoothSocket.getOutputStream();
            Toast.makeText(this, "블루투스 연결 성공!", Toast.LENGTH_SHORT).show();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "블루투스 연결 실패", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendKoreanHexData(String koreanText) {
        try {
            if (outputStream != null) {
                String hexData = convertToHexForKorean(koreanText);
                byte[] hexBytes = hexStringToByteArray(hexData);
                outputStream.write(hexBytes);
                outputStream.write("\n".getBytes());
                Toast.makeText(this, "16진수 데이터 전송 완료!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "데이터 전송 실패", Toast.LENGTH_SHORT).show();
        } finally {
            try {
                if (outputStream != null) outputStream.close();
                if (bluetoothSocket != null) bluetoothSocket.close();
            } catch (Exception ignored) {}
        }
    }

    private String convertToHexForKorean(String text) {
        StringBuilder hexString = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (isKoreanCharacter(c)) {
                String hex = String.format("%04X", (int) c);
                hexString.append(hex);
            } else {
                hexString.append(String.format("%02X", (int) c));
            }
        }
        return hexString.toString();
    }

    private boolean isKoreanCharacter(char c) {
        return (c >= 0xAC00 && c <= 0xD7AF);
    }

    private byte[] hexStringToByteArray(String hexString) {
        int length = hexString.length();
        byte[] byteArray = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            byteArray[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return byteArray;
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
