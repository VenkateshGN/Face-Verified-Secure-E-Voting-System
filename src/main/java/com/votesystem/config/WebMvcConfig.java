package com.votesystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final VoterSessionInterceptor voterSessionInterceptor;
    private final AdminSessionInterceptor adminSessionInterceptor;

    public WebMvcConfig(VoterSessionInterceptor voterSessionInterceptor, AdminSessionInterceptor adminSessionInterceptor) {
        this.voterSessionInterceptor = voterSessionInterceptor;
        this.adminSessionInterceptor = adminSessionInterceptor;
    }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(voterSessionInterceptor)
                .addPathPatterns("/verify-face/**", "/verify-otp/**", "/ballot/**");

        registry.addInterceptor(adminSessionInterceptor)
                .addPathPatterns("/admin/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String imagesPath = new File("images").getAbsolutePath() + File.separator;
        String capturedPath = new File("captured_faces").getAbsolutePath() + File.separator;

        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + imagesPath, "classpath:/static/images/");

        registry.addResourceHandler("/captured_faces/**")
                .addResourceLocations("file:" + capturedPath);

        registry.addResourceHandler("/static/**")
                .addResourceLocations("classpath:/static/");
    }
}
