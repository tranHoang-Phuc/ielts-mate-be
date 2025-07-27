package com.fptu.sep490.personalservice.service.impl;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.personalservice.constants.Constants;
import com.fptu.sep490.personalservice.helper.Helper;
import com.fptu.sep490.personalservice.model.*;
import com.fptu.sep490.personalservice.model.Module;
import com.fptu.sep490.personalservice.repository.*;
import com.fptu.sep490.personalservice.service.ModuleService;
import com.fptu.sep490.personalservice.viewmodel.request.ModuleRequest;
import com.fptu.sep490.personalservice.viewmodel.request.ShareModuleRequest;
import com.fptu.sep490.personalservice.viewmodel.response.FlashCardResponse;
import com.fptu.sep490.personalservice.viewmodel.response.ModuleResponse;
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
    FlashCardModuleRepository flashCardModuleRepository;
    ModuleUsersRepository moduleUsersRepository;
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
        newModule.setIsPublic(moduleRequest.isPublic());
        newModule.setCreatedBy(userId);

        // Save module first to get its ID
        Module savedModule = moduleRepository.save(newModule);

        for (UUID vocabularyId : vocabularyIds) {
            Vocabulary vocabulary = vocabularyRepository.findById(vocabularyId)
                    .orElseThrow(() -> new Exception("Vocabulary not found: " + vocabularyId));

            Optional<FlashCard> existingFlashCard = flashCardRepository.findByVocabularyId(vocabularyId, userId);

            FlashCard flashCard = existingFlashCard.orElseGet(() -> {
                FlashCard newCard = new FlashCard();
                newCard.setVocabulary(vocabulary);
                newCard.setCreatedBy(userId);
                return flashCardRepository.save(newCard);
            });


            FlashCardModule flashCardModule = new FlashCardModule();
            flashCardModule.setFlashCard(flashCard);
            flashCardModule.setModule(savedModule);

            flashCard.getFlashCardModules().add(flashCardModule);
            savedModule.getFlashCardModules().add(flashCardModule);

            flashCardModuleRepository.save(flashCardModule);
        }

        // Build response
        List<FlashCardResponse> flashCardResponses = savedModule.getFlashCards().stream()
                .map(flashCard -> FlashCardResponse.builder()
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
                        .build())
                .toList();

        return ModuleResponse.builder()
                .moduleId(savedModule.getModuleId())
                .moduleName(savedModule.getModuleName())
                .description(savedModule.getDescription())
                .isPublic(savedModule.getIsPublic())
                .flashCardIds(flashCardResponses)
                .createdBy(savedModule.getCreatedBy())
                .createdAt(savedModule.getCreatedAt())
                .build();
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
                            .updatedBy(module.getUpdatedBy())
                            .updatedAt(module.getUpdatedAt())
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
                            .updatedBy(module.getUpdatedBy())
                            .updatedAt(module.getUpdatedAt())
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

        // Cập nhật thông tin module
        module.setModuleName(moduleRequest.moduleName());
        module.setDescription(moduleRequest.moduleDescription());
        module.setIsPublic(moduleRequest.isPublic());
        module.setUpdatedBy(userId);

        // Xoá toàn bộ quan hệ cũ
        module.getFlashCardModules().clear();
        moduleRepository.save(module);

        List<UUID> vocabularyIds = moduleRequest.vocabularyIds();

        for (UUID vocabularyId : vocabularyIds) {
            Vocabulary vocabulary = vocabularyRepository.findById(vocabularyId)
                    .orElseThrow(() -> new AppException("Vocabulary not found", "NOT_FOUND", HttpStatus.NOT_FOUND.value()));

            Optional<FlashCard> existingFlashCardOpt = flashCardRepository.findByVocabularyId(vocabularyId, userId);

            FlashCard flashCard = existingFlashCardOpt.orElseGet(() -> {
                FlashCard newCard = new FlashCard();
                newCard.setVocabulary(vocabulary);
                newCard.setCreatedBy(userId);
                return flashCardRepository.save(newCard);
            });

            // Tránh tạo FlashCardModule trùng
            boolean alreadyLinked = flashCardModuleRepository.existsByFlashCardAndModule(flashCard, module);
            if (!alreadyLinked) {
                FlashCardModule flashCardModule = new FlashCardModule();
                flashCardModule.setFlashCard(flashCard);
                flashCardModule.setModule(module);
                flashCardModule.setOrderIndex(0); // hoặc gán theo thứ tự nếu có

                flashCard.getFlashCardModules().add(flashCardModule);
                module.getFlashCardModules().add(flashCardModule);

                flashCardModuleRepository.save(flashCardModule);
            }
        }

        module = moduleRepository.save(module); // refresh

        // Chuẩn bị response
        List<FlashCardResponse> flashCardResponses = module.getFlashCards().stream()
                .map(flashCard -> FlashCardResponse.builder()
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

    @Override
    public void shareModule(String moduleId, ShareModuleRequest shareModuleRequest, HttpServletRequest request) throws Exception {
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
                    Constants.ErrorCodeMessage.FORBIDDEN,
                    Constants.ErrorCodeMessage.FORBIDDEN,
                    HttpStatus.FORBIDDEN.value()
            );
        }
        for (String sharedUserId : shareModuleRequest.users()) {
            if (sharedUserId.equals(userId)) {
                continue; // Không chia sẻ cho chính mình
            }
            // Kiểm tra xem người dùng đã được chia sẻ chưa
            boolean alreadyShared = module.getModuleUsers().stream()
                    .anyMatch(moduleUser -> moduleUser.getUserId().equals(sharedUserId));
            if (!alreadyShared) {
                ModuleUsers moduleUsers = new ModuleUsers();
                moduleUsers.setModule(module);
                moduleUsers.setUserId(sharedUserId);
                module.getModuleUsers().add(moduleUsers);
                moduleUsersRepository.save(moduleUsers);
            }
        }
        moduleRepository.save(module);



    }


}
