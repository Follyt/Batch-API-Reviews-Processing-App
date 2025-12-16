package service.CategoryService;

import java.util.Set;

public interface CategoryService {

    /**
     * Загружает все категории из БД в память.
     * Вызывается один раз при старте приложения.
     */
    void loadAll();

    /**
     * Возвращает true, если категория уже известна.
     */
    boolean exists(String categoryName);

    /**
     * Регистрирует категорию, если её ещё нет.
     * Безопасно при параллельных вызовах.
     */
    void registerIfMissing(String categoryName);

    /**
     * Для отладки / мониторинга
     */
    Set<String> getAll();
}
