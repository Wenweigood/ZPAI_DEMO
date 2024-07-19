package com.example.zp_ai_demo.entity;


import jakarta.annotation.Nullable;
import lombok.Data;

import java.util.List;

@Data
public class Content {

    /**
     * 大类
     * text、image、code、tool_calls
     */
    private String type;

    /**
     * 文本
     */
    private String text;

    /**
     * 图片
     */
    private List<Image> image;

    /**
     * 代码段
     */
    private String code;

    /**
     * 工具调用
     */
    private ToolCalls tool_calls;

    /**
     * 执行结果（代码段、工具调用）
     */
    private String content;

    @Nullable
    public String getResult(){
        return switch (type) {
            case "image" -> image.toString();
            case "text" -> text;
            case "code" -> content;
            case "tool_calls" -> content;
            default -> null;
        };
    }

}
