package com.fptu.sep490.commonlibrary.viewmodel.response.feign;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class OverviewProgress {
    private Integer exam;
    private Integer task;
    private Integer totalExams;
    private Integer totalTasks;
    private String timeFrame; //1w, 1m, 3m, 3d, ...
    private Double averageBandInTimeFrame;
    private Integer numberOfExamsInTimeFrame;
    private Integer numberOfTasksInTimeFrame;
    private LocalDateTime lastLearningDate;
}
