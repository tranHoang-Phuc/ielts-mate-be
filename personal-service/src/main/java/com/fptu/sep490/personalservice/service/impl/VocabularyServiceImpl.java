package com.fptu.sep490.personalservice.service.impl;


import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.personalservice.constants.Constants;
import com.fptu.sep490.personalservice.helper.Helper;
import com.fptu.sep490.personalservice.model.Vocabulary;
import com.fptu.sep490.personalservice.repository.ConfigRepository;
import com.fptu.sep490.personalservice.repository.VocabularyRepository;
import com.fptu.sep490.personalservice.service.VocabularyService;
import com.fptu.sep490.personalservice.viewmodel.request.VocabularyRequest;
import com.fptu.sep490.personalservice.viewmodel.response.VocabularyResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;


import java.util.List;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PACKAGE, makeFinal = true)
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class VocabularyServiceImpl  implements VocabularyService {
    ConfigRepository configRepository;
    VocabularyRepository vocabularyRepository;
    Helper helper;
    AIServiceImpl aiServiceImpl;


    @Override
    public VocabularyResponse createVocabulary(VocabularyRequest vocabularyRequest, HttpServletRequest httpServletRequest) throws Exception {
        String userId = helper.getUserIdFromToken(httpServletRequest);
        if (userId == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value()
            );
        }

        if (vocabularyRequest.word() == null || vocabularyRequest.word().isBlank()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        Vocabulary existingVocabulary = vocabularyRepository.findByWordAndContextAndCreatedBy(vocabularyRequest.word(), vocabularyRequest.context(), userId);
        if (existingVocabulary != null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.VOCABULARY_ALREADY_EXISTS,
                    Constants.ErrorCode.VOCABULARY_ALREADY_EXISTS,
                    HttpStatus.CONFLICT.value()
            );
        }
        Vocabulary newVocabulary = new Vocabulary();
        newVocabulary.setWord(vocabularyRequest.word());
        newVocabulary.setContext(vocabularyRequest.context());
        newVocabulary.setIsPublic(vocabularyRequest.isPublic());
        newVocabulary.setCreatedBy(userId);


        // Auto-fill meaning using AI API if not provided
        String meaning = vocabularyRequest.meaning();
        if (meaning == null || meaning.isBlank()) {
            // Replace with your actual AI API call

            meaning = aiServiceImpl.getVocabularyDefinition(vocabularyRequest.word(), vocabularyRequest.context(), vocabularyRequest.language());
            if(vocabularyRequest.context() == null || vocabularyRequest.context().isBlank()){
                String contextVocab = aiServiceImpl.generateSemanticContextFromMeaning(vocabularyRequest.word(), meaning);
                if(vocabularyRequest.context() == null || vocabularyRequest.context().isBlank()) {
                    newVocabulary.setContext(contextVocab);
                }
            }
        }
        newVocabulary.setMeaning(meaning);

        vocabularyRepository.save(newVocabulary);

        return VocabularyResponse.builder()
                .vocabularyId(newVocabulary.getWordId())
                .word(newVocabulary.getWord())
                .context(newVocabulary.getContext())
                .meaning(newVocabulary.getMeaning())
                .createdBy(newVocabulary.getCreatedBy())
                .createdAt(newVocabulary.getCreatedAt())
                .build();
    }

    @Override
    public VocabularyResponse getVocabularyById(String vocabularyId, HttpServletRequest request) throws Exception {
        String UserId = helper.getUserIdFromToken(request);
        if (UserId == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value()
            );
        }
        Vocabulary vocabulary = vocabularyRepository.findById(UUID.fromString(vocabularyId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        if (vocabulary.getIsDeleted()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.NOT_FOUND,
                    Constants.ErrorCodeMessage.NOT_FOUND,
                    HttpStatus.GONE.value()
            );
        }

        return VocabularyResponse.builder()
                .vocabularyId(vocabulary.getWordId())
                .word(vocabulary.getWord())
                .context(vocabulary.getContext())
                .meaning(vocabulary.getMeaning())
                .createdBy(vocabulary.getCreatedBy())
                .createdAt(vocabulary.getCreatedAt())
                .build();
    }

    @Override
    public void deleteVocabularyById(String vocabularyId, HttpServletRequest request) throws Exception {
        String UserId = helper.getUserIdFromToken(request);
        if (UserId == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value()
            );
        }

        Vocabulary vocabulary = vocabularyRepository.findById(UUID.fromString(vocabularyId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        vocabulary.setIsDeleted(true);
        vocabularyRepository.save(vocabulary); // Đừng quên lưu lại thay đổi!

    }

    @Override
    public Page<VocabularyResponse> getAllVocabulary(HttpServletRequest request, int page, int size, String sortBy, String sortDirection, String keyword) throws Exception {
        String UserId = helper.getUserIdFromToken(request);
        if (UserId == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value()
            );
        }
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

        Page<Vocabulary> vocabularyPage;
        try {

            vocabularyPage = vocabularyRepository.searchVocabulary(keyword, pageable, UserId);

        } catch (Exception e) {
            log.error("Database error when fetching exams for user: {}", UserId, e);
            throw new AppException(
                    Constants.ErrorCodeMessage.INTERNAL_SERVER_ERROR,
                    Constants.ErrorCode.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }
        List<VocabularyResponse> vocabularyResponses = vocabularyPage.stream()
                .map(vocabulary -> VocabularyResponse.builder()
                        .vocabularyId(vocabulary.getWordId())
                        .word(vocabulary.getWord())
                        .context(vocabulary.getContext())
                        .meaning(vocabulary.getMeaning())
                        .createdBy(vocabulary.getCreatedBy())
                        .createdAt(vocabulary.getCreatedAt())
                        .build())
                .toList();


        return new PageImpl<>(vocabularyResponses, pageable, vocabularyPage.getTotalElements());





    }

    @Override
    public VocabularyResponse updateVocabulary(String vocabularyId, VocabularyRequest vocabularyRequest, HttpServletRequest request) throws Exception {
        String userId = helper.getUserIdFromToken(request);
        if (userId == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value()
            );
        }
        Vocabulary vocabulary = vocabularyRepository.findById(UUID.fromString(vocabularyId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        if (!vocabulary.getCreatedBy().equals(userId)) {
            throw new AppException(
                    Constants.ErrorCodeMessage.FORBIDDEN,
                    Constants.ErrorCodeMessage.FORBIDDEN,
                    HttpStatus.FORBIDDEN.value()
            );
        }
        if (vocabularyRequest.word() == null || vocabularyRequest.word().isBlank()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    Constants.ErrorCode.INVALID_REQUEST,
                    HttpStatus.BAD_REQUEST.value()
            );
        }
        Vocabulary existingVocabulary = vocabularyRepository.findByWordAndCreatedBy(vocabularyRequest.word(), userId);
        if (existingVocabulary != null && !existingVocabulary.getWordId().equals(vocabulary.getWordId())) {
            throw new AppException(
                    Constants.ErrorCodeMessage.VOCABULARY_ALREADY_EXISTS,
                    Constants.ErrorCode.VOCABULARY_ALREADY_EXISTS,
                    HttpStatus.CONFLICT.value()
            );
        }
        vocabulary.setWord(vocabularyRequest.word());
        vocabulary.setContext(vocabularyRequest.context());
        vocabulary.setMeaning(vocabularyRequest.meaning());
        vocabulary.setIsPublic(vocabularyRequest.isPublic());
        vocabulary.setUpdatedBy(userId);
        vocabularyRepository.save(vocabulary);
        return VocabularyResponse.builder()
                .vocabularyId(vocabulary.getWordId())
                .word(vocabulary.getWord())
                .context(vocabulary.getContext())
                .meaning(vocabulary.getMeaning())
                .createdBy(vocabulary.getCreatedBy())
                .createdAt(vocabulary.getCreatedAt())
                .build();


    }


}
