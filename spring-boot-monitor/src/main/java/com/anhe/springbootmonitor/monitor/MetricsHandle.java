package com.anhe.springbootmonitor.monitor;

import com.alibaba.fastjson.JSON;
import org.jolokia.client.J4pClient;
import org.jolokia.client.request.J4pReadRequest;
import org.jolokia.client.request.J4pReadResponse;
import org.jolokia.client.request.J4pResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import javax.management.remote.JMXConnector;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MetricsHandle implements InitializingBean {

    private static Logger logger = LoggerFactory.getLogger(MetricsHandle.class);

    @Resource
    private DiscoveryClient discoveryClient;

    @Value("${prometheus.ignore.application.name}")
    private String[] ignoreAppNames;

    private final static Integer delay = 5000;
    private final static Integer period = 30000;

    private final static Set<String> ignoreAppNameSet = new HashSet<>();
    private final static Map<String, Long> calculateMap = new ConcurrentHashMap<>();

    @Override
    public void afterPropertiesSet() throws Exception {
        //init
        //不需要监控的服务
        for (String appName : ignoreAppNames) {
            ignoreAppNameSet.add(appName);
        }
        logger.info("JolokiaMonitor,ignoreAppNameSet,size:{},content:{}", ignoreAppNameSet.size(), JSON.toJSONString(ignoreAppNameSet));

        JMXConnector jmxConnector = null;
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    Map<String, List<ServiceInstance>> clientMap = doDiscoveryService();
                    logger.info("JolokiaMonitor,clientMap:{}", JSON.toJSONString(clientMap));
                    clientMap.forEach((serviceName, instanceList) -> instanceList.forEach(instance -> {

                        //遍历所有service
                        if (!ignoreAppNameSet.contains(serviceName)) {
                            setMetrics(serviceName, instance);
                        }

                    }));
                } catch (Throwable e) {
                    logger.error("JolokiaMonitor run fail:{}", e.getMessage(), e);

                } finally {
                    try {
                        if (jmxConnector != null) {
                            jmxConnector.close();
                        }
                    } catch (Exception e) {
                        logger.error("JolokiaMonitor,jmxConnector close fail", e);
                    }
                }
            }
        }, delay, period);
    }


    public Map<String, List<ServiceInstance>> doDiscoveryService() {
        Map<String, List<ServiceInstance>> clientMap = new HashMap<>();
        List<String> serviceIds = discoveryClient.getServices();
        if (!CollectionUtils.isEmpty(serviceIds)) {
            for (String s : serviceIds) {
                List<ServiceInstance> serviceInstances = discoveryClient.getInstances(s);
                if (!CollectionUtils.isEmpty(serviceInstances)) {
                    clientMap.put(s, serviceInstances);
                }
            }
        }
        return clientMap;
    }


    @Async
    public void setMetrics(String serviceName, ServiceInstance instance) {


        String url = instance.getUri() + "/jolokia";
        String service = serviceName.replace("-", "_");
        logger.info("j4pClient url:{}", url);
        try {
            J4pClient j4pClient = new J4pClient(url);

            //jvm内存
            J4pReadResponse resp = j4pClient.execute(new J4pReadRequest("java.lang:type=Memory",
                    "HeapMemoryUsage", "NonHeapMemoryUsage"));
            logger.info("Memory resp:{}", JSON.toJSONString(resp.getValue()));
            Map vals = resp.getValue();
            Map<String, Long> heapMemoryUsage = (Map) vals.get("HeapMemoryUsage");
            Map<String, Long> nonHeapMemoryUsage = (Map) vals.get("NonHeapMemoryUsage");

            MonitorMetrics.HEAP_MEMORY_USED_GAUGE.labels(service, instance.getHost()).set(heapMemoryUsage.get("used") / 1024 / 1024);
            MonitorMetrics.HEAP_MEMORY_MAX_GAUGE.labels(service, instance.getHost()).set(heapMemoryUsage.get("max") / 1024 / 1024);
            MonitorMetrics.NON_HEAP_MEMORY_USED_GAUGE.labels(service, instance.getHost()).set(nonHeapMemoryUsage.get("used") / 1024 / 1024);


            //tomcat 线程
            J4pResponse<J4pReadRequest> tomcatResp = j4pClient.execute(new J4pReadRequest("Tomcat:type=ThreadPool,name=\"http-nio-" + instance.getPort() + "\"",
                    "maxThreads", "currentThreadCount", "currentThreadsBusy"));
            Map<String, Long> tomcatValues = tomcatResp.getValue();
            MonitorMetrics.TOMCAT_MAX_THREADS_GAUGE.labels(service, instance.getHost()).set(tomcatValues.get("maxThreads"));
            MonitorMetrics.TOMCAT_CURRENT_THREAD_COUNT_GAUGE.labels(service, instance.getHost()).set(tomcatValues.get("currentThreadCount"));
            MonitorMetrics.TOMCAT_CURRENT_THREADS_BUSY_GAUGE.labels(service, instance.getHost()).set(tomcatValues.get("currentThreadsBusy"));

            //tomcat RT,QPS
            J4pResponse<J4pReadRequest> tomcatRequestCountResp = j4pClient.execute(new J4pReadRequest("Tomcat:type=GlobalRequestProcessor,name=\"http-nio-" + instance.getPort() + "\"",
                    MonitorMetrics.TOMCAT_REQUEST_COUNT, MonitorMetrics.TOMCAT_ERROR_COUNT, MonitorMetrics.TOMCAT_PROCESSING_TIME, MonitorMetrics.TOMCAT_BYTES_RECEIVED, MonitorMetrics.TOMCAT_BYTES_SENT));
            Map<String, Long> tomcatRequestCountRespValue = tomcatRequestCountResp.getValue();
            logger.info("tomcatRequestCountRespValue:{}", JSON.toJSONString(tomcatRequestCountRespValue));
//
            if (calculateMap.get(MonitorMetrics.TOMCAT_REQUEST_COUNT + service) == null) {
                setCalculateMap(tomcatRequestCountRespValue, service, instance.getHost());
            } else {


                MonitorMetrics.TOMCAT_REQUEST_COUNT_COUNTER.labels(service, instance.getHost())
                        .inc(tomcatRequestCountRespValue.get(MonitorMetrics.TOMCAT_REQUEST_COUNT) - calculateMap.get(MonitorMetrics.TOMCAT_REQUEST_COUNT + service));
                MonitorMetrics.TOMCAT_ERROR_COUNT_COUNTER.labels(service, instance.getHost())
                        .inc(tomcatRequestCountRespValue.get(MonitorMetrics.TOMCAT_ERROR_COUNT) - calculateMap.get(MonitorMetrics.TOMCAT_ERROR_COUNT + service));
                MonitorMetrics.TOMCAT_PROCESSING_TIME_COUNTER.labels(service, instance.getHost())
                        .inc(tomcatRequestCountRespValue.get(MonitorMetrics.TOMCAT_PROCESSING_TIME) - calculateMap.get(MonitorMetrics.TOMCAT_PROCESSING_TIME + service));
                MonitorMetrics.TOMCAT_BYTES_RECEIVED_COUNTER.labels(service, instance.getHost())
                        .inc(tomcatRequestCountRespValue.get(MonitorMetrics.TOMCAT_BYTES_RECEIVED) - calculateMap.get(MonitorMetrics.TOMCAT_BYTES_RECEIVED + service));
                MonitorMetrics.TOMCAT_BYTES_SENT_COUNTER.labels(service, instance.getHost())
                        .inc(tomcatRequestCountRespValue.get(MonitorMetrics.TOMCAT_BYTES_SENT) - calculateMap.get(MonitorMetrics.TOMCAT_BYTES_SENT + service));

                setCalculateMap(tomcatRequestCountRespValue, service, instance.getHost());

            }

        } catch (Throwable e) {
            logger.error("ThreadMonitor run fail:{}", e.getMessage(), e);
            MonitorMetrics.HEAP_MEMORY_USED_GAUGE.labels(service, instance.getHost()).set(0);
            MonitorMetrics.HEAP_MEMORY_MAX_GAUGE.labels(service, instance.getHost()).set(0);
            MonitorMetrics.NON_HEAP_MEMORY_USED_GAUGE.labels(service, instance.getHost()).set(0);
            MonitorMetrics.TOMCAT_MAX_THREADS_GAUGE.labels(service, instance.getHost()).set(0);
            MonitorMetrics.TOMCAT_CURRENT_THREAD_COUNT_GAUGE.labels(service, instance.getHost()).set(0);
            MonitorMetrics.TOMCAT_CURRENT_THREADS_BUSY_GAUGE.labels(service, instance.getHost()).set(0);

        }
    }

    private void setCalculateMap(Map<String, Long> tomcatRequestCountRespValue, String service, String host) {
        calculateMap.put(MonitorMetrics.TOMCAT_REQUEST_COUNT + service + host, tomcatRequestCountRespValue.get(MonitorMetrics.TOMCAT_REQUEST_COUNT));
        calculateMap.put(MonitorMetrics.TOMCAT_ERROR_COUNT + service + host, tomcatRequestCountRespValue.get(MonitorMetrics.TOMCAT_ERROR_COUNT));
        calculateMap.put(MonitorMetrics.TOMCAT_PROCESSING_TIME + service + host, tomcatRequestCountRespValue.get(MonitorMetrics.TOMCAT_PROCESSING_TIME));
        calculateMap.put(MonitorMetrics.TOMCAT_BYTES_RECEIVED + service + host, tomcatRequestCountRespValue.get(MonitorMetrics.TOMCAT_BYTES_RECEIVED));
        calculateMap.put(MonitorMetrics.TOMCAT_BYTES_SENT + service + host, tomcatRequestCountRespValue.get(MonitorMetrics.TOMCAT_BYTES_SENT));
    }



}
