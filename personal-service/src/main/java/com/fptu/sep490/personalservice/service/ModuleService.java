package com.fptu.sep490.personalservice.service;

import com.fptu.sep490.personalservice.viewmodel.request.ModuleRequest;
import com.fptu.sep490.personalservice.viewmodel.response.ModuleResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;

import java.net.http.HttpRequest;

public interface ModuleService {
    ModuleResponse createModule(ModuleRequest moduleRequest, HttpServletRequest request) throws Exception;

    Page<ModuleResponse> getAllModules(HttpServletRequest request, int i, int size, String sortBy, String sortDirection, String keyword) throws  Exception;

    Page<ModuleResponse> getAllPublicModules(int page, int size, String sortBy, String sortDirection, String keyword, HttpServletRequest httpServletRequest) throws Exception;

    void deleteModuleById(String moduleId, HttpServletRequest request) throws Exception;

    ModuleResponse getModuleById(String moduleId, HttpServletRequest request) throws Exception;

    ModuleResponse updateModule(String moduleId, @Valid ModuleRequest moduleRequest, HttpServletRequest request)  throws Exception;
}
