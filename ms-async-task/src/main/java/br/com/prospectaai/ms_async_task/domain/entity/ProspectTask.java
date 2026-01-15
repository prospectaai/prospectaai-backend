package br.com.prospectaai.ms_async_task.domain.entity;

import java.time.Instant;
import java.util.List;

import br.com.prospectaai.ms_async_task.domain.enums.AsyncTaskStatus;
import br.com.prospectaai.shared.dto.async.AsyncTaskPlatform;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "prospect_tasks")
@Getter
@Setter
public class ProspectTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 256)
    private String userEmail;

    @Column(nullable = false, length = 512)
    private String query;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private AsyncTaskPlatform platform;

    @Column(length = 256)
    private String location;

    @Column(length = 256)
    private String businessType;

    private Integer radiusKm;

    @Column(length = 64)
    private String companySize;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AsyncTaskStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "task", fetch = FetchType.LAZY)
    private List<ProspectionRecord> results;
}
