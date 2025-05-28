package com.fptu.sep490.readingservice.model.enumeration;

public enum QuestionCategory {
    SHORT_ANSWER("Short Answer Questions"),
    CHOICE("Choice"),
    MATCHING("Matching"),
    MAP("Map"),
    IDENTIFY("Identify"),
    COMPLETION("Completion"),
    BAR_CHART("Bar Chart"),
    PIE_CHART("Pie Chart"),
    TABLE("Table"),
    MULTIPLE("Multiple"),
    OPTION("Option"),
    ADVANTAGE_DISADVANTAGE("Advantage&Disadvantage"),
    DISCUSSION("Discussion"),
    MULTIPLE_CHOICE("Multiple Choice"),
    COMPLAINT("Complaint"),
    INQUIRY("Inquiry"),
    REQUEST_FOR_ASSISTANCE("Request for Assistance"),
    APOLOGY("Apology"),
    SUGGESTION("Suggestion"),
    INVITATION("Invitation"),
    GRATITUDE("Gratitude"),
    JOB_APPLICATION("Job Application"),
    ANALYSIS_AND_RECOMMENDATION("Analysis and Recommendation"),
    PERSPECTIVES_AND_REASONS("Perspectives and Reasons"),
    AGREEMENT_AND_DISAGREEMENT("Agreement and Disagreement");

    private final String description;
    QuestionCategory(String description) {
        this.description = description;
    }
    public String getDescription() {
        return description;
    }
}
