package com.fptu.sep490.notificationservice.service.impl;

import com.fptu.sep490.commonlibrary.constants.ErrorCodeMessage;
import com.fptu.sep490.commonlibrary.exceptions.BrevoException;
import com.fptu.sep490.commonlibrary.utils.MessagesUtils;
import com.fptu.sep490.event.ReminderEvent;
import com.fptu.sep490.event.RecipientUser;
import com.fptu.sep490.notificationservice.constants.Constants;
import com.fptu.sep490.notificationservice.repository.client.EmailClient;
import com.fptu.sep490.notificationservice.viewmodel.event.EmailResponse;
import com.fptu.sep490.notificationservice.viewmodel.event.consume.EmailSenderEvent;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class EmailServiceImplTest {

	@InjectMocks
	EmailServiceImpl service;
	@Mock EmailClient emailClient;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		ReflectionTestUtils.setField(service, "apiKey", "apikey");
	}

	@Test
	void sendEmail_success_callsClientWithApiKey() {
		RecipientUser recipient = RecipientUser.builder().email("user@example.com").firstName("John").lastName("Doe").build();
		EmailSenderEvent evt = EmailSenderEvent.builder()
				.recipientUser(recipient)
				.subject("Subject")
				.htmlContent("<b>Content</b>")
				.build();
		when(emailClient.sendEmail(anyString(), any())).thenReturn(new EmailResponse("id-123"));

		EmailResponse res = service.sendEmail(evt);

		assertEquals("id-123", res.messageId());
		verify(emailClient).sendEmail(eq("apikey"), any());
	}

	@Test
	void sendEmail_feignError_wrapsAsBrevoException() {
		RecipientUser recipient = RecipientUser.builder().email("user@example.com").firstName("John").lastName("Doe").build();
		EmailSenderEvent evt = EmailSenderEvent.builder()
				.recipientUser(recipient)
				.subject("Subject")
				.htmlContent("<b>Content</b>")
				.build();
		FeignException fe = mock(FeignException.class);
		doThrow(fe).when(emailClient).sendEmail(anyString(), any());

		BrevoException ex = assertThrows(BrevoException.class, () -> service.sendEmail(evt));
		assertEquals(MessagesUtils.getMessage(Constants.ErrorCode.ERROR_WHEN_SENDING_EMAIL), ex.getMessage());
	}

	@Test
	void test_success_callsClient() {
		when(emailClient.sendEmail(anyString(), any())).thenReturn(new EmailResponse("id-test"));
		EmailResponse res = service.test();
		assertEquals("id-test", res.messageId());
		verify(emailClient).sendEmail(eq("apikey"), any());
	}

	@Test
	void test_feignError_wrapsAsBrevoException() {
		FeignException fe = mock(FeignException.class);
		doThrow(fe).when(emailClient).sendEmail(anyString(), any());
		BrevoException ex = assertThrows(BrevoException.class, () -> service.test());
		assertEquals(MessagesUtils.getMessage(Constants.ErrorCode.ERROR_WHEN_SENDING_EMAIL), ex.getMessage());
	}

	@Test
	void sendReminder_success_callsClient() {
		ReminderEvent reminder = ReminderEvent.builder()
				.subject("S")
				.htmlContent("H")
				.email(List.of("a@x.com", "b@x.com"))
				.build();
		when(emailClient.sendEmail(anyString(), any())).thenReturn(new EmailResponse("id-r"));

		EmailResponse res = service.sendReminder(reminder);
		assertEquals("id-r", res.messageId());
		verify(emailClient).sendEmail(eq("apikey"), any());
	}

	@Test
	void sendReminder_feignError_wrapsAsBrevoException() {
		ReminderEvent reminder = ReminderEvent.builder()
				.subject("S")
				.htmlContent("H")
				.email(List.of("a@x.com"))
				.build();
		FeignException fe = mock(FeignException.class);
		doThrow(fe).when(emailClient).sendEmail(anyString(), any());

		BrevoException ex = assertThrows(BrevoException.class, () -> service.sendReminder(reminder));
		assertEquals(MessagesUtils.getMessage(Constants.ErrorCode.ERROR_WHEN_SENDING_EMAIL), ex.getMessage());
	}
}

