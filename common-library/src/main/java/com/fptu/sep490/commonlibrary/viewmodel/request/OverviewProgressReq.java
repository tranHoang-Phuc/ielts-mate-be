package com.fptu.sep490.commonlibrary.viewmodel.request;

import lombok.Data;

@Data
public class OverviewProgressReq {
    private String timeFrame; // 1w, 1m, 3m, 3d, ...
}
