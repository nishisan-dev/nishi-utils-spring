package dev.nishisan.utils.spring.stats;

import dev.nishisan.utils.stats.IStatsListener;
import dev.nishisan.utils.stats.dto.HitCounterDTO;
import dev.nishisan.utils.stats.dto.SimpleValueDTO;
import dev.nishisan.utils.stats.list.FixedSizeList;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.HashMap;
import java.util.Map;

public class StatsUtilsMetricBind implements IStatsListener<Long> {

    private final MeterRegistry meterRegistry;

    private Map<String, Counter> counters = new HashMap<>();

    public StatsUtilsMetricBind(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void onAverageCounterCreated(FixedSizeList<Long> fixedSizeList) {
        Meter gauge = Gauge.builder(fixedSizeList.getName(), fixedSizeList, FixedSizeList::getAverage)
                .description(fixedSizeList.getName())
                .register(meterRegistry);
    }

    @Override
    public void onAverageCounterValueAdded(FixedSizeList<Long> fixedSizeList) {
        // No-op: the gauge reads the current average directly from the same list instance.
    }

    @Override
    public void onCurrentValueCounterUpdated(SimpleValueDTO simpleValueDTO) {
        // No-op: the gauge reads the current value directly from the same DTO instance.
    }

    @Override
    public void onCurrentValueCounterCreated(SimpleValueDTO simpleValueDTO) {
        Meter gauge = Gauge.builder(simpleValueDTO.getName(), simpleValueDTO, SimpleValueDTO::getValue)
                .description(simpleValueDTO.getName())
                .register(meterRegistry);

    }
    @Override
    public void onHitCounterIncremented(HitCounterDTO hitCounterDTO) {
        if (counters.containsKey(hitCounterDTO.getName())) {
            counters.get(hitCounterDTO.getName()).increment();
        }
    }

    @Override
    public void onHitCounterCreated(HitCounterDTO hitCounterDTO) {
        Meter gauge = Gauge.builder(hitCounterDTO.getName(), hitCounterDTO, HitCounterDTO::getRate)
                .description(hitCounterDTO.getName())
                .register(meterRegistry);
        Counter counter = Counter.builder(hitCounterDTO.getName())
                .description(hitCounterDTO.getName())
                .register(meterRegistry);
        counter.increment();
        this.counters.put(hitCounterDTO.getName(), counter);
    }

    @Override
    public void onHitCounterRemoved(HitCounterDTO hitCounterDTO) {
        this.counters.remove(hitCounterDTO.getName());
    }
}
