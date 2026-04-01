package controller;

import model.DistanceModel;
import model.DistanceReading;

public class SosController {
    private final DistanceModel model;

    // details
    private static final String FROM_EMAIL = "kirti.exe@gmail.com";
    private static final String APP_PASSWORD = "lqwtrmbbkjtpxncv";
    private static final String TO_SMS = "7389640115@alerts.airtel.in";

    public SosController(DistanceModel model){
        this.model = model;
    }

    public void sendSOS() {
        new Thread(() -> {
            try {
                java.util.Properties props = new java.util.Properties();
                props.put("mail.smtp.auth",            "true");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.host",            "smtp.gmail.com");
                props.put("mail.smtp.port",            "587");

                javax.mail.Session session = javax.mail.Session.getInstance(props,
                        new javax.mail.Authenticator() {
                            protected javax.mail.PasswordAuthentication getPasswordAuthentication() {
                                return new javax.mail.PasswordAuthentication(FROM_EMAIL, APP_PASSWORD);
                            }
                        });

                model.DistanceReading latest = model.getLatestReading();
                java.lang.String distance = latest != null
                        ? java.lang.String.format("%.1f cm", latest.getDistance())
                        : "unknown";
                java.lang.String time = latest != null
                        ? latest.getFormattedTimestamp()
                        : new java.util.Date().toString();

                javax.mail.Message msg = new javax.mail.internet.MimeMessage(session);
                msg.setFrom(new javax.mail.internet.InternetAddress(FROM_EMAIL));
                msg.setRecipients(javax.mail.Message.RecipientType.TO,
                        javax.mail.internet.InternetAddress.parse(TO_SMS));
                msg.setSubject("\uD83D\uDEA8 SOS ALERT — Distance Monitoring System");
                msg.setText(
                                "CRITICAL OBJECT DETECTED\n" +
                                "━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                "Distance : " + distance + "\n" +
                                "Status   : CRITICAL\n" +
                                "Time     : " + time     + "\n" +
                                "━━━━━━━━━━━━━━━━━━━━━━━\n" +
                                "This is an automated alert from your\n" +
                                "Ultrasonic Distance Monitoring System."
                );

                javax.mail.Transport.send(msg);
                System.out.println("SOS sent to " + TO_SMS);

            } catch (Exception e) {
                System.out.println("SOS failed: " + e.getMessage());
                e.printStackTrace();
            }
        }, "SosThread").start();
    }
}