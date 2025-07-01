package com.fptu.sep490.listeningservice.service;

import com.fptu.sep490.listeningservice.viewmodel.request.ListeningTaskCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.ListeningTaskCreationResponse;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;

public interface ListeningTaskService {
    ListeningTaskCreationResponse createListeningTask(ListeningTaskCreationRequest request,
                                                      HttpServletRequest httpServletRequest) throws IOException;
}
