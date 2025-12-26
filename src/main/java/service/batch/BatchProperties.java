package service.batch;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.batch")
public class BatchProperties {

    private int pollIntervalSeconds;

    private int maxRetry;

}