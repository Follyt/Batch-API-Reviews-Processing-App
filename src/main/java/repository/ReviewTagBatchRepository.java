package repository;

import domain.entity.ReviewTagBatch;
import domain.enums.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReviewTagBatchRepository
        extends JpaRepository<ReviewTagBatch, Long> {

    List<ReviewTagBatch> findAllByStatus(BatchStatus status);

    List<ReviewTagBatch> findByStatus(BatchStatus status);

}
