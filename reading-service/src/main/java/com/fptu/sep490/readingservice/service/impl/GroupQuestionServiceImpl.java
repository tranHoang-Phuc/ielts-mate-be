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
import com.fptu.sep490.readingservice.model.ReadingPassage;
import com.fptu.sep490.readingservice.service.GroupQuestionService;
import com.fptu.sep490.readingservice.viewmodel.request.ChoiceCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.request.QuestionCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.response.AddGroupQuestionResponse;
import com.fptu.sep490.readingservice.viewmodel.response.QuestionCreationResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GroupQuestionServiceImpl implements GroupQuestionService {

    ReadingPassageRepository readingPassageRepository;
    QuestionGroupRepository questionGroupRepository;
    Helper helper;
    @Override
    public AddGroupQuestionResponse createGroupQuestion(String passageId, AddGroupQuestionRequest request, HttpServletRequest httpsRequest) throws Exception {
        String userId = helper.getUserIdFromToken(httpsRequest);

        ReadingPassage readingPassage = readingPassageRepository.findById(UUID.fromString(passageId))
                .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.PASSAGE_NOT_FOUND,
                        Constants.ErrorCode.PASSAGE_NOT_FOUND, HttpStatus.NOT_FOUND.value()));
        QuestionGroup group = new QuestionGroup();
        group.setSectionOrder(request.sectionOrder());
        group.setSectionLabel(request.sectionLabel());
        group.setInstruction(request.instruction());
        group.setQuestionType(QuestionType.fromValue(request.questionType()));
        group.setReadingPassage(readingPassage);
        group.setCreatedBy(userId);

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



    private QuestionGroup getLatestCurrentGroup(QuestionGroup group) {
        QuestionGroup current = group;

        while (current.getChildren() != null && !current.getChildren().isEmpty()) {
            boolean found = false;
            for (QuestionGroup child : current.getChildren()) {
                if (Boolean.TRUE.equals(child.getIsCurrent()) && Boolean.FALSE.equals(child.getIsDeleted())) {
                    current = child;  // cập nhật current để duyệt tiếp
                    found = true;
                    break; // chỉ lấy 1 nhánh đầu tiên hợp lệ (giống logic cũ)
                }
            }

            if (!found) {
                break; // nếu không có child nào hợp lệ, thoát vòng lặp
            }
        }

        if (Boolean.TRUE.equals(current.getIsCurrent()) && Boolean.FALSE.equals(current.getIsDeleted())) {
            return current;
        }

        return null;
    }


    @Override
    public List<AddGroupQuestionResponse> getAllQuestionsGroupsOfPassages(String passageId, HttpServletRequest httpsRequest) throws Exception {
        String userId = helper.getUserIdFromToken(httpsRequest);

        ReadingPassage readingPassage = readingPassageRepository.findById(UUID.fromString(passageId))
                .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.PASSAGE_NOT_FOUND,
                        Constants.ErrorCode.PASSAGE_NOT_FOUND, HttpStatus.NOT_FOUND.value()));

        List<QuestionGroup> questionGroups = questionGroupRepository.findAllByReadingPassageByPassageId(UUID.fromString(passageId));
        List<QuestionGroup> result = new ArrayList<>();

        for (QuestionGroup group : questionGroups) {
            QuestionGroup latest = getLatestCurrentGroup(group);
            if (latest != null) {
                result.add(latest);
            }
        }

        if (result.isEmpty()) {
            throw new AppException(Constants.ErrorCodeMessage.QUESTION_GROUP_NOT_FOUND,
                    Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND, HttpStatus.NOT_FOUND.value());
        }

        List<AddGroupQuestionResponse>  final_result = result.stream()
                .map(group -> Helper.mapToGroupQuestionResponse1(
                        group,
                        new AddGroupQuestionResponse(
                                group.getGroupId().toString(),
                                group.getSectionOrder(),
                                group.getSectionLabel(),
                                group.getInstruction(),
                                group.getQuestionType().ordinal(),
                                group.getQuestions().stream()
                                        .map(q -> {
                                            String dragItemId = null;
                                            String dragItemContent = null;

                                            if (q.getDragItem() != null) {
                                                dragItemId = q.getDragItem().getDragItemId() != null
                                                        ? q.getDragItem().getDragItemId().toString()
                                                        : null;

                                                DragItem currentDragItem = getCurrentDragItem(q.getDragItem());
                                                dragItemContent = currentDragItem != null
                                                        ? currentDragItem.getContent()
                                                        : null;
                                            }

                                            return new QuestionCreationRequest(
                                                    q.getQuestionOrder(),
                                                    q.getPoint(),
                                                    q.getQuestionType() != null ? q.getQuestionType().ordinal() : null,
                                                    group.getGroupId().toString(),
                                                    q.getCategories() != null
                                                            ? q.getCategories().stream()
                                                            .map(Enum::name)
                                                            .toList()
                                                            : null,
                                                    q.getExplanation(),
                                                    q.getNumberOfCorrectAnswers(),
                                                    q.getInstructionForChoice(),
                                                    q.getChoices().stream()
                                                            .map(c -> new QuestionCreationRequest.ChoiceRequest(
                                                                    c.getLabel(),
                                                                    c.getContent(),
                                                                    c.getChoiceOrder(),
                                                                    c.isCorrect()))
                                                            .toList(),
                                                    q.getBlankIndex(),
                                                    q.getCorrectAnswer(),
                                                    q.getInstructionForMatching(),
                                                    q.getCorrectAnswerForMatching(),
                                                    q.getZoneIndex(),
                                                    dragItemId,
                                                    dragItemContent
                                            );
                                        })
                                        .toList(),
                                group.getDragItems().stream()
                                        .filter(d -> Boolean.TRUE.equals(d.getIsCurrent()) && Boolean.FALSE.equals(d.getIsDeleted()))
                                        .map(d -> QuestionCreationResponse.DragItemResponse.builder()
                                                .dragItemId(d.getDragItemId().toString())
                                                .content(d.getContent())
                                                .isCurrent(d.getIsCurrent())
                                                .build())
                                        .toList()


                        )
                ))
                .toList();



        return final_result;
    }


    public DragItem getCurrentDragItem(DragItem dragItem){
        if(dragItem.getIsCurrent() && !dragItem.getIsDeleted()) {
            return dragItem;
        }
        for(DragItem item: dragItem.getChildren()) {
            if (item.getIsCurrent() && !item.getIsDeleted()) {
                return getCurrentDragItem(item);
            }
        }
        return null;



    }


    @Override
    public AddGroupQuestionResponse updateGroupQuestion(String passageId, String groupId, AddGroupQuestionRequest request, HttpServletRequest httpsRequest) throws Exception {
        String userId = helper.getUserIdFromToken(httpsRequest);

        ReadingPassage readingPassage = readingPassageRepository.findById(UUID.fromString(passageId))
                .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.PASSAGE_NOT_FOUND,
                        Constants.ErrorCode.PASSAGE_NOT_FOUND, HttpStatus.NOT_FOUND.value()));

        QuestionGroup originalGroup = questionGroupRepository.findById(UUID.fromString(groupId))
                .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.QUESTION_GROUP_NOT_FOUND,
                        Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND, HttpStatus.NOT_FOUND.value()));

        // Find the latest current, not deleted version among originalGroup's children (or itself)
        QuestionGroup latestCurrentGroup = getLatestCurrentGroup(originalGroup);
        if (latestCurrentGroup == null) {
            throw new AppException(Constants.ErrorCodeMessage.QUESTION_GROUP_NOT_FOUND,
                    Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND, HttpStatus.NOT_FOUND.value());
        }

        // Mark the current group as not current
        latestCurrentGroup.setIsCurrent(false);

        // Create new group as a new version (child of originalGroup)
        QuestionGroup newGroup = new QuestionGroup();
        newGroup.setReadingPassage(readingPassage);
        newGroup.setCreatedBy(userId);
        newGroup.setIsCurrent(true);
        newGroup.setIsDeleted(false);
        newGroup.setIsOriginal(false);
        newGroup.setVersion(latestCurrentGroup.getVersion() + 1);
        newGroup.setParent(originalGroup);

        // Only update fields present in request, otherwise copy from previous version
        newGroup.setSectionOrder(request.sectionOrder() != null ? request.sectionOrder() : latestCurrentGroup.getSectionOrder());
        newGroup.setSectionLabel(request.sectionLabel() != null ? request.sectionLabel() : latestCurrentGroup.getSectionLabel());
        newGroup.setInstruction(request.instruction() != null ? request.instruction() : latestCurrentGroup.getInstruction());

        // Handle group-level drag items
        List<DragItem> groupDragItems = new ArrayList<>();
        if (request.dragItems() != null) {
            for (String item : request.dragItems()) {
                DragItem dragItem = new DragItem();
                dragItem.setContent(item);
                dragItem.setQuestionGroup(newGroup);
                groupDragItems.add(dragItem);
            }
        } else {
            for (DragItem oldItem : latestCurrentGroup.getDragItems()) {
                DragItem dragItem = new DragItem();
                dragItem.setContent(oldItem.getContent());
                dragItem.setQuestionGroup(newGroup);
                groupDragItems.add(dragItem);
            }
        }
        newGroup.setDragItems(groupDragItems);

        // Handle questions
        List<Question> questions = new ArrayList<>();
        if (request.questions() != null) {
            for (QuestionCreationRequest questionDto : request.questions()) {
                Question question = new Question();
                question.setQuestionOrder(questionDto.questionOrder());
                question.setPoint(questionDto.point());
                question.setQuestionType(
                        questionDto.questionType() == null ? null : QuestionType.values()[questionDto.questionType()]
                );
                if (questionDto.questionCategories() != null) {
                    Set<QuestionCategory> categories =
                            questionDto.questionCategories().stream()
                                    .filter(java.util.Objects::nonNull)
                                    .map(QuestionCategory::valueOf)
                                    .collect(Collectors.toSet());
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
                question.setQuestionGroup(newGroup);

                // Handle choices
                List<Choice> choices = new ArrayList<>();
                if (questionDto.choices() != null) {
                    for (QuestionCreationRequest.ChoiceRequest choiceDto : questionDto.choices()) {
                        Choice choice = new Choice();
                        choice.setContent(choiceDto.content());
                        choice.setLabel(choiceDto.label());
                        choice.setCorrect(choiceDto.isCorrect());
                        choice.setQuestion(question);
                        choices.add(choice);
                    }
                }
                question.setChoices(choices);

                // Handle question-level drag item
                if (questionDto.dragItemId() != null && !questionDto.dragItemId().isEmpty()) {
                    DragItem dragItem = new DragItem();
                    dragItem.setContent(questionDto.dragItemId());
                    dragItem.setQuestion(question);
                    dragItem.setQuestionGroup(newGroup);
                    question.setDragItem(dragItem);
                } else {
                    question.setDragItem(null);
                }
                questions.add(question);
            }
        } else {
            for (Question oldQ : latestCurrentGroup.getQuestions()) {
                Question question = new Question();
                question.setQuestionOrder(oldQ.getQuestionOrder());
                question.setPoint(oldQ.getPoint());
                question.setQuestionType(oldQ.getQuestionType());
                question.setCategories(oldQ.getCategories());
                question.setExplanation(oldQ.getExplanation());
                question.setNumberOfCorrectAnswers(oldQ.getNumberOfCorrectAnswers());
                question.setInstructionForChoice(oldQ.getInstructionForChoice());
                question.setBlankIndex(oldQ.getBlankIndex());
                question.setCorrectAnswer(oldQ.getCorrectAnswer());
                question.setInstructionForMatching(oldQ.getInstructionForMatching());
                question.setCorrectAnswerForMatching(oldQ.getCorrectAnswerForMatching());
                question.setZoneIndex(oldQ.getZoneIndex());
                question.setCreatedBy(userId);
                question.setQuestionGroup(newGroup);

                // Copy choices
                List<Choice> choices = new ArrayList<>();
                for (Choice oldC : oldQ.getChoices()) {
                    Choice choice = new Choice();
                    choice.setContent(oldC.getContent());
                    choice.setLabel(oldC.getLabel());
                    choice.setCorrect(oldC.isCorrect());
                    choice.setQuestion(question);
                    choices.add(choice);
                }
                question.setChoices(choices);

                // Copy drag item
                if (oldQ.getDragItem() != null) {
                    DragItem dragItem = new DragItem();
                    dragItem.setContent(oldQ.getDragItem().getContent());
                    dragItem.setQuestion(question);
                    dragItem.setQuestionGroup(newGroup);
                    question.setDragItem(dragItem);
                } else {
                    question.setDragItem(null);
                }
                questions.add(question);
            }
        }
        newGroup.setQuestions(questions);

        // Add newGroup to originalGroup's children
        originalGroup.getChildren().add(newGroup);

        // Save both parent and new group
        questionGroupRepository.save(originalGroup);
        QuestionGroup savedGroup = questionGroupRepository.save(newGroup);

        return Helper.mapToGroupQuestionResponse(savedGroup, request);
    }
    @Override
    public void deleteGroupQuestion(String passageId, String groupId, HttpServletRequest httpsRequest) throws Exception {
        String userId = helper.getUserIdFromToken(httpsRequest);
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