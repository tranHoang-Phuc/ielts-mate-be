package com.fptu.sep490.personalservice.service.impl;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.personalservice.constants.Constants;
import com.fptu.sep490.personalservice.helper.Helper;
import com.fptu.sep490.personalservice.model.FlashCard;
import com.fptu.sep490.personalservice.model.Vocabulary;
import com.fptu.sep490.personalservice.repository.FlashCardRepository;
import com.fptu.sep490.personalservice.repository.ModuleRepository;
import com.fptu.sep490.personalservice.repository.VocabularyRepository;
import com.fptu.sep490.personalservice.service.ModuleService;
import com.fptu.sep490.personalservice.viewmodel.request.ModuleRequest;
import com.fptu.sep490.personalservice.viewmodel.response.FlashCardResponse;
import com.fptu.sep490.personalservice.viewmodel.response.ModuleResponse;
import com.fptu.sep490.personalservice.model.Module;
import com.fptu.sep490.personalservice.viewmodel.response.VocabularyResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@FieldDefaults(level = AccessLevel.PACKAGE, makeFinal = true)
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class ModuleServiceImpl implements ModuleService {
    FlashCardRepository flashCardRepository;
    ModuleRepository moduleRepository;
    VocabularyRepository vocabularyRepository;
    Helper helper;

    @Override
    public ModuleResponse createModule(ModuleRequest moduleRequest, HttpServletRequest request) throws Exception {
        String userId = helper.getUserIdFromToken(request);
        if (userId == null) {
            throw new Exception("Unauthorized: User ID is null");
        }
        List<UUID> vocabularyIds = moduleRequest.vocabularyIds();

        Module newModule = new Module();
        newModule.setModuleName(moduleRequest.moduleName());
        newModule.setDescription(moduleRequest.moduleDescription());
        for (UUID vocabularyId : vocabularyIds) {
            Optional<Vocabulary> optionalVocabulary = vocabularyRepository.findById(vocabularyId);
            Vocabulary vocabulary = optionalVocabulary.get();

            FlashCard flashCard = new FlashCard();
            flashCard.setVocabulary(vocabulary);
            newModule.getFlashCards().add(flashCard);
            flashCard.getModules().add(newModule);


        }
        newModule.setIsPublic(moduleRequest.isPublic());
        newModule.setCreatedBy(userId);
        moduleRepository.save(newModule);
        flashCardRepository.saveAll(newModule.getFlashCards());

        List<FlashCardResponse> flashCardResponses = newModule.getFlashCards().stream()
                .map(flashCard -> FlashCardResponse.builder()
                        .flashCardId(flashCard.getCardId().toString())
                        .vocabularyResponse(
                                VocabularyResponse.builder()
                                        .vocabularyId(flashCard.getCardId())
                                        .word(flashCard.getVocabulary().getWord())
                                        .context(flashCard.getVocabulary().getContext())
                                        .meaning(flashCard.getVocabulary().getMeaning())
                                        .createdBy(flashCard.getVocabulary().getCreatedBy())
                                        .createdAt(flashCard.getVocabulary().getCreatedAt())
                                        .build()
                        )
                        .build())
                .toList();
        ModuleResponse response = ModuleResponse.builder()
                .moduleId(newModule.getModuleId())
                .moduleName(newModule.getModuleName())
                .description(newModule.getDescription())
                .isPublic(newModule.getIsPublic())
                .flashCardIds(flashCardResponses)
                .createdBy(newModule.getCreatedBy())
                .createdAt(newModule.getCreatedAt())
                .build();



        return response;
    }

    @Override
    public Page<ModuleResponse> getAllModules(HttpServletRequest request, int page, int size, String sortBy, String sortDirection, String keyword) throws Exception {
        String UserId = helper.getUserIdFromToken(request);
        if (UserId == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value()
            );        }
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
        Page<Module> modulePage;
        try {
            modulePage = moduleRepository.searchModule(keyword, pageable, UserId);
        } catch (Exception e) {
            log.error("Database error when fetching modules for user: {}", UserId, e);
            throw new AppException(
                    Constants.ErrorCodeMessage.INTERNAL_SERVER_ERROR,
                    Constants.ErrorCode.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }
        List<ModuleResponse> moduleResponses = modulePage.stream()
                .map(module -> {
                    List<FlashCardResponse> flashCardResponses = new ArrayList<>();
                    for (FlashCard flashCard : module.getFlashCards()) {
                        flashCardResponses.add(FlashCardResponse.builder()
                                .flashCardId(flashCard.getCardId().toString())
                                .vocabularyResponse(
                                        VocabularyResponse.builder()
                                                .vocabularyId(flashCard.getCardId())
                                                .word(flashCard.getVocabulary().getWord())
                                                .context(flashCard.getVocabulary().getContext())
                                                .meaning(flashCard.getVocabulary().getMeaning())
                                                .createdBy(flashCard.getVocabulary().getCreatedBy())
                                                .createdAt(flashCard.getVocabulary().getCreatedAt())
                                                .build()
                                )
                                .build());
                    }
                    return ModuleResponse.builder()
                            .moduleId(module.getModuleId())
                            .moduleName(module.getModuleName())
                            .description(module.getDescription())
                            .isPublic(module.getIsPublic())
                            .flashCardIds(flashCardResponses)
                            .createdBy(module.getCreatedBy())
                            .createdAt(module.getCreatedAt())
                            .build();
                })
                .toList();

        return new PageImpl<>(moduleResponses, pageable,modulePage.getTotalElements());
    }

    @Override
    public Page<ModuleResponse> getAllPublicModules(int page, int size, String sortBy, String sortDirection, String keyword, HttpServletRequest httpServletRequest) throws Exception {
        String UserId = helper.getUserIdFromToken(httpServletRequest);
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
        Page<Module> modulePage;
        try {
            modulePage = moduleRepository.searchMyAndPublicModules(keyword, pageable, UserId);
        } catch (Exception e) {
            log.error("Database error when fetching public modules for user: {}", UserId, e);
            throw new AppException(
                    Constants.ErrorCodeMessage.INTERNAL_SERVER_ERROR,
                    Constants.ErrorCode.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }
        List<ModuleResponse> moduleResponses = modulePage.stream()
                .map(module -> {
                    List<FlashCardResponse> flashCardResponses = new ArrayList<>();
                    for (FlashCard flashCard : module.getFlashCards()) {
                        flashCardResponses.add(FlashCardResponse.builder()
                                .flashCardId(flashCard.getCardId().toString())
                                .vocabularyResponse(
                                        VocabularyResponse.builder()
                                                .vocabularyId(flashCard.getVocabulary().getWordId())
                                                .word(flashCard.getVocabulary().getWord())
                                                .context(flashCard.getVocabulary().getContext())
                                                .meaning(flashCard.getVocabulary().getMeaning())
                                                .createdBy(flashCard.getVocabulary().getCreatedBy())
                                                .createdAt(flashCard.getVocabulary().getCreatedAt())
                                                .build()
                                )
                                .build());
                    }
                    return ModuleResponse.builder()
                            .moduleId(module.getModuleId())
                            .moduleName(module.getModuleName())
                            .description(module.getDescription())
                            .isPublic(module.getIsPublic())
                            .flashCardIds(flashCardResponses)
                            .createdBy(module.getCreatedBy())
                            .createdAt(module.getCreatedAt())
                            .build();
                })
                .toList();

        return new PageImpl<>(moduleResponses, pageable,modulePage.getTotalElements());





    }

    @Override
    public void deleteModuleById(String moduleId, HttpServletRequest request) throws Exception {
        String userId = helper.getUserIdFromToken(request);
        if (userId == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value()
            );
        }

        Module module = moduleRepository.findById(UUID.fromString(moduleId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        if (!module.getCreatedBy().equals(userId)) {
            throw new AppException(
                    Constants.ErrorCodeMessage.FORBIDDEN,
                    Constants.ErrorCodeMessage.FORBIDDEN,
                    HttpStatus.FORBIDDEN.value()
            );
        }
        module.setIsDeleted(true);
        moduleRepository.save(module);


    }

    @Override
    public ModuleResponse getModuleById(String moduleId, HttpServletRequest request) throws Exception {
        String userId = helper.getUserIdFromToken(request);
        if (userId == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value()
            );
        }
        Module module = moduleRepository.findById(UUID.fromString(moduleId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        if (module.getIsDeleted()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.NOT_FOUND,
                    Constants.ErrorCodeMessage.NOT_FOUND,
                    HttpStatus.NOT_FOUND.value()
            );
        }
        if (!module.getCreatedBy().equals(userId) && !module.getIsPublic()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.FORBIDDEN,
                    Constants.ErrorCodeMessage.FORBIDDEN,
                    HttpStatus.FORBIDDEN.value()
            );
        }

        List<FlashCardResponse> flashCardResponses = new ArrayList<>();
        for (FlashCard flashCard : module.getFlashCards()) {
            flashCardResponses.add(FlashCardResponse.builder()
                    .flashCardId(flashCard.getCardId().toString())
                    .vocabularyResponse(
                            VocabularyResponse.builder()
                                    .vocabularyId(flashCard.getCardId())
                                    .word(flashCard.getVocabulary().getWord())
                                    .context(flashCard.getVocabulary().getContext())
                                    .meaning(flashCard.getVocabulary().getMeaning())
                                    .createdBy(flashCard.getVocabulary().getCreatedBy())
                                    .createdAt(flashCard.getVocabulary().getCreatedAt())
                                    .build()
                    )
                    .build());
        }
        ModuleResponse response = ModuleResponse.builder()
                .moduleId(module.getModuleId())
                .moduleName(module.getModuleName())
                .description(module.getDescription())
                .isPublic(module.getIsPublic())
                .flashCardIds(flashCardResponses)
                .createdBy(module.getCreatedBy())
                .createdAt(module.getCreatedAt())
                .build();

        return response;




    }

    @Override
    public ModuleResponse updateModule(String moduleId, ModuleRequest moduleRequest, HttpServletRequest request) throws Exception {
        String userId = helper.getUserIdFromToken(request);
        if (userId == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value()
            );
        }

        Module module = moduleRepository.findById(UUID.fromString(moduleId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        if (module.getIsDeleted()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.NOT_FOUND,
                    Constants.ErrorCodeMessage.NOT_FOUND,
                    HttpStatus.NOT_FOUND.value()
            );
        }

        if (!module.getCreatedBy().equals(userId)) {
            throw new AppException(
                    "not your module",
                    Constants.ErrorCodeMessage.FORBIDDEN,
                    HttpStatus.FORBIDDEN.value()
            );
        }

        module.setModuleName(moduleRequest.moduleName());
        module.setDescription(moduleRequest.moduleDescription());
        module.setIsPublic(moduleRequest.isPublic());

//        flashCardRepository.deleteAllFlashCardsInModule(module.getModuleId());
        module.getFlashCards().clear();
        moduleRepository.save(module);
        List<UUID> vocabularyIds = moduleRequest.vocabularyIds();

        for (UUID vocabularyId : vocabularyIds) {
            Vocabulary vocabulary = vocabularyRepository.findById(vocabularyId)
                    .orElseThrow(() -> new AppException("Vocabulary not found", "NOT_FOUND", HttpStatus.NOT_FOUND.value()));

            Optional<FlashCard> existingFlashCardOpt = flashCardRepository
                    .findByVocabularyId(vocabularyId, userId);

            FlashCard flashCard;
            if (existingFlashCardOpt.isPresent()) {
                flashCard = existingFlashCardOpt.get();
            } else {
                flashCard = new FlashCard();
                flashCard.setVocabulary(vocabulary);
                flashCard.setCreatedBy(userId);
                flashCard = flashCardRepository.save(flashCard);
            }

            module.getFlashCards().add(flashCard);
            flashCard.getModules().add(module);
        }

        module.setUpdatedBy(userId);
        moduleRepository.save(module);

        // Chuẩn bị response
        List<FlashCardResponse> flashCardResponses = module.getFlashCards().stream()
                .map(flashCard -> FlashCardResponse.builder()
                        .flashCardId(flashCard.getCardId().toString())
                        .vocabularyResponse(
                                VocabularyResponse.builder()
                                        .vocabularyId(flashCard.getCardId())
                                        .word(flashCard.getVocabulary().getWord())
                                        .context(flashCard.getVocabulary().getContext())
                                        .meaning(flashCard.getVocabulary().getMeaning())
                                        .createdBy(flashCard.getVocabulary().getCreatedBy())
                                        .createdAt(flashCard.getVocabulary().getCreatedAt())
                                        .build()
                        )
                        .build())
                .toList();

        return ModuleResponse.builder()
                .moduleId(module.getModuleId())
                .moduleName(module.getModuleName())
                .description(module.getDescription())
                .isPublic(module.getIsPublic())
                .flashCardIds(flashCardResponses)
                .createdBy(module.getCreatedBy())
                .createdAt(module.getCreatedAt())
                .build();
    }

}
