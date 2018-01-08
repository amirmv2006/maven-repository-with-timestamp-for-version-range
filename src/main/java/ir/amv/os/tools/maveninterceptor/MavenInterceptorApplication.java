package ir.amv.os.tools.maveninterceptor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AdviceMode;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Collection;

@SpringBootApplication
@EnableCaching(mode = AdviceMode.PROXY)
@EnableScheduling
@Import(HazelcastConfiguration.class)
public class MavenInterceptorApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext applicationContext = SpringApplication.run(MavenInterceptorApplication.class, args);
        CacheManager cacheManager = applicationContext.getBean(CacheManager.class);
        System.out.println("cacheManager = " + cacheManager);
        System.out.println("cacheManager.getClass() = " + cacheManager.getClass());
        Collection<String> cacheNames = cacheManager.getCacheNames();
        for (String cacheName : cacheNames) {
            System.out.println("cacheName = " + cacheName);
        }

    }
}
