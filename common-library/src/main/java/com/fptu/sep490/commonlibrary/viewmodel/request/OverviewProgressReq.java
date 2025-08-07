package com.fptu.sep490.commonlibrary.viewmodel.request;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OverviewProgressReq {
    private String timeFrame; // 1w, 1m, 3m, 3d, ...
}
