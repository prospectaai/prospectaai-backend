package br.com.prospectaai.ms_async_task.domain.entity;

import java.time.Instant;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "prospection_records")
@Getter
@Setter
public class ProspectionRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String userEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id", nullable = false)
    private ProspectTask task;

    @Column(nullable = false, length = 512)
    private String query;

    @Column(nullable = false, length = 64)
    private String platform;

    @Column(length = 256)
    private String nomeEmpresa;

    @Column(length = 64)
    private String telefone;

    @Column(length = 512)
    private String endereco;

    @Column(length = 256)
    private String website;

    @Column(length = 512)
    private String imageUrl;

    private Double rating;

    private Integer reviews;

    @Column(length = 1024)
    private String especialidades;

    @Column(nullable = false)
    private Instant createdAt;
}
