package dev.nishisan.utils.spring.configuration;

import dev.nishisan.utils.stats.StatsUtils;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests da auto-configuração de estatísticas.
 *
 * <p>A configuração é {@code @ConditionalOnWebApplication(SERVLET)}, por isso usa-se o
 * {@link WebApplicationContextRunner} (que sobe um contexto web servlet em memória). Registrar
 * a classe via {@link AutoConfigurations} garante que ela seja processada após os beans de
 * usuário, o que faz o {@code @ConditionalOnBean(MeterRegistry.class)} enxergar o registry.</p>
 */
class NishisanStatsAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(NishisanStatsAutoConfiguration.class));

    @Test
    void registraStatsUtilsQuandoHabilitadoComMeterRegistry() {
        this.contextRunner
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .withPropertyValues("nishi.utils.stats.enabled=true")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasBean("statsUtils");
                    assertThat(context.getBean("statsUtils")).isInstanceOf(StatsUtils.class);
                });
    }

    @Test
    void naoRegistraStatsUtilsQuandoPropriedadeAusente() {
        this.contextRunner
                .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean("statsUtils");
                });
    }
}
