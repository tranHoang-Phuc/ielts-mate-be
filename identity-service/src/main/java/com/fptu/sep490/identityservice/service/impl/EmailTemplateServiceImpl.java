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

    @Override
    public String buildEmailVerificationSuccess(String email, String fullName) {
        Context context = new Context();
        context.setVariable("email", email);
        context.setVariable("fullName", fullName);
        return templateEngine.process("email-verified", context);
    }

    @Override
    public String buildCustomEmail(String email, String fullName, String moduleName) {
        Context context = new Context();
        context.setVariable("email", email);
        context.setVariable("fullName", fullName);
        context.setVariable("moduleName", moduleName);
        return templateEngine.process("share-modules", context);
    }


}
