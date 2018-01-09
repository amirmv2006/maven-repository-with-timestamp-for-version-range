package ir.amv.os.tools.maveninterceptor;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastConfiguration {

    @Bean
    public Config hazelCastConfig(){
        return new Config()
                .setInstanceName("hazelcast-instance")
                .addMapConfig(
                        new MapConfig()
                                .setName("branchCommits")
                                .setMaxSizeConfig(new MaxSizeConfig(1000, MaxSizeConfig.MaxSizePolicy
                                        .PER_NODE))
                                .setEvictionPolicy(EvictionPolicy.LRU)
                                )
                .addMapConfig(
                        new MapConfig()
                                .setName("commitBuildFinishDate")
                                .setMaxSizeConfig(new MaxSizeConfig(10000, MaxSizeConfig.MaxSizePolicy.PER_NODE))
                                .setEvictionPolicy(EvictionPolicy.LRU)
                                )
                .addMapConfig(
                        new MapConfig()
                                .setName("artifactReleaseMap")
                                .setMaxSizeConfig(new MaxSizeConfig(10000, MaxSizeConfig.MaxSizePolicy.PER_NODE))
                                .setEvictionPolicy(EvictionPolicy.LRU)
                                )
                .addMapConfig(
                        new MapConfig()
                                .setName("buildFinishDate")
                                .setMaxSizeConfig(new MaxSizeConfig(1000, MaxSizeConfig.MaxSizePolicy.PER_NODE))
                                .setEvictionPolicy(EvictionPolicy.LRU)
                                )
                .addMapConfig(
                        new MapConfig()
                                .setName("buildCommits")
                                .setMaxSizeConfig(new MaxSizeConfig(1000, MaxSizeConfig.MaxSizePolicy.PER_NODE))
                                .setEvictionPolicy(EvictionPolicy.LRU)
                                )
                ;
    }

}