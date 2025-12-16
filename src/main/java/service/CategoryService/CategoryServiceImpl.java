package service.CategoryService;

import domain.entity.ReviewTagCategory;
import repository.ReviewTagCategoryRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final ReviewTagCategoryRepository categoryRepository;

    /**
     * In-memory cache категорий.
     * Храним нормализованные имена.
     */
    private final Set<String> categories =
            ConcurrentHashMap.newKeySet();

    public CategoryServiceImpl(ReviewTagCategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /**
     * Вызывается автоматически при старте Spring Context
     */
    @PostConstruct
    @Override
    public void loadAll() {
        categoryRepository.findAll()
                .stream()
                .map(c -> normalize(c.getName()))
                .forEach(categories::add);
    }

    @Override
    public boolean exists(String categoryName) {
        return categories.contains(normalize(categoryName));
    }

    @Override
    public void registerIfMissing(String categoryName) {
        String normalized = normalize(categoryName);

        // быстрый путь — уже есть в кеше
        if (categories.contains(normalized)) {
            return;
        }

        try {
            categoryRepository.save(new ReviewTagCategory(normalized));
            categories.add(normalized);
        } catch (DataIntegrityViolationException e) {
            // категория уже была добавлена другим потоком / инстансом
            categories.add(normalized);
        }
    }

    @Override
    public Set<String> getAll() {
        return Set.copyOf(categories);
    }

    /**
     * Единое правило нормализации
     */
    private String normalize(String name) {
        return name
                .trim()
                .toLowerCase();
    }
}
