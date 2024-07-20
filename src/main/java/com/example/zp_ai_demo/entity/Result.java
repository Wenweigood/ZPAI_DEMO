package com.example.zp_ai_demo.entity;

import lombok.Data;

import java.util.List;

@Data
public class Result {
    private String conversation_id;

    private String history_id;

    private String status;

    private List<Output> output;

    private Message message;
}
