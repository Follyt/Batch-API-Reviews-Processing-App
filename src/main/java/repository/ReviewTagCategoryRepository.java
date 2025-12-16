package repository;

import domain.entity.ReviewTagCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface ReviewTagCategoryRepository extends JpaRepository<ReviewTagCategory, Long> {

    Optional<ReviewTagCategory> findByName(String name);
}
