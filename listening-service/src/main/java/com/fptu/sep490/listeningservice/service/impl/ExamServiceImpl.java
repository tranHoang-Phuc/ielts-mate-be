package com.fptu.sep490.listeningservice.service.impl;

import com.fptu.sep490.commonlibrary.constants.CookieConstants;
import com.fptu.sep490.commonlibrary.constants.DataMarkup;
import com.fptu.sep490.commonlibrary.constants.ErrorCodeMessage;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.utils.CookieUtils;
import com.fptu.sep490.listeningservice.constants.Constants;
import com.fptu.sep490.listeningservice.helper.Helper;
import com.fptu.sep490.listeningservice.model.ListeningExam;
import com.fptu.sep490.listeningservice.model.ListeningTask;
import com.fptu.sep490.listeningservice.model.enumeration.ExamStatus;
import com.fptu.sep490.listeningservice.model.enumeration.PartNumber;
import com.fptu.sep490.listeningservice.repository.*;
import com.fptu.sep490.listeningservice.repository.client.MarkupClient;
import com.fptu.sep490.listeningservice.service.ExamService;
import com.fptu.sep490.listeningservice.viewmodel.request.ExamRequest;
import com.fptu.sep490.listeningservice.viewmodel.response.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;


@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    //CreateExam logic goes here
    ListeningTaskRepository listeningTaskRepository;
    QuestionGroupRepository questionGroupRepository;
    DragItemRepository dragItemRepository;
    QuestionRepository questionRepository;
    ChoiceRepository choiceRepository;
    ListeningExamRepository listeningExamRepository;
    MarkupClient markupClient;

    Helper helper;


    @Override
    public ExamResponse createExam(ExamRequest request, HttpServletRequest httpServletRequest) throws Exception {
        String userId = helper.getUserIdFromToken(httpServletRequest);
        String incoming = request.urlSlug();
        String urlSlug;

        if (incoming == null || incoming.isBlank()) {
            urlSlug = genUrlSlug(request.examName()).urlSlug();
        } else if (checkUrlSlug(incoming).isValid()) {
            urlSlug = incoming;
        } else {
            throw new AppException(
                    Constants.ErrorCodeMessage.EXISTED_SLUG,
                    Constants.ErrorCode.EXISTED_SLUG,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        if (userId == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    Constants.ErrorCode.UNAUTHORIZED,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        ListeningTask part1 = listeningTaskRepository.findById(request.part1Id())
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        if (findCurrentOrChildCurrentTask(part1).getPartNumber() != PartNumber.PART_1) {
            throw new AppException(
                    Constants.ErrorCodeMessage.WRONG_PART,
                    Constants.ErrorCode.WRONG_PART,
                    HttpStatus.BAD_REQUEST.value()
            );
        }


        ListeningTask part2 = listeningTaskRepository.findById(request.part2Id())
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        if (findCurrentOrChildCurrentTask(part2).getPartNumber() != PartNumber.PART_2) {
            throw new AppException(
                    Constants.ErrorCodeMessage.WRONG_PART,
                    Constants.ErrorCode.WRONG_PART,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        ListeningTask part3 = listeningTaskRepository.findById(request.part3Id())
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        if (findCurrentOrChildCurrentTask(part3).getPartNumber() != PartNumber.PART_3) {
            throw new AppException(
                    Constants.ErrorCodeMessage.WRONG_PART,
                    Constants.ErrorCode.WRONG_PART,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        ListeningTask part4 = listeningTaskRepository.findById(request.part4Id())
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        if (findCurrentOrChildCurrentTask(part4).getPartNumber() != PartNumber.PART_4) {
            throw new AppException(
                    Constants.ErrorCodeMessage.WRONG_PART,
                    Constants.ErrorCode.WRONG_PART,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        ListeningExam listeningExam = ListeningExam.builder()
                .examName(request.examName())
                .examDescription(request.examDescription())
                .urlSlug(urlSlug)
                .isCurrent(true)
                .isDeleted(false)
                .isOriginal(true)
                .status(ExamStatus.ACTIVE)
                .version(1)
                .part1(part1)
                .part2(part2)
                .part3(part3)
                .part4(part4)
                .createdBy(userId)
                .updatedBy(userId)
                .build();

        ListeningExam savedExam = listeningExamRepository.save(listeningExam);


        ListeningTask part1Current = findCurrentOrChildCurrentTask(part1);

        ListeningTask part2Current = findCurrentOrChildCurrentTask(part2);
        ListeningTask part3Current = findCurrentOrChildCurrentTask(part3);
        ListeningTask part4Current = findCurrentOrChildCurrentTask(part4);
        ExamResponse response = new ExamResponse(
                savedExam.getListeningExamId(),
                savedExam.getExamName(),
                savedExam.getExamDescription(),
                savedExam.getUrlSlug(),

                ListeningTaskResponse.builder()
                        .taskId(part1.getTaskId())
                        .ieltsType(part1Current.getIeltsType().ordinal())
                        .partNumber(part1Current.getPartNumber().ordinal())
                        .instruction(part1Current.getInstruction())
                        .title(part1Current.getTitle())
                        .audioFileId(part1Current.getAudioFileId())
                        .transcription(part1Current.getTranscription())
                        .build(),

                ListeningTaskResponse.builder()
                        .taskId(part2.getTaskId())
                        .ieltsType(part2Current.getIeltsType().ordinal())
                        .partNumber(part2Current.getPartNumber().ordinal())
                        .instruction(part2Current.getInstruction())
                        .title(part2Current.getTitle())
                        .audioFileId(part2Current.getAudioFileId())
                        .transcription(part2Current.getTranscription())
                        .build(),

                ListeningTaskResponse.builder()
                        .taskId(part3.getTaskId())
                        .ieltsType(part3Current.getIeltsType().ordinal())
                        .partNumber(part3Current.getPartNumber().ordinal())
                        .instruction(part3Current.getInstruction())
                        .title(part3Current.getTitle())
                        .audioFileId(part3Current.getAudioFileId())
                        .transcription(part3Current.getTranscription())
                        .build(),

                ListeningTaskResponse.builder()
                        .taskId(part4.getTaskId())
                        .ieltsType(part4Current.getIeltsType().ordinal())
                        .partNumber(part4Current.getPartNumber().ordinal())
                        .instruction(part4Current.getInstruction())
                        .title(part4Current.getTitle())
                        .audioFileId(part4Current.getAudioFileId())
                        .transcription(part4Current.getTranscription())
                        .build(),

                savedExam.getCreatedBy(),
                savedExam.getCreatedAt(),
                savedExam.getUpdatedBy(),
                savedExam.getUpdatedAt(),
                savedExam.getIsCurrent(),
                savedExam.getVersion(),
                savedExam.getIsOriginal(),
                savedExam.getIsDeleted(),
                null,
                null
        );
        return response;
    }

    @Override
    public ExamResponse getExamById(String examId, HttpServletRequest httpServletRequest) throws Exception {
        String userId = helper.getUserIdFromToken(httpServletRequest);
        if (userId == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    Constants.ErrorCode.UNAUTHORIZED,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        ListeningExam listeningExam = listeningExamRepository.findById(UUID.fromString(examId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        ListeningExam currentExam = findCurrentOrChildCurrentExam(listeningExam);

        ListeningTask part1Current = findCurrentOrChildCurrentTask(currentExam.getPart1());
        ListeningTask part2Current = findCurrentOrChildCurrentTask(currentExam.getPart2());
        ListeningTask part3Current = findCurrentOrChildCurrentTask(currentExam.getPart3());
        ListeningTask part4Current = findCurrentOrChildCurrentTask(currentExam.getPart4());

        ExamResponse response = new ExamResponse(
                currentExam.getListeningExamId(),
                currentExam.getExamName(),
                currentExam.getExamDescription(),
                currentExam.getUrlSlug(),

                ListeningTaskResponse.builder()
                        .taskId(currentExam.getPart1().getTaskId())
                        .ieltsType(part1Current.getIeltsType().ordinal())
                        .partNumber(part1Current.getPartNumber().ordinal())
                        .instruction(part1Current.getInstruction())
                        .title(part1Current.getTitle())
                        .audioFileId(part1Current.getAudioFileId())
                        .transcription(part1Current.getTranscription())
                        .build(),

                ListeningTaskResponse.builder()
                        .taskId(currentExam.getPart2().getTaskId())
                        .ieltsType(part2Current.getIeltsType().ordinal())
                        .partNumber(part2Current.getPartNumber().ordinal())
                        .instruction(part2Current.getInstruction())
                        .title(part2Current.getTitle())
                        .audioFileId(part2Current.getAudioFileId())
                        .transcription(part2Current.getTranscription())
                        .build(),

                ListeningTaskResponse.builder()
                        .taskId(currentExam.getPart3().getTaskId())
                        .ieltsType(part3Current.getIeltsType().ordinal())
                        .partNumber(part3Current.getPartNumber().ordinal())
                        .instruction(part3Current.getInstruction())
                        .title(part3Current.getTitle())
                        .audioFileId(part3Current.getAudioFileId())
                        .transcription(part3Current.getTranscription())
                        .build(),

                ListeningTaskResponse.builder()
                        .taskId(currentExam.getPart4().getTaskId())
                        .ieltsType(part4Current.getIeltsType().ordinal())
                        .partNumber(part4Current.getPartNumber().ordinal())
                        .instruction(part4Current.getInstruction())
                        .title(part4Current.getTitle())
                        .audioFileId(part4Current.getAudioFileId())
                        .transcription(part4Current.getTranscription())
                        .build(),

                currentExam.getCreatedBy(),
                currentExam.getCreatedAt(),
                currentExam.getUpdatedBy(),
                currentExam.getUpdatedAt(),
                currentExam.getIsCurrent(),
                currentExam.getVersion(),
                currentExam.getIsOriginal(),
                currentExam.getIsDeleted(),
                null,
                null
        );
        return response;
    }

    @Override
    public void deleteExam(String examId, HttpServletRequest httpServletRequest) throws Exception {
        ListeningExam exam = listeningExamRepository.findById(UUID.fromString(examId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        List<ListeningExam> list = listeningExamRepository.findAllCurrentByParentId(UUID.fromString(examId));
        for (ListeningExam item:list){
            item.setIsDeleted(true);
            listeningExamRepository.save(item);
        }
        exam.setIsDeleted(true);
        listeningExamRepository.save(exam);


    }

    @Override
    public ExamResponse updateExam(String examId, ExamRequest request, HttpServletRequest httpServletRequest) throws Exception {
        String User_Id = helper.getUserIdFromToken(httpServletRequest);
        if (User_Id == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    Constants.ErrorCode.UNAUTHORIZED,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        ListeningExam listeningExam = listeningExamRepository.findById(UUID.fromString(examId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        if(listeningExam.getIsDeleted()){
            throw new AppException(
                    Constants.ErrorCodeMessage.EXAM_DELETED,
                    Constants.ErrorCode.EXAM_DELETED,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        if(request.status() == 1){
            listeningExam.setStatus(ExamStatus.ACTIVE);
        }else if (request.status() == 0) {
            listeningExam.setStatus(ExamStatus.INACTIVE);
        }
        ListeningExam currentExam = findCurrentOrChildCurrentExam(listeningExam);
        if (currentExam == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.NOT_FOUND,
                    Constants.ErrorCode.NOT_FOUND,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        currentExam.setIsCurrent(false);

        ListeningExam newExam = new ListeningExam();
        newExam.setExamName(request.examName() != null ? request.examName() : currentExam.getExamName());
        newExam.setExamDescription(request.examDescription() != null ? request.examDescription(): currentExam.getExamDescription());
        newExam.setUrlSlug(request.urlSlug() != null ? request.urlSlug(): currentExam.getUrlSlug());
        newExam.setStatus(currentExam.getStatus());
        if(request.part2Id()!= null){
            ListeningTask part1 = listeningTaskRepository.findById(request.part1Id())
                    .orElseThrow(() -> new AppException(
                            Constants.ErrorCodeMessage.NOT_FOUND,
                            Constants.ErrorCode.NOT_FOUND,
                            HttpStatus.NOT_FOUND.value()
                    ));
            if (part1.getPartNumber() != PartNumber.PART_1) {
                throw new AppException(
                        Constants.ErrorCodeMessage.WRONG_PART,
                        Constants.ErrorCode.WRONG_PART,
                        HttpStatus.BAD_REQUEST.value()
                );
            }
            if (part1.getIsDeleted()){
                throw new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.BAD_REQUEST.value()
                );
            }
            newExam.setPart1(part1);
        }else{
            newExam.setPart1(currentExam.getPart1());
        }
        if(request.part2Id()!= null) {
            ListeningTask part2 = listeningTaskRepository.findById(request.part2Id())
                    .orElseThrow(() -> new AppException(
                            Constants.ErrorCodeMessage.NOT_FOUND,
                            Constants.ErrorCode.NOT_FOUND,
                            HttpStatus.NOT_FOUND.value()
                    ));
            if (part2.getPartNumber() != PartNumber.PART_2) {
                throw new AppException(
                        Constants.ErrorCodeMessage.WRONG_PART,
                        Constants.ErrorCode.WRONG_PART,
                        HttpStatus.BAD_REQUEST.value()
                );
            }
            if (part2.getIsDeleted()){
                throw new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.BAD_REQUEST.value()
                );
            }
            newExam.setPart2(part2);
        }else {
            newExam.setPart2(currentExam.getPart2());
        }
        if(request.part3Id()!= null) {
            ListeningTask part3 = listeningTaskRepository.findById(request.part3Id())
                    .orElseThrow(() -> new AppException(
                            Constants.ErrorCodeMessage.NOT_FOUND,
                            Constants.ErrorCode.NOT_FOUND,
                            HttpStatus.NOT_FOUND.value()
                    ));
            if (part3.getPartNumber() != PartNumber.PART_3) {
                throw new AppException(
                        Constants.ErrorCodeMessage.WRONG_PART,
                        Constants.ErrorCode.WRONG_PART,
                        HttpStatus.BAD_REQUEST.value()
                );
            }
            if (part3.getIsDeleted()){
                throw new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.BAD_REQUEST.value()
                );
            }
            newExam.setPart3(part3);
        }else{
            newExam.setPart3(currentExam.getPart3());
        }
        if(request.part4Id()!= null) {
            ListeningTask part4 = listeningTaskRepository.findById(request.part4Id())
                    .orElseThrow(() -> new AppException(
                            Constants.ErrorCodeMessage.NOT_FOUND,
                            Constants.ErrorCode.NOT_FOUND,
                            HttpStatus.NOT_FOUND.value()
                    ));
            if (part4.getPartNumber() != PartNumber.PART_4) {
                throw new AppException(
                        Constants.ErrorCodeMessage.WRONG_PART,
                        Constants.ErrorCode.WRONG_PART,
                        HttpStatus.BAD_REQUEST.value()
                );
            }
            if (part4.getIsDeleted()){
                throw new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.BAD_REQUEST.value()
                );
            }
            newExam.setPart4(part4);
        }else{
            newExam.setPart4(currentExam.getPart4());
        }


        newExam.setCreatedBy(User_Id);
        newExam.setUpdatedBy(User_Id);
        newExam.setIsCurrent(true);
        newExam.setVersion(currentExam.getVersion()+1);
        newExam.setParent(listeningExam);
        newExam.setIsOriginal(false);

        listeningExamRepository.save(listeningExam);
        listeningExamRepository.save(currentExam);
        ListeningExam newSave = listeningExamRepository.save(newExam);

        return new ExamResponse(
                newSave.getListeningExamId(),
                newSave.getExamName(),
                newSave.getExamDescription(),
                newSave.getUrlSlug(),

                ListeningTaskResponse.builder()
                        .taskId(newSave.getPart1().getTaskId())
                        .ieltsType(newSave.getPart1().getIeltsType().ordinal())
                        .partNumber(newSave.getPart1().getPartNumber().ordinal())
                        .instruction(newSave.getPart1().getInstruction())
                        .title(newSave.getPart1().getTitle())
                        .audioFileId(newSave.getPart1().getAudioFileId())
                        .transcription(newSave.getPart1().getTranscription())
                        .build(),

                ListeningTaskResponse.builder()
                        .taskId(newSave.getPart2().getTaskId())
                        .ieltsType(newSave.getPart2().getIeltsType().ordinal())
                        .partNumber(newSave.getPart2().getPartNumber().ordinal())
                        .instruction(newSave.getPart2().getInstruction())
                        .title(newSave.getPart2().getTitle())
                        .audioFileId(newSave.getPart2().getAudioFileId())
                        .transcription(newSave.getPart2().getTranscription())
                        .build(),

                ListeningTaskResponse.builder()
                        .taskId(newSave.getPart3().getTaskId())
                        .ieltsType(newSave.getPart3().getIeltsType().ordinal())
                        .partNumber(newSave.getPart3().getPartNumber().ordinal())
                        .instruction(newSave.getPart3().getInstruction())
                        .title(newSave.getPart3().getTitle())
                        .audioFileId(newSave.getPart3().getAudioFileId())
                        .transcription(newSave.getPart3().getTranscription())
                        .build(),

                ListeningTaskResponse.builder()
                        .taskId(newSave.getPart4().getTaskId())
                        .ieltsType(newSave.getPart4().getIeltsType().ordinal())
                        .partNumber(newSave.getPart4().getPartNumber().ordinal())
                        .instruction(newSave.getPart4().getInstruction())
                        .title(newSave.getPart4().getTitle())
                        .audioFileId(newSave.getPart4().getAudioFileId())
                        .transcription(newSave.getPart4().getTranscription())
                        .build(),

                newSave.getCreatedBy(),
                newSave.getCreatedAt(),
                newSave.getUpdatedBy(),
                newSave.getUpdatedAt(),
                newSave.getIsCurrent(),
                newSave.getVersion(),
                newSave.getIsOriginal(),
                newSave.getIsDeleted(),
                null,
                null
        );

    }

    @Override
    public Page<ExamResponse> getAllExamsForCreator(HttpServletRequest httpServletRequest, int page, int size, String sortBy, String sortDirection, String keyword) throws Exception {
        String userId = helper.getUserIdFromToken(httpServletRequest);

        if (userId == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    Constants.ErrorCode.UNAUTHORIZED,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        // Validate sort field fallback
        if (sortBy == null || sortBy.isBlank()) sortBy = "createdAt";

        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ListeningExam> exams = listeningExamRepository.searchCurrentExamsByCreator(userId, keyword, pageable);
        return exams.map(this::mapToExamResponse);
    }

    @Override
    public Page<ExamResponse> getActiveExams(int page, int size, String sortBy, String sortDirection, HttpServletRequest httpServletRequest, String keyword) throws Exception {
        String userId = helper.getUserIdFromToken(httpServletRequest);

        if (userId == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    Constants.ErrorCode.UNAUTHORIZED,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        // Validate sort field fallback
        if (sortBy == null || sortBy.isBlank()) {
            sortBy = "createdAt";
        }

        if (sortDirection == null || sortDirection.isBlank()) {
            sortDirection = "asc";
        }
        Sort sort = sortDirection.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<ListeningExam> exams = listeningExamRepository.searchCurrentExamsActivated(userId, keyword, pageable);
        Map<UUID, Integer> taskIdsMarkup;
        String accessToken = CookieUtils.getCookieValue(httpServletRequest, CookieConstants.ACCESS_TOKEN);
        if(accessToken != null) {
            var response = markupClient.getMarkedUpData("Bearer " + accessToken, DataMarkup.LISTENING_EXAM);
            if(response.getStatusCode() == HttpStatus.OK) {
                var body = response.getBody();
                if (body != null) {
                    taskIdsMarkup = body.data().markedUpIdsMapping();
                    return exams.map(e -> mapToExamResponse(e, taskIdsMarkup));

                }
            }
        }
        return exams.map(this::mapToExamResponse);
    }

    @Override
    public List<TaskTitle> getExamTitle(List<UUID> ids) {
        List<ListeningExam> exams = listeningExamRepository.findAllById(ids);
        return exams.stream().map(e -> TaskTitle.builder().taskId(e.getListeningExamId()).title(e.getExamName()).build()).toList();
    }

    @Override
    public SlugStatusResponse checkUrlSlug(String urlSlug) {
        boolean isValidSlug = !listeningExamRepository.existsByUrlSlug(urlSlug);

        if(!isValidSlug) {
            throw new AppException(Constants.ErrorCodeMessage.EXISTED_SLUG,
                    Constants.ErrorCode.EXISTED_SLUG, HttpStatus.BAD_REQUEST.value());
        }

        return SlugStatusResponse.builder()
                .isValid(isValidSlug)
                .build();
    }

    @Override
    public SlugGenResponse genUrlSlug(String examName) {
        String slug = examName == null ? "" : examName.trim().toLowerCase(Locale.ROOT);

        slug = Normalizer.normalize(slug, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "");
        slug = slug.replace('đ', 'd').replace('Đ', 'd');

        slug = slug.replaceAll("[^a-z0-9\\s-]", " ");
        slug = slug.trim().replaceAll("\\s+", "-");
        slug = slug.replaceAll("^-+|-+$", "");
        if (slug.isEmpty()) slug = "exam";

        String utcStamp = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());

        String urlSlug = slug + "-" + utcStamp;

        return SlugGenResponse.builder()
                .urlSlug(urlSlug)
                .build();
    }

    private ListeningTask findCurrentOrChildCurrentTask(ListeningTask listeningTask) {
        if (listeningTask.getIsCurrent() && !listeningTask.getIsDeleted()) {
            return listeningTask;
        }
        for (ListeningTask child : listeningTask.getChildren()) {
            if (child.getIsCurrent() && !child.getIsDeleted()) {
                return child;
            }
        }
        return listeningTask;
    }


    public ListeningExam findCurrentOrChildCurrentExam(ListeningExam listeningExam) {
        if (listeningExam.getIsCurrent() && !listeningExam.getIsDeleted()) {
            return listeningExam;
        }
        for(ListeningExam child : listeningExam.getChildren()) {
            if(child.getIsCurrent() && !child.getIsDeleted()) {
                return child;
            }
        }
        return null;
    }
    public ExamResponse mapToExamResponse(ListeningExam exam, Map<UUID, Integer> markedUpIdsMapping) {

        ListeningTask currentPart1 = findCurrentOrChildCurrentTask(exam.getPart1());
        ListeningTask currentPart2 = findCurrentOrChildCurrentTask(exam.getPart2());
        ListeningTask currentPart3 = findCurrentOrChildCurrentTask(exam.getPart3());
        ListeningTask currentPart4 = findCurrentOrChildCurrentTask(exam.getPart4());
        return new ExamResponse(
                exam.getListeningExamId(),
                exam.getExamName(),
                exam.getExamDescription(),
                exam.getUrlSlug(),

                ListeningTaskResponse.builder()
                        .taskId(exam.getPart1().getTaskId())
                        .ieltsType(currentPart1.getIeltsType().ordinal())
                        .partNumber(currentPart1.getPartNumber().ordinal())
                        .instruction(currentPart1.getInstruction())
                        .title(currentPart1.getTitle())

                        .build(),

                ListeningTaskResponse.builder()
                        .taskId(exam.getPart2().getTaskId())
                        .ieltsType(currentPart2.getIeltsType().ordinal())
                        .partNumber(currentPart2.getPartNumber().ordinal())
                        .instruction(currentPart2.getInstruction())
                        .title(currentPart2.getTitle())

                        .build(),

                ListeningTaskResponse.builder()
                        .taskId(exam.getPart3().getTaskId())
                        .ieltsType(currentPart3.getIeltsType().ordinal())
                        .partNumber(currentPart3.getPartNumber().ordinal())
                        .instruction(currentPart3.getInstruction())
                        .title(currentPart3.getTitle())

                        .build(),

                ListeningTaskResponse.builder()
                        .taskId(exam.getPart4().getTaskId())
                        .ieltsType(currentPart4.getIeltsType().ordinal())
                        .partNumber(currentPart4.getPartNumber().ordinal())
                        .instruction(currentPart4.getInstruction())
                        .title(currentPart4.getTitle())

                        .build(),

                exam.getCreatedBy(),
                exam.getCreatedAt(),
                exam.getUpdatedBy(),
                exam.getUpdatedAt(),
                exam.getIsCurrent(),
                exam.getVersion(),
                exam.getIsOriginal(),
                exam.getIsDeleted(),
                markedUpIdsMapping.get(exam.getListeningExamId()) != null,
                markedUpIdsMapping.get(exam.getListeningExamId())
        );
    }

    private ExamResponse mapToExamResponse(ListeningExam exam) {

        ListeningTask currentPart1 = findCurrentOrChildCurrentTask(exam.getPart1());
        ListeningTask currentPart2 = findCurrentOrChildCurrentTask(exam.getPart2());
        ListeningTask currentPart3 = findCurrentOrChildCurrentTask(exam.getPart3());
        ListeningTask currentPart4 = findCurrentOrChildCurrentTask(exam.getPart4());
        return new ExamResponse(
                exam.getListeningExamId(),
                exam.getExamName(),
                exam.getExamDescription(),
                exam.getUrlSlug(),

                ListeningTaskResponse.builder()
                        .taskId(exam.getPart1().getTaskId())
                        .ieltsType(currentPart1.getIeltsType().ordinal())
                        .partNumber(currentPart1.getPartNumber().ordinal())
                        .instruction(currentPart1.getInstruction())
                        .title(currentPart1.getTitle())
                        .build(),

                ListeningTaskResponse.builder()
                        .taskId(exam.getPart2().getTaskId())
                        .ieltsType(currentPart2.getIeltsType().ordinal())
                        .partNumber(currentPart2.getPartNumber().ordinal())
                        .instruction(currentPart2.getInstruction())
                        .title(currentPart2.getTitle())
                        .build(),

                ListeningTaskResponse.builder()
                        .taskId(exam.getPart3().getTaskId())
                        .ieltsType(currentPart3.getIeltsType().ordinal())
                        .partNumber(currentPart3.getPartNumber().ordinal())
                        .instruction(currentPart3.getInstruction())
                        .title(currentPart3.getTitle())
                        .build(),

                ListeningTaskResponse.builder()
                        .taskId(exam.getPart4().getTaskId())
                        .ieltsType(currentPart4.getIeltsType().ordinal())
                        .partNumber(currentPart4.getPartNumber().ordinal())
                        .instruction(currentPart4.getInstruction())
                        .title(currentPart4.getTitle())
                        .build(),

                exam.getCreatedBy(),
                exam.getCreatedAt(),
                exam.getUpdatedBy(),
                exam.getUpdatedAt(),
                exam.getIsCurrent(),
                exam.getVersion(),
                exam.getIsOriginal(),
                exam.getIsDeleted(),
                null,
                null
        );
    }

}
