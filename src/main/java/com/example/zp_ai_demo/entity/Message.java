package com.example.zp_ai_demo.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
public class Message {

    private String id;

    private String status;

    private Content content;
}
