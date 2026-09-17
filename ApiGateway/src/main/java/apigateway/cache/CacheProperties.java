package apigateway.cache;

import cache.BaseCacheProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cache")
public class CacheProperties extends BaseCacheProperties {}
