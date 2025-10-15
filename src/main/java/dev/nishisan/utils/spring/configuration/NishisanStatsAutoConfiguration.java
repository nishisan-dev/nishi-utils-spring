package dev.nishisan.utils.spring.configuration;

import dev.nishisan.utils.spring.stats.StatsUtilsMetricBind;
import dev.nishisan.utils.stats.StatsUtils;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.condition.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnProperty(
        name = "nishi.utils.stats.enabled",     // O nome da propriedade
        havingValue = "true",                   // Só ative se o valor for "true"
        matchIfMissing = false                  // ❗ ESSENCIAL: Se a propriedade não existir, considere como "true" (ativado por padrão)
)
public class NishisanStatsAutoConfiguration {

    /**
     * Creates and returns an instance of {@link StatsUtils} without integrating metrics functionality.
     * This method is invoked when the Micrometer {@code MeterRegistry} class is not available on the classpath.
     * The returned {@link StatsUtils} instance starts its internal statistics thread upon creation.
     *
     * @return a {@link StatsUtils} instance configured without metrics functionality
     */
    @Bean(name = "statsUtils")
    @ConditionalOnMissingClass("io.micrometer.core.instrument.MeterRegistry")
    public StatsUtils statsUtilsNoMetrics() {
        StatsUtils instance = new StatsUtils();

        instance.startStatsThread();
        return instance;
    }

    /**
     * Creates and returns an instance of {@link StatsUtils} with integrated metrics functionality.
     * This method is invoked when the Micrometer {@code MeterRegistry} class is available on the classpath
     * and registered as a bean. The returned {@link StatsUtils} instance starts its internal statistics
     * thread and integrates with the provided {@code MeterRegistry} to bind metrics.
     *
     * @param registry the {@code MeterRegistry} instance used for binding metrics to the {@link StatsUtils} instance
     * @return a {@link StatsUtils} instance configured with metrics functionality
     */
    @Bean(name = "statsUtils")
    @ConditionalOnClass(MeterRegistry.class)
    @ConditionalOnBean(MeterRegistry.class)
    public StatsUtils statsUtilsWithMetrics(MeterRegistry registry) {
        StatsUtils instance = new StatsUtils();
        instance.registerListener(new StatsUtilsMetricBind(registry));
        instance.startStatsThread();
        return instance;
    }
}
