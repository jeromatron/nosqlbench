package io.nosqlbench.driver.pulsar;

import com.codahale.metrics.Timer;
import io.nosqlbench.driver.pulsar.ops.PulsarOp;
import io.nosqlbench.driver.pulsar.ops.ReadyPulsarOp;
import io.nosqlbench.driver.pulsar.util.PulsarNBClientConf;
import io.nosqlbench.engine.api.activityapi.core.ActivityDefObserver;
import io.nosqlbench.engine.api.activityapi.errorhandling.modular.NBErrorHandler;
import io.nosqlbench.engine.api.activityapi.planning.OpSequence;
import io.nosqlbench.engine.api.activityimpl.ActivityDef;
import io.nosqlbench.engine.api.activityimpl.SimpleActivity;
import io.nosqlbench.engine.api.metrics.ActivityMetrics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.LongFunction;
import java.util.function.Supplier;

public class PulsarActivity extends SimpleActivity implements ActivityDefObserver {

    private final static Logger logger = LogManager.getLogger(PulsarActivity.class);

    public Timer bindTimer;
    public Timer executeTimer;
    private PulsarSpaceCache pulsarCache;

    private NBErrorHandler errorhandler;

    private PulsarNBClientConf clientConf;

    private OpSequence<LongFunction<PulsarOp>> sequencer;
    // private PulsarClient activityClient;

    private Supplier<PulsarSpace> clientSupplier;
    // private ThreadLocal<Supplier<PulsarClient>> tlClientSupplier;

    public PulsarActivity(ActivityDef activityDef) {
        super(activityDef);
    }

    @Override
    public void initActivity() {
        super.initActivity();

        bindTimer = ActivityMetrics.timer(activityDef, "bind");
        executeTimer = ActivityMetrics.timer(activityDef, "execute");

        String pulsarClntConfFile = activityDef.getParams().getOptionalString("config").orElse("config.properties");
        clientConf = new PulsarNBClientConf(pulsarClntConfFile);

        pulsarCache = new PulsarSpaceCache(this);

        this.sequencer = createOpSequence((ot) -> new ReadyPulsarOp(ot, pulsarCache));
        setDefaultsFromOpSequence(sequencer);
        onActivityDefUpdate(activityDef);

        this.errorhandler = new NBErrorHandler(
            () -> activityDef.getParams().getOptionalString("errors").orElse("stop"),
            this::getExceptionMetrics
        );
    }

    @Override
    public synchronized void onActivityDefUpdate(ActivityDef activityDef) {
        super.onActivityDefUpdate(activityDef);
    }

    public OpSequence<LongFunction<PulsarOp>> getSequencer() {
        return sequencer;
    }

    public PulsarNBClientConf getPulsarConf() {
        return clientConf;
    }

    public Timer getBindTimer() {
        return bindTimer;
    }

    public Timer getExecuteTimer() {
        return this.executeTimer;
    }
}
