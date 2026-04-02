package za.ac.tut.integration;

import java.io.File;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import za.ac.tut.notification.EmailService;

public class EmailHealthCheck {

    public static void main(String[] args) throws Exception {
        ensureSmtpPropertiesLoaded();

        String from = requiredProperty("tickify.smtp.from");
        String to = System.getProperty("tickify.test.email.to", from).trim();
        String resetLink = System.getProperty(
                "tickify.test.email.resetLink",
                "http://localhost:8080/Tickify-SWP-Web-App/ClientPasswordReset.do?token=email-health-check");

        EmailService emailService = new EmailService();
        emailService.sendPasswordResetEmail(to, resetLink);

        System.out.println("EmailHealthCheck: PASS (sent to " + to + ")");
    }

    private static void ensureSmtpPropertiesLoaded() {
        if (hasSmtpProperties()) {
            return;
        }

        String[] candidateConfigs = new String[]{
            System.getProperty("tickify.domain.config", ""),
            System.getProperty("user.home") + "/GlassFish_Server/glassfish/domains/domain1/config/domain.xml"
        };

        for (String path : candidateConfigs) {
            if (path == null || path.trim().isEmpty()) {
                continue;
            }
            File file = new File(path.trim());
            if (!file.exists() || !file.isFile()) {
                continue;
            }
            if (loadFromDomainXml(file)) {
                return;
            }
        }
    }

    private static boolean loadFromDomainXml(File domainXml) {
        try {
            Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(domainXml);
            NodeList nodes = doc.getElementsByTagName("system-property");
            for (int i = 0; i < nodes.getLength(); i++) {
                Element el = (Element) nodes.item(i);
                String name = el.getAttribute("name");
                String value = el.getAttribute("value");
                if (name == null || value == null || value.trim().isEmpty()) {
                    continue;
                }
                if (name.startsWith("tickify.smtp.")) {
                    System.setProperty(name, value.trim());
                }
            }
            return hasSmtpProperties();
        } catch (Exception ex) {
            return false;
        }
    }

    private static boolean hasSmtpProperties() {
        return has("tickify.smtp.host")
                && has("tickify.smtp.user")
                && has("tickify.smtp.password")
                && has("tickify.smtp.from");
    }

    private static boolean has(String key) {
        String value = System.getProperty(key);
        return value != null && !value.trim().isEmpty();
    }

    private static String requiredProperty(String key) {
        String value = System.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException("Missing property: " + key);
        }
        return value.trim();
    }
}
