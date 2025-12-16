package domain.entity;

import jakarta.persistence.*;
import lombok.Getter;

@Getter
@Entity
@Table(name = "review_tags")
public class ReviewTagCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long id;

    @Column(name = "category_name", nullable = false)
    private String name;

    protected ReviewTagCategory() {}

    public ReviewTagCategory(String name) {
        this.name = name;
    }

}
