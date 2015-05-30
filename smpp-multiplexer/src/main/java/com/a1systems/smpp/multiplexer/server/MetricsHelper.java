package com.a1systems.smpp.multiplexer.server;

import com.codahale.metrics.MetricRegistry;

public class MetricsHelper {
    public static final String JMX_SENDER_ERROR_NAME = "sender.exception";
    public static final String JMX_INPUT_SENDER_ERROR_NAME = "input.sender.exception";
    public static final String JMX_OUTPUT_SENDER_ERROR_NAME = "output.sender.exception";
    public static final String JMX_INPUT_SENDER_QUEUE_FULL = "input.sender.queuefull";
    public static final String JMX_OUTPUT_SENDER_QUEUE_FULL = "output.sender.queuefull";
    public static final String JMX_SENDER_QUEUE_FULL = "sender.queuefull";
    public static final String JMX_POOL_QUEUE_SIZE = "pool.queue.size";
    public static final String JMX_SENDER_PROCESSING_TOO_LONG = "sender.processing.toolong";
    public static final String JMX_INPUT_SENDER_PROCESSING_TOO_LONG = "input.sender.processing.toolong";
    public static final String JMX_OUTPUT_SENDER_PROCESSING_TOO_LONG = "output.sender.processing.toolong";
    public static final String JMX_OUTPUT_SENDER_NO_SESSION = "output.sender.nosession";
    public static final String JMX_INPUT_SENDER_NO_SESSION = "input.sender.nosession";
    public static final String JMX_SENDER_NO_SESSION = "sender.nosession";
    public static final String JMX_SENDER_SUCCESS = "sender.success";
    public static final String JMX_SENDER_REQUEST_SUCCESS = "sender.request.success";
    public static final String JMX_SENDER_RESPONSE_SUCCESS = "sender.response.success";
    public static final String JMX_INPUT_SENDER_SUCCESS = "input.sender.success";
    public static final String JMX_INPUT_SENDER_REQUEST_SUCCESS = "input.sender.request.success";
    public static final String JMX_INPUT_SENDER_RESPONSE_SUCCESS = "input.sender.response.success";
    public static final String JMX_OUTPUT_SENDER_SUCCESS = "output.sender.success";
    public static final String JMX_OUTPUT_SENDER_REQUEST_SUCCESS = "output.sender.request.success";
    public static final String JMX_OUTPUT_SENDER_RESPONSE_SUCCESS = "output.sender.response.success";
    public static final String JMX_FAILED_LOGINS = "login.failed";
    public static final String JMX_SUCCESS_LOGINS = "login.success";
    public static final String JMX_GAUGE_POOL_SIZE = "size.pool";
    public static final String JMX_GAUGE_ACTIVE_POOL_SIZE = "active.size.pool";
    public static final String JMX_GAUGE_ASYNC_POOL_SIZE = "size.async.pool";
    public static final String JMX_GAUGE_ACTIVE_ASYNC_POOL_SIZE = "active.async.size.pool";
    public static final String JMX_GAUGE_MONITOR_POOL_SIZE = "size.monitor.pool";
    public static final String JMX_GAUGE_ACTIVE_MONITOR_POOL_SIZE = "active.size.monitor.pool";
    public static final String JMX_GAUGE_MONITOR_POOL_QUEUE_SIZE = "queue.size.monitor.pool";
    public static final String JMX_GAUGE_POOL_QUEUE_SIZE = "queue.size.pool";
    public static final String JMX_GAUGE_ASYNC_POOL_QUEUE_SIZE = "queue.size.async.pool";
    public static final String JMX_GAUGE_HANDLERS_SIZE = "smpp.handlers.size";
    public static final String JMX_GAUGE_FAILEDLOGINS_SIZE = "failedlogins.size";
    
    public static void senderQueueFull(MetricRegistry registry) {
        registry.meter(JMX_SENDER_QUEUE_FULL).mark();
    }
    
    public static void successLogins(MetricRegistry registry) {
        registry.meter(JMX_SUCCESS_LOGINS).mark();
    }
    
    public static void failedLogins(MetricRegistry registry) {
        registry.meter(JMX_FAILED_LOGINS).mark();
    }
    
    public static void outputSenderQueueFull(MetricRegistry registry) {
        registry.meter(JMX_OUTPUT_SENDER_QUEUE_FULL).mark();
        senderQueueFull(registry);
    }
    
    public static void inputSenderQueueFull(MetricRegistry registry) {
        registry.meter(JMX_INPUT_SENDER_QUEUE_FULL).mark();
        senderQueueFull(registry);
    }
    
    public static void outputSenderException(MetricRegistry registry) {
        registry.meter(JMX_INPUT_SENDER_ERROR_NAME).mark();
        senderException(registry);
    }
    
    public static void inputSenderException(MetricRegistry registry) {
        registry.meter(JMX_OUTPUT_SENDER_ERROR_NAME).mark();
        senderException(registry);
    }
    
    public static void senderException(MetricRegistry registry) {
        registry.meter(JMX_SENDER_ERROR_NAME).mark();
    }
    
    public static void poolQueueSize(MetricRegistry registry, int size) {
        registry.meter(JMX_POOL_QUEUE_SIZE).mark(size);
    }
    
    public static void senderProcessingTooLong(MetricRegistry registry) {
        registry.meter(JMX_SENDER_PROCESSING_TOO_LONG).mark();
    }
    
    public static void inputSenderProcessingTooLong(MetricRegistry registry) {
        registry.meter(JMX_INPUT_SENDER_PROCESSING_TOO_LONG).mark();
        senderProcessingTooLong(registry);
    }
    
    public static void outputSenderProcessingTooLong(MetricRegistry registry) {
        registry.meter(JMX_OUTPUT_SENDER_PROCESSING_TOO_LONG).mark();
        senderProcessingTooLong(registry);
    }

    public static void outputSenderNoSession(MetricRegistry registry) {
        registry.meter(JMX_OUTPUT_SENDER_NO_SESSION).mark();
        senderNoSession(registry);
    }
    
    public static void inputSenderNoSession(MetricRegistry registry) {
        registry.meter(JMX_INPUT_SENDER_NO_SESSION).mark();
        senderNoSession(registry);
    }
    
    public static void senderNoSession(MetricRegistry registry) {
        registry.meter(JMX_SENDER_NO_SESSION).mark();
    }
    
    public static void senderSuccess(MetricRegistry registry) {
        registry.meter(JMX_SENDER_SUCCESS).mark();
    }
    
    public static void senderRequestSuccess(MetricRegistry registry) {
        registry.meter(JMX_SENDER_REQUEST_SUCCESS).mark();
        senderSuccess(registry);
    }
    
    public static void senderResponseSuccess(MetricRegistry registry) {
        registry.meter(JMX_SENDER_RESPONSE_SUCCESS).mark();
        senderSuccess(registry);
    }
    
    public static void inputSenderSuccess(MetricRegistry registry) {
        registry.meter(JMX_INPUT_SENDER_SUCCESS).mark();
    }
    
    public static void inputSenderRequestSuccess(MetricRegistry registry) {
        registry.meter(JMX_INPUT_SENDER_REQUEST_SUCCESS).mark();
        inputSenderSuccess(registry);
        senderRequestSuccess(registry);
    }
    
    public static void inputSenderResponseSuccess(MetricRegistry registry) {
        registry.meter(JMX_INPUT_SENDER_RESPONSE_SUCCESS).mark();
        inputSenderSuccess(registry);
        senderResponseSuccess(registry);
    }
    
    public static void outputSenderSuccess(MetricRegistry registry) {
        registry.meter(JMX_OUTPUT_SENDER_SUCCESS).mark();
    }
    
    public static void outputSenderRequestSuccess(MetricRegistry registry) {
        registry.meter(JMX_OUTPUT_SENDER_REQUEST_SUCCESS).mark();
        outputSenderSuccess(registry);
        senderRequestSuccess(registry);
    }
    
    public static void outputSenderResponseSuccess(MetricRegistry registry) {
        registry.meter(JMX_OUTPUT_SENDER_RESPONSE_SUCCESS).mark();
        outputSenderSuccess(registry);
        senderResponseSuccess(registry);
    }
}
