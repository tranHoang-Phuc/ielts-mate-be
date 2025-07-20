package com.fptu.sep490.notificationservice.service;

import com.fptu.sep490.event.ReminderEvent;
import com.fptu.sep490.notificationservice.viewmodel.event.EmailResponse;
import com.fptu.sep490.notificationservice.viewmodel.event.consume.EmailSenderEvent;

public interface EmailService {
    EmailResponse sendEmail(EmailSenderEvent sendEmailRequest);

    EmailResponse test();

    EmailResponse sendReminder(ReminderEvent reminderRequest);
}
