package com.example.suduku_ai_edition.entity;

import lombok.Data;

@Data
public class ZPAIResponse {

    private String message;

    private Integer status;

    private Result result;
}
