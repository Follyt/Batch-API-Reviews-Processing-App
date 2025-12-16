package repository;

import domain.entity.ReviewTagResult;
import domain.enums.TagResultStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

public interface ReviewTagResultRepository extends JpaRepository<ReviewTagResult, Long> {

    Optional<ReviewTagResult> findByReviewId(Long reviewId);

    List<ReviewTagResult> findByStatus(TagResultStatus status);
}
