package com.fptu.sep490.identityservice.service.impl;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class EmailTemplateServiceImplTest {

    private EmailTemplateServiceImpl newServiceWith(TemplateEngine engine) throws Exception {
        Constructor<EmailTemplateServiceImpl> ctor = EmailTemplateServiceImpl.class
                .getDeclaredConstructor(TemplateEngine.class);
        ctor.setAccessible(true);
        return ctor.newInstance(engine);
    }

    @Test
    void buildVerificationEmail_usesTemplateAndSetsOtp() throws Exception {
        TemplateEngine engine = mock(TemplateEngine.class);
        when(engine.process(anyString(), any(Context.class))).thenReturn("rendered");
        EmailTemplateServiceImpl svc = newServiceWith(engine);

        String rendered = svc.buildVerificationEmail("123456");
        assertEquals("rendered", rendered);

        ArgumentCaptor<String> templateCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(engine).process(templateCaptor.capture(), contextCaptor.capture());
        assertEquals("verify-email", templateCaptor.getValue());
        assertEquals("123456", contextCaptor.getValue().getVariable("otp"));
    }

    @Test
    void buildForgotPasswordEmail_usesTemplateAndSetsUrl() throws Exception {
        TemplateEngine engine = mock(TemplateEngine.class);
        when(engine.process(anyString(), any(Context.class))).thenReturn("rendered-forgot");
        EmailTemplateServiceImpl svc = newServiceWith(engine);

        String rendered = svc.buildForgotPasswordEmail("http://reset");
        assertEquals("rendered-forgot", rendered);

        ArgumentCaptor<String> templateCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(engine).process(templateCaptor.capture(), contextCaptor.capture());
        assertEquals("forgot-password", templateCaptor.getValue());
        assertEquals("http://reset", contextCaptor.getValue().getVariable("forgotPasswordUrl"));
    }

    @Test
    void buildEmailVerificationSuccess_usesTemplateAndSetsEmailAndFullName() throws Exception {
        TemplateEngine engine = mock(TemplateEngine.class);
        when(engine.process(anyString(), any(Context.class))).thenReturn("rendered-success");
        EmailTemplateServiceImpl svc = newServiceWith(engine);

        String rendered = svc.buildEmailVerificationSuccess("e@x.com", "First Last");
        assertEquals("rendered-success", rendered);

        ArgumentCaptor<String> templateCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(engine).process(templateCaptor.capture(), contextCaptor.capture());
        assertEquals("email-verified", templateCaptor.getValue());
        Context ctx = contextCaptor.getValue();
        assertEquals("e@x.com", ctx.getVariable("email"));
        assertEquals("First Last", ctx.getVariable("fullName"));
    }
}
