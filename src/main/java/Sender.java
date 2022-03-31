import javax.mail.*;
import javax.mail.internet.*;
import java.util.Map;
import java.util.Properties;

public class Sender {
    public void sendMails(Map<String, String> data, String name, String password) throws MessagingException {
        Properties prop = new Properties();
        prop.put("mail.smtp.host", "smtp.mail.ru");
        prop.put("mail.smtp.socketFactory.port", "465");
        prop.put("mail.smtp.socketFactory.class",
                 "javax.net.ssl.SSLSocketFactory");
        prop.put("mail.smtp.auth", "true");
        prop.put("mail.smtp.port", "805");

        Session session = Session.getInstance(prop, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(name, password);
            }
        });

        for (Map.Entry<String, String> entry : data.entrySet()) {
            String email = entry.getKey();
            String msg = entry.getValue();

            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress("misha2kon@mail.ru"));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email));
            message.setSubject("PASSWORD");
            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(msg, "text/html; charset=utf-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);

            message.setContent(multipart);

            Transport.send(message);
        }
    }
}
