package com.fptu.sep490.personalservice.controller;

import com.fptu.sep490.personalservice.model.ChatGroup;
import com.fptu.sep490.personalservice.model.Message;
import com.fptu.sep490.personalservice.model.enumeration.MessageType;
import com.fptu.sep490.personalservice.repository.ChatGroupRepository;
import com.fptu.sep490.personalservice.repository.MessageRepository;
import com.fptu.sep490.personalservice.service.AIService;
import com.fptu.sep490.personalservice.viewmodel.request.ChatMessage;
import com.fptu.sep490.personalservice.viewmodel.response.AIResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@RequestMapping("/chat")

public class ChatController {

    private final MessageRepository messageRepository;
    private final SimpMessageSendingOperations messagingTemplate;
    private final ChatGroupRepository chatGroupRepository;

    private final AIService chatService;

    @MessageMapping("chat.ai")
    public void chatWithAI(@Payload ChatMessage chatMessage,
                           SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();

        // Gọi AI Service để trả lời
        AIResponse aiResponse = chatService.chat(chatMessage.getContent(), sessionId);

        // Gửi phản hồi lại cho client
        ChatMessage aiMessage = ChatMessage.builder()
                .sender("IELTS AI Tutor")
                .content(aiResponse.getContent())
                .messageType(MessageType.CHAT)
                .build();

        messagingTemplate.convertAndSend("/topic/ai." + sessionId, aiMessage);
    }

    /**
     * Khi cần kết thúc chat AI thì clear session
     */
    @MessageMapping("chat.ai.clear")
    public void clearAISession(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        chatService.clearSession(sessionId);
    }
    @GetMapping("/history/{groupId}")
    @ResponseBody
    public List<ChatMessage> getChatHistory(@PathVariable String groupId) throws Exception {

        ChatGroup group = chatGroupRepository.findById(groupId)
                .orElseThrow(() -> new Exception("Group not found: " + groupId));

        List<Message> messages = messageRepository.findByGroupIdOrderByCreatedAtAsc(group);

        List<ChatMessage> chatMessages = messages.stream()
                .map(m -> ChatMessage.builder()
                        .senderId(m.getSenderId())
                        .sender(m.getSenderName())
                        .content(m.getContent())
                        .groupId(group.getId())
                        .build())
                .toList();
        return chatMessages;
    }
    /**
     * Gửi tin nhắn public (chat tổng)
     */
    @MessageMapping("chat.sendPublic")
    public void sendPublic(@Payload ChatMessage chatMessage) {
        // Lưu tin nhắn public vào DB nếu muốn

        Optional<ChatGroup> chatGroup = chatGroupRepository.findByName("Public Chat");
        ChatGroup currentChatGroup = chatGroup.orElse(null);
        if (chatGroup.isEmpty()) {
            // Nếu không có nhóm chat, tạo nhóm chat mặc định
            ChatGroup newChatGroup = new ChatGroup();
            newChatGroup.setName("Public Chat");
            chatGroupRepository.save(newChatGroup);
            currentChatGroup = newChatGroup;
        }
        Message msg = Message.builder()
                .group(currentChatGroup)
                .senderId(chatMessage.getSenderId())
                .senderName(chatMessage.getSender())
                .content(chatMessage.getContent())
                .build();
        messageRepository.save(msg);

        messagingTemplate.convertAndSend("/topic/public", chatMessage);
    }

    /**
     * Thêm user vào chat tổng
     */
    @MessageMapping("chat.addUser")
    public void addUser(@Payload ChatMessage chatMessage,
                        SimpMessageHeaderAccessor headerAccessor) {
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSender());
        headerAccessor.getSessionAttributes().put("userId", chatMessage.getSenderId());

        messagingTemplate.convertAndSend("/topic/public", chatMessage);
    }

    /**
     * Gửi tin nhắn trong group cụ thể
     */
    @MessageMapping("chat.sendGroup")
    public void sendGroup(@Payload ChatMessage chatMessage) throws Exception {
        ChatGroup chatGroup = chatGroupRepository.findById(chatMessage.getGroupId()).
                orElseThrow(() -> new Exception("Group not found: " + chatMessage.getGroupId()));
        // Lưu tin nhắn nhóm vào DB
        Message msg = Message.builder()
                .group(chatGroup)
                .senderId(chatMessage.getSenderId())
                .senderName(chatMessage.getSender())
                .content(chatMessage.getContent())
                .build();
        messageRepository.save(msg);

        // Gửi tới topic của nhóm
        messagingTemplate.convertAndSend("/topic/group." + chatMessage.getGroupId(), chatMessage);
    }

    @MessageMapping("chat.joinGroup")
    public void joinGroup(@Payload ChatMessage chatMessage,
                          SimpMessageHeaderAccessor headerAccessor) {
        headerAccessor.getSessionAttributes().put("groupId", chatMessage.getGroupId());
        messagingTemplate.convertAndSend("/topic/group." + chatMessage.getGroupId(), chatMessage);
    }
    @MessageMapping("chat.getMessages")
    public void getMessages(@Payload ChatMessage chatMessage) throws Exception {
        ChatGroup group = chatGroupRepository.findById(chatMessage.getGroupId())
                .orElseThrow(() -> new Exception("Group not found: " + chatMessage.getGroupId()));

        List<Message> messages = messageRepository.findByGroupIdOrderByCreatedAtAsc(group);

        List<ChatMessage> chatMessages = messages.stream()
                .map(m -> ChatMessage.builder()
                        .senderId(m.getSenderId())
                        .sender(m.getSenderName())
                        .content(m.getContent())
                        .groupId(group.getId())
                        .build())
                .toList();

        messagingTemplate.convertAndSendToUser(
                chatMessage.getSenderId(),
                "/queue/messages." + group.getId(),
                chatMessages
        );
    }

}
