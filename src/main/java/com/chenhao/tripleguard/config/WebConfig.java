package com.chenhao.tripleguard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 * <p>
 * 配置CORS跨域访问、静态资源处理、视图控制器等
 * </p>
 *
 * @author chenhao
 * @since 1.0.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 配置CORS跨域访问
     * <p>
     * 允许前端应用从不同域名访问API
     * </p>
     *
     * @param registry CORS注册器
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    /**
     * 配置静态资源处理
     *
     * @param registry 资源处理注册器
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Swagger UI 资源（如果后续添加）
        registry.addResourceHandler("/swagger-ui/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/swagger-ui/");

        // 静态页面资源
        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }

    /**
     * 配置视图控制器
     * <p>
     * 用于简单的URL到视图的映射，无需控制器处理
     * </p>
     *
     * @param registry 视图控制器注册器
     */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        // 健康检查页面重定向
        registry.addRedirectViewController("/health", "/actuator/health");

        // API文档重定向（如果后续添加Swagger）
        registry.addRedirectViewController("/api-docs", "/v3/api-docs");
    }
}
