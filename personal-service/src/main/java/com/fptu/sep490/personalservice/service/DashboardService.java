package com.fptu.sep490.personalservice.service;

import com.fptu.sep490.personalservice.viewmodel.response.CreatorDefaultDashboard;
import jakarta.servlet.http.HttpServletRequest;

public interface DashboardService {
    CreatorDefaultDashboard getDashboard(HttpServletRequest request);
}
