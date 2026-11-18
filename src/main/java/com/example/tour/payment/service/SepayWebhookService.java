package com.example.tour.payment.service;

import com.example.tour.payment.dto.request.SepayWebhookRequest;

public interface SepayWebhookService {
    void handle(SepayWebhookRequest req);
}
