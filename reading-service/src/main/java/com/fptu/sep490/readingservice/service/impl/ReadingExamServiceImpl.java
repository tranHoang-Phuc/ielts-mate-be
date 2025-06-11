package com.fptu.sep490.readingservice.service.impl;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.readingservice.constants.Constants;
import com.fptu.sep490.readingservice.helper.Helper;
import com.fptu.sep490.readingservice.model.ReadingExam;
import com.fptu.sep490.readingservice.model.ReadingPassage;
import com.fptu.sep490.readingservice.repository.ReadingExamRepository;
import com.fptu.sep490.readingservice.repository.ReadingPassageRepository;
import com.fptu.sep490.readingservice.service.ReadingExamService;
import com.fptu.sep490.readingservice.viewmodel.request.ReadingExamCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.response.ReadingExamResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReadingExamServiceImpl implements ReadingExamService  {
    Helper helper;
    ReadingPassageRepository readingPassageRepository;
    ReadingExamRepository readingExamRepository;
    @Override
    public ReadingExamResponse createReadingExam(ReadingExamCreationRequest readingExamCreationRequest, HttpServletRequest request) throws Exception {
        String userId = helper.getUserIdFromToken(request);

        if (readingExamCreationRequest == null) {
            throw new IllegalArgumentException("Reading exam creation request cannot be null");
        }
        //create new ReadingExamObject
        ReadingExam readingExam = new ReadingExam();
        readingExam.setExamName(readingExamCreationRequest.readingExamName());
        readingExam.setExamDescription(readingExamCreationRequest.readingExamDescription());
        readingExam.setUrlSlug(readingExamCreationRequest.urlSlung());
        readingExam.setCreatedBy(userId);

        if(!readingExamCreationRequest.readingPassageIdPart1().isEmpty()){
            Optional<ReadingPassage> readingPassagePart1 = readingPassageRepository.findById(UUID.fromString(readingExamCreationRequest.readingPassageIdPart1()));
            if(readingPassagePart1.isEmpty()){
                throw new IllegalArgumentException("Reading passage part 1 does not exist");
            }
            readingExam.setPart1(readingPassagePart1.get());
        }
        if(!readingExamCreationRequest.readingPassageIdPart2().isEmpty()){
            Optional<ReadingPassage> readingPassagePart2 = readingPassageRepository.findById(UUID.fromString(readingExamCreationRequest.readingPassageIdPart2()));

            if(readingPassagePart2.isEmpty()){
                throw new IllegalArgumentException("Reading passage part 2 does not exist");
            }
            readingExam.setPart2(readingPassagePart2.get());
        }
        if(!readingExamCreationRequest.readingPassageIdPart3().isEmpty()){
            Optional<ReadingPassage> readingPassagePart3 = readingPassageRepository.findById(UUID.fromString(readingExamCreationRequest.readingPassageIdPart3()));

            if(readingPassagePart3.isEmpty()){
                throw new IllegalArgumentException("Reading passage part 3 does not exist");
            }
            readingExam.setPart3(readingPassagePart3.get());
        }
        readingExam.setCreatedBy(userId);
        readingExamRepository.save(readingExam);
        // Create and return the response object
        ReadingExamResponse response = new ReadingExamResponse(
                readingExam.getReadingExamId().toString(),
                readingExam.getExamName(),
                readingExam.getExamDescription(),
                readingExam.getUrlSlug(),
                new ReadingExamResponse.ReadingPassageResponse(
                        readingExam.getPart1().getPassageId().toString(),
                        readingExam.getPart1().getTitle(),
                        readingExam.getPart1().getContent()
                ),
                new ReadingExamResponse.ReadingPassageResponse(
                        readingExam.getPart2() != null ? readingExam.getPart2().getPassageId().toString() : null,
                        readingExam.getPart2() != null ? readingExam.getPart2().getTitle() : null,
                        readingExam.getPart2() != null ? readingExam.getPart2().getContent() : null
                ),
                new ReadingExamResponse.ReadingPassageResponse(
                        readingExam.getPart3() != null ? readingExam.getPart3().getPassageId().toString() : null,
                        readingExam.getPart3() != null ? readingExam.getPart3().getTitle() : null,
                        readingExam.getPart3() != null ? readingExam.getPart3().getContent() : null
                )
        );


        // Implementation logic for creating a reading exam
        return response; // Replace with actual implementation
    }
    @Override
    public ReadingExamResponse updateReadingExam(String readingExamId, ReadingExamCreationRequest readingExamCreationRequest, HttpServletRequest httpServletRequest) throws Exception {
        String userId = helper.getUserIdFromToken(httpServletRequest);
        if (readingExamCreationRequest == null) {
            throw  new AppException(
                    Constants.ErrorCodeMessage.INVALID_INPUT,
                    Constants.ErrorCode.INVALID_INPUT,
                    HttpStatus.NOT_ACCEPTABLE.value()
            );
        }
        // Check if the reading exam exists
        Optional<ReadingExam> existingReadingExam = readingExamRepository.findById(UUID.fromString(readingExamId));
        if (existingReadingExam.isEmpty()) {
            throw new AppException(
                    Constants.ErrorCodeMessage.READING_EXAM_NOT_FOUND,
                    Constants.ErrorCode.READING_EXAM_NOT_FOUND,
                    HttpStatus.NOT_FOUND.value()
            );        }
        ReadingExam readingExam = existingReadingExam.get();
        readingExam.setUpdatedBy(userId);
        readingExam.setExamName(readingExamCreationRequest.readingExamName());
        readingExam.setExamDescription(readingExamCreationRequest.readingExamDescription());
        readingExam.setUrlSlug(readingExamCreationRequest.urlSlung());

        if (!readingExamCreationRequest.readingPassageIdPart1().isEmpty()) {
            Optional<ReadingPassage> readingPassagePart1 = readingPassageRepository.findById(UUID.fromString(readingExamCreationRequest.readingPassageIdPart1()));
            if (readingPassagePart1.isEmpty()) {
                throw new AppException(
                        Constants.ErrorCodeMessage.PASSAGE_NOT_FOUND,
                        Constants.ErrorCode.PASSAGE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                );
            }
            readingExam.setPart1(readingPassagePart1.get());
        }
        if (!readingExamCreationRequest.readingPassageIdPart2().isEmpty()) {
            Optional<ReadingPassage> readingPassagePart2 = readingPassageRepository.findById(UUID.fromString(readingExamCreationRequest.readingPassageIdPart2()));

            if (readingPassagePart2.isEmpty()) {
                throw new AppException(
                        Constants.ErrorCodeMessage.PASSAGE_NOT_FOUND,
                        Constants.ErrorCode.PASSAGE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                );            }
            readingExam.setPart2(readingPassagePart2.get());
        }
        if (!readingExamCreationRequest.readingPassageIdPart3().isEmpty()) {
            Optional<ReadingPassage> readingPassagePart3 = readingPassageRepository.findById(UUID.fromString(readingExamCreationRequest.readingPassageIdPart3()));

            if (readingPassagePart3.isEmpty()) {
                throw new AppException(
                        Constants.ErrorCodeMessage.PASSAGE_NOT_FOUND,
                        Constants.ErrorCode.PASSAGE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value()
                );              }
            readingExam.setPart3(readingPassagePart3.get());
        }
        readingExamRepository.save(readingExam);
        ReadingExamResponse response = new ReadingExamResponse(
                readingExam.getReadingExamId().toString(),
                readingExam.getExamName(),
                readingExam.getExamDescription(),
                readingExam.getUrlSlug(),
                new ReadingExamResponse.ReadingPassageResponse(
                        readingExam.getPart1().getPassageId().toString(),
                        readingExam.getPart1().getTitle(),
                        readingExam.getPart1().getContent()
                ),
                new ReadingExamResponse.ReadingPassageResponse(
                        readingExam.getPart2() != null ? readingExam.getPart2().getPassageId().toString() : null,
                        readingExam.getPart2() != null ? readingExam.getPart2().getTitle() : null,
                        readingExam.getPart2() != null ? readingExam.getPart2().getContent() : null
                ),
                new ReadingExamResponse.ReadingPassageResponse(
                        readingExam.getPart3() != null ? readingExam.getPart3().getPassageId().toString() : null,
                        readingExam.getPart3() != null ? readingExam.getPart3().getTitle() : null,
                        readingExam.getPart3() != null ? readingExam.getPart3().getContent() : null
                )
        );
        return response;
    }


    @Override
    public ReadingExamResponse getReadingExam(String readingExamId, HttpServletRequest httpServletRequest){
        String userId = helper.getUserIdFromToken(httpServletRequest);

        Optional<ReadingExam> readingExamOptional = readingExamRepository.findById(UUID.fromString(readingExamId));
        if(readingExamOptional.isEmpty()){
            throw new AppException(
                    Constants.ErrorCodeMessage.READING_EXAM_NOT_FOUND,
                    Constants.ErrorCode.READING_EXAM_NOT_FOUND,
                    HttpStatus.NOT_FOUND.value()
            );
        }
        ReadingExam readingExam = readingExamOptional.get();

        ReadingExamResponse response = new ReadingExamResponse(
                readingExam.getReadingExamId().toString(),
                readingExam.getExamName(),
                readingExam.getExamDescription(),
                readingExam.getUrlSlug(),
                new ReadingExamResponse.ReadingPassageResponse(
                        readingExam.getPart1().getPassageId().toString(),
                        readingExam.getPart1().getTitle(),
                        readingExam.getPart1().getContent()
                ),
                new ReadingExamResponse.ReadingPassageResponse(
                        readingExam.getPart2() != null ? readingExam.getPart2().getPassageId().toString() : null,
                        readingExam.getPart2() != null ? readingExam.getPart2().getTitle() : null,
                        readingExam.getPart2() != null ? readingExam.getPart2().getContent() : null
                ),
                new ReadingExamResponse.ReadingPassageResponse(
                        readingExam.getPart3() != null ? readingExam.getPart3().getPassageId().toString() : null,
                        readingExam.getPart3() != null ? readingExam.getPart3().getTitle() : null,
                        readingExam.getPart3() != null ? readingExam.getPart3().getContent() : null
                )
        );
        return response;

    }
}
