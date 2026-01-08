package br.com.prospectaai.ms_notification.controller;

import java.time.Instant;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import lombok.RequiredArgsConstructor;
import br.com.prospectaai.ms_notification.domain.service.NotificationService;
import br.com.prospectaai.ms_notification.domain.dto.NotificationItem;
import br.com.prospectaai.ms_notification.domain.dto.NotificationPageResponse;
import br.com.prospectaai.shared.util.DatePeriodFormatter;
import br.com.prospectaai.ms_notification.domain.util.JwtUtil;

@RestController
@RequestMapping("/api/v1/notification")
@RequiredArgsConstructor
public class NotificationQueryController {
    private final NotificationService notificationService;
    private final JwtUtil jwtUtil;

    @GetMapping("/items")
    public ResponseEntity<NotificationPageResponse> list(@RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "limit", required = false, defaultValue = "20") int limit) {
        if (authorization == null || authorization.isBlank() || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userEmail;
        try {
            userEmail = jwtUtil.extractUserId(authorization);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        int size = Math.min(Math.max(limit, 1), 100);
        var now = Instant.now();
        var pageRes = notificationService.listByUser(userEmail, PageRequest.of(page, size));
        var items = pageRes.stream().map(n -> new NotificationItem(
                n.getId().toString(),
                n.getTitle(),
                n.getDatetime().toString(),
                DatePeriodFormatter.format(n.getDatetime(), now),
                n.getIcon(),
                n.getContent(),
                n.getLink(),
                n.isRead()
        )).collect(Collectors.toList());
        long totalUnread = notificationService.unreadCount(userEmail);
        return ResponseEntity.ok(new NotificationPageResponse(items, totalUnread));
    }

    @GetMapping("/item/{id}")
    public ResponseEntity<NotificationItem> getOne(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable("id") String id) {
        if (authorization == null || authorization.isBlank() || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userEmail;
        try {
            userEmail = jwtUtil.extractUserId(authorization);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        var now = Instant.now();
        var opt = notificationService.getByIdForUser(uuid, userEmail);
        if (opt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        var n = opt.get();
        var item = new NotificationItem(
            n.getId().toString(),
            n.getTitle(),
            n.getDatetime().toString(),
            DatePeriodFormatter.format(n.getDatetime(), now),
            n.getIcon(),
            n.getContent(),
            n.getLink(),
            n.isRead()
        );
        return ResponseEntity.ok(item);
    }

    @PatchMapping("/item/{id}/read")
    public ResponseEntity<Void> markRead(@RequestHeader(value = "Authorization", required = false) String authorization, @PathVariable("id") String id) {
        if (authorization == null || authorization.isBlank() || !authorization.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userEmail;
        try {
            userEmail = jwtUtil.extractUserId(authorization);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
        boolean ok = notificationService.markAsReadForUser(uuid, userEmail);
        if (!ok) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok().build();
    }
}
