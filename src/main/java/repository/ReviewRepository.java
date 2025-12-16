package repository;

import domain.entity.Review;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface ReviewRepository extends Repository<Review, Integer> {

    @Query("""
        select r
        from Review r
        where not exists (
          select 1
          from ReviewTagResult tr
          where tr.reviewId = r.reviewId
        )
        order by r.reviewId asc
        """)
    List<Review> findNextUnprocessed(Pageable pageable);
}
