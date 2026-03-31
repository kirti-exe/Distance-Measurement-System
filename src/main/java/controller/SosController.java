package controller;

import model.DistanceModel;
import model.DistanceReading;

import javax.mail.Message;

public class SosController {
    private final DistanceModel model;

    // details
    private static final String FROM_EMAIL = "kirti.exe@gmai.com";
    private static final String APP_PASSWORD = "lqwtrmbbkjtpxncv";
    private static final String TO_SMS = "7389640115@airtel.in";

    public SosController(DistanceModel model){
        this.model = model;
    }

    public void sendSOS() {
        new Thread(() -> {
            try {
                // ── Mail server properties ─────────────────────────────
                java.util.Properties props = new java.util.Properties();
                props.put("mail.smtp.auth",            "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host",            "smtp.gmail.com");
                props.put("mail.smtp.port",            "587");

                // ── Session ────────────────────────────────────────────
                javax.mail.Session session = javax.mail.Session.getInstance(props,
                        new javax.mail.Authenticator() {
                            protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                                return new javax.mail.PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
                            }
                        });

                // ── Build message ──────────────────────────────────────
                DistanceReading latest = model.getLatestReading();
                String distance = latest != null
                        ? String.format("%.1f cm", latest.getDistance())
                        : "unknown";
                String time = latest != null
                        ? latest.getFormattedTimestamp()
                        : new java.util.Date().toString();

                javax.mail.Message message = new javax.mail.internet.MimeMessage(session);
                message.setFrom(new javax.mail.internet.InternetAddress(FROM_EMAIL));
                message.setRecipients(javax.mail.Message.RecipientType.TO,
                        javax.mail.internet.InternetAddress.parse(TO_SMS));
                message.setSubject("SOS ALERT");
                message.setText(
                        "CRITICAL OBJECT DETECTED\n" +
                                "Distance: " + distance + "\n" +
                                "Time: " + time
                );

                // ── Send ───────────────────────────────────────────────
                javax.mail.Transport.send(message);
                System.out.println("SOS sent to " + TO_SMS);

            } catch (Exception e) {
                System.out.println("SOS failed: " + e.getMessage());
                e.printStackTrace();
            }
        }, "SosThread").start();
    }
}