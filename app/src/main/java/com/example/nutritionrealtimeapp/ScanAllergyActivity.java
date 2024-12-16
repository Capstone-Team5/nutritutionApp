package com.example.nutritionrealtimeapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.vision.v1.AnnotateImageResponse;

import com.google.cloud.vision.v1.ImageAnnotatorClient;
import com.google.cloud.vision.v1.AnnotateImageRequest;
import com.google.cloud.vision.v1.ImageAnnotatorSettings;
import com.google.cloud.vision.v1.TextAnnotation;
import com.google.cloud.vision.v1.Image;
import com.google.cloud.vision.v1.Feature;
import com.google.protobuf.ByteString;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

import android.os.Environment;


public class ScanAllergyActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private TextView resultTextView;
    private static ImageAnnotatorClient visionClient;
    private Uri photoURI;
    private SharedPreferences sharedPreferences;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.display_allergy);

        resultTextView = findViewById(R.id.resultTextView);
        Button captureButton = findViewById(R.id.captureButton);

        // 요청 권한
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_IMAGE_CAPTURE);

        // text 출력
        captureButton.setOnClickListener(v -> dispatchTakePictureIntent());

        Intent intent = getIntent();
        ArrayList<String> selectedAllergies = intent.getStringArrayListExtra("selectedAllergies");
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            try {
                // 임시 파일 생성
                File photoFile = createImageFile();
                if (photoFile != null) {
                    photoURI = FileProvider.getUriForFile(this,
                            "com.example.nutritionrealtimeapp.fileprovider",
                            photoFile);
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            } catch (IOException ex) {
                Log.e("ScanActivity", "Error occurred while creating the file", ex);
            }
        }
    }

    private File createImageFile() throws IOException {
        // 파일 이름 생성
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            if (photoURI != null) {
                try {
                    Bitmap imageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), photoURI);
                    processImage(imageBitmap);
                } catch (IOException e) {
                    Log.e("ScanActivity", "Failed to load the captured image", e);
                }
            } else {
                Log.e("ScanActivity", "Photo URI is null.");
            }
        } else {
            Log.e("ScanActivity", "Image capture failed or canceled.");
        }
    }

    public static class VisionHelper {
        public static ImageAnnotatorClient createVisionClient(Context context) {
            try {
                // 인증 JSON 파일을 res/raw에서 읽어오기
                InputStream credentialStream = context.getResources().openRawResource(R.raw.ocr);
                GoogleCredentials credentials = GoogleCredentials.fromStream(credentialStream);

                // 클라이언트에 인증 정보 설정
                ImageAnnotatorSettings settings = ImageAnnotatorSettings.newBuilder()
                        .setCredentialsProvider(() -> credentials)
                        .build();

                return ImageAnnotatorClient.create(settings);
            } catch (IOException e) {
                Log.e("VisionHelper", "Failed to load credentials: " + e.getMessage());
            }
            return null;
        }
        public static ImageAnnotatorClient getVisionClient(Context context) {
            if (visionClient == null) {
                visionClient = VisionHelper.createVisionClient(context);
            }
            return visionClient;
        }
    }

    private void processImage(Bitmap bitmap) {
        new Thread(() -> {
            try {
                ImageAnnotatorClient visionClient = VisionHelper.getVisionClient(this);
                if (visionClient == null) {
                    Log.e("VisionHelper", "Vision client creation failed.");
                    return;
                }

                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 800, 600, true);
                resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);  // 압축 품질 80%

                byte[] imageBytes = byteArrayOutputStream.toByteArray();

                ByteString imgBytes = ByteString.copyFrom(imageBytes);
                Image img = Image.newBuilder().setContent(imgBytes).build();


                Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
                AnnotateImageRequest request = AnnotateImageRequest.newBuilder()
                        .addFeatures(feat)
                        .setImage(img)
                        .build();

                List<AnnotateImageResponse> responses = null;
                StringBuilder fullResponse = new StringBuilder();
                try {
                    long startTime = System.currentTimeMillis();
                    responses = visionClient.batchAnnotateImages(Collections.singletonList(request)).getResponsesList();
                    long endTime = System.currentTimeMillis();
                    Log.i("VisionHelper", "Response processing time: " + (endTime - startTime) + "ms");
                } catch (Exception e) {
                    Log.e("VisionHelper", "API call failed: " + e.getMessage());
                }

                for (AnnotateImageResponse response : responses) {
                    if (response.hasError()) {
                        Log.e("VisionHelper", "Error: " + response.getError());
                    } else {
                        TextAnnotation text = response.getFullTextAnnotation();
                        String recognizedText = text.getText();
                        fullResponse.append(recognizedText);
                        Log.i("VisionHelper", "recognized : " + recognizedText);
                    }
                }

                runOnUiThread(() -> displayText(fullResponse.toString()));


            } catch (Exception e) {
                Log.e("ScanActivity", "Error in processImage: " + e.getMessage());
            }
        }).start();
    }


    private void displayText(String visionText) {
        sharedPreferences = getSharedPreferences("NutritionApp", Context.MODE_PRIVATE);

        // 저장된 알러기 데이터 로드
        Set<String> savedAllergies = sharedPreferences.getStringSet("selectedAllergies", new HashSet<>());

        // Set<String>을 ArrayList<String>로 변환
        ArrayList<String> selectedAllergies = new ArrayList<>(savedAllergies);

        // Vision에서 인식된 텍스트와 알러지 데이터 비교
        StringBuilder displayMessage = new StringBuilder();
        if (selectedAllergies != null && !selectedAllergies.isEmpty()) {
            for (String allergy : selectedAllergies) {
                if (visionText.contains(allergy)) {
                    displayMessage.append(allergy).append("포함\n");
                }
            }
        }
        if(displayMessage.length() == 0){
            displayMessage.append("없음");
        }

        resultTextView.setText(displayMessage.toString());
    }
}

