package com.dhobigo.backend.service;

import com.dhobigo.backend.dto.MessageDtos.MessageResponse;
import com.dhobigo.backend.exception.ApiException;
import com.dhobigo.backend.model.Message;
import com.dhobigo.backend.model.Order;
import com.dhobigo.backend.model.Role;
import com.dhobigo.backend.model.User;
import com.dhobigo.backend.repository.MessageRepository;
import com.dhobigo.backend.repository.OrderRepository;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final OrderRepository orderRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageService(MessageRepository messageRepository, OrderRepository orderRepository, SimpMessagingTemplate messagingTemplate) {
        this.messageRepository = messageRepository;
        this.orderRepository = orderRepository;
        this.messagingTemplate = messagingTemplate;
    }

    public List<MessageResponse> getMessages(User requester, Long orderId) {
        Order order = findOrderOrThrow(orderId);
        assertParticipant(order, requester);
        return messageRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MessageResponse send(User sender, Long orderId, String content) {
        Order order = findOrderOrThrow(orderId);
        assertParticipant(order, sender);

        Message message = messageRepository.save(
                Message.builder().order(order).sender(sender).content(content).build()
        );

        MessageResponse response = toResponse(message);
        // Push to anyone subscribed to this order's chat right now —
        // StompAuthInterceptor already ensured only the customer, the
        // assigned dhobi, or an admin could have subscribed.
        messagingTemplate.convertAndSend("/topic/orders/" + orderId + "/messages", response);
        return response;
    }

    private void assertParticipant(Order order, User user) {
        boolean isCustomer = order.getCustomer().getId().equals(user.getId());
        boolean isDhobi = order.getDhobi() != null && order.getDhobi().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == Role.ADMIN;
        if (!isCustomer && !isDhobi && !isAdmin) {
            throw new ApiException("You don't have access to this order's chat", HttpStatus.FORBIDDEN);
        }
    }

    private Order findOrderOrThrow(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ApiException("Order not found", HttpStatus.NOT_FOUND));
    }

    private MessageResponse toResponse(Message m) {
        return new MessageResponse(
                m.getId(),
                m.getOrder().getId(),
                m.getSender().getId(),
                m.getSender().getFullName(),
                m.getSender().getRole().name(),
                m.getContent(),
                m.getCreatedAt()
        );
    }
}
