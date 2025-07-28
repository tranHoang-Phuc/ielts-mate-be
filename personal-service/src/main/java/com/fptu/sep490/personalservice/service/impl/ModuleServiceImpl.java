package com.fptu.sep490.personalservice.service.impl;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.personalservice.constants.Constants;
import com.fptu.sep490.personalservice.helper.Helper;
import com.fptu.sep490.personalservice.model.*;
import com.fptu.sep490.personalservice.model.Module;
import com.fptu.sep490.personalservice.model.enumeration.ModuleUserStatus;
import com.fptu.sep490.personalservice.repository.*;
import com.fptu.sep490.personalservice.service.ModuleService;
import com.fptu.sep490.personalservice.viewmodel.request.ModuleRequest;
import com.fptu.sep490.personalservice.viewmodel.request.ShareModuleRequest;
import com.fptu.sep490.personalservice.viewmodel.response.FlashCardResponse;
import com.fptu.sep490.personalservice.viewmodel.response.ModuleResponse;
import com.fptu.sep490.personalservice.viewmodel.response.ModuleUserResponse;
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
            Optional<ModuleUsers> existingModuleUserOpt = module.getModuleUsers().stream()
                    .filter(mu -> mu.getUserId().equals(sharedUserId))
                    .findFirst();
            if (existingModuleUserOpt.isPresent()) {
                ModuleUsers existingModuleUser = existingModuleUserOpt.get();
                if (existingModuleUser.getStatus() == 2) {
                    existingModuleUser.setStatus(0);
                    moduleUsersRepository.save(existingModuleUser);
                    log.info("Updated status of ModuleUsers for user {} to 0", sharedUserId);
                } else {
                    log.warn("Module {} is already shared with user {}", moduleId, sharedUserId);
                }
            } else {
                ModuleUsers moduleUsers = new ModuleUsers();
                moduleUsers.setModule(module);
                moduleUsers.setUserId(sharedUserId);
                moduleUsers.setStatus(0); // set default status when sharing
                moduleUsers.setCreatedBy(userId);
                module.getModuleUsers().add(moduleUsers);
                moduleUsersRepository.save(moduleUsers);
                log.info("Shared module {} with new user {}", moduleId, sharedUserId);
            }
        }
        moduleRepository.save(module);



    }

    @Override
    public Page<ModuleUserResponse> getAllSharedModules(HttpServletRequest request, int page, int size, String sortBy, String sortDirection, String keyword, int status) throws Exception {
        String userId = helper.getUserIdFromToken(request);
        if (userId == null) {
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
        Page<ModuleUsers> modulePage;
        try {
            modulePage = moduleUsersRepository.searchShareModules(keyword, pageable, userId, status);
        } catch (Exception e) {
            log.error("Database error when fetching shared modules for user: {}", userId, e);
            throw new AppException(
                    Constants.ErrorCodeMessage.INTERNAL_SERVER_ERROR,
                    Constants.ErrorCode.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }

        List<ModuleUserResponse> moduleResponses = modulePage.stream()
                .map(moduleUser -> {
                    Module module = moduleUser.getModule();
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
                    return ModuleUserResponse.builder()
                            .moduleId(module.getModuleId())
                            .moduleName(module.getModuleName())
                            .description(module.getDescription())
                            .isPublic(module.getIsPublic())
                            .flashCardIds(flashCardResponses)
                            .createdBy(module.getCreatedBy())
                            .createdAt(module.getCreatedAt())
                            .updatedBy(module.getUpdatedBy())
                            .status(moduleUser.getStatus())
                            .updatedAt(module.getUpdatedAt())
                            .build();
                })
                .toList();

        return new PageImpl<>(moduleResponses, pageable, modulePage.getTotalElements());


    }

    @Override
    public void updateSharedModuleRequest(String moduleId, int status, HttpServletRequest request) throws Exception {
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
        Optional<ModuleUsers> moduleUserOpt = module.getModuleUsers().stream()
                .filter(mu -> mu.getUserId().equals(userId))
                .findFirst();
        if (moduleUserOpt.isEmpty()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.NOT_FOUND,
                    "ModuleUser not found for user: " + userId,
                    HttpStatus.NOT_FOUND.value()
            );
        }
        ModuleUsers moduleUser = moduleUserOpt.get();
        if (moduleUser.getStatus() == 1) {
            throw new AppException(
                    Constants.ErrorCodeMessage.FORBIDDEN,
                    "You have already accepted this module",
                    HttpStatus.FORBIDDEN.value()
            );
        }
        if (moduleUser.getStatus() == 2) {
            throw new AppException(
                    Constants.ErrorCodeMessage.FORBIDDEN,
                    "You have already denied this module",
                    HttpStatus.FORBIDDEN.value()
            );
        }
        if (status == ModuleUserStatus.ACCEPTED.ordinal()) {
            moduleUser.setStatus(1); // 1: allowed
        } else if (status == ModuleUserStatus.REJECTED.ordinal()) {
            moduleUser.setStatus(2); // 2: denied
        } else {
            throw new AppException(
                    Constants.ErrorCodeMessage.INVALID_REQUEST,
                    "Invalid status: " + status,
                    HttpStatus.BAD_REQUEST.value()
            );
        }

        moduleUsersRepository.save(moduleUser);

    }

    @Override
    public Page<ModuleUserResponse> getAllMySharedModules(HttpServletRequest request, int i, int size, String sortBy, String sortDirection, String keyword) throws Exception {
        String userId = helper.getUserIdFromToken(request);
        if (userId == null) {
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
        Pageable pageable = PageRequest.of(i, size, sort);
        Page<ModuleUsers> modulePage;
        try {
            modulePage = moduleUsersRepository.searchMyShareModules(keyword, pageable, userId);
        } catch (Exception e) {
            log.error("Database error when fetching shared modules for user: {}", userId, e);
            throw new AppException(
                    Constants.ErrorCodeMessage.INTERNAL_SERVER_ERROR,
                    Constants.ErrorCode.INTERNAL_SERVER_ERROR,
                    HttpStatus.INTERNAL_SERVER_ERROR.value()
            );
        }
        List<ModuleUserResponse> moduleResponses = modulePage.stream()
                .map(moduleUser -> {
                    Module module = moduleUser.getModule();
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
                    return ModuleUserResponse.builder()
                            .moduleId(module.getModuleId())
                            .moduleName(module.getModuleName())
                            .description(module.getDescription())
                            .isPublic(module.getIsPublic())
                            .flashCardIds(flashCardResponses)
                            .createdBy(module.getCreatedBy())
                            .createdAt(module.getCreatedAt())
                            .updatedBy(module.getUpdatedBy())
                            .updatedAt(module.getUpdatedAt())
                            .status(moduleUser.getStatus())
                            .build();
                })
                .toList();
        return new PageImpl<>(moduleResponses, pageable, modulePage.getTotalElements());




    }

    @Override
    public ModuleResponse cloneModule(String moduleId, HttpServletRequest request) throws Exception {
        String userId = helper.getUserIdFromToken(request);
        if (userId == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value()
            );
        }

        Module originalModule = moduleRepository.findById(UUID.fromString(moduleId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

        if (originalModule.getIsDeleted()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.NOT_FOUND,
                    Constants.ErrorCodeMessage.NOT_FOUND,
                    HttpStatus.NOT_FOUND.value()
            );
        }

        Optional<ModuleUsers> moduleUser = moduleUsersRepository.findByModuleIdAndUserId(UUID.fromString(moduleId), userId);

        if (!originalModule.getIsPublic() && moduleUser.isEmpty()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.FORBIDDEN,
                    "You do not have permission to clone this module",
                    HttpStatus.FORBIDDEN.value()
            );
        }

        // Tạo module mới
        Module clonedModule = new Module();
        clonedModule.setModuleName(originalModule.getModuleName() + " - Copy");
        clonedModule.setDescription(originalModule.getDescription());
        clonedModule.setIsPublic(false);
        clonedModule.setCreatedBy(userId);

        Module savedClonedModule = moduleRepository.save(clonedModule);

        int index = 0; // optional: để đặt thứ tự nếu cần
        for (FlashCard flashCard : originalModule.getFlashCards()) {
            Vocabulary vocabulary = flashCard.getVocabulary();
            Optional<FlashCard> existingFlashCard = flashCardRepository.findByVocabularyId(vocabulary.getWordId(), userId);

            FlashCard flashCardClone = existingFlashCard.orElseGet(() -> {
                FlashCard newCard = new FlashCard();
                newCard.setVocabulary(vocabulary);
                newCard.setCreatedBy(userId);
                return flashCardRepository.save(newCard);
            });

            FlashCardModule flashCardModule = new FlashCardModule();
            flashCardModule.setFlashCard(flashCardClone);
            flashCardModule.setModule(savedClonedModule);
            flashCardModule.setOrderIndex(index++); // nếu cần giữ thứ tự

            flashCardModuleRepository.save(flashCardModule);

            // Nếu có mappedBy (bidirectional)
            flashCardClone.getFlashCardModules().add(flashCardModule);
            savedClonedModule.getFlashCardModules().add(flashCardModule);
        }

        // Build response như ở createModule
        List<FlashCardResponse> flashCardResponses = savedClonedModule.getFlashCards().stream()
                .map(card -> FlashCardResponse.builder()
                        .flashCardId(card.getCardId().toString())
                        .vocabularyResponse(
                                VocabularyResponse.builder()
                                        .vocabularyId(card.getVocabulary().getWordId())
                                        .word(card.getVocabulary().getWord())
                                        .context(card.getVocabulary().getContext())
                                        .meaning(card.getVocabulary().getMeaning())
                                        .createdBy(card.getVocabulary().getCreatedBy())
                                        .createdAt(card.getVocabulary().getCreatedAt())
                                        .build()
                        )
                        .build())
                .toList();

        return ModuleResponse.builder()
                .moduleId(savedClonedModule.getModuleId())
                .moduleName(savedClonedModule.getModuleName())
                .description(savedClonedModule.getDescription())
                .isPublic(savedClonedModule.getIsPublic())
                .flashCardIds(flashCardResponses)
                .createdBy(savedClonedModule.getCreatedBy())
                .createdAt(savedClonedModule.getCreatedAt())
                .build();
    }



}
