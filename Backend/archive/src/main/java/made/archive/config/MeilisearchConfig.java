package made.archive.config;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class MeilisearchConfig
{
    private final MeilisearchProperties props;

    @Bean
    public Client meilisearchClient()
    {
        return new Client(new Config(props.getHost(), props.getSearchKey()));
    }
}