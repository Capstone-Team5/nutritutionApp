package com.example.nutritionrealtimeapp;

import java.util.List;

public class NutritionData {
    private String barcode;
    private List<String> nutritionInfo;

    // 기본 생성자 (Firestore에 필요)
    public NutritionData() {
    }

    // 생성자
    public NutritionData(String barcode, List<String> nutritionInfo) {
        this.barcode = barcode;
        this.nutritionInfo = nutritionInfo;
    }

    // Getter와 Setter
    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public List<String> getNutritionInfo() {
        return nutritionInfo;
    }

    public void setNutritionInfo(List<String> nutritionInfo) {
        this.nutritionInfo = nutritionInfo;
    }
}
