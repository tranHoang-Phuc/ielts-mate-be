package com.fptu.sep490.personalservice.service;

import com.fptu.sep490.commonlibrary.viewmodel.request.OverviewProgressReq;
import com.fptu.sep490.personalservice.viewmodel.response.OverviewProgressResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface ProgressService {
    OverviewProgressResponse getOverviewProgress(
            OverviewProgressReq overviewProgressReq,
            HttpServletRequest request
    );
}
