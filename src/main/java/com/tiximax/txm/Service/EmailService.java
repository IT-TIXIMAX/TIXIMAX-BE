package com.tiximax.txm.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import com.sendgrid.*;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Email;
import com.tiximax.txm.Model.DTOResponse.Auth.EmailDetail;
import com.sendgrid.helpers.mail.objects.Content;

import java.io.IOException;

@Service
public class EmailService {

    @Autowired
    private TemplateEngine templateEngine;

    private final String FROM_EMAIL = "thinhvan.231003@gmail.com";

    @Value("${sendgrid.api.key}")
    private String sendGridKey;

    // 📨 Gửi email với template chung
    public void sendMailTemplate(EmailDetail emailDetail) {
        try {
            Context context = new Context();
            context.setVariable("name", emailDetail.getFullName());
            context.setVariable("link", emailDetail.getLink());
            context.setVariable("button", emailDetail.getButtonValue());

            String html = templateEngine.process("emailtemplate", context);
            sendEmail(emailDetail.getRecipient(), emailDetail.getSubject(), html);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 📨 Gửi email thông báo tùy chỉnh (có template khác)
    public void sendMailNotification(EmailDetail emailDetail, String template) {
        try {
            Context context = new Context();
            context.setVariable("name", emailDetail.getFullName());
            context.setVariable("link", emailDetail.getLink());
            context.setVariable("button", emailDetail.getButtonValue());
            context.setVariable("valuation", emailDetail.getValuation());
            context.setVariable("productName", emailDetail.getProductName());
            context.setVariable("date", emailDetail.getDate());
            context.setVariable("auctionId", emailDetail.getAuctionId());

            String html = templateEngine.process(template, context);
            sendEmail(emailDetail.getRecipient(), emailDetail.getSubject(), html);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 📨 Gửi thông báo phiên đấu giá (async)
    @Async
    public void sendMailNotificationSession(EmailDetail emailDetail, String template) {
        try {
            Context context = new Context();
            context.setVariable("name", emailDetail.getFullName());
            context.setVariable("link", emailDetail.getLink());
            context.setVariable("button", emailDetail.getButtonValue());
            context.setVariable("productName", emailDetail.getProductName());

            String html = templateEngine.process(template, context);
            sendEmail(emailDetail.getRecipient(), emailDetail.getSubject(), html);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 📨 Gửi OTP (async)
    @Async
    public void sendOtp(EmailDetail emailDetail, String otp) {
        try {
            Context context = new Context();
            context.setVariable("name", emailDetail.getFullName());
            context.setVariable("otp", otp);
            context.setVariable("purpose", emailDetail.getSubject());

            String html = templateEngine.process("otptemplate", context);
            sendEmail(emailDetail.getRecipient(), "Your OTP Code - Tiximax", html);

            System.out.println("✅ OTP email sent successfully to " + emailDetail.getRecipient());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Failed to send OTP email: " + e.getMessage());
        }
    }

    // ==============================
    // 🔧 HÀM CHUNG GỬI EMAIL QUA SENDGRID
    // ==============================
    private void sendEmail(String toEmail, String subject, String htmlContent) throws IOException {
        Email from = new Email(FROM_EMAIL);
        Email to = new Email(toEmail);
        Content content = new Content("text/html", htmlContent);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(sendGridKey);
        Request request = new Request();

        request.setMethod(Method.POST);
        request.setEndpoint("mail/send");
        request.setBody(mail.build());

        Response response = sg.api(request);
        System.out.println("📬 SendGrid Response Code: " + response.getStatusCode());
    }
}
