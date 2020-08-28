package com.nimasystems.lightcast.network;

import androidx.annotation.NonNull;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ApiServerErrorModel {

    public int code;
    public String message;
    public String domainName;
    public JSONObject extraData;
    public int statusCode;
    public Map<String, List<String>> responseHeaders = null;
    public String responseBody = null;

    public ArrayList<ApiServerValidationError> validationErrors;

    public int getStatusCode() {
        return statusCode;
    }

    public ApiServerErrorModel setStatusCode(int statusCode) {
        this.statusCode = statusCode;
        return this;
    }

    public Map<String, List<String>> getResponseHeaders() {
        return responseHeaders;
    }

    public ApiServerErrorModel setResponseHeaders(Map<String, List<String>> responseHeaders) {
        this.responseHeaders = responseHeaders;
        return this;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public ApiServerErrorModel setResponseBody(String responseBody) {
        this.responseBody = responseBody;
        return this;
    }

    public int getCode() {
        return code;
    }

    public ApiServerErrorModel setCode(int code) {
        this.code = code;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public ApiServerErrorModel setMessage(String message) {
        this.message = message;
        return this;
    }

    public String getDomainName() {
        return domainName;
    }

    public ApiServerErrorModel setDomainName(String domainName) {
        this.domainName = domainName;
        return this;
    }

    public JSONObject getExtraData() {
        return extraData;
    }

    public ApiServerErrorModel setExtraData(JSONObject extraData) {
        this.extraData = extraData;
        return this;
    }

    public ArrayList<ApiServerValidationError> getValidationErrors() {
        return validationErrors;
    }

    public ApiServerErrorModel setValidationErrors(ArrayList<ApiServerValidationError> validationErrors) {
        this.validationErrors = validationErrors;
        return this;
    }

    @NonNull
    @Override
    public String toString() {
        return message;
    }
}
