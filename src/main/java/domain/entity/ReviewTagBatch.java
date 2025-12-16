package domain.entity;

import domain.enums.BatchStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@Entity
@Table(name = "review_tag_batch")
public class ReviewTagBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "openai_batch_id", nullable = false)
    private String openaiBatchId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BatchStatus status;

    @Column(name = "request_file_id")
    private String requestFileId;

    @Column(name = "output_file_id")
    private String outputFileId;

    @Column(name = "error_file_id")
    private String errorFileId;

    @Column(name = "requested_count", nullable = false)
    private Integer requestedCount;

    @Column(name = "completed_count", nullable = false)
    private Integer completedCount = 0;

    @Column(name = "attempt", nullable = false)
    private Integer attempt = 0;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @PrePersist
    void prePersist() {
        var now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (completedCount == null) completedCount = 0;
        if (attempt == null) attempt = 0;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

}
