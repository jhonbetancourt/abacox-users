package com.infomedia.abacox.users.service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.infomedia.abacox.users.dto.recaptcha.GoogleRecaptchaResponse;
import lombok.extern.log4j.Log4j2;
import okhttp3.*;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Objects;

@Service
@Log4j2
public class RecaptchaService {

    private final OkHttpClient okHttpClient;
    private final ObjectMapper objectMapper;
    @Value("${google.recaptcha.verify-url}")
    private String verifyUrl;
    @Value("${google.recaptcha.secret-key}")
    private String secretKey;

    // Constructor-based dependency injection
    public RecaptchaService(ObjectMapper objectMapper) {
        HttpLoggingInterceptor httpLoggingInterceptor = new HttpLoggingInterceptor();
        httpLoggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
        this.okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(httpLoggingInterceptor)
                .build();
        this.objectMapper = objectMapper;
    }

    /**
     * Validates the reCAPTCHA token by sending it to Google's verification API.
     *
     * @param token The reCAPTCHA token from the client.
     * @return true if the token is valid, false otherwise.
     */
    public boolean validateRecaptcha(String token) {
        if (!StringUtils.hasText(token)) {
            log.warn("reCAPTCHA token is empty or null.");
            return false;
        }

        // Create the form body with secret and response token
        RequestBody formBody = new FormBody.Builder()
                .add("secret", secretKey)
                .add("response", token)
                // You can also add "remoteip" here if you have the user's IP address
                .build();

        // Build the POST request to Google's verification URL
        Request request = new Request.Builder()
                .url(verifyUrl)
                .post(formBody)
                .build();

        try (Response response = okHttpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                log.error("Unsuccessful response from Google reCAPTCHA API: {}", response);
                return false;
            }

            String responseBody = Objects.requireNonNull(response.body()).string();
            GoogleRecaptchaResponse recaptchaResponse = objectMapper.readValue(responseBody, GoogleRecaptchaResponse.class);

            if (recaptchaResponse.success()) {
                log.info("reCAPTCHA validation successful.");
                return true;
            } else {
                log.warn("reCAPTCHA validation failed. Error codes: {}", recaptchaResponse.errorCodes());
                return false;
            }

        } catch (IOException e) {
            log.error("Could not contact Google reCAPTCHA API.", e);
            return false; // Fail safely in case of network issues
        }
    }
}