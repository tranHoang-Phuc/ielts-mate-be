package com.fptu.sep490.personalservice.service.impl;

import com.fptu.sep490.personalservice.service.TopicMasterService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@Slf4j
public class TopicMasterServiceImpl implements TopicMasterService {
}
