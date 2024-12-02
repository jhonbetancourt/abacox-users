package com.infomedia.abacox.users.component.functiontools;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class FunctionCall {
    @NotBlank
    private String service;
    @NotBlank
    private String function;
    @Size(min = 1)
    private Map<String, Object> arguments;
}
