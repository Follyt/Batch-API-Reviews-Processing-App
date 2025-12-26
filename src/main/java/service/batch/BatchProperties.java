package service.batch;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.batch")
public class BatchProperties {

    /**
     * Number of reviews to fetch and send as individual OpenAI requests per batch.
     */
    private int portionSize = 5;

    private int pollIntervalSeconds;

    private int maxRetry;

}