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
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = AccessLevel.PACKAGE, makeFinal = true)
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class VocabularyServiceImpl  implements VocabularyService {
    ConfigRepository configRepository;
    VocabularyRepository vocabularyRepository;
    Helper helper;


    @Override
    public VocabularyResponse createVocabulary(VocabularyRequest vocabularyRequest, HttpServletRequest httpServletRequest) throws Exception {
        String UserId = helper.getUserIdFromToken(httpServletRequest);
        if (UserId == null) {
            throw new AppException(
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    Constants.ErrorCodeMessage.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value()
            );
        }

        Vocabulary newVocabulary = new Vocabulary();
        newVocabulary.setWord(vocabularyRequest.word());
        newVocabulary.setContext(vocabularyRequest.context());
        newVocabulary.setMeaning(vocabularyRequest.meaning());
        newVocabulary.setCreatedBy(UserId);

        // goi ve APi AI de set gia tri context va meaning

        VocabularyResponse response = VocabularyResponse.builder()
                .vocabularyId(newVocabulary.getWordId())
                .word(newVocabulary.getWord())
                .context(newVocabulary.getContext())
                .meaning(newVocabulary.getMeaning())
                .createdBy(newVocabulary.getCreatedBy())
                .createdAt(newVocabulary.getCreatedAt())
                .build();

        return response;

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
        Vocabulary vocabulary = vocabularyRepository.findById(Integer.parseInt(vocabularyId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));

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

        Vocabulary vocabulary = vocabularyRepository.findById(Integer.parseInt(vocabularyId))
                .orElseThrow(() -> new AppException(
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        Constants.ErrorCodeMessage.NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                ));
        vocabulary.setIsDeleted(true);
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







        return null;
    }


}
