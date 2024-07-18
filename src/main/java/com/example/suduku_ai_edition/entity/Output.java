package com.example.suduku_ai_edition.entity;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class Output {

    private List<Content> content;

    private Date create_at;

    private String id;

    private String recipient;

    private String status;
}
