package com.fptu.sep490.personalservice.service;

import com.fptu.sep490.personalservice.viewmodel.request.ModuleRequest;
import com.fptu.sep490.personalservice.viewmodel.response.ModuleResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;

import java.net.http.HttpRequest;

public interface ModuleService {
    ModuleResponse createModule(ModuleRequest moduleRequest, HttpServletRequest request) throws Exception;

    Page<ModuleResponse> getAllModules(HttpServletRequest request, int i, int size, String sortBy, String sortDirection, String keyword) throws  Exception;
}
