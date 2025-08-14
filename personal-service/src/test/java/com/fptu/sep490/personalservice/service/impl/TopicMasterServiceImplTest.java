package com.fptu.sep490.personalservice.service.impl;

import com.fptu.sep490.personalservice.service.TopicMasterService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TopicMasterServiceImplTest {

    @Test
    void canInstantiateAndImplementsInterface() {
        TopicMasterServiceImpl service = new TopicMasterServiceImpl();
        assertNotNull(service);
        assertTrue(service instanceof TopicMasterService);
    }
}
