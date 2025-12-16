package repository;

import domain.entity.ReviewTagBatch;
import domain.enums.BatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface ReviewTagBatchRepository extends JpaRepository<ReviewTagBatch, Long> {

    Optional<ReviewTagBatch> findByOpenaiBatchId(String openaiBatchId);

    List<ReviewTagBatch> findByStatusIn(List<BatchStatus> statuses);
}
