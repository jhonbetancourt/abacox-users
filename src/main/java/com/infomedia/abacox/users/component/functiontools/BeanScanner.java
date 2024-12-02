package com.infomedia.abacox.users.component.functiontools;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Component
public class BeanScanner {
    private final ApplicationContext applicationContext;

    @Autowired
    public BeanScanner(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public Map<String, Object> scanAndGetBeans(String basePackage) {
        Map<String, Object> beans = new HashMap<>();
        
        ClassPathScanningCandidateComponentProvider scanner =
            new ClassPathScanningCandidateComponentProvider(false);

        // Add filters for the annotations you want to scan
        scanner.addIncludeFilter(new AnnotationTypeFilter(Service.class));
        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));
        // Add more annotations as needed

        Set<BeanDefinition> candidates =
            scanner.findCandidateComponents(basePackage);

        for (org.springframework.beans.factory.config.BeanDefinition beanDefinition : candidates) {
            try {
                String className = beanDefinition.getBeanClassName();
                Class<?> clazz = Class.forName(className);
                
                // Get all beans of this type from the Spring context
                Map<String, ?> beansOfType = applicationContext.getBeansOfType(clazz);
                beans.putAll(beansOfType);
                
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to load class", e);
            }
        }

        return beans;
    }
}