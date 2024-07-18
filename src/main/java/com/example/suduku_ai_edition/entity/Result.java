package com.example.suduku_ai_edition.entity;

import lombok.Data;

import java.util.List;

@Data
public class Result {
    private String conversation_id;

    private String history_id;

    private String status;

    private List<Output> output;
}
