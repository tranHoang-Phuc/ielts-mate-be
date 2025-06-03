package com.fptu.sep490.readingservice.service.impl;
import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.utils.CookieUtils;
import com.fptu.sep490.readingservice.viewmodel.request.AddGroupQuestionRequest;
import com.fptu.sep490.readingservice.constants.Constants;
import com.fptu.sep490.readingservice.model.*;
import com.fptu.sep490.readingservice.model.Question;
import com.fptu.sep490.readingservice.model.QuestionGroup;
import com.fptu.sep490.readingservice.model.enumeration.QuestionCategory;
import com.fptu.sep490.readingservice.model.enumeration.QuestionType;
import com.fptu.sep490.readingservice.repository.QuestionGroupRepository;
import com.fptu.sep490.readingservice.repository.ReadingPassageRepository;
import com.fptu.sep490.readingservice.helper.Helper;
import com.fptu.sep490.readingservice.viewmodel.request.ChoiceCreationRequest;
import com.fptu.sep490.readingservice.model.ReadingPassage;
import com.fptu.sep490.readingservice.service.GroupQuestionService;
import com.fptu.sep490.readingservice.viewmodel.request.QuestionCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.response.AddGroupQuestionResponse;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupQuestionServiceImpl implements GroupQuestionService {

    ReadingPassageRepository readingPassageRepository;
    QuestionGroupRepository questionGroupRepository;

    @Override
    public AddGroupQuestionResponse createGroupQuestion(String passageId, AddGroupQuestionRequest request, HttpServletRequest httpsRequest) throws Exception {
        String userId = getUserIdFromToken(httpsRequest);

        ReadingPassage readingPassage = readingPassageRepository.findById(UUID.fromString(passageId))
                .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.PASSAGE_NOT_FOUND,
                        Constants.ErrorCode.PASSAGE_NOT_FOUND, HttpStatus.NOT_FOUND.value()));
        QuestionGroup group = new QuestionGroup();
        group.setSectionOrder(request.sectionOrder());
        group.setSectionLabel(request.sectionLabel());
        group.setInstruction(request.instruction());
        group.setReadingPassage(readingPassage);

        // Handle group-level drag items
        if (request.dragItems() != null && !request.dragItems().isEmpty()) {
            List<DragItem> listDragItem = new ArrayList<>();
            for (String item : request.dragItems()) {
                DragItem dragItem = new DragItem();
                dragItem.setContent(item);
                dragItem.setQuestionGroup(group);
                listDragItem.add(dragItem);
            }
            group.setDragItems(listDragItem);
        }

        // Handle questions
        List<Question> questions = new ArrayList<>();
        if (request.questions() != null && !request.questions().isEmpty()) {
            for (QuestionCreationRequest questionDto : request.questions()) {
                Question question = new Question();
                question.setQuestionOrder(questionDto.questionOrder());
                question.setPoint(questionDto.point());
                question.setQuestionType(
                        questionDto.questionType() == null ? null : QuestionType.values()[questionDto.questionType()]
                );
                // Map Set<Integer> to Set<QuestionCategory>
                if (questionDto.questionCategories() != null) {
                    Set<QuestionCategory> categories =
                            questionDto.questionCategories().stream()
                                    .filter(java.util.Objects::nonNull)
                                    .map(QuestionCategory::valueOf)
                                    .collect(java.util.stream.Collectors.toSet());
                    question.setCategories(categories);
                } else {
                    question.setCategories(null);
                }
                question.setExplanation(questionDto.explanation());
                question.setNumberOfCorrectAnswers(
                        questionDto.numberOfCorrectAnswers() == null ? 0 : questionDto.numberOfCorrectAnswers()
                );
                question.setInstructionForChoice(questionDto.instructionForChoice());
                question.setBlankIndex(questionDto.blankIndex());
                question.setCorrectAnswer(questionDto.correctAnswer());
                question.setInstructionForMatching(questionDto.instructionForMatching());
                question.setCorrectAnswerForMatching(questionDto.correctAnswerForMatching());
                question.setZoneIndex(questionDto.zoneIndex());
                question.setCreatedBy(userId);
                question.setQuestionGroup(group);

                // Handle choices
                if (questionDto.choices() != null && !questionDto.choices().isEmpty()) {
                    List<Choice> choices = new ArrayList<>();
                    for (QuestionCreationRequest.ChoiceRequest choiceDto : questionDto.choices()) {
                        Choice choice = new Choice();
                        choice.setContent(choiceDto.content());
                        choice.setLabel(choiceDto.label());
                        choice.setCorrect(choiceDto.isCorrect());
                        choice.setQuestion(question);
                        choices.add(choice);
                    }
                    question.setChoices(choices);
                }

                // Handle question-level drag items
                if (questionDto.dragItemId() != null && !questionDto.dragItemId().isEmpty()) {

                        DragItem dragItem = new DragItem();
                        dragItem.setContent(questionDto.dragItemId());
                        dragItem.setQuestion(question);
                        dragItem.setQuestionGroup(group);

                    question.setDragItem(dragItem);
                }

                questions.add(question);
            }
        }
        group.setQuestions(questions);

        // Save the group (cascade saves questions, choices, drag items)
        questionGroupRepository.save(group);
        AddGroupQuestionResponse response = Helper.mapToGroupQuestionResponse(group, request);
        return response;
    }

    private String getUserIdFromToken(HttpServletRequest request) {
        String token = CookieUtils.getCookieValue(request, "Authorization");
        if (token == null || token.isEmpty()) {
            return null;
        }
        try {
            return SecurityContextHolder.getContext().getAuthentication().getName();
        } catch (Exception e) {
            throw new AppException(Constants.ErrorCodeMessage.UNAUTHORIZED, Constants.ErrorCode.UNAUTHORIZED,
                    HttpStatus.UNAUTHORIZED.value());
        }
    }

    @Override
    public List<AddGroupQuestionResponse> getAllQuestionsGroupsOfPassages(String passageId, HttpServletRequest httpsRequest) throws Exception {
//        String userId = getUserIdFromToken(httpsRequest);
//        ReadingPassage readingPassage = readingPassageRepository.findById(UUID.fromString(passageId))
//                .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.PASSAGE_NOT_FOUND,
//                        Constants.ErrorCode.PASSAGE_NOT_FOUND, HttpStatus.NOT_FOUND.value()));
//        List<QuestionGroup> questionGroups = questionGroupRepository.findAllByReadingPassageByPassageId(UUID.fromString(passageId));
//        if (questionGroups.isEmpty()) {
//            throw new AppException(Constants.ErrorCodeMessage.QUESTION_GROUP_NOT_FOUND,
//                    Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND, HttpStatus.NOT_FOUND.value());
//        }
//        return questionGroups.stream()
//                .map(group -> Helper.mapToGroupQuestionResponse(group, new AddGroupQuestionRequest(
//                        group.getSectionOrder(),
//                        group.getSectionLabel(),
//                        group.getInstruction(),
//                        group.getQuestions().stream()
//                                .map(q -> new QuestionCreationRequest(
//                                        q.getQuestionOrder(),
//                                        q.getPoint(),
//                                        q.getQuestionType() != null ? q.getQuestionType().ordinal() : null,
//                                        q.getCategories() != null ? q.getCategories().stream()
//                                                .map(Enum::name)
//                                                .collect(Collectors.toSet()) : null,
//                                        q.getExplanation(),
//                                        q.getNumberOfCorrectAnswers(),
//                                        q.getInstructionForChoice(),
//                                        q.getBlankIndex(),
//                                        q.getCorrectAnswer(),
//                                        q.getInstructionForMatching(),
//                                        q.getCorrectAnswerForMatching(),
//                                        q.getZoneIndex(),
//                                        q.getChoices().stream()
//                                                .map(c -> new ChoiceCreationRequest(c.getLabel(), c.getContent(),c.getChoiceOrder(), c.isCorrect()))
//                                                .toList(),
//                                        q.getDragItem() != null ? q.getDragItem().getContent() : null
//                                )).toList(),
//                        group.getDragItems().stream().map(DragItem::getContent).toList()
//                )))
//                .toList();
        return null;
    }

    @Override
    public AddGroupQuestionResponse updateGroupQuestion(String passageId, String groupId, AddGroupQuestionRequest request, HttpServletRequest httpsRequest) throws Exception {
        String userId = getUserIdFromToken(httpsRequest);
        ReadingPassage readingPassage = readingPassageRepository.findById(UUID.fromString(passageId))
                .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.PASSAGE_NOT_FOUND,
                        Constants.ErrorCode.PASSAGE_NOT_FOUND, HttpStatus.NOT_FOUND.value()));
        QuestionGroup group = questionGroupRepository.findById(UUID.fromString(groupId))
                .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.QUESTION_GROUP_NOT_FOUND,
                        Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND, HttpStatus.NOT_FOUND.value()));

        // Update group properties
        group.setSectionOrder(request.sectionOrder());
        group.setSectionLabel(request.sectionLabel());
        group.setInstruction(request.instruction());
        group.setReadingPassage(readingPassage);

        // Handle group-level drag items
        if (request.dragItems() != null && !request.dragItems().isEmpty()) {
            List<DragItem> listDragItem = new ArrayList<>();
            for (String item : request.dragItems()) {
                DragItem dragItem = new DragItem();
                dragItem.setContent(item);
                dragItem.setQuestionGroup(group);
                listDragItem.add(dragItem);
            }
            group.getDragItems().clear();
            group.getDragItems().addAll(listDragItem);
        } else {
            group.getDragItems().clear();
        }

        // Handle questions
        List<Question> questions = new ArrayList<>();
        if (request.questions() != null && !request.questions().isEmpty()) {
            for (QuestionCreationRequest questionDto : request.questions()) {
                Question question = new Question();
                question.setQuestionOrder(questionDto.questionOrder());
                question.setPoint(questionDto.point());
                question.setQuestionType(
                        questionDto.questionType() == null ? null : QuestionType.values()[questionDto.questionType()]
                );
                // Map Set<Integer> to Set<QuestionCategory>
                if (questionDto.questionCategories() != null) {
                    Set<QuestionCategory> categories =
                            questionDto.questionCategories().stream()
                                    .filter(java.util.Objects::nonNull)
                                    .map(QuestionCategory::valueOf)
                                    .collect(java.util.stream.Collectors.toSet());
                    question.setCategories(categories);
                } else {
                    question.setCategories(null);
                }
                question.setExplanation(questionDto.explanation());
                question.setNumberOfCorrectAnswers(
                        questionDto.numberOfCorrectAnswers() == null ? 0 : questionDto.numberOfCorrectAnswers()
                );
                question.setInstructionForChoice(questionDto.instructionForChoice());
                question.setBlankIndex(questionDto.blankIndex());
                question.setCorrectAnswer(questionDto.correctAnswer());
                question.setInstructionForMatching(questionDto.instructionForMatching());
                question.setCorrectAnswerForMatching(questionDto.correctAnswerForMatching());
                question.setZoneIndex(questionDto.zoneIndex());
                question.setCreatedBy(userId);
                question.setQuestionGroup(group);
                // Handle choices
                if (questionDto.choices() != null && !questionDto.choices().isEmpty()) {
                    List<Choice> choices = new ArrayList<>();
                    for (QuestionCreationRequest.ChoiceRequest choiceDto : questionDto.choices()) {
                        Choice choice = new Choice();
                        choice.setContent(choiceDto.content());
                        choice.setLabel(choiceDto.label());
                        choice.setCorrect(choiceDto.isCorrect());
                        choice.setQuestion(question);
                        choices.add(choice);
                    }
                    question.setChoices(choices);
                } else {
                    question.setChoices(null);
                }
                if (questionDto.dragItemId() != null && !questionDto.dragItemId().isEmpty()) {
                        DragItem dragItem = new DragItem();
                        dragItem.setContent(questionDto.dragItemId());
                        dragItem.setQuestion(question);
                        dragItem.setQuestionGroup(group);

                    question.setDragItem(dragItem);
                } else {
                    question.setDragItem(null);
                }
                questions.add(question);
            }
        }
        group.getQuestions().clear();
        group.getQuestions().addAll(questions);
        // Save the group (cascade saves questions, choices, drag items)
        questionGroupRepository.save(group);
        AddGroupQuestionResponse response = Helper.mapToGroupQuestionResponse(group, request);
        return response;
    }

    @Override
    public void deleteGroupQuestion(String passageId, String groupId, HttpServletRequest httpsRequest) throws Exception {
        String userId = getUserIdFromToken(httpsRequest);
        ReadingPassage readingPassage = readingPassageRepository.findById(UUID.fromString(passageId))
                .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.PASSAGE_NOT_FOUND,
                        Constants.ErrorCode.PASSAGE_NOT_FOUND, HttpStatus.NOT_FOUND.value()));
        QuestionGroup group = questionGroupRepository.findById(UUID.fromString(groupId))
                .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.QUESTION_GROUP_NOT_FOUND,
                        Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND, HttpStatus.NOT_FOUND.value()));
        if (group.getReadingPassage() == null || !group.getReadingPassage().getPassageId().equals(readingPassage.getPassageId())) {
            throw new AppException("Group does not belong to the specified passage",
                    Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND, HttpStatus.NOT_FOUND.value());
        }
        questionGroupRepository.delete(group);
    }




}