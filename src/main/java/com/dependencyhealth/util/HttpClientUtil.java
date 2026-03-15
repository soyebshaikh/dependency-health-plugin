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
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(chain -> {
                Request request = chain.request();
                Response response = null;
                int attempts = 0;
                while (attempts < 3) {
                    try {
                        response = chain.proceed(request);
                        if (response.isSuccessful())
                            return response;

                        // If it's a client error (except 429), just return it and let the caller handle
                        if (response.code() != 429 && response.code() >= 400 && response.code() < 500) {
                            return response;
                        }

                        // Otherwise (429 or 5xx), we might retry. We MUST close the body of the failing
                        // response
                        // to avoid leaking connections in the pool.
                        if (response.body() != null) {
                            response.close();
                        }
                    } catch (IOException e) {
                        if (attempts == 2)
                            throw e;
                    }
                    attempts++;
                    try {
                        Thread.sleep(attempts * 1000L);
                    } catch (InterruptedException ignored) {
                    }
                }
                return response;
            })
            .build();

    public static String get(String url) throws IOException {
        return get(url, null);
    }

    public static String get(String url, String apiKey) throws IOException {
        Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

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
                .header("Accept", "application/json, text/javascript, */*; q=0.01")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Content-Type", "application/json")
                .header("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
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
