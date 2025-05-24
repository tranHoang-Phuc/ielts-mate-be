package com.fptu.sep490.identityservice.service.impl;

import com.fptu.sep490.identityservice.service.EmailTemplateService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;


@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class EmailTemplateServiceImpl implements EmailTemplateService {
    TemplateEngine templateEngine;


    @Override
    public String buildVerificationEmail(String otp) {
        Context context = new Context();
        context.setVariable("otp", otp);
        return templateEngine.process("verify-email", context);
    }

    @Override
    public String buildForgotPasswordEmail(String url) {
        Context context = new Context();
        context.setVariable("forgotPasswordUrl", url);
        return templateEngine.process("forgot-password", context);
    }


}
