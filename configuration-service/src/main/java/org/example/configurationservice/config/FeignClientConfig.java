package org.example.configurationservice.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@Configuration
public class FeignClientConfig {
    
    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                try {
                    ServletRequestAttributes attributes = 
                        (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    
                    if (attributes != null) {
                        HttpServletRequest request = attributes.getRequest();
                        String authorization = request.getHeader("Authorization");
                        
                        if (authorization != null && !authorization.isEmpty()) {
                            log.debug("du token vers: {}", template.url());
                            template.header("Authorization", authorization);
                        }
                    }
                } catch (Exception e) {
                    log.warn("lors de la propagation du token: {}", e.getMessage());
                }
            }
        };
    }
}