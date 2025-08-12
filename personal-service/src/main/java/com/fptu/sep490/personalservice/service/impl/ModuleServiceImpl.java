package com.fptu.sep490.personalservice.service.impl;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.viewmodel.response.BaseResponse;
import com.fptu.sep490.personalservice.constants.Constants;
import com.fptu.sep490.personalservice.helper.Helper;
import com.fptu.sep490.personalservice.model.*;
import com.fptu.sep490.personalservice.model.Module;
import com.fptu.sep490.personalservice.model.enumeration.LearningStatus;
import com.fptu.sep490.personalservice.model.enumeration.ModuleUserStatus;
import com.fptu.sep490.personalservice.repository.*;
import com.fptu.sep490.personalservice.repository.client.AuthClient;
import com.fptu.sep490.personalservice.service.ModuleService;
import com.fptu.sep490.personalservice.viewmodel.request.*;
import com.fptu.sep490.personalservice.viewmodel.response.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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
    FlashCardProgressRepository flashCardProgressRepository;
    Helper helper;
    AuthClient authClient;

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

        // Save connection at ModuleUsers then the creator can access the module
        ModuleUsers moduleUsers = new ModuleUsers();
        moduleUsers.setModule(savedModule);
        moduleUsers.setUserId(userId);
        moduleUsers.setStatus(ModuleUserStatus.ACCEPTED.ordinal());
        moduleUsers.setCreatedBy(userId);
        moduleUsers.setStatus(1);
        moduleUsersRepository.save(moduleUsers);

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
            modulePage = moduleRepository.searchModuleByUser(keyword, pageable, UserId);
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
                    ModuleUsers moduleUser = module.getModuleUsers().stream()
                            .filter(mu -> mu.getUserId().equals(UserId))
                            .findFirst()
                            .orElse(null);

                    Long timeSpent = moduleUser != null ? moduleUser.getTimeSpent() : null;
                    Double progress = moduleUser != null ? moduleUser.getProgress() : null;
                    LearningStatus learningStatus = moduleUser != null ? moduleUser.getLearningStatus() :null;
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
                            .timeSpent(timeSpent)
                            .progress(progress)
                            .learningStatus(learningStatus)
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
        ModuleUsers moduleUser = moduleUsersRepository.findByModuleIdAndUserId(UUID.fromString(moduleId), userId)
                .orElse(null);

        if (!module.getCreatedBy().equals(userId) && !module.getIsPublic() && moduleUser == null) {
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
        String accessToken = helper.getAccessToken(request);

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
        // now not share with usser, that is email
        ResponseEntity<BaseResponse<UserAccessInfo>> user = null;
        for (String email : shareModuleRequest.users()) {
            try {
                user = authClient.getUserInfoByEmail(email, "Bearer " + accessToken);
                if (user == null) {
                    throw new AppException(
                            Constants.ErrorCodeMessage.NOT_FOUND,
                            Constants.ErrorCode.NOT_FOUND,
                            HttpStatus.NOT_FOUND.value()
                    );
                }
            }catch (Exception e){
                throw new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCode.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                );            }
            var body = user.getBody();
            String sharedUserId = body.data().id();
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
                    UserProfileResponse user = UserProfileResponse.builder().build();
                    UserProfileResponse share_to = UserProfileResponse.builder().build();
                    try {
                        user = helper.getUserProfileById(module.getCreatedBy());
                        share_to = helper.getUserProfileById(moduleUser.getUserId());
                    }catch (Exception e){
                        log.error("Error fetching user profile for module creator: {}", module.getCreatedBy());
                        throw new AppException(
                                Constants.ErrorCodeMessage.INTERNAL_SERVER_ERROR,
                                "Error fetching user profile",
                                HttpStatus.INTERNAL_SERVER_ERROR.value()
                        );
                    }
                    return ModuleUserResponse.builder()
                            .moduleId(module.getModuleId())
                            .moduleName(module.getModuleName())
                            .description(module.getDescription())
                            .isPublic(module.getIsPublic())
                            .flashCardIds(flashCardResponses)
                            .createdBy(user != null ? user.email() : module.getCreatedBy())
                            .shareTo(share_to != null ? share_to.email() : moduleUser.getUserId())
                            .createdAt(module.getCreatedAt())
                            .updatedBy(module.getUpdatedBy())
                            .status(moduleUser.getStatus() != null ? moduleUser.getStatus() : 0)
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
                    UserProfileResponse user = UserProfileResponse.builder().build();
                    UserProfileResponse share_to = UserProfileResponse.builder().build();
                    try {
                        user = helper.getUserProfileById(module.getCreatedBy());
                        share_to = helper.getUserProfileById(moduleUser.getUserId());
                    } catch (Exception e) {
                        log.error("Error fetching user profile for module creator: {}", module.getCreatedBy());
                        throw new AppException(
                                Constants.ErrorCodeMessage.INTERNAL_SERVER_ERROR,
                                "Error fetching user profile",
                                HttpStatus.INTERNAL_SERVER_ERROR.value()
                        );
                    }
                    return ModuleUserResponse.builder()
                            .moduleId(module.getModuleId())
                            .moduleName(module.getModuleName())
                            .description(module.getDescription())
                            .isPublic(module.getIsPublic())
                            .flashCardIds(flashCardResponses)
                            .createdBy(user != null ? user.email() : module.getCreatedBy())
                            .shareTo(share_to != null ? share_to.email() : moduleUser.getUserId())
                            .createdAt(module.getCreatedAt())
                            .updatedBy(module.getUpdatedBy())
                            .updatedAt(module.getUpdatedAt())
                            .status(moduleUser.getStatus() != null ? moduleUser.getStatus() : 0)
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

    @Override
    public ModuleProgressResponse getModuleProgress(String moduleId, HttpServletRequest request) throws Exception {
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
        Optional<ModuleUsers> moduleUser = moduleUsersRepository.findByModuleIdAndUserId(UUID.fromString(moduleId), userId);
        if (moduleUser.isEmpty()) {
            throw new AppException(
                    "you do not have permission to view this module, or still not clone this module",
                    Constants.ErrorCodeMessage.FORBIDDEN,
                    HttpStatus.FORBIDDEN.value()
            );
        }

        ModuleUsers moduleUsers = moduleUser.get();
        Integer status = moduleUsers.getStatus() != null ? moduleUsers.getStatus() : 0;
        if (status == 2) {
            throw new AppException(
                    "you have denied or still not accept this module, please contact the owner to re-allow you",
                    Constants.ErrorCodeMessage.FORBIDDEN,
                    HttpStatus.FORBIDDEN.value()
            );
        }
        List<FlashCard> flashCards = module.getFlashCards();
        for( FlashCard flashCard : flashCards) {
            if(flashCardProgressRepository.findByModuleUserIdAndFlashcardId(moduleUsers.getId(), flashCard.getCardId().toString()).isEmpty()) {
                FlashCardProgress flashCardProgress = new FlashCardProgress();
                flashCardProgress.setFlashcardId(flashCard.getCardId().toString());
                flashCardProgress.setModuleUsers(moduleUsers);
                flashCardProgress.setStatus(0); // default status
                flashCardProgressRepository.save(flashCardProgress);
                moduleUsers.getFlashcardProgressList().add(flashCardProgress);
            }
        }

        List<FlashCardProgressResponse> flashCardProgressResponses = moduleUsers.getFlashcardProgressList().stream()
                .map(flashCardProgress -> {

                    FlashCard flashCard = flashCardRepository.findById(UUID.fromString(flashCardProgress.getFlashcardId()))
                            .orElseThrow(() -> new AppException(
                                    Constants.ErrorCodeMessage.NOT_FOUND,
                                    "Flashcard not found: " + flashCardProgress.getFlashcardId(),
                                    HttpStatus.NOT_FOUND.value()
                            ));

                    Vocabulary vocabulary = flashCard.getVocabulary();

                    FlashCardResponse flashCardResponse = FlashCardResponse.builder()
                            .flashCardId(flashCard.getCardId().toString())
                            .vocabularyResponse(
                                    VocabularyResponse.builder()
                                            .vocabularyId(vocabulary.getWordId())
                                            .word(vocabulary.getWord())
                                            .context(vocabulary.getContext())
                                            .meaning(vocabulary.getMeaning())
                                            .createdBy(vocabulary.getCreatedBy())
                                            .createdAt(vocabulary.getCreatedAt())
                                            .build()
                            )
                            .build();

                    return FlashCardProgressResponse.builder()
                            .flashcardId(flashCardProgress.getFlashcardId())
                            .status(flashCardProgress.getStatus())
                            .isHighlighted(flashCardProgress.getIsHighlighted())
                            .flashcardDetail(flashCardResponse)
                            .build();
                })
                .toList();






        ModuleProgressResponse progressResponse = ModuleProgressResponse.builder().
                id(moduleUsers.getId())
                .moduleId(module.getModuleId().toString())
                .moduleName(module.getModuleName())
                .userId(userId)
                .status(status)
                .attempts(moduleUsers.getAttempts())
                .progress(moduleUsers.getProgress())
                .learningStatus(moduleUsers.getLearningStatus())
                .timeSpent(moduleUsers.getTimeSpent() != null ? moduleUsers.getTimeSpent() : 0L)
                .lastIndexRead(moduleUsers.getLastIndexRead() != null ? moduleUsers.getLastIndexRead() : 0)
                .flashcardProgresses(flashCardProgressResponses)
                .build();



        return progressResponse;
    }

    @Override
    public ModuleProgressResponse updateModuleProgress(String moduleId, ModuleProgressRequest moduleProgressRequest, HttpServletRequest request) throws Exception {
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
        Optional<ModuleUsers> moduleUser = moduleUsersRepository.findByModuleIdAndUserId(UUID.fromString(moduleId), userId);
        if (moduleUser.isEmpty()) {
            throw new AppException(
                    "you do not have permission to view this module, or still not clone this module",
                    Constants.ErrorCodeMessage.FORBIDDEN,
                    HttpStatus.FORBIDDEN.value()
            );
        }

        ModuleUsers moduleUsers = moduleUser.get();
        Integer currentStatus = moduleUsers.getStatus() != null ? moduleUsers.getStatus() : 0;
        if (currentStatus == 2) {
            throw new AppException(
                    "you have denied or still not accept this module, please contact the owner to re-allow you",
                    Constants.ErrorCodeMessage.FORBIDDEN,
                    HttpStatus.FORBIDDEN.value()
            );
        }

        if (moduleProgressRequest.status() != null) {
            moduleUsers.setStatus(moduleProgressRequest.status());
        }
        if (moduleProgressRequest.lastIndexRead() != null) {
            moduleUsers.setLastIndexRead(moduleProgressRequest.lastIndexRead());
        }
        if (moduleProgressRequest.highlightedFlashcardIds() != null) {
            for (String flashcardId : moduleProgressRequest.highlightedFlashcardIds()) {
                if (moduleUsers.getFlashcardProgressList() == null) {
                    moduleUsers.setFlashcardProgressList(new ArrayList<>());
                }

                if (flashCardProgressRepository.findByModuleUserIdAndFlashcardId(moduleUsers.getId(), flashcardId).isEmpty()) {
                    FlashCardProgress flashcardProgress = new FlashCardProgress();
                    flashcardProgress.setFlashcardId(flashcardId);
                    flashcardProgress.setModuleUsers(moduleUsers);
                    flashcardProgress.setStatus(0); // default status
                    flashcardProgress = flashCardProgressRepository.save(flashcardProgress);
                    moduleUsers.getFlashcardProgressList().add(flashcardProgress);
                }
            }
        }
        if (moduleProgressRequest.timeSpent() != null) {
            Long current = moduleUsers.getTimeSpent() == null ? 0L : moduleUsers.getTimeSpent();
            moduleUsers.setTimeSpent(current + moduleProgressRequest.timeSpent());
        }
        if (moduleProgressRequest.progress() != null) {
            moduleUsers.setProgress(moduleProgressRequest.progress());
        }
        if (moduleProgressRequest.learningStatus() != null) {
            moduleUsers.setLearningStatus(LearningStatus.valueOf(moduleProgressRequest.learningStatus()));
        }

        moduleUsers.setUpdatedBy(userId);
        moduleUsers = moduleUsersRepository.save(moduleUsers);
        List<FlashCardProgress> flashCardProgresses = moduleUsers.getFlashcardProgressList();
        List<FlashCardProgressResponse> flashCardProgressResponses = flashCardProgresses.stream()
                .map(flashCardProgress -> FlashCardProgressResponse.builder()
                        .flashcardId(flashCardProgress.getFlashcardId())
                        .status(flashCardProgress.getStatus())
                        .isHighlighted(flashCardProgress.getIsHighlighted())
                        .build())
                .toList();
        ModuleProgressResponse progressResponse = ModuleProgressResponse.builder()
                .id(moduleUsers.getId())
                .moduleId(module.getModuleId().toString())
                .moduleName(module.getModuleName())
                .attempts(moduleUsers.getAttempts())
                .userId(userId)
                .timeSpent(moduleUsers.getTimeSpent())
                .progress(moduleUsers.getProgress())
                .status(moduleUsers.getStatus() != null ? moduleUsers.getStatus() : 0)
                .lastIndexRead(moduleUsers.getLastIndexRead() != null ? moduleUsers.getLastIndexRead() : 0)
                .learningStatus(moduleUsers.getLearningStatus())
                .flashcardProgresses(flashCardProgressResponses)
                .build();

        return  progressResponse;

    }

    @Override
    public void updateFlashcardProgress(String moduleId, FlashcardProgressRequest flashcardProgressRequest, HttpServletRequest request) throws Exception {
        String userId = helper.getUserIdFromToken(request);
        if (userId == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value()
            );
        }
        
        // Verify module exists and user has access
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
        
        // Check if user has access to this module
        Optional<ModuleUsers> moduleUser = moduleUsersRepository.findByModuleIdAndUserId(UUID.fromString(moduleId), userId);
        if (moduleUser.isEmpty()) {
            throw new AppException(
                    "You do not have permission to access this module",
                    Constants.ErrorCodeMessage.FORBIDDEN,
                    HttpStatus.FORBIDDEN.value()
            );
        }
        
        ModuleUsers moduleUsers = moduleUser.get();
        Integer status = moduleUsers.getStatus() != null ? moduleUsers.getStatus() : 0;
        if (status == 2) {
            throw new AppException(
                    "You have denied access to this module",
                    Constants.ErrorCodeMessage.FORBIDDEN,
                    HttpStatus.FORBIDDEN.value()
            );
        }
        moduleUsers.setLearningStatus(LearningStatus.LEARNING);
        
        // Update last index read (increment if correct answer)
        if (flashcardProgressRequest.isCorrect()) {
            Integer currentIndex = moduleUsers.getLastIndexRead() != null ? moduleUsers.getLastIndexRead() : 0;
            moduleUsers.setLastIndexRead(currentIndex + 1);


        }

        
        // Add to highlighted flashcards if incorrect
        List<FlashCardProgress> flashCardProgresses = moduleUsers.getFlashcardProgressList();
        if (flashCardProgresses == null) {
            flashCardProgresses = new ArrayList<>();
        }
        FlashCardProgress existingProgress = flashCardProgressRepository.findByModuleUserIdAndFlashcardId(moduleUsers.getId(), flashcardProgressRequest.flashcardId())
                .orElse(null);
        if (existingProgress == null) {
            FlashCardProgress flashcardProgress = new FlashCardProgress();
            flashcardProgress.setFlashcardId(flashcardProgressRequest.flashcardId());
            flashcardProgress.setModuleUsers(moduleUsers);
            if (flashcardProgressRequest.isCorrect()) {
                flashcardProgress.setStatus(2);
            }else{
                flashcardProgress.setStatus(1);

            }
            flashcardProgress = flashCardProgressRepository.save(flashcardProgress);
            flashCardProgresses.add(flashcardProgress);
        }else{
            existingProgress.setStatus(flashcardProgressRequest.isCorrect() ? 2 : 1);
            flashCardProgressRepository.save(existingProgress);
        }
        if (flashcardProgressRequest.isHighlighted()){
            Optional<FlashCardProgress> flashCardProgress = flashCardProgressRepository.findByModuleUserIdAndFlashcardId(moduleUsers.getId(), flashcardProgressRequest.flashcardId());
            FlashCardProgress flashcardProgress2 = flashCardProgress.orElseThrow(() -> new AppException(
                    Constants.ErrorCodeMessage.NOT_FOUND,
                    "Flashcard progress not found for module user: " + moduleUsers.getId() + " and flashcard: " + flashcardProgressRequest.flashcardId(),
                    HttpStatus.NOT_FOUND.value()
            ));
            flashcardProgress2.setIsHighlighted(flashcardProgressRequest.isHighlighted());
            flashCardProgressRepository.save(flashcardProgress2);

        }
        // Update progress
        Double currentProgress = moduleUsers.getProgress() != null ? moduleUsers.getProgress() : 0.0;
        // caculate the correct flashcard vs total flashcard of thif moduleUsers
        long correctCount = flashCardProgresses.stream()
                .filter(fcp -> fcp.getStatus() == 2) // Assuming status 2 means correct
                .count();
        long totalCount = flashCardProgresses.size();
        if (totalCount > 0) {
            double newProgress = (double) correctCount / totalCount * 100; // Calculate percentage
            moduleUsers.setProgress(newProgress);
            if (newProgress >= 100.0) {
                moduleUsers.setLearningStatus(LearningStatus.MASTERED);
            } else if (newProgress > 0.0) {
                moduleUsers.setLearningStatus(LearningStatus.LEARNING);
            } else {
                moduleUsers.setLearningStatus(LearningStatus.NEW);
            }
        } else {
            moduleUsers.setProgress(0.0); // No flashcards, reset progress
        }

        
        moduleUsers.setUpdatedBy(userId);
        moduleUsersRepository.save(moduleUsers);

    }

    @Override
    public ModuleProgressResponse refreshModuleProgress(String moduleId, ModuleFlashCardRequest moduleFlashCardRequest, HttpServletRequest request) {
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
        ModuleUsers moduleUsers = moduleUsersRepository.findByModuleIdAndUserId(UUID.fromString(moduleId), userId)
                .orElseThrow(() -> new AppException(
                        "You do not have permission to view this module, or still not clone this module",
                        Constants.ErrorCodeMessage.FORBIDDEN,
                        HttpStatus.FORBIDDEN.value()
                ));
        moduleUsers.setProgress(0.0);
        if(moduleUsers.getLearningStatus() != LearningStatus.NEW && moduleFlashCardRequest.learningStatus() == LearningStatus.NEW) {
            moduleUsers.setLearningStatus(LearningStatus.LEARNING);
            moduleUsers.setAttempts(moduleUsers.getAttempts() != null ? moduleUsers.getAttempts() + 1 : 1);
        }
        moduleUsers.setLearningStatus(moduleFlashCardRequest.learningStatus());
        moduleUsers.setUpdatedBy(userId);
        moduleUsers.setUpdatedAt(LocalDateTime.now());
        if(moduleFlashCardRequest.learningStatus() == LearningStatus.NEW){
            List<FlashCardProgress> flashCardProgresses = moduleUsers.getFlashcardProgressList();
            for(FlashCardProgress flashCardProgress : flashCardProgresses) {
                flashCardProgress.setStatus(0); // reset status to 0
                flashCardProgress.setIsHighlighted(false); // reset highlight
                flashCardProgressRepository.save(flashCardProgress);
            }

        }
        moduleUsers = moduleUsersRepository.save(moduleUsers);
        List<FlashCardProgress> flashCardProgresses = moduleUsers.getFlashcardProgressList();
        List<FlashCardProgressResponse> flashCardProgressResponses = flashCardProgresses.stream()
                .map(flashCardProgress -> FlashCardProgressResponse.builder()
                        .flashcardId(flashCardProgress.getFlashcardId())
                        .status(flashCardProgress.getStatus())
                        .isHighlighted(flashCardProgress.getIsHighlighted())
                        .build())
                .toList();
        ModuleProgressResponse progressResponse = ModuleProgressResponse.builder()
                .id(moduleUsers.getId())
                .moduleId(module.getModuleId().toString())
                .moduleName(module.getModuleName())
                .attempts(moduleUsers.getAttempts())
                .userId(userId)
                .timeSpent(moduleUsers.getTimeSpent())
                .progress(moduleUsers.getProgress())
                .status(moduleUsers.getStatus() != null ? moduleUsers.getStatus() : 0)
                .lastIndexRead(moduleUsers.getLastIndexRead() != null ? moduleUsers.getLastIndexRead() : 0)
                .learningStatus(moduleUsers.getLearningStatus())
                .flashcardProgresses(flashCardProgressResponses)
                .build();

        return  progressResponse;

    }


}
