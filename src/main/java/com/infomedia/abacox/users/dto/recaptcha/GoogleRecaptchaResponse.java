package com.infomedia.abacox.users.dto.recaptcha;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

// Using a record for an immutable data carrier
// @JsonIgnoreProperties is important to prevent errors if Google adds new fields
@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleRecaptchaResponse(
    boolean success,
    String hostname,
    @JsonProperty("challenge_ts") String challengeTs,
    @JsonProperty("error-codes") List<String> errorCodes
) {
}