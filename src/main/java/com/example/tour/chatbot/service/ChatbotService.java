package com.example.tour.chatbot.service;

import com.example.tour.chatbot.dto.request.ChatRequest;
import com.example.tour.chatbot.dto.response.ChatResponse;

public interface ChatbotService {

    ChatResponse chat(ChatRequest request);
}
