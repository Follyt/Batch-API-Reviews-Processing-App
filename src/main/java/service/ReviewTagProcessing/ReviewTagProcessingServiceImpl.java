package service.ReviewTagProcessing;

import domain.entity.Review;
import domain.entity.ReviewTagBatch;
import domain.entity.ReviewTagResult;
import domain.enums.BatchStatus;
import domain.enums.TagResultStatus;
import repository.ReviewRepository;
import repository.ReviewTagBatchRepository;
import repository.ReviewTagResultRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import service.openAI.OpenAiBatchService;

@Slf4j
@Service
public class ReviewTagProcessingServiceImpl implements ReviewTagProcessingService {

    private final ReviewRepository reviewRepository;
    private final ReviewTagResultRepository resultRepository;
    private final ReviewTagBatchRepository batchRepository;
    private final OpenAiBatchService openAiBatchService;

    private final int portionSize;

    public ReviewTagProcessingServiceImpl(
            ReviewRepository reviewRepository,
            ReviewTagResultRepository resultRepository,
            ReviewTagBatchRepository batchRepository,
            OpenAiBatchService openAiBatchService
    ) {
        this.reviewRepository = reviewRepository;
        this.resultRepository = resultRepository;
        this.batchRepository = batchRepository;
        this.openAiBatchService = openAiBatchService;
        this.portionSize = 10;
    }

    @Override
    @Transactional
    public void processNextBatch() {

        log.info("processNextBatch called");

        // 1️⃣ Берём следующую порцию review
        List<Review> reviews = reviewRepository.findNextUnprocessed(
                PageRequest.of(0, portionSize)
        );

        if (reviews.isEmpty()) {
            return;
        }

        // 2️⃣ Создаём batch-запись
        ReviewTagBatch batch = new ReviewTagBatch();
        batch.setStatus(BatchStatus.CREATED);
        batch.setRequestedCount(reviews.size());

        batch = batchRepository.save(batch);

        // 3️⃣ Резервируем review (PENDING)
        for (Review review : reviews) {
            ReviewTagResult result = new ReviewTagResult(
                    review.getReviewId(),
                    TagResultStatus.PENDING
            );
            result.setBatch(batch);
            resultRepository.save(result);
        }

        openAiBatchService.sendBatch(batch);

        // На этом этапе:
        // - batch создан
        // - отзывы зарезервированы
        // - можно безопасно идти в OpenAI
    }
}
