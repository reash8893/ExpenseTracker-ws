package com.org.Util;

import java.sql.Connection;
import java.sql.DriverManager;

public class OracleDBTest {

    public static void main(String[] args) {
        String url = "jdbc:oracle:thin:@localhost:1521/XEPDB1"; // Update if needed
        String username = "expensetracker";
        String password = "79432";

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            if (conn != null) {
                System.out.println("✅ Connection Successful!");
            } else {
                System.out.println("❌ Connection Failed!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
