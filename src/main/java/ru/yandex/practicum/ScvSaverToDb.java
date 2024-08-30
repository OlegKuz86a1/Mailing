package ru.yandex.practicum;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ScvSaverToDb {

    private static String host = "127.0.0.1";
    private static String user = "myuser";
    private static String password = "mypassword";
    private static String dbName = "mydatabase";
    private static Integer port = 5432;

    public static void main(String[] args) {
        save();
    }

    private static void save() {
        String url = String.format("jdbc:postgresql://%s:%d/%s", host, port, dbName);
        try (Connection connection = DriverManager.getConnection(url, user, password)) {
            String insert = "INSERT INTO db(company, tax_number, tax_code, address, first_name, last_name, middle_name, " +
                    "job_title, type_of_activity, tel_info, gain, request, site_1, site_2, email) " +
                    "VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement ps = connection.prepareStatement(insert);
            try (Reader reader = Files.newBufferedReader(Paths.get("src/main/resources/db.csv"))) {
                Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
                for (CSVRecord record : records) {
                    try {
                        ps.setString(1, record.get("company"));
                        ps.setLong(2, Long.parseLong(record.get("tax_number")));
                        ps.setLong(3, Long.parseLong(record.get("tax_code")));
                        ps.setString(4, record.get("address") != null ? record.get("address") : null);
                        ps.setString(5, record.get("first_name") != null ? record.get("first_name") : null);
                        ps.setString(6, record.get("last_name") != null ? record.get("last_name") : null);
                        ps.setString(7, record.get("middle_name") != null ? record.get("middle_name") : null);
                        ps.setString(8, record.get("job_title") != null ? record.get("job_title") : null);
                        ps.setString(9, record.get("type_of_activity") != null ? record.get("type_of_activity") : null);
                        ps.setString(10, record.get("tel_info") != null ? record.get("tel_info") : null);
                        String gain = record.get("gain") != null ? record.get("gain").replace(" ", "") : null;
                        ps.setObject(11, gain != null && !gain.trim().isBlank() ? Long.parseLong(gain) : null);
                        ps.setString(12, record.get("request") != null ? record.get("request") : null);
                        ps.setString(13, record.get("site_1") != null ? record.get("site_1") : null);
                        ps.setString(14, record.get("site_2") != null ? record.get("site_2") : null);
                        ps.setString(15, record.get("email"));
                    } catch (NumberFormatException e) {

                        throw e;
                    }

                    ps.addBatch();
                }
                ps.executeBatch();
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("ERROR while csv read");
            }
            System.out.println("SUCCESS");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

}
