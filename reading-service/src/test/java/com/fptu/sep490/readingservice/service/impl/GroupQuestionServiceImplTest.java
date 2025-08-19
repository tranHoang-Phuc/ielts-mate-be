package com.fptu.sep490.readingservice.service.impl;

import com.fptu.sep490.commonlibrary.exceptions.AppException;
import com.fptu.sep490.commonlibrary.redis.RedisService;
import com.fptu.sep490.readingservice.constants.Constants;
import com.fptu.sep490.readingservice.model.*;
import com.fptu.sep490.readingservice.model.enumeration.QuestionCategory;
import com.fptu.sep490.readingservice.model.enumeration.QuestionType;
import com.fptu.sep490.readingservice.repository.ChoiceRepository;
import com.fptu.sep490.readingservice.repository.QuestionGroupRepository;
import com.fptu.sep490.readingservice.repository.QuestionRepository;
import com.fptu.sep490.readingservice.repository.ReadingPassageRepository;
import com.fptu.sep490.readingservice.helper.Helper;
import com.fptu.sep490.readingservice.repository.client.KeyCloakTokenClient;
import com.fptu.sep490.readingservice.repository.client.KeyCloakUserClient;
import com.fptu.sep490.readingservice.viewmodel.request.AddGroupQuestionRequest;
import com.fptu.sep490.readingservice.viewmodel.request.QuestionCreationRequest;
import com.fptu.sep490.readingservice.viewmodel.response.AddGroupQuestionResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GroupQuestionServiceImplTest {

    @Mock
    ReadingPassageRepository readingPassageRepository;

    @Mock
    QuestionGroupRepository questionGroupRepository;

    @Mock
    ChoiceRepository choiceRepository;

    @Mock
    QuestionRepository questionRepository;

    @Mock
    KeyCloakTokenClient keyCloakTokenClient;
    @Mock
    KeyCloakUserClient keyCloakUserClient;
    @Mock
    RedisService redisService;

    @Mock
    Helper helper;

    @InjectMocks
    private ChoiceServiceImpl service;

    @Mock
    private HttpServletRequest httpServletRequest;

    @InjectMocks
    GroupQuestionServiceImpl groupQuestionService;
    private ReadingPassage passage;
    private QuestionGroup originalGroup;
    private QuestionGroup latestGroup;
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        passage = new ReadingPassage();
        passage.setPassageId(UUID.randomUUID());

        originalGroup = new QuestionGroup();
        originalGroup.setGroupId(UUID.randomUUID());
        originalGroup.setChildren(new ArrayList<>());

        latestGroup = new QuestionGroup();
        latestGroup.setVersion(1);
        latestGroup.setIsCurrent(true);
        latestGroup.setQuestions(new ArrayList<>());
        latestGroup.setDragItems(new ArrayList<>());

        // Mock getLatestCurrentGroup behavior
        service = spy(new ChoiceServiceImpl(
                choiceRepository,
                questionRepository,
                keyCloakTokenClient,
                keyCloakUserClient,
                redisService
        ));
    }

    @Test
    void createGroupQuestion_success() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");

        UUID passageId = UUID.randomUUID();

        // Mock ReadingPassage
        ReadingPassage passage = ReadingPassage.builder()
                .passageId(passageId)
                .title("Sample Passage")
                .content("Some content")
                .build();
        when(readingPassageRepository.findById(passageId)).thenReturn(Optional.of(passage));

        // Build request
        AddGroupQuestionRequest request = new AddGroupQuestionRequest(
                1,
                "Section A",
                "Read carefully",
                1, // questionType
                List.of(
                        new QuestionCreationRequest(
                                1,                      // questionOrder
                                5,                      // point
                                1,                      // questionType
                                null,                   // questionGroupId
                                List.of("MULTIPLE_CHOICE"), // questionCategories
                                "Explain this question",          // explanation
                                1,                      // numberOfCorrectAnswers
                                "Instruction",          // instructionForChoice
                                List.of(
                                        new QuestionCreationRequest.ChoiceRequest("A", "Content A", 2, true)
                                ),                      // choices
                                null,                   // blankIndex
                                null,                   // correctAnswer
                                null,                   // instructionForMatching
                                null,                   // correctAnswerForMatching
                                null,                   // zoneIndex
                                "drag-question-1",      // dragItemId
                                null                    // dragItemContent
                        )

                ),
                List.of("drag1", "drag2")

                );

        // Call service
        AddGroupQuestionResponse response = groupQuestionService.createGroupQuestion(passageId.toString(), request, req);

        // Assertions
        assertNotNull(response);
        assertEquals("Section A", response.sectionLabel());
        assertEquals(1, response.sectionOrder());
        assertEquals("Read carefully", response.instruction());
        assertEquals(1, response.questions().size());

        var questionResp = response.questions().get(0);
        assertEquals(1, questionResp.questionOrder());
        assertEquals(5, questionResp.point());
        assertEquals(1, questionResp.choices().size());
        assertEquals("Content A", questionResp.choices().get(0).content());
        assertTrue(questionResp.choices().get(0).isCorrect());

        // Verify repository save
        verify(questionGroupRepository).save(any(QuestionGroup.class));
    }

    @Test
    void createGroupQuestion_passageNotFound_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");

        UUID passageId = UUID.randomUUID();
        when(readingPassageRepository.findById(passageId)).thenReturn(Optional.empty());

        AddGroupQuestionRequest request = mock(AddGroupQuestionRequest.class);

        assertThrows(AppException.class,
                () -> groupQuestionService.createGroupQuestion(passageId.toString(), request, req));
    }
    @Test
    void getLatestCurrentGroup_returnsLatestCurrent() throws Exception {
        // Prepare group hierarchy
        QuestionGroup parent = new QuestionGroup();
        parent.setIsCurrent(false);
        parent.setIsDeleted(false);

        QuestionGroup child1 = new QuestionGroup();
        child1.setIsCurrent(true);
        child1.setIsDeleted(false);

        QuestionGroup child2 = new QuestionGroup();
        child2.setIsCurrent(true);
        child2.setIsDeleted(false);

        // Set children hierarchy
        parent.setChildren(List.of(child1));
        child1.setChildren(List.of(child2));

        // Access private method via reflection
        Method method = GroupQuestionServiceImpl.class.getDeclaredMethod("getLatestCurrentGroup", QuestionGroup.class);
        method.setAccessible(true);

        QuestionGroup result = (QuestionGroup) method.invoke(groupQuestionService, parent);

        // Verify that the latest current child is returned
        assertNotNull(result);
        assertEquals(child2, result);
    }

    @Test
    void getLatestCurrentGroup_noCurrent_returnsNull() throws Exception {
        QuestionGroup parent = new QuestionGroup();
        parent.setIsCurrent(false);
        parent.setIsDeleted(false);

        QuestionGroup child = new QuestionGroup();
        child.setIsCurrent(false);
        child.setIsDeleted(false);

        parent.setChildren(List.of(child));

        Method method = GroupQuestionServiceImpl.class.getDeclaredMethod("getLatestCurrentGroup", QuestionGroup.class);
        method.setAccessible(true);

        QuestionGroup result = (QuestionGroup) method.invoke(groupQuestionService, parent);

        assertNull(result);
    }

    @Test
    void getLatestCurrentGroup_currentDeleted_returnsNull() throws Exception {
        QuestionGroup group = new QuestionGroup();
        group.setIsCurrent(true);
        group.setIsDeleted(true); // deleted

        Method method = GroupQuestionServiceImpl.class.getDeclaredMethod("getLatestCurrentGroup", QuestionGroup.class);
        method.setAccessible(true);

        QuestionGroup result = (QuestionGroup) method.invoke(groupQuestionService, group);

        assertNull(result);
    }
    @Test
    void getAllQuestionsGroupsOfPassages_passageNotFound_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        UUID passageId = UUID.randomUUID();
        when(helper.getUserIdFromToken(req)).thenReturn("user1");
        when(readingPassageRepository.findById(passageId)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () ->
                groupQuestionService.getAllQuestionsGroupsOfPassages(passageId.toString(), req)
        );
        assertEquals(Constants.ErrorCode.PASSAGE_NOT_FOUND, ex.getBusinessErrorCode());
    }

    @Test
    void getAllQuestionsGroupsOfPassages_noLatestGroup_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        UUID passageId = UUID.randomUUID();
        when(helper.getUserIdFromToken(req)).thenReturn("user1");

        ReadingPassage passage = new ReadingPassage();
        when(readingPassageRepository.findById(passageId)).thenReturn(Optional.of(passage));

        // Parent group with children that are all not current → getLatestCurrentGroup returns null
        QuestionGroup childGroup = new QuestionGroup();
        childGroup.setIsCurrent(false);
        childGroup.setIsDeleted(false);

        QuestionGroup parentGroup = new QuestionGroup();
        parentGroup.setChildren(List.of(childGroup));
        parentGroup.setIsCurrent(false);
        parentGroup.setIsDeleted(false);

        when(questionGroupRepository.findAllByReadingPassageByPassageId(passageId))
                .thenReturn(List.of(parentGroup));

        AppException ex = assertThrows(AppException.class, () ->
                groupQuestionService.getAllQuestionsGroupsOfPassages(passageId.toString(), req)
        );

        assertEquals(Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND, ex.getBusinessErrorCode());
    }


    @Test
    void getAllQuestionsGroupsOfPassages_withValidGroups_success() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        UUID passageId = UUID.randomUUID();
        when(helper.getUserIdFromToken(req)).thenReturn("user1");

        // Mock ReadingPassage
        ReadingPassage passage = new ReadingPassage();
        when(readingPassageRepository.findById(passageId)).thenReturn(Optional.of(passage));

        // Parent group
        QuestionGroup parentGroup = new QuestionGroup();
        parentGroup.setGroupId(UUID.randomUUID());
        parentGroup.setSectionOrder(1);
        parentGroup.setSectionLabel("Section A");
        parentGroup.setInstruction("Read carefully");
        parentGroup.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        parentGroup.setIsCurrent(true);
        parentGroup.setIsDeleted(false);

        // Question with choices
        Question question = new Question();
        question.setQuestionOrder(1);
        question.setPoint(5);
        question.setQuestionType(QuestionType.MULTIPLE_CHOICE);
        question.setCategories(Set.of(QuestionCategory.MULTIPLE_CHOICE));
        Choice choice = new Choice();
        choice.setLabel("A");
        choice.setContent("Content A");
        choice.setChoiceOrder(1);
        choice.setCorrect(true);
        question.setChoices(List.of(choice));
        question.setQuestionGroup(parentGroup);

        parentGroup.setQuestions(List.of(question));
        parentGroup.setChildren(null); // no children, so parent is latest current group

        when(questionGroupRepository.findAllByReadingPassageByPassageId(passageId))
                .thenReturn(List.of(parentGroup));

        // Call service directly
        List<AddGroupQuestionResponse> responses =
                groupQuestionService.getAllQuestionsGroupsOfPassages(passageId.toString(), req);

        // Assertions
        assertNotNull(responses);
        assertEquals(1, responses.size());
        AddGroupQuestionResponse groupResp = responses.get(0);
        assertEquals("Section A", groupResp.sectionLabel());
        assertEquals(1, groupResp.questions().size());

        QuestionCreationRequest questionResp = groupResp.questions().get(0);
        assertEquals(1, questionResp.questionOrder());
        assertEquals(5, questionResp.point());
        assertEquals(1, questionResp.choices().size());
        assertTrue(questionResp.choices().get(0).isCorrect());

        // Verify repository call
        verify(questionGroupRepository).findAllByReadingPassageByPassageId(passageId);
    }

    @Test
    void getCurrentDragItem_variousCases() {
        // Case: root is current
        DragItem root = new DragItem();
        root.setIsCurrent(true);
        root.setIsDeleted(false);
        root.setChildren(new ArrayList<>());

        assertEquals(root, groupQuestionService.getCurrentDragItem(root));

        // Case: child is current
        DragItem parent = new DragItem();
        parent.setIsCurrent(false);
        parent.setIsDeleted(false);

        DragItem child = new DragItem();
        child.setIsCurrent(true);
        child.setIsDeleted(false);
        child.setChildren(new ArrayList<>());

        parent.setChildren(List.of(child));
        assertEquals(child, groupQuestionService.getCurrentDragItem(parent));
    }
    @Test
    void updateGroupQuestion_success() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");

        UUID passageId = passage.getPassageId();
        UUID groupId = originalGroup.getGroupId();

        when(readingPassageRepository.findById(passageId)).thenReturn(Optional.of(passage));
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(originalGroup));
        when(questionGroupRepository.save(any(QuestionGroup.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Build request
        AddGroupQuestionRequest request = new AddGroupQuestionRequest(
                2,
                "Updated Section",
                "Updated instruction",
                1,
                List.of(
                        new QuestionCreationRequest(
                                1,
                                5,
                                1,
                                null,
                                List.of("MULTIPLE_CHOICE"),
                                "Updated explanation",
                                1,
                                "Updated instruction for choice",
                                List.of(
                                        new QuestionCreationRequest.ChoiceRequest("A", "Updated Content", 1, true)
                                ),
                                null,
                                null,
                                null,
                                null,
                                null,
                                "drag-question-updated",
                                null
                        )
                ),
                List.of("drag1-updated", "drag2-updated")
        );

        AddGroupQuestionResponse response = groupQuestionService.updateGroupQuestion(
                passageId.toString(),
                groupId.toString(),
                request,
                req
        );

        assertNotNull(response);
        assertEquals("Updated Section", response.sectionLabel());
        assertEquals(2, response.sectionOrder());
        assertEquals("Updated instruction", response.instruction());
        assertEquals(1, response.questions().size());

        var questionResp = response.questions().get(0);
        assertEquals(1, questionResp.questionOrder());
        assertEquals(5, questionResp.point());
        assertEquals(1, questionResp.choices().size());
        assertEquals("Updated Content", questionResp.choices().get(0).content());
        assertTrue(questionResp.choices().get(0).isCorrect());

        // Verify repository saves
        verify(questionGroupRepository, times(2)).save(any(QuestionGroup.class));
    }

    @Test
    void updateGroupQuestion_passageNotFound_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");

        UUID passageId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        when(readingPassageRepository.findById(passageId)).thenReturn(Optional.empty());

        AddGroupQuestionRequest request = mock(AddGroupQuestionRequest.class);

        assertThrows(AppException.class,
                () -> groupQuestionService.updateGroupQuestion(
                        passageId.toString(),
                        groupId.toString(),
                        request,
                        req
                ));
    }

    @Test
    void updateGroupQuestion_groupNotFound_throws() {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(helper.getUserIdFromToken(req)).thenReturn("user1");

        UUID passageId = passage.getPassageId();
        UUID groupId = UUID.randomUUID();

        when(readingPassageRepository.findById(passageId)).thenReturn(Optional.of(passage));
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.empty());

        AddGroupQuestionRequest request = mock(AddGroupQuestionRequest.class);

        assertThrows(AppException.class,
                () -> groupQuestionService.updateGroupQuestion(
                        passageId.toString(),
                        groupId.toString(),
                        request,
                        req
                ));
    }
    @Test
    void updateGroupQuestion_copyOldQuestions_withNullQuestions_success() throws Exception {
        HttpServletRequest requestMock = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID passageId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        when(helper.getUserIdFromToken(requestMock)).thenReturn(userId);

        // Original passage
        ReadingPassage passage = new ReadingPassage();
        passage.setPassageId(passageId);
        when(readingPassageRepository.findById(passageId)).thenReturn(Optional.of(passage));

        // Original group
        QuestionGroup originalGroup = new QuestionGroup();
        originalGroup.setGroupId(groupId);
        originalGroup.setChildren(new ArrayList<>());

        // Existing current group with questions
        QuestionGroup latestCurrentGroup = new QuestionGroup();
        latestCurrentGroup.setVersion(1);
        latestCurrentGroup.setIsCurrent(true);
        latestCurrentGroup.setDragItems(Collections.emptyList());

        Question oldQuestion = new Question();
        oldQuestion.setQuestionOrder(1);
        oldQuestion.setPoint(5);

        Choice newChoice = new Choice();
        newChoice.setContent("Label A");
        newChoice.setLabel("A");
        newChoice.setCorrect(true);
        newChoice.setChoiceOrder(1);

        oldQuestion.setChoices(Collections.singletonList(newChoice));

        DragItem newDragItem = new DragItem();
        newDragItem.setContent("drag1");
        newDragItem.setIsCurrent(true);
        newDragItem.setIsDeleted(false);
        newDragItem.setQuestion(oldQuestion);

        oldQuestion.setDragItem(newDragItem);
        latestCurrentGroup.setQuestions(Collections.singletonList(oldQuestion));
        originalGroup.getChildren().add(latestCurrentGroup);

        Map<UUID, QuestionGroup> store = new HashMap<>();
        store.put(groupId, originalGroup);

        when(questionGroupRepository.findById(any(UUID.class)))
                .thenAnswer(invocation -> Optional.ofNullable(store.get(invocation.getArgument(0))));

        when(questionGroupRepository.save(any(QuestionGroup.class)))
                .thenAnswer(invocation -> {
                    QuestionGroup group = invocation.getArgument(0);
                    if (group.getGroupId() == null) group.setGroupId(UUID.randomUUID());
                    if (group.getDragItems() == null) group.setDragItems(new ArrayList<>());
                    store.put(group.getGroupId(), group);
                    return group;
                });

        // Request with null questions list
        AddGroupQuestionRequest request = new AddGroupQuestionRequest(
                null, null, null,
                QuestionType.MULTIPLE_CHOICE.ordinal(),
                null, // questions is null
                List.of("drag1-updated", "drag2-updated")
        );

        AddGroupQuestionResponse response = groupQuestionService.updateGroupQuestion(
                passageId.toString(),
                groupId.toString(),
                request,
                requestMock
        );

        assertNotNull(response);

        // Verify latestCurrentGroup was marked not current
        assertFalse(latestCurrentGroup.getIsCurrent());

        // Verify new group created as current
        String newGroupId = response.groupId();
        QuestionGroup newGroup = questionGroupRepository.findById(UUID.fromString(newGroupId))
                .orElseThrow(() -> new AppException(Constants.ErrorCodeMessage.QUESTION_GROUP_NOT_FOUND,
                        Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND, HttpStatus.NOT_FOUND.value()));

        assertTrue(newGroup.getIsCurrent());
        assertEquals(2, newGroup.getVersion());

        // Verify old question copied
        assertEquals(1, newGroup.getQuestions().size());
        Question copiedQuestion = newGroup.getQuestions().get(0);
        assertEquals(oldQuestion.getQuestionOrder(), copiedQuestion.getQuestionOrder());
        assertEquals(oldQuestion.getPoint(), copiedQuestion.getPoint());

        // Verify choices copied
        assertEquals(1, copiedQuestion.getChoices().size());
        Choice copiedChoice = copiedQuestion.getChoices().get(0);
        assertEquals("Label A", copiedChoice.getContent());
        assertEquals("A", copiedChoice.getLabel());
        assertTrue(copiedChoice.isCorrect());

        // Verify dragItem copied
        assertNotNull(copiedQuestion.getDragItem());
        assertEquals("drag1", copiedQuestion.getDragItem().getContent());
    }


    @Test
    void deleteGroupQuestion_success() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        String userId = "user1";
        UUID passageId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        ReadingPassage passage = new ReadingPassage();
        passage.setPassageId(passageId);

        QuestionGroup group = new QuestionGroup();
        group.setGroupId(groupId);
        group.setReadingPassage(passage);

        when(helper.getUserIdFromToken(request)).thenReturn(userId);
        when(readingPassageRepository.findById(passageId)).thenReturn(Optional.of(passage));
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

        // Call the method
        groupQuestionService.deleteGroupQuestion(passageId.toString(), groupId.toString(), request);

        // Verify delete was called
        verify(questionGroupRepository, times(1)).delete(group);
    }

    @Test
    void deleteGroupQuestion_passageNotFound_throwsException() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        UUID passageId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        when(helper.getUserIdFromToken(request)).thenReturn("user1");
        when(readingPassageRepository.findById(passageId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> groupQuestionService.deleteGroupQuestion(passageId.toString(), groupId.toString(), request));
        assertEquals(HttpStatus.NOT_FOUND.value(), exception.getHttpStatusCode());
        assertEquals(Constants.ErrorCode.PASSAGE_NOT_FOUND, exception.getBusinessErrorCode());
    }

    @Test
    void deleteGroupQuestion_groupNotFound_throwsException() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        UUID passageId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        ReadingPassage passage = new ReadingPassage();
        passage.setPassageId(passageId);

        when(helper.getUserIdFromToken(request)).thenReturn("user1");
        when(readingPassageRepository.findById(passageId)).thenReturn(Optional.of(passage));
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class,
                () -> groupQuestionService.deleteGroupQuestion(passageId.toString(), groupId.toString(), request));
        assertEquals(HttpStatus.NOT_FOUND.value(), exception.getHttpStatusCode());
        assertEquals(Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND, exception.getBusinessErrorCode());
    }

    @Test
    void deleteGroupQuestion_groupNotMatchingPassage_throwsException() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        UUID passageId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        ReadingPassage passage = new ReadingPassage();
        passage.setPassageId(passageId);

        ReadingPassage otherPassage = new ReadingPassage();
        otherPassage.setPassageId(UUID.randomUUID());

        QuestionGroup group = new QuestionGroup();
        group.setGroupId(groupId);
        group.setReadingPassage(otherPassage);

        when(helper.getUserIdFromToken(request)).thenReturn("user1");
        when(readingPassageRepository.findById(passageId)).thenReturn(Optional.of(passage));
        when(questionGroupRepository.findById(groupId)).thenReturn(Optional.of(group));

        AppException exception = assertThrows(AppException.class,
                () -> groupQuestionService.deleteGroupQuestion(passageId.toString(), groupId.toString(), request));
        assertEquals(HttpStatus.NOT_FOUND.value(), exception.getHttpStatusCode());
        assertEquals(Constants.ErrorCode.QUESTION_GROUP_NOT_FOUND, exception.getBusinessErrorCode());
    }




}
