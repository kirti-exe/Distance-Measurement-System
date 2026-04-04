# Ultrasonic Distance Monitoring System
## How to Run — With or Without IntelliJ

---

## Option 1 — Run the Fat JAR (Easiest, no IntelliJ needed)

This is a single JAR file that contains everything inside it.

### Step 1 — Prerequisites
- Java 8 installed (JDK or JRE)
- MySQL running on localhost:3306
- Database `distance_system` created with tables:
  - `distance_record`
  - `incident_log`

### Step 2 — Build the fat JAR (do this once)
If you have Maven installed:
```
mvn package
```
The JAR will be at:
```
target/UltrasonicDistanceMonitor-1.0.0.jar
```

### Step 3 — Run it
Double-click the JAR, or from command prompt:
```
java -jar target/UltrasonicDistanceMonitor-1.0.0.jar
```

---

## Option 2 — Run with libs folder (no Maven needed)

If Maven can't download dependencies (e.g. Windows 7 TLS issue),
download all JARs manually and put them in a folder called `libs/`.

### Download these JARs manually:

| Library         | Download URL |
|----------------|--------------|
| jSerialComm     | https://repo1.maven.org/maven2/com/fazecast/jSerialComm/2.10.4/jSerialComm-2.10.4.jar |
| mysql-connector | https://repo1.maven.org/maven2/mysql/mysql-connector-java/5.1.49/mysql-connector-java-5.1.49.jar |
| jfreechart      | https://repo1.maven.org/maven2/org/jfree/jfreechart/1.5.4/jfreechart-1.5.4.jar |
| jcommon         | https://repo1.maven.org/maven2/org/jfree/jcommon/1.0.24/jcommon-1.0.24.jar |
| flatlaf         | https://repo1.maven.org/maven2/com/formdev/flatlaf/3.4/flatlaf-3.4.jar |
| javax.mail      | https://repo1.maven.org/maven2/com/sun/mail/javax.mail/1.6.2/javax.mail-1.6.2.jar |

### Run with libs folder (Windows):
```
java -cp "target/classes;libs/*" Main
```

### Run with libs folder (Mac/Linux):
```
java -cp "target/classes:libs/*" Main
```

---

## Option 3 — Open in IntelliJ

1. Open IntelliJ → File → Open → select the project folder
2. Go to File → Project Structure → Libraries → + → Java
3. Add all JARs from the `libs/` folder
4. Set Run Configuration Main class to `Main`
5. Click Run

---

## MySQL Setup

Run these SQL statements before starting the app:

```sql
CREATE DATABASE IF NOT EXISTS distance_system;

USE distance_system;

CREATE TABLE IF NOT EXISTS distance_record (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    distance  DOUBLE,
    status    VARCHAR(20),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS incident_log (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    distance  DOUBLE,
    status    VARCHAR(20),
    note      TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

## Configuration

Before running, update these files with your own details:

**`src/main/java/model/AppConfig.java`**
```java
public static final String DB_URL      = "jdbc:mysql://localhost:3306/distance_system?useSSL=false";
public static final String DB_USER     = "root";
public static final String DB_PASSWORD = "your_mysql_password";
public static boolean SIMULATION_MODE  = true;  // false when Arduino connected
public static String  SERIAL_PORT      = "COM3"; // your Arduino COM port
```

**`src/main/java/controller/SosController.java`**
```java
private static final String FROM_EMAIL   = "your_email@gmail.com";
private static final String APP_PASSWORD = "your_16char_app_password";
private static final String TO_SMS       = "your_email@gmail.com";
```

---

## Dependencies Summary

| Library          | Version | Purpose                        |
|-----------------|---------|--------------------------------|
| jSerialComm      | 2.10.4  | Arduino serial communication   |
| mysql-connector  | 5.1.49  | MySQL database connection      |
| jfreechart       | 1.5.4   | Live distance chart            |
| jcommon          | 1.0.24  | Required by JFreeChart         |
| flatlaf          | 3.4     | Modern UI look and feel        |
| javax.mail       | 1.6.2   | SOS email alerts               |
