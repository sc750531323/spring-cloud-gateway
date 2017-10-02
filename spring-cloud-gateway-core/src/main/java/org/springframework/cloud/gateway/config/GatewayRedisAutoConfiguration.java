package org.springframework.cloud.gateway.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scripting.support.ResourceScriptSource;

@Configuration
@AutoConfigureAfter(RedisReactiveAutoConfiguration.class)
@ConditionalOnBean(ReactiveRedisTemplate.class)
@ConditionalOnClass(RedisTemplate.class)
class GatewayRedisAutoConfiguration {
	@Bean
	public RedisScript<String> redisRequestRateLimiterScript() {
		DefaultRedisScript<String> redisScript = new DefaultRedisScript<>();
		redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("META-INF/scripts/request_rate_limiter.lua")));
		redisScript.setResultType(String.class);
		return redisScript;
	}

	@Bean
	public ReactiveRedisTemplate<String, String> stringReactiveRedisTemplate(
			ReactiveRedisConnectionFactory reactiveRedisConnectionFactory,
			ResourceLoader resourceLoader) {
		RedisSerializer<String> serializer = new StringRedisSerializer();
		RedisSerializationContext<String , String> serializationContext = RedisSerializationContext
				.<String, String>newSerializationContext()
				.key(serializer)
				.value(serializer)
				.hashKey(serializer)
				.hashValue(serializer)
				.build();
		return new ReactiveRedisTemplate<>(reactiveRedisConnectionFactory,
				serializationContext);
	}

	@Bean
	public RedisRateLimiter redisRateLimiter(ReactiveRedisTemplate<String, String> redisTemplate,
											 @Qualifier("redisRequestRateLimiterScript") RedisScript<String> redisScript) {
		return new RedisRateLimiter(redisTemplate, redisScript);
	}
}
