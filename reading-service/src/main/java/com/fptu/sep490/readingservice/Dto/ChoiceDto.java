package com.fptu.sep490.readingservice.Dto;
import lombok.Data;

@Data
public class ChoiceDto {
    private String label;
    private String content;
    private int choice_order;
    private boolean is_correct;
}
