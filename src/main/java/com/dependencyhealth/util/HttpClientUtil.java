package com.dependencyhealth.util;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.RequestBody;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Utility class for executing HTTP requests.
 */
public class HttpClientUtil {

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            // Add retry interceptors if needed
            .build();

    public static String get(String url) throws IOException {
        return get(url, null);
    }

    public static String get(String url, String apiKey) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .header("Accept", "application/json");

        if (apiKey != null && !apiKey.isEmpty()) {
            requestBuilder.header("apiKey", apiKey);
        }

        Request request = requestBuilder.build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            if (response.body() != null) {
                return response.body().string();
            }
            return null;
        }
    }

    public static String post(String url, RequestBody body) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .build();

        try (Response response = client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }
            if (response.body() != null) {
                return response.body().string();
            }
            return null;
        }
    }
}
