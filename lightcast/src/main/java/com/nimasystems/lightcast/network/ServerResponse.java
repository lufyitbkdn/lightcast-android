/*
 * While Clicking Favorite button in Nutrition Sub Catorgory
 * Its used to Update the favorite task form backend
 * After getting success response status will updated for the corressponding item
 * */
package com.nimasystems.lightcast.network;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class ServerResponse<T> {
    private boolean isSuccessful;
    private HashMap<String, Object> data;
    private ApiServerErrorModel serverError;

    private int fetchedDataCount;
    private List<T> fetchedData;

    public ServerResponse(final T fetchedDataItem) {
        this.fetchedData = new ArrayList<T>() {{
            add(fetchedDataItem);
        }};
        this.fetchedDataCount = 1;
        isSuccessful = true;
    }

    public ServerResponse(final List<T> fetchedData, int fetchedDataCount) {
        this.fetchedData = fetchedData;
        this.fetchedDataCount = fetchedDataCount;
        isSuccessful = true;
    }

    public ServerResponse(final ApiServerErrorModel serverError) {
        this.serverError = serverError;
        isSuccessful = false;
    }

    public int getCode() {
        return serverError != null ? serverError.code : 0;
    }

    public String getMessage() {
        return serverError != null ? serverError.message : null;
    }

    public HashMap<String, Object> getData() {
        return data;
    }

    public ApiServerErrorModel getServerError() {
        return serverError;
    }

    public int getInt(@NonNull String key) {
        return data != null ? (int) data.get(key) : 0;
    }

    public float getFloat(@NonNull String key) {
        return data != null ? (float) data.get(key) : 0.0f;
    }

    public double getDouble(@NonNull String key) {
        return data != null ? (double) data.get(key) : 0.0f;
    }

    public List<?> getList(@NonNull String key) {
        return data != null ? (List<?>) data.get(key) : null;
    }

    public Date getDate(@NonNull String key) {
        return data != null ? (Date) data.get(key) : null;
    }

    public String getString(@NonNull String key) {
        return data != null ? (String) data.get(key) : "";
    }

    public boolean getBoolean(@NonNull String key) {
        return data != null && (boolean) data.get(key);
    }

    public HashMap<String, Object> getMap(@NonNull String key) {
        //noinspection unchecked
        return data != null ? (HashMap<String, Object>) data.get(key) : null;
    }

    public List<HashMap<String, Object>> getMapList(@NonNull String key) {
        //noinspection unchecked
        return data != null ? (List<HashMap<String, Object>>) data.get(key) : null;
    }

    public int getFetchedDataCount() {
        return fetchedDataCount;
    }

    public List<T> getFetchedData() {
        return fetchedData;
    }

    public T getFirstObject() {
        return fetchedData != null && fetchedData.size() > 0 ? fetchedData.get(0) : null;
    }

    public boolean isSuccessful() {
        return isSuccessful;
    }
}