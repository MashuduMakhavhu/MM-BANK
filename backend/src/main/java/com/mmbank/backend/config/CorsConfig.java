package com.mmbank.backend.config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                    .allowedOrigins(
                        "https://mm-bank-atm.onrender.com",
                        "https://mm-bank-mobile.onrender.com",
                        "https://localhost",
                        "http://127.0.0.1:8000",
                        "http://localhost:8000"
                    )
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("Content-Type", "X-XSRF-TOKEN")
                    .allowCredentials(true)
                    .maxAge(3600);
            }
        };
    }
}
