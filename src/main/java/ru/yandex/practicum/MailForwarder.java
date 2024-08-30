package ru.yandex.practicum;

import com.itextpdf.pdfcleanup.PdfCleaner;
import com.itextpdf.pdfcleanup.autosweep.ICleanupStrategy;
import com.itextpdf.pdfcleanup.autosweep.RegexBasedCleanupStrategy;
import com.spire.pdf.PdfPageBase;
import com.spire.pdf.texts.PdfTextReplaceOptions;
import com.spire.pdf.texts.PdfTextReplacer;
import com.spire.pdf.texts.ReplaceActionType;
import com.sun.mail.util.MailConnectException;
import lombok.extern.slf4j.Slf4j;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.mail.*;
import javax.mail.internet.*;
import java.io.*;
import java.net.SocketException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.text.DateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.text.StringEscapeUtils;

@Slf4j
public class MailForwarder {

    private static Long minId = 30258L;
    private static Long maxId;
    private static long mailNumber;
    private static Integer batchSize = 5;
    private static File file = new File("src/main/resources/Коммерческое предложение.pdf");
    private static String tempFile = "src/main/resources/temp/Коммерческое предложение_temp.pdf";
    private static String targetFile = "src/main/resources/temp/Коммерческое предложение.pdf";
    private static String host = "mail.ls-gp.com";
    private static int port = 587;
    private static  String username = "o.kuznecov@ls-gp.com";
    private static String password = "013120161c";
    private static final String REPLACE_CACHE_DOC_NUMBER = "$</w:t></w:r><w:bookmarkStart w:id=\"0\" w:name=\"_Hlk15551868\"/><w:r><w:rPr><w:rFonts w:cs=\"\" w:asciiTheme=\"minorHAnsi\" w:cstheme=\"minorHAnsi\" w:hAnsiTheme=\"minorHAnsi\"/><w:sz w:val=\"22\"/><w:szCs w:val=\"22\"/><w:lang w:val=\"en-US\"/></w:rPr><w:t>number</w:t></w:r><w:bookmarkEnd w:id=\"0\"/><w:r><w:rPr><w:rFonts w:cs=\"\" w:asciiTheme=\"minorHAnsi\" w:cstheme=\"minorHAnsi\" w:hAnsiTheme=\"minorHAnsi\"/><w:sz w:val=\"22\"/><w:szCs w:val=\"22\"/><w:lang w:val=\"en-US\"/></w:rPr><w:t xml:space=\"preserve\"> </w:t></w:r><w:r><w:rPr><w:rFonts w:cs=\"\" w:asciiTheme=\"minorHAnsi\" w:cstheme=\"minorHAnsi\" w:hAnsiTheme=\"minorHAnsi\"/><w:sz w:val=\"22\"/><w:szCs w:val=\"22\"/></w:rPr><w:t xml:space=\"preserve\">от </w:t></w:r><w:r><w:rPr><w:rFonts w:cs=\"\" w:asciiTheme=\"minorHAnsi\" w:cstheme=\"minorHAnsi\" w:hAnsiTheme=\"minorHAnsi\"/><w:sz w:val=\"22\"/><w:szCs w:val=\"22\"/><w:lang w:val=\"en-US\"/></w:rPr><w:t>date</w:t></w:r><w:r><w:rPr><w:rFonts w:cs=\"\" w:asciiTheme=\"minorHAnsi\" w:cstheme=\"minorHAnsi\" w:hAnsiTheme=\"minorHAnsi\"/><w:sz w:val=\"22\"/><w:szCs w:val=\"22\"/></w:rPr><w:t>$";
    private static final String REPLACE_CACHE_USER = "$postfix name$";

    public MailForwarder() {
    }

    public static void main(String[] args) throws SQLException {
        final ConnectionPoolConfig config = new ConnectionPoolConfig(batchSize);
        log.info("Start process, batchSize = {}", batchSize);
        initialize(config);
        log.info("Initialization completed: minId = {}, maxId = {}", minId, maxId);

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", String.valueOf(port));
        props.put("mail.smtp.timeout", "30000");
        props.put("mail.smtp.connectiontimeout", "30000");

        Session session = Session.getInstance(props,
                new Authenticator() {
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });
        //session.setDebug(true);


//        log.info("Start batch process minId = {}", minId);
//        List<CompanyEntity> users = getUsers(config, minId);
//        log.info("Users for process = {}", users.size());
//        List<MailingResult> results = new ArrayList<>();
//        users.forEach(user -> results.addAll(sendMail(user, session)));
//        log.info("Results for saving = {}", results.size());
//        saveResults(results, config);
//        minId = users.getLast().getId() + 1;

        while (minId <= maxId) {
            log.info("Start batch process minId = {}", minId);
            List<CompanyEntity> users = getUsers(config, minId);
            log.info("Users for process = {}", users.size());
            List<MailingResult> results = new ArrayList<>();
            users.forEach(user -> results.addAll(sendMail(user, session)));
            log.info("Results for saving = {}", results.size());
            saveResults(results, config);
            minId = users.getLast().getId() + 1;
            log.info("min id = {}", minId);
        }

    }

    private static void initialize(ConnectionPoolConfig config) {
        log.info("Start initialization");
        try (Connection connection = config.getDataSource().getConnection()) {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT min(id) as minId, max(id) as maxId from public.db " +
                    "WHERE gender is not null and LENGTH(email) - LENGTH(REPLACE(email, ' ', '')) <= 3");
            while (rs.next()) {
                minId = minId != null ? minId : rs.getLong("minId");
                maxId = rs.getLong("maxId");
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        try (Connection connection = config.getDataSource().getConnection()) {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery("SELECT MAX(order_number) as mail_number from public.mailing_result");
            while (rs.next()) {
                mailNumber = rs.getObject("mail_number") == null ? 1 : rs.getLong("mail_number") + 1;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        log.info("Finish initialization");
    }

    private static void saveResults(List<MailingResult> results, ConnectionPoolConfig config) {
        log.info("Start saving results to DB: {}", results);
        try (Connection connection = config.getDataSource().getConnection()) {
            String insert = "INSERT INTO public.mailing_result(company_id, status_success, error_message, email_content, email, file_name, order_number) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement ps = connection.prepareStatement(insert);

            results.forEach(mailingResult -> {
                try {
                    ps.setLong(1, mailingResult.getCompanyId());
                    ps.setBoolean(2, mailingResult.getErrorMessage() == null);
                    ps.setString(3, mailingResult.getErrorMessage());
                    ps.setBinaryStream(4, new ByteArrayInputStream(mailingResult.getContent()));
                    ps.setString(5, mailingResult.getEmail());
                    ps.setString(6, mailingResult.getFilename());
                    ps.setLong(7, mailingResult.getOrderNumber());
                    ps.addBatch();
                } catch (NumberFormatException e) {
                    throw e;
                } catch (SQLException e) {
                    throw new RuntimeException(e);
                }
            });

            ps.executeBatch();
        } catch (SQLException e) {
            log.error("SQL exception while saving results to DB: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        log.info("Finish to save results to DB");
    }

    private static List<MailingResult> sendMail(CompanyEntity user, Session session) {
        log.info("Start sending mail message");
        // Create a PdfDocument object
        com.spire.pdf.PdfDocument doc = new com.spire.pdf.PdfDocument();

        // Load a PDF file
        doc.loadFromFile("src/main/resources/Коммерческое предложение.pdf");

        // Create a PdfTextReplaceOptions object
        PdfTextReplaceOptions textReplaceOptions = new PdfTextReplaceOptions();

        // Specify the options for text replacement
        textReplaceOptions.setReplaceType(EnumSet.of(ReplaceActionType.IgnoreCase));
        textReplaceOptions.setReplaceType(EnumSet.of(ReplaceActionType.WholeWord));

        // Get a specific page
        PdfPageBase page = doc.getPages().get(0);

        // Create a PdfTextReplacer object based on the page
        PdfTextReplacer textReplacer = new PdfTextReplacer(page);

        // Set the replace options
        textReplacer.setOptions(textReplaceOptions);

        String name = user.getGender().getPostfix() + " " + user.getFirstName() + (user.getMiddleName() != null ? " "
                + user.getMiddleName() : "");
        String mailN = "%d/%s от %s".formatted(mailNumber,  LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMM")),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));

        // Replace all instances of target text with new text
        textReplacer.replaceAllText("$number от date$", mailN);
        textReplacer.replaceAllText("$postfix name$", name);
        textReplacer.replaceAllText("Evaluation Warning : The document was created with Spire.PDF for Java.", "");

        // Save the document to a different PDF file
        doc.saveToFile("src/main/resources/temp/Коммерческое предложение_temp.pdf");

        // Dispose resources
        doc.dispose();

        String company = user.getCompanyName().replace("\"", "");
        String filename = "исх. %d_%s %s %s.pdf".formatted(mailNumber, LocalDateTime.now().format(DateTimeFormatter.ofPattern("ddMM")),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), company);

        InputStream inputPdf = null;
        OutputStream outputPdf = null;
        try {
            inputPdf = new FileInputStream("src/main/resources/temp/Коммерческое предложение_temp.pdf");
            outputPdf = new FileOutputStream("src/main/resources/temp/" + filename);
            ICleanupStrategy strategy = new RegexBasedCleanupStrategy("Evaluation Warning : The document was created with Spire.PDF for Java.");
            PdfCleaner.autoSweepCleanUp(inputPdf, outputPdf, strategy);
        } catch (Exception e) {
            log.error("Error while creating pdf file: {}", e.getMessage());
            //e.printStackTrace();
        } finally {
            if (inputPdf != null) {
                try {
                    inputPdf.close();
                } catch (IOException e) {
                    log.error("Failed to close inputPdf: {}", e.getMessage());
                    e.printStackTrace();
                }
            }
            if (outputPdf != null) {
                try {
                    outputPdf.close();
                } catch (IOException e) {
                    log.error("Failed to close outputPdf: {}", e.getMessage());
                    e.printStackTrace();
                }
            }
        }

        AtomicReference<String> errorMessage = new AtomicReference<>();

        List<MailingResult> results = user.getEmails().stream()
                .map(email -> {
                    log.info("Email process {}", email);
                    try {
                        int maxAttempts = 3;
                        int attempt = 0;
                        while (attempt < maxAttempts) {
                            try {
                                String recipient = createRecipient(user.getLastName(), user.getFirstName(), user.getMiddleName(), user.getGender());
                                String subject = "исх. %s %s".formatted(mailN.replace("от ", ""), recipient);
                                log.info("Recipient: {}", recipient);

                                Message message = new MimeMessage(session);
                                message.setFrom(new InternetAddress(username));
                                message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(email, false));
                                message.setSubject("Коммерческое предложение от компании ООО ЭЛЭС ГРУП");

                                // Создаем Multipart
                                Multipart multipart = new MimeMultipart();

                                // Добавляем текст письма в Multipart
                                MimeBodyPart textPart = new MimeBodyPart();
                                // Устанавливаем тип контента как HTML
                                textPart.setHeader("Content-Type", "text/html; charset=utf-8");
                                // Добавляем текст письма с HTML-тегом для переноса строки
                                textPart.setContent("<html><body>" +
                                        "<p>Добрый день!<br>" +
                                        "<br>Во вложении официальное письмо на тему: <br><strong>Автоматизация бизнес-процессов, связанных с ведением учета в программах семейства «1С».</strong><br>" +
                                        "<br>Если у Вас возникли вопросы, просто ответьте на это письмо и с Вами свяжется наш менеджер!<br></p>" +
                                        "<p>" +
                                        "<br><em>С уважением,</em><br><br>" +
                                        "<em>Кузнецов Олег Юрьевич</em><br>" +
                                        "<em>Руководитель направления развития 1С решений</em><br><br>" +
                                        "<em>тел. <strong>+7 (495) 080-89-22</strong></em><br>" +
                                        "<em>сот. <strong>+7 (925) 350-86-39</strong></em><br>" +
                                        "<em>почта: <strong>o.kuznecov@ls-gp.com</strong></em><br>" +
                                        "<p> <a href='https://ls-gp.ru/' style='color:blue;'><strong>LS GROUP</strong></a></p>" +
                                        //"<p style='color:gray;'>Чтобы отписаться от этой рассылки, перейдите по </p>" +
                                        "<p>" +
                                        "<hr>" +
                                        "<p style='color:lightgray; font-size: 14px; margin: 0; padding: 0;'>ООО 'ЭЛЭС ГРУП' ИНН 5047242766</p>" +
                                        "<p style='color:lightgray; font-size: 14px; margin: 0; padding: 0;'>Если вы хотите продолжать получать от нас письма с актуальной и полезной информацией по 1С, нажмите <a href='https://ls-gp.ru/podpiska-na-rassylku/' style='color:lightgray;size=4'>Подписаться</a></p>" +
                                        "<p style='color:lightgray; font-size: 14px; margin: 0; padding: 0;'>Если вы хотите отписаться от рассылки, нажмите <a href='https://ls-gp.ru/otpiska-ot-rassilki/' style='color:lightgray;size=4'>Отписаться</a></p>" +
                                        "</body></html>", "text/html; charset=utf-8");
                                multipart.addBodyPart(textPart);

                                // Добавляем вложение в Multipart
                                MimeBodyPart attachment = new MimeBodyPart();
                                FileDataSource fileDataSource = new FileDataSource("src/main/resources/temp/" + filename);
                                attachment.setDataHandler(new DataHandler(fileDataSource));
                                String filenameUpd = "Коммерческое предложение от компании ООО ЭЛЭС ГРУП.pdf";
                                String contentDispositionHeader = "attachment; filename=\"" + filenameUpd + "\"";
                                attachment.setHeader("Content-Disposition", contentDispositionHeader);
                                multipart.addBodyPart(attachment);

                                // Добавляем подпись в Multipart
//                                MimeBodyPart signature = new MimeBodyPart();
//                                signature.setHeader("Content-Type", "text/html; charset=utf-8");
//                                signature.setContent("<html><body>" +
//                                        "<br>С уважением,<br><br>" +
//                                        "Кузнецов Олег Юрьевич<br>" +
//                                        "Руководитель направления развития 1С решений<br>" +
//                                        "Компания «ЭЛЭС ГРУП»<br>" +
//                                        "тел. +7 (495) 080-89-22<br>" +
//                                        "сот. +7 (925) 350-86-39<br>" +
//                                        "o.kuznecov@ls-gp.com<br>" +
//                                        "https://ls-gp.ru</body></html>", "text/html; charset=utf-8");
//                                multipart.addBodyPart(signature);

                                // Устанавливаем Multipart в качестве содержимого письма
                                message.setContent(multipart);
                                Transport.send(message);
                                log.info("Message sent to {}", email);
                                break;
                            } catch (MessagingException e) {
                                if (e.getCause() instanceof MailConnectException || e.getCause() instanceof SocketException) {
                                    try {
                                        Thread.sleep(100_000L * attempt); // Sleep for 5 seconds before retrying
                                    } catch (InterruptedException ex) {
                                        ex.printStackTrace();
                                    }
                                    attempt++;
                                    if (attempt >= maxAttempts) {
                                        log.error("Failed to send message to {} after max attempt", email);
                                        errorMessage.set("MessagingException: " + e.getMessage());
                                    } else {
                                        log.warn("Try to send message to {}: {} attempt", email, attempt);
                                    }
                                } else {
                                    throw e;
                                }
                            }

                            try {
                                Thread.sleep(1000); // Sleep for 1 second
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    } catch (AddressException e) {
                        log.error("Error while sending message to {}: {}", email, e.getMessage());
                        //e.printStackTrace();
                        errorMessage.set("AddressException: " + e.getMessage());
                    } catch (MessagingException e) {
                        log.error("Error while sending message to {}: {}", email, e.getMessage());
                        //e.printStackTrace();
                        errorMessage.set("MessagingException: " + e.getMessage());
                    }

                    try {
                        MailingResult build = MailingResult.builder()
                                .errorMessage(errorMessage.get())
                                .email(email)
                                .content(Files.readAllBytes(Path.of("src/main/resources/temp/" + filename)))
                                .success(errorMessage.get() == null)
                                .companyId(user.getId())
                                .filename(filename)
                                .orderNumber(mailNumber)
                                .build();
                        return build;
                    } catch (IOException e) {
                        log.error("Error while create result after sending to {}: {}", email, e.getMessage());
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());

        try {
            FileUtils.forceDelete(new File("src/main/resources/temp/" + filename));
            FileUtils.forceDelete(new File(tempFile));
            mailNumber++;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return results;
    }

    private static List<CompanyEntity> getUsers(ConnectionPoolConfig config, Long minId) {
        log.info("Start getting users");
        try (Connection connection = config.getDataSource().getConnection()) {
            Statement statement = connection.createStatement();
            ResultSet rs = statement.executeQuery(batchSize == null ?
                    "SELECT id, company, first_name, middle_name, last_name, email, gender from public.db " +
                    "WHERE gender is not null and (trim(email) != '' and LENGTH(email) - LENGTH(REPLACE(email, ' ', '')) <= 3)" :
                    "SELECT id, first_name, middle_name, last_name, email, company, gender from public.db " +
                            "WHERE gender is not null and (trim(email) != '' and LENGTH(email) - LENGTH(REPLACE(email, ' ', '')) <= 3) " +
                            "AND id >= " + minId +
                            " ORDER BY id LIMIT " + batchSize);

            List<CompanyEntity> users = new ArrayList<>();
            while (rs.next()) {
                users.add(CompanyEntity.builder()
                        .id(rs.getLong("id"))
                        .firstName(rs.getString("first_name"))
                        .middleName(rs.getString("middle_name"))
                        .lastName(rs.getString("last_name"))
                        .companyName(rs.getString("company"))
                        .gender(Gender.getByShortName(rs.getString("gender")))
                        .emails(new HashSet<>(Arrays.asList(rs.getString("email").split(", "))))
                        .build());
            }
            log.info("Finish getting users");
            return users;
        } catch (SQLException e) {
            log.error("Error while getting users from public.db: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static String createRecipient(String lastName, String firstName, String middleName, Gender gender) {
        log.info("Start creating recipient text");
        return lastName != null ? switch (gender) {
            case MALE -> Set.of("ов", "ев", "ин", "цын").stream().anyMatch(lastName::endsWith) ? lastName + "y" :
                    (lastName.endsWith("ий") ? lastName.replace("ий", "ому") : lastName);
            case FEMALE -> Set.of("ова", "ева", "ина", "цына").stream().anyMatch(lastName::endsWith) ?
                    lastName.substring(0, lastName.length() - 1) + "ой" : (lastName.endsWith("ая") ?
                    lastName.substring(0, lastName.length() - 2) + "ой" : lastName);
        } + " " + firstName.charAt(0) + "." + (middleName != null && !middleName.isBlank() ? "" + middleName.charAt(0) + "." : "") : "";
    }

    private static byte[] toZip(byte[] file, String textDocNumber, String user) {
        ByteArrayOutputStream out = new ByteArrayOutputStream(file.length);

        try(ZipInputStream zip = new ZipInputStream( new ByteArrayInputStream(file));
            ZipOutputStream zipOut = new ZipOutputStream(out)) {
            byte[] oldBytes = IOUtils.toByteArray(zip);
            byte[] newBytes = replace(oldBytes, textDocNumber, user);
            zipOut.write(newBytes);
            zipOut.finish();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return out.toByteArray();
    }

    private static byte[] replace(byte[] file, String textDocNumber, String user) {
        Charset charset = getCharset(file);
        String source = new String(file, charset);
        String text = source;
        String value1 = StringEscapeUtils.escapeXml10(REPLACE_CACHE_DOC_NUMBER);
        String value2 = StringEscapeUtils.escapeXml10(REPLACE_CACHE_USER);
        text = replaceByPatterns(value1, text, textDocNumber);
        text = replaceByPatterns(value2, text, user);
        return source == text ? file : text.getBytes(charset);
    }

    private static String replaceByPatterns(String cache, String text, String value) {
        String result = text;
        Matcher matcher = Pattern.compile(cache).matcher(result);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            matcher.appendReplacement(sb, value);
        }
        if (!sb.isEmpty()) {
            matcher.appendTail(sb);
            result = sb.toString();
        }

        return result;
    }

    private static Charset getCharset(byte[] bytes) {
        String header = new String(bytes, 0, Math.min(500, bytes.length), StandardCharsets.UTF_8);
        int end = header.indexOf("?>");
        if (end > 0) {
            header = header.substring(0, end);
        }
        Matcher matcher = Pattern.compile(" encoding=\"([^\"]+)\"").matcher(header);
        if (matcher.find()) {
            return Charset.forName(matcher.group(1));
        }
        return StandardCharsets.UTF_8;
    }
}