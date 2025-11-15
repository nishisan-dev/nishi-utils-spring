package dev.nishisan.utils.spring.stats;

import dev.nishisan.utils.stats.IStatsListener;
import dev.nishisan.utils.stats.dto.HitCounterDTO;
import dev.nishisan.utils.stats.dto.SimpleValueDTO;
import dev.nishisan.utils.stats.list.FixedSizeList;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.HashMap;
import java.util.Map;

/**
 * A class that implements the {@link IStatsListener} interface to bind metrics
 * to the provided Micrometer {@link MeterRegistry}. It integrates metric counters
 * and gauges to monitor and collect statistical data.
 *
 * This class manages multiple types of metrics:
 * - Gauges for monitoring average values and current values.
 * - Counters for tracking increments in hit counters.
 *
 * The metrics are registered with the provided {@link MeterRegistry}.
 * Updates to the metric source objects (e.g., {@code FixedSizeList}, {@code HitCounterDTO})
 * are directly reflected in the registered metrics.
 */
public class StatsUtilsMetricBind implements IStatsListener {

    private final MeterRegistry meterRegistry;

    private final Map<String, Counter> counters = new HashMap<>();
    private final Map<String, Gauge> gauges = new HashMap<>();

    /**
     * Constructs a new instance of StatsUtilsMetricBind with the specified MeterRegistry.
     * This constructor initializes the metric binding functionality by associating it with
     * the provided MeterRegistry. The MeterRegistry is used for monitoring and recording
     * statistical data for the application.
     *
     * @param meterRegistry the MeterRegistry instance used to bind and record metrics.
     */
    public StatsUtilsMetricBind(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Handles the creation of an average counter based on a FixedSizeList instance.
     * This method creates a Gauge metric using the specified FixedSizeList, where
     * the average value of the list is dynamically calculated and recorded.
     * The resulting Gauge is registered to the meterRegistry and stored in the local gauges map.
     *
     * @param fixedSizeList the FixedSizeList instance representing the data source
     *                      for the average counter. The list's average value is used
     *                      to populate the corresponding Gauge metric.
     */
    @Override
    public void onAverageCounterCreated(FixedSizeList fixedSizeList) {
        Gauge gauge = Gauge.builder(fixedSizeList.getName(), fixedSizeList, FixedSizeList::getAverage)
                .description(fixedSizeList.getName())
                .register(meterRegistry);
        gauges.put(fixedSizeList.getName(), gauge);
    }

    /**
     * Handles the creation of a gauge metric to represent the current value of a given SimpleValueDTO instance.
     * A Gauge is registered to the meterRegistry using the name and value of the provided SimpleValueDTO
     * and stored in the local gauges map for reference.
     *
     * @param simpleValueDTO the SimpleValueDTO instance containing the name and value
     *                       used to create and populate the Gauge metric.
     */
    @Override
    public void onCurrentValueCounterCreated(SimpleValueDTO simpleValueDTO) {
        Gauge gauge = Gauge.builder(simpleValueDTO.getName(), simpleValueDTO, SimpleValueDTO::getValue)
                .description(simpleValueDTO.getName())
                .register(meterRegistry);
        gauges.put(simpleValueDTO.getName(), gauge);
    }

    /**
     * Handles the increment of a hit counter identified by the given HitCounterDTO.
     * If the specified counter name exists in the local counters map, its value
     * is incremented.
     *
     * @param hitCounterDTO the data transfer object containing the name of the hit
     *                      counter to be incremented.
     */
    @Override
    public void onHitCounterIncremented(HitCounterDTO hitCounterDTO) {
        if (counters.containsKey(hitCounterDTO.getName())) {
            counters.get(hitCounterDTO.getName()).increment();
        }
    }

    /**
     * Handles the creation of a hit counter based on the provided HitCounterDTO.
     * This method registers a Gauge and a Counter to the MeterRegistry using
     * the information from the HitCounterDTO. The Gauge tracks the rate specified
     * by the DTO, whereas the Counter increments its value. The registered metrics
     * are stored in the local gauges and counters maps for reference.
     *
     * @param hitCounterDTO the data transfer object containing the name and rate
     *                      for the hit counter to be created and registered.
     */
    @Override
    public void onHitCounterCreated(HitCounterDTO hitCounterDTO) {
        Gauge gauge = Gauge.builder(hitCounterDTO.getName(), hitCounterDTO, HitCounterDTO::getRate)
                .description(hitCounterDTO.getName())
                .register(meterRegistry);
        gauges.put(hitCounterDTO.getName() + ".rate", gauge);
        
        Counter counter = Counter.builder(hitCounterDTO.getName())
                .description(hitCounterDTO.getName())
                .register(meterRegistry);
        counter.increment();
        this.counters.put(hitCounterDTO.getName(), counter);
    }

    /**
     * Handles the removal of a previously registered hit counter.
     * This method removes the specified hit counter and its associated rate gauge
     * from the internal tracking maps, ensuring that these metrics are no longer monitored.
     *
     * @param hitCounterDTO the data transfer object containing the name of the hit counter
     *                      to be removed along with its associated rate gauge.
     */
    @Override
    public void onHitCounterRemoved(HitCounterDTO hitCounterDTO) {
        this.counters.remove(hitCounterDTO.getName());
        this.gauges.remove(hitCounterDTO.getName() + ".rate");
    }

    /**
     * Handles the event triggered when a new value is added to the average counter
     * represented by the given FixedSizeList. This method ensures that no specific
     * action is required, as the associated gauge automatically reads the updated
     * average via the FixedSizeList's getAverage method.
     *
     * @param fixedSizeList the FixedSizeList instance to which the value is added,
     *                      and from which the updated average is calculated.
     */
    @Override
    public void onAverageCounterValueAdded(FixedSizeList fixedSizeList) {
        // No action needed - the gauge automatically reads the updated average
        // through the FixedSizeList::getAverage method reference
    }

    /**
     * Handles the event triggered when the current value of a counter represented by
     * the given SimpleValueDTO is updated. No explicit action is required as the gauge
     * automatically reads the updated value through the SimpleValueDTO::getValue method reference.
     *
     * @param simpleValueDTO the SimpleValueDTO instance containing the updated value
     *                       of the current value counter.
     */
    @Override
    public void onCurrentValueCounterUpdated(SimpleValueDTO simpleValueDTO) {
        // No action needed - the gauge automatically reads the updated value
        // through the SimpleValueDTO::getValue method reference
    }
}
