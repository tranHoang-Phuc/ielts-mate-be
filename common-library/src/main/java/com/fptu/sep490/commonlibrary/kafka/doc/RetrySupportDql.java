package com.fptu.sep490.commonlibrary.kafka.doc;

import org.springframework.core.annotation.AliasFor;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.SameIntervalTopicReuseStrategy;
import org.springframework.retry.annotation.Backoff;

import java.lang.annotation.*;

@Documented
@RetryableTopic
@Target({ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RetrySupportDql {

    @AliasFor(annotation = RetryableTopic.class, attribute = "backoff")
    Backoff backoff() default @Backoff(value = 6000);

    @AliasFor(annotation = RetryableTopic.class, attribute = "attempts")
    String attempts() default "4";

    @AliasFor(annotation = RetryableTopic.class, attribute = "autoCreateTopics")
    String autoCreateTopics() default "true";

    @AliasFor(annotation = RetryableTopic.class, attribute = "listenerContainerFactory")
    String listenerContainerFactory() default "";

    @AliasFor(annotation = RetryableTopic.class, attribute = "exclude")
    Class<? extends Throwable>[] exclude() default {};

    @AliasFor(annotation = RetryableTopic.class, attribute = "sameIntervalTopicReuseStrategy")
    SameIntervalTopicReuseStrategy sameIntervalTopicReuseStrategy() default SameIntervalTopicReuseStrategy.SINGLE_TOPIC;

}
