package com.fptu.sep490.personalservice.viewmodel.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public record CreatorDefaultDashboard(
        @JsonProperty("number_of_reading_tasks")
        int numberOfReadingTasks,

        @JsonProperty("number_of_listening_tasks")
        int numberOfListeningTasks,

        @JsonProperty("number_of_reading_exams")
        int numberOfReadingExams,

        @JsonProperty("number_of_listening_exams")
        int numberOfListeningExams


) {
}
