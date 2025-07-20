package com.fptu.sep490.personalservice.service.impl;

import com.fptu.sep490.personalservice.service.EmailTemplateService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
@Slf4j
public class EmailTemplateServiceImpl implements EmailTemplateService {
    TemplateEngine templateEngine;
    @Override
    public String buildReminderTemplate() {
        Context context = new Context();
         return templateEngine.process("reminder", context);
    }
}
