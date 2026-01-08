package br.com.prospectaai.ms_notification.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import br.com.prospectaai.ms_notification.sse.NotificationSseService;
import lombok.RequiredArgsConstructor;
import br.com.prospectaai.ms_notification.domain.util.JwtUtil;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/v1/notification")
@RequiredArgsConstructor
public class SseController {
    private final NotificationSseService sseService;
    private final JwtUtil jwtUtil;

    @GetMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<SseEmitter> stream(@RequestHeader(value = "Authorization", required = false) String authorization,
                                             @RequestParam(value = "token", required = false) String token) {
        String auth = authorization != null ? authorization : (token != null ? "Bearer " + token : null);
        if (auth == null || auth.isBlank() || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userId;
        try {
            userId = jwtUtil.extractUserId(auth);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(sseService.subscribe(userId));
    }

    @PostMapping("/disconnect")
    public ResponseEntity<Void> disconnect(@RequestHeader(value = "Authorization", required = false) String authorization,
                                           @RequestParam(value = "token", required = false) String token) {
        String auth = authorization != null ? authorization : (token != null ? "Bearer " + token : null);
        if (auth == null || auth.isBlank() || !auth.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userId;
        try {
            userId = jwtUtil.extractUserId(auth);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        sseService.unsubscribe(userId);
        return ResponseEntity.ok().build();
    }
}
