package br.com.prospectaai.ms_notification.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import br.com.prospectaai.ms_notification.sse.NotificationSseService;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/notification")
@RequiredArgsConstructor
public class SseController {
    private final NotificationSseService sseService;

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return sseService.subscribe();
    }
}
