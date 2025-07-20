package com.fptu.sep490.listeningservice.service;

import com.fptu.sep490.listeningservice.viewmodel.request.ListeningTaskCreationRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.CreateExamAttemptResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.ListeningTaskGetAllResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.ListeningTaskGetResponse;
import com.fptu.sep490.listeningservice.viewmodel.response.ListeningTaskResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

public interface ListeningTaskService {
    ListeningTaskResponse createListeningTask(ListeningTaskCreationRequest request,
                                              HttpServletRequest httpServletRequest) throws IOException;

    ListeningTaskResponse updateTask(UUID taskId, Integer status ,Integer ieltsType, Integer partNumber, String instruction,
                                     String title, MultipartFile audioFile, String transcription,
                                     HttpServletRequest httpServletRequest) throws IOException;

    void deleteTask(UUID taskId);

    Page<ListeningTaskGetResponse> getActivatedTask(int page, int size, List<Integer> ieltsTypeList,
                                                    List<Integer> partNumberList, String questionCategory,
                                                    String sortBy, String sortDirection, String title, String createdBy);

    Page<ListeningTaskGetResponse> getListeningTask(int page, int size, List<Integer>status, List<Integer> ieltsType,
                                                    List<Integer> partNumber, String questionCategory,
                                                    String sortBy, String sortDirection,
                                                    String title, String createdBy);

    ListeningTaskGetAllResponse getTaskById(UUID taskId);

    CreateExamAttemptResponse.ListeningExamResponse.ListeningTaskResponse fromListeningTask(String taskId);
}
