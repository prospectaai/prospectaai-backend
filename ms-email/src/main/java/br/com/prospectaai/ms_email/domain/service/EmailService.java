package br.com.prospectaai.ms_email.domain.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import br.com.prospectaai.shared.email.EmailEnvelope;

@Service
public class EmailService {
    @Autowired
    private JavaMailSender mailSender;

    public void sendEmail(EmailEnvelope emailEnvelope) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(emailEnvelope.getRecipient());
        message.setSubject(emailEnvelope.getTitle());
        message.setText(emailEnvelope.getEmailContent());
        mailSender.send(message);
    }
}
