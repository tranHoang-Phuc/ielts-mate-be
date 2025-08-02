package com.fptu.sep490.commonlibrary.viewmodel.response.feign;

import lombok.Data;

import java.util.Date;

@Data
public class OverviewProgress {
    private Integer exam;
    private Integer task;
    private Integer totalExams;
    private Integer totalTasks;
    private String timeFrame;
    private Double averageBand;
    private Date lastLearningDate;
}
