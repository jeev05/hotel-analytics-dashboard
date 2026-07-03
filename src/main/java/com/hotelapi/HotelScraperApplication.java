package com.hotelapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
public class HotelScraperApplication {

    public static void main(String[] args) {
        SpringApplication.run(HotelScraperApplication.class, args);
    }

  @Bean
public WebMvcConfigurer corsConfigurer() {
    return new WebMvcConfigurer() {

        @Override
        public void addCorsMappings(CorsRegistry registry) {

            registry.addMapping("/api/**")
                    .allowedOriginPatterns(
                            "http://localhost:*",
                            "http://127.0.0.1:*",
                            "https://hotel-analytics-dashboard.vercel.app"
                    )
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                    .allowedHeaders("*")
                    .allowCredentials(true)
                    .maxAge(3600);
        }
    };
}

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                // Serve frontend assets directly from workspace folder.
                registry.addResourceHandler("/frontend/**")
                        .addResourceLocations("file:frontend/");
            }
        };
    }
}
