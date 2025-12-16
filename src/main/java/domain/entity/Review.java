package domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "review")
public class Review {

    @Id
    @Column(name = "review_id")
    private Integer reviewId;

    @Column(name = "content")
    private String content;

    public Integer getReviewId() {
        return reviewId;
    }

    public String getContent() {
        return content;
    }
}
