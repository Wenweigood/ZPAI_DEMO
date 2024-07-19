package com.example.zp_ai_demo.entity;

import lombok.Data;

@Data
public class ZPAIResponse {

    private String message;

    private Integer status;

    private Result result;
}
