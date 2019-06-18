package com.anhe.springbootmonitor.monitor;

import io.prometheus.client.Counter;
import io.prometheus.client.Gauge;

/**
 * 这个类是负责制定监控的度量指标类型
 *
 * 创建度量指标类型，Normal users should use {@link Gauge}, {@link Counter}, {@link Summary} and {@link Histogram}
 * 首先我们要先确定我们的指标是哪一种类型，prom支持4种度量指标类型：Counter（变化的增减量）, Gauge（瞬时值）, Histogram（采样并统计）, Summary（采样结果）
 * 创建度量指标通过使用build的方式，必须标明name和labelNames，name说明是哪一个度量指标，labelNames（标签名）说明这个度量指标是属于哪种类型的，以便我们将来安装应用去监控，
 */
public class MonitorMetrics {

    public final static String HEAP_MEMORY_USED = "HeapMemoryUsed";
    public final static String HEAP_MEMORY_MAX = "HeapMemoryMax";
    public final static String HEAP_MEMORY_COMMIT = "HeapMemoryCommit";
    public final static String NON_HEAP_MEMORY_USED = "NonHeapMemoryUsed";
    public final static String NON_HEAP_MEMORY_COMMIT = "NonHeapMemoryCommit";

    public final static String TOMCAT_MAX_THREADS = "Tomcat_maxThreads";
    public final static String TOMCAT_CURRENT_THREAD_COUNT = "Tomcat_currentThreadCount";
    public final static String TOMCAT_CURRENT_THREADS_BUSY = "Tomcat_currentThreadsBusy";
    public final static String TOMCAT_REQUEST_COUNT = "requestCount";
    public final static String TOMCAT_ERROR_COUNT = "errorCount";
    public final static String TOMCAT_PROCESSING_TIME = "processingTime";
    public final static String TOMCAT_BYTES_RECEIVED = "bytesReceived";
    public final static String TOMCAT_BYTES_SENT = "bytesSent";
    public final static String LABEL_SERVICE = "service";
    public final static String LABEL_IP = "host";


    /**
     * Create and register the Collector with the default registry.
     * 使用默认注册表注册Collector
     * 建立度量指标的名称，标签名，help信息,其他的属性是可选的，可以点击源码进行查看
     * name: the name of the metric. Required.
     * labelNames: the labelNames of the metric. Optional, defaults to no labels.
     * help: the help string of the metric. Required.
     */

    /**
     * ----------------- JVM -------------------------
     */
    public static final Gauge HEAP_MEMORY_USED_GAUGE = Gauge.build().labelNames(LABEL_SERVICE, LABEL_IP).name(HEAP_MEMORY_USED).help("jmx info").register();
    public static final Gauge HEAP_MEMORY_MAX_GAUGE = Gauge.build().labelNames(LABEL_SERVICE, LABEL_IP).name(HEAP_MEMORY_MAX).help("jmx info").register();
    public static final Gauge HEAP_MEMORY_COMMIT_GAUGE = Gauge.build().labelNames(LABEL_SERVICE, LABEL_IP).name(HEAP_MEMORY_COMMIT).help("jmx info").register();
    public static final Gauge NON_HEAP_MEMORY_USED_GAUGE = Gauge.build().labelNames(LABEL_SERVICE, LABEL_IP).name(NON_HEAP_MEMORY_USED).help("jmx info").register();
    public static final Gauge NON_HEAP_MEMORY_COMMIT_GAUGE = Gauge.build().labelNames(LABEL_SERVICE, LABEL_IP).name(NON_HEAP_MEMORY_COMMIT).help("jmx info").register();

    /**
     * ----------------- Thread Pool ----------------
     */
    public static final Gauge TOMCAT_MAX_THREADS_GAUGE = Gauge.build().labelNames(LABEL_SERVICE, LABEL_IP).name(TOMCAT_MAX_THREADS).help("tomcat info").register();
    public static final Gauge TOMCAT_CURRENT_THREAD_COUNT_GAUGE = Gauge.build().labelNames(LABEL_SERVICE, LABEL_IP).name(TOMCAT_CURRENT_THREAD_COUNT).help("tomcat info").register();
    public static final Gauge TOMCAT_CURRENT_THREADS_BUSY_GAUGE = Gauge.build().labelNames(LABEL_SERVICE, LABEL_IP).name(TOMCAT_CURRENT_THREADS_BUSY).help("tomcat info").register();

    /**
     * ----------------- Tomcat Request ----------------
     */
    public static final Counter TOMCAT_REQUEST_COUNT_COUNTER = Counter.build().labelNames(LABEL_SERVICE, LABEL_IP).name(TOMCAT_REQUEST_COUNT).help("tomcat info").register();
    public static final Counter TOMCAT_ERROR_COUNT_COUNTER = Counter.build().labelNames(LABEL_SERVICE, LABEL_IP).name(TOMCAT_ERROR_COUNT).help("tomcat info").register();
    public static final Counter TOMCAT_PROCESSING_TIME_COUNTER = Counter.build().labelNames(LABEL_SERVICE, LABEL_IP).name(TOMCAT_PROCESSING_TIME).help("tomcat info").register();
    public static final Counter TOMCAT_BYTES_RECEIVED_COUNTER = Counter.build().labelNames(LABEL_SERVICE, LABEL_IP).name(TOMCAT_BYTES_RECEIVED).help("tomcat info").register();
    public static final Counter TOMCAT_BYTES_SENT_COUNTER = Counter.build().labelNames(LABEL_SERVICE, LABEL_IP).name(TOMCAT_BYTES_SENT).help("tomcat info").register();
}
