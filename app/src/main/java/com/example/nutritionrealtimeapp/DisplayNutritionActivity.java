package com.example.nutritionrealtimeapp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

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
    private static final int REQUEST_PERMISSION_BT_CONNECT = 2;
    private static final int REQUEST_PERMISSION_BT_SCAN = 3;
    private static final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // SPP UUID

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private TextToSpeech tts; // Text-to-Speech 객체

    private List<BluetoothDevice> devices;
    private int pairedDeviceCount;

    private byte[] readBuffer;
    private int readBufferPosition;

    private Handler handler; // Handler for receiving messages on the main thread
    private Thread workerThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_nutrition);

        TextView barcodeTextView = findViewById(R.id.barcodeTextView);
        TextView nutritionInfoTextView = findViewById(R.id.nutritionInfoTextView);
        Button speechButton = findViewById(R.id.speechButton);
        Button brailleButton = findViewById(R.id.brailleButton);

        // 이전 액티비티로부터 데이터 가져오기
        String barcode = getIntent().getStringExtra("barcode");
        String foodName = getIntent().getStringExtra("foodName");
        String nutritionInfo = getIntent().getStringExtra("nutritionInfo");

        // 데이터 표시
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

        // TextToSpeech 초기화
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.KOREAN);
            } else {
                Toast.makeText(this, "Text-to-Speech 초기화 실패", Toast.LENGTH_SHORT).show();
            }
        });

        // 영양 정보 음성으로 읽기
        speechButton.setOnClickListener(v -> {
            String text = nutritionInfoTextView.getText().toString();
            if (!text.isEmpty()) {
                speakText(text);
            } else {
                Toast.makeText(this, "읽을 텍스트가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        // 블루투스를 통해 영양 정보 전송
        brailleButton.setOnClickListener(v -> {
            String textToSend = nutritionInfoTextView.getText().toString();
            if (!textToSend.isEmpty()) {
                // 블루투스 연결 창을 띄웁니다.
                checkBluetoothSupportAndEnable();
            } else {
                Toast.makeText(this, "전송할 데이터가 없습니다.", Toast.LENGTH_SHORT).show();
            }
        });

        // 블루투스 어댑터 초기화
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            // 블루투스를 지원하지 않는 기기일 경우
            Toast.makeText(this, "이 기기는 블루투스를 지원하지 않습니다.", Toast.LENGTH_LONG).show();
            finish(); // 애플리케이션 종료
        }

        handler = new Handler();
    }

    /**
     * 블루투스 지원 여부를 확인하고 활성화 요청을 합니다.
     */
    private void checkBluetoothSupportAndEnable() {
        if (bluetoothAdapter == null) {
            // 블루투스를 지원하지 않는 기기일 경우 이미 처리됨
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            // 블루투스가 비활성화 상태일 경우 활성화를 요청
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            // 블루투스가 이미 활성화 되어 있을 경우 권한 확인 및 디바이스 선택
            checkAndRequestPermissions();
        }
    }

    /**
     * 권한을 확인하고 요청하는 메서드
     */
    private void checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            List<String> permissionsToRequest = new ArrayList<>();

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_SCAN);
            }

            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_CONNECT);
            }

            if (!permissionsToRequest.isEmpty()) {
                // 사용자에게 권한 설명을 제공할 필요가 있는지 확인
                boolean shouldProvideRationaleScan = ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.BLUETOOTH_SCAN);
                boolean shouldProvideRationaleConnect = ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.BLUETOOTH_CONNECT);

                if (shouldProvideRationaleScan || shouldProvideRationaleConnect) {
                    // 사용자에게 권한의 필요성을 설명하는 다이얼로그 표시
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("블루투스 권한이 필요합니다");
                    builder.setMessage("이 애플리케이션은 블루투스를 사용하여 영양 정보를 전송하기 위해 권한이 필요합니다.");
                    builder.setPositiveButton("허용", (dialog, which) -> {
                        // 권한 요청
                        ActivityCompat.requestPermissions(DisplayNutritionActivity.this, permissionsToRequest.toArray(new String[0]), REQUEST_PERMISSION_BT_CONNECT);
                    });
                    builder.setNegativeButton("거부", (dialog, which) -> {
                        dialog.dismiss();
                        Toast.makeText(DisplayNutritionActivity.this, "블루투스 권한이 필요합니다.", Toast.LENGTH_LONG).show();
                        finish(); // 권한이 없으면 애플리케이션 종료 또는 기능 제한
                    });
                    builder.show();
                } else {
                    // 직접 권한 요청
                    ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), REQUEST_PERMISSION_BT_CONNECT);
                }
            } else {
                // 모든 권한이 이미 허용됨
                selectBluetoothDevice();
            }
        } else {
            // Android 11 이하에서는 BLUETOOTH 권한만 확인
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "BLUETOOTH 권한이 없음. 요청합니다.");
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.BLUETOOTH}, REQUEST_PERMISSION_BT_CONNECT);
            } else {
                // 권한이 이미 있는 경우 디바이스 선택
                selectBluetoothDevice();
            }
        }
    }

    /**
     * onActivityResult를 오버라이드하여 블루투스 활성화 결과를 처리합니다.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // '사용'을 눌렀을 때 블루투스가 활성화 되었으므로 권한 확인
                checkAndRequestPermissions();
            } else {
                // '취소'를 눌렀을 때 블루투스를 활성화할 수 없으므로 처리
                Toast.makeText(this, "블루투스를 활성화할 수 없습니다. 프로그램을 종료합니다.", Toast.LENGTH_LONG).show();
                finish(); // 애플리케이션 종료
            }
        }
    }

    /**
     * onRequestPermissionsResult를 오버라이드하여 권한 요청 결과를 처리합니다.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSION_BT_CONNECT) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (allGranted) {
                // 모든 권한이 허용된 경우 디바이스 선택
                Log.d(TAG, "모든 블루투스 권한이 허용됨.");
                selectBluetoothDevice();
            } else {
                // 일부 권한이 거부된 경우 처리
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("권한 제한");
                builder.setMessage("블루투스 권한이 허용되지 않았으므로 블루투스를 검색 및 연결할 수 없습니다.");
                builder.setPositiveButton(android.R.string.ok, (dialog, which) -> {
                    dialog.dismiss();
                    finish(); // 권한이 없으면 애플리케이션 종료 또는 기능 제한
                });
                builder.setCancelable(false);
                builder.show();
            }
        }
    }

    /**
     * 페어링된 블루투스 디바이스를 선택하는 다이얼로그를 표시합니다.
     */
    public void selectBluetoothDevice() {
        // 이미 페어링 되어있는 블루투스 기기를 찾습니다.
        devices = new ArrayList<>(bluetoothAdapter.getBondedDevices());
        // 페어링 된 디바이스의 크기를 저장
        pairedDeviceCount = devices.size();

        // 페어링 되어있는 장치가 없는 경우
        if (pairedDeviceCount == 0) {
            Toast.makeText(this, "페어링된 블루투스 디바이스가 없습니다.", Toast.LENGTH_SHORT).show();
            // 페어링을 하기 위한 추가적인 로직을 여기에 구현할 수 있습니다.
            return;
        }

        // 페어링 되어있는 장치가 있는 경우
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("페어링 되어있는 블루투스 디바이스 목록");

        // 페어링 된 각각의 디바이스의 이름과 주소를 저장
        List<String> deviceNames = new ArrayList<>();
        for (BluetoothDevice bluetoothDevice : devices) {
            deviceNames.add(bluetoothDevice.getName());
        }
        deviceNames.add("취소"); // 취소 옵션 추가

        // List를 CharSequence 배열으로 변경
        final CharSequence[] charSequences = deviceNames.toArray(new CharSequence[deviceNames.size()]);

        // 해당 아이템을 눌렀을 때 호출 되는 이벤트 리스너
        builder.setItems(charSequences, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == charSequences.length - 1) {
                    // '취소'를 선택했을 때
                    dialog.dismiss();
                } else {
                    // 해당 디바이스와 연결하는 함수 호출
                    String selectedDeviceName = charSequences[which].toString();
                    Log.d(TAG, "선택된 디바이스: " + selectedDeviceName);
                    connectDevice(selectedDeviceName);
                }
            }
        });

        // 뒤로가기 버튼 누를 때 창이 닫히지 않도록 설정
        builder.setCancelable(false);
        // 다이얼로그 생성 및 표시
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    /**
     * 선택된 블루투스 디바이스와 연결을 시도합니다.
     * @param deviceName 선택된 디바이스의 이름
     */
    private void connectDevice(String deviceName) {
        BluetoothDevice targetDevice = null;

        try {
            // 권한 체크
            if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED ||
                    (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)) {
                Log.d(TAG, "BLUETOOTH_CONNECT 또는 BLUETOOTH_SCAN 권한이 없음. 요청합니다.");
                List<String> permissionsToRequest = new ArrayList<>();
                permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_CONNECT);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    permissionsToRequest.add(android.Manifest.permission.BLUETOOTH_SCAN);
                }
                ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), REQUEST_PERMISSION_BT_CONNECT);
                return;
            }

            // 디바이스 검색
            for (BluetoothDevice device : devices) {
                if (device.getName().equalsIgnoreCase(deviceName)) {
                    targetDevice = device;
                    Log.d(TAG, "디바이스 찾음: " + device.getName());
                    break;
                }
            }

            if (targetDevice == null) {
                Log.d(TAG, "디바이스를 찾을 수 없음.");
                Toast.makeText(this, "블루투스 장치를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                return;
            }

            // 비동기 작업 시작 (새로운 스레드 사용)
            BluetoothDevice finalTargetDevice = targetDevice;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        // RFCOMM 소켓 생성
                        bluetoothSocket = finalTargetDevice.createRfcommSocketToServiceRecord(BT_UUID);
                        bluetoothAdapter.cancelDiscovery(); // 디스커버리 중지

                        Log.d(TAG, "소켓 연결 시도 중...");
                        bluetoothSocket.connect();
                        outputStream = bluetoothSocket.getOutputStream();
                        inputStream = bluetoothSocket.getInputStream();
                        Log.d(TAG, "소켓 연결 성공.");
                        runOnUiThread(() -> Toast.makeText(DisplayNutritionActivity.this, "블루투스 연결 성공!", Toast.LENGTH_SHORT).show());

                        // 데이터 수신 시작
                        receiveData();

                        // 데이터 전송
                        String textToSend = ((TextView) findViewById(R.id.nutritionInfoTextView)).getText().toString();
                        sendKoreanHexData(textToSend);

                    } catch (SecurityException se) {
                        Log.e(TAG, "SecurityException: " + se.getMessage());
                        se.printStackTrace();
                        runOnUiThread(() -> Toast.makeText(DisplayNutritionActivity.this, "권한 문제로 블루투스 연결에 실패했습니다.", Toast.LENGTH_SHORT).show());
                    } catch (IOException e) {
                        Log.e(TAG, "IOException: " + e.getMessage());
                        e.printStackTrace();
                        runOnUiThread(() -> Toast.makeText(DisplayNutritionActivity.this, "블루투스 연결 실패", Toast.LENGTH_SHORT).show());
                    }
                }
            }).start();

        } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "블루투스 연결 실패", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 데이터를 수신하는 메서드
     */
    public void receiveData() {
        readBufferPosition = 0;
        readBuffer = new byte[1024];
        handler = new Handler();

        // 데이터 수신을 위한 쓰레드
        workerThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    if (inputStream == null) {
                        return;
                    }

                    // 데이터 수신 확인
                    int byteAvailable = inputStream.available();

                    if (byteAvailable > 0) {
                        byte[] bytes = new byte[byteAvailable];
                        inputStream.read(bytes);

                        for (int i = 0; i < byteAvailable; i++) {
                            byte tempByte = bytes[i];
                            if (tempByte == '\n') {
                                // 개행문자를 만나면 버퍼를 문자열로 변환
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                final String receivedText = new String(encodedBytes, "UTF-8");
                                readBufferPosition = 0;

                                // UI 업데이트는 메인 스레드에서 수행
                                handler.post(() -> Toast.makeText(DisplayNutritionActivity.this, "수신 데이터: " + receivedText, Toast.LENGTH_SHORT).show());
                            } else {
                                readBuffer[readBufferPosition++] = tempByte;
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }

                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    break;
                }
            }
        });

        workerThread.start();
    }

    /**
     * 한글을 포함한 문자열을 16진수으로 변환하여 전송합니다.
     * @param koreanText 변환할 텍스트
     */
    private void sendKoreanHexData(String koreanText) {
        try {
            if (outputStream != null) {
                String hexData = convertToHexForKorean(koreanText);
                byte[] hexBytes = hexStringToByteArray(hexData);
                outputStream.write(hexBytes);
                outputStream.write("\n".getBytes());
                runOnUiThread(() -> Toast.makeText(DisplayNutritionActivity.this, "16진수 데이터 전송 완료!", Toast.LENGTH_SHORT).show());
            }
        } catch (Exception e) {
            Log.e(TAG, "데이터 전송 실패: " + e.getMessage());
            e.printStackTrace();
            runOnUiThread(() -> Toast.makeText(DisplayNutritionActivity.this, "데이터 전송 실패", Toast.LENGTH_SHORT).show());
        }
    }

    /**
     * 한글을 포함한 문자열을 16진수 문자열로 변환합니다.
     * @param text 변환할 텍스트
     * @return 16진수 문자열
     */
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

    /**
     * 문자가 한글인지 확인합니다.
     * @param c 확인할 문자
     * @return 한글이면 true, 아니면 false
     */
    private boolean isKoreanCharacter(char c) {
        return (c >= 0xAC00 && c <= 0xD7AF);
    }

    /**
     * 16진수 문자열을 바이트 배열로 변환합니다.
     * @param hexString 변환할 16진수 문자열
     * @return 바이트 배열
     */
    private byte[] hexStringToByteArray(String hexString) {
        int length = hexString.length();
        byte[] byteArray = new byte[length / 2];
        for (int i = 0; i < length; i += 2) {
            byteArray[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                    + Character.digit(hexString.charAt(i + 1), 16));
        }
        return byteArray;
    }

    /**
     * 텍스트를 음성으로 읽어줍니다.
     * @param text 읽을 텍스트
     */
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

        // 블루투스 소켓 및 스트림 닫기
        try {
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 워커 스레드 인터럽트
        if (workerThread != null && workerThread.isAlive()) {
            workerThread.interrupt();
        }

        super.onDestroy();
    }
}
