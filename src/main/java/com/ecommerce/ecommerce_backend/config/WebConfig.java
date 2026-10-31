package com.ecommerce.ecommerce_backend.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Value("${file.upload-dir:uploads/}")
    private String uploadDir;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization", "Content-Type")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Resolve uploads/ to an absolute path so file: works reliably
        String absoluteUploadPath = java.nio.file.Paths.get(uploadDir)
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();

        // Ensure it ends with a slash (Spring requires it for directories)
        if (!absoluteUploadPath.endsWith("/")) {
            absoluteUploadPath += "/";
        }

        System.out.println(">>> Serving uploads from: " + absoluteUploadPath);

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absoluteUploadPath)
                .setCachePeriod(3600);

        // Serve static resources (if any)
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");

        // Serve webjars (if using)
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
    }
}
