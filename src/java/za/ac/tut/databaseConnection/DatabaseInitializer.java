/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package za.ac.tut.databaseConnection;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import za.ac.tut.security.PasswordUtil;

/**
 *
 * @author ntoam
 */
public class DatabaseInitializer {
    
    public static void initialize() {
        try (Connection conn = DatabaseConnection.getConnection()) {
            createTables(conn);
            ensureEventMetadataColumns(conn);
            ensureClientProfileColumns(conn);
            ensureClientEmailVerificationColumns(conn);
            ensureUniqueClientUsernameIndexes(conn);
            ensureEngagementData(conn);
            ensureRootPasswordConfig(conn);
            seedData(conn);
            repairRoleCredentials(conn);
            ensureAdditionalCampusAttendees(conn, 100);
            seedAdverts(conn);
            System.out.println("Tickify DB initialized successfully.");
        } catch (SQLException e) {
            System.err.println("DB initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }
 
    // ----------------------------------------------------------------
    // DDL
    // ----------------------------------------------------------------
    private static void createTables(Connection conn) throws SQLException {
        Statement st = conn.createStatement();
 
        // venue
        if (!tableExists(conn, "VENUE")) {
            st.execute(
                "CREATE TABLE venue (" +
                "  venueID     INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  name        VARCHAR(45)," +
                "  address     VARCHAR(205)" +
                ")"
            );
        }
 
        // event
        if (!tableExists(conn, "EVENT")) {
            st.execute(
                "CREATE TABLE event (" +
                "  eventID  INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  name     VARCHAR(45)," +
                "  type     VARCHAR(45)," +
                "  date     TIMESTAMP," +
                "  venueID  INT NOT NULL," +
                "  description VARCHAR(1200)," +
                "  infoUrl VARCHAR(255)," +
                "  status VARCHAR(20) DEFAULT 'ACTIVE'," +
                "  imageFilename VARCHAR(255)," +
                "  imageMimeType VARCHAR(100)," +
                "  imageData BLOB," +
                "  FOREIGN KEY (venueID) REFERENCES venue(venueID)" +
                ")"
            );
        }

        ensureEventImageColumns(conn);
 
        // qrcode
        if (!tableExists(conn, "QRCODE")) {
            st.execute(
                "CREATE TABLE qrcode (" +
                "  QRcodeID  INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  barstring VARCHAR(255)," +
                "  number    INT NOT NULL" +
                ")"
            );
        }
 
        // admin
        if (!tableExists(conn, "ADMIN")) {
            st.execute(
                "CREATE TABLE admin (" +
                "  adminID   INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  firstname VARCHAR(45)," +
                "  lastname  VARCHAR(45)," +
                "  email     VARCHAR(45) UNIQUE," +
                "  password  VARCHAR(100)," +
                "  eventID   INT NOT NULL," +
                "  FOREIGN KEY (eventID) REFERENCES event(eventID)" +
                ")"
            );
        }
 
        // attendee
        if (!tableExists(conn, "ATTENDEE")) {
            st.execute(
                "CREATE TABLE attendee (" +
                "  attendeeID           INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  username             VARCHAR(45)," +
                "  clientType           VARCHAR(20)," +
                "  tertiaryInstitution  VARCHAR(45)," +
                "  phoneNumber          VARCHAR(25)," +
                "  studentNumber        VARCHAR(45)," +
                "  idPassportNumber     VARCHAR(45)," +
                "  dateOfBirth          DATE," +
                "  biography            VARCHAR(1200)," +
                "  firstname            VARCHAR(45)," +
                "  lastname             VARCHAR(45)," +
                "  email                VARCHAR(45) UNIQUE," +
                "  password             VARCHAR(100)," +
                "  emailVerified        BOOLEAN DEFAULT TRUE," +
                "  emailVerifiedAt      TIMESTAMP," +
                "  qrcode_QRcodeID      INT NOT NULL," +
                "  FOREIGN KEY (qrcode_QRcodeID) REFERENCES qrcode(QRcodeID)" +
                ")"
            );
        }
 
        // venue_guard
        if (!tableExists(conn, "VENUE_GUARD")) {
            st.execute(
                "CREATE TABLE venue_guard (" +
                "  venueGuardID INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  firstname    VARCHAR(45)," +
                "  lastname     VARCHAR(45)," +
                "  email        VARCHAR(45) UNIQUE," +
                "  password     VARCHAR(100)," +
                "  eventID      INT NOT NULL," +
                "  venueID      INT NOT NULL," +
                "  QRcodeID     INT NOT NULL," +
                "  FOREIGN KEY (eventID)  REFERENCES event(eventID)," +
                "  FOREIGN KEY (venueID)  REFERENCES venue(venueID)," +
                "  FOREIGN KEY (QRcodeID) REFERENCES qrcode(QRcodeID)" +
                ")"
            );
        }
 
        // event_manager
        if (!tableExists(conn, "EVENT_MANAGER")) {
            st.execute(
                "CREATE TABLE event_manager (" +
                "  eventManagerID INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  firstname      VARCHAR(45)," +
                "  lastname       VARCHAR(45)," +
                "  email          VARCHAR(45) UNIQUE," +
                "  password       VARCHAR(100)," +
                "  venueGuardID   INT NOT NULL," +
                "  FOREIGN KEY (venueGuardID) REFERENCES venue_guard(venueGuardID)" +
                ")"
            );
        }
 
        // tertiary_presenter
        if (!tableExists(conn, "TERTIARY_PRESENTER")) {
            st.execute(
                "CREATE TABLE tertiary_presenter (" +
                "  tertiaryPresenterID INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  username            VARCHAR(45)," +
                "  firstname           VARCHAR(45)," +
                "  lastname            VARCHAR(45)," +
                "  email               VARCHAR(45) UNIQUE," +
                "  password            VARCHAR(100)," +
                "  tertiaryInstitution VARCHAR(45)," +
                "  phoneNumber         VARCHAR(25)," +
                "  biography           VARCHAR(1200)," +
                "  emailVerified       BOOLEAN DEFAULT TRUE," +
                "  emailVerifiedAt     TIMESTAMP," +
                "  eventID             INT NOT NULL," +
                "  venueID             INT NOT NULL," +
                "  FOREIGN KEY (eventID) REFERENCES event(eventID)," +
                "  FOREIGN KEY (venueID) REFERENCES venue(venueID)" +
                ")"
            );
        }
 
        // ticket
        if (!tableExists(conn, "TICKET")) {
            st.execute(
                "CREATE TABLE ticket (" +
                "  ticketID  INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  name      VARCHAR(45)," +
                "  price     DECIMAL(6,2)," +
                "  QRcodeID  INT NOT NULL," +
                "  FOREIGN KEY (QRcodeID) REFERENCES qrcode(QRcodeID)" +
                ")"
            );
        }
 
        // junction tables
        if (!tableExists(conn, "ATTENDEE_HAS_EVENT")) {
            st.execute(
                "CREATE TABLE attendee_has_event (" +
                "  attendeeID INT NOT NULL," +
                "  eventID    INT NOT NULL," +
                "  PRIMARY KEY (attendeeID, eventID)," +
                "  FOREIGN KEY (attendeeID) REFERENCES attendee(attendeeID)," +
                "  FOREIGN KEY (eventID)    REFERENCES event(eventID)" +
                ")"
            );
        }
 
        if (!tableExists(conn, "ATTENDEE_HAS_TICKET")) {
            st.execute(
                "CREATE TABLE attendee_has_ticket (" +
                "  attendeeID INT NOT NULL," +
                "  ticketID   INT NOT NULL," +
                "  PRIMARY KEY (attendeeID, ticketID)," +
                "  FOREIGN KEY (attendeeID) REFERENCES attendee(attendeeID)," +
                "  FOREIGN KEY (ticketID)   REFERENCES ticket(ticketID)" +
                ")"
            );
        }

        if (!tableExists(conn, "ATTENDEE_WISHLIST")) {
            st.execute(
                "CREATE TABLE attendee_wishlist (" +
                "  attendeeID INT NOT NULL," +
                "  eventID    INT NOT NULL," +
                "  createdAt  TIMESTAMP NOT NULL," +
                "  PRIMARY KEY (attendeeID, eventID)," +
                "  FOREIGN KEY (attendeeID) REFERENCES attendee(attendeeID)," +
                "  FOREIGN KEY (eventID) REFERENCES event(eventID)" +
                ")"
            );
        }
 
        if (!tableExists(conn, "EVENT_HAS_TICKET")) {
            st.execute(
                "CREATE TABLE event_has_ticket (" +
                "  eventID  INT NOT NULL," +
                "  ticketID INT NOT NULL," +
                "  PRIMARY KEY (eventID, ticketID)," +
                "  FOREIGN KEY (eventID)  REFERENCES event(eventID)," +
                "  FOREIGN KEY (ticketID) REFERENCES ticket(ticketID)" +
                ")"
            );
        }
 
        if (!tableExists(conn, "EVENT_HAS_MANAGER")) {
            st.execute(
                "CREATE TABLE event_has_manager (" +
                "  eventID        INT NOT NULL," +
                "  eventManagerID INT NOT NULL," +
                "  PRIMARY KEY (eventID, eventManagerID)," +
                "  FOREIGN KEY (eventID)        REFERENCES event(eventID)," +
                "  FOREIGN KEY (eventManagerID) REFERENCES event_manager(eventManagerID)" +
                ")"
            );
        }
 
        if (!tableExists(conn, "EVENTMANAGER_HAS_TICKET")) {
            st.execute(
                "CREATE TABLE eventmanager_has_ticket (" +
                "  eventManagerID INT NOT NULL," +
                "  ticketID       INT NOT NULL," +
                "  PRIMARY KEY (eventManagerID, ticketID)," +
                "  FOREIGN KEY (eventManagerID) REFERENCES event_manager(eventManagerID)," +
                "  FOREIGN KEY (ticketID)       REFERENCES ticket(ticketID)" +
                ")"
            );
        }

        if (!tableExists(conn, "SCAN_LOG")) {
            st.execute(
                "CREATE TABLE scan_log (" +
                "  scanLogID    INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  venueGuardID INT NOT NULL," +
                "  ticketID     INT," +
                "  rawCode      VARCHAR(255)," +
                "  result       VARCHAR(20) NOT NULL," +
                "  reason       VARCHAR(255)," +
                "  scannedAt    TIMESTAMP NOT NULL," +
                "  FOREIGN KEY (venueGuardID) REFERENCES venue_guard(venueGuardID)," +
                "  FOREIGN KEY (ticketID) REFERENCES ticket(ticketID)" +
                ")"
            );
        }

        if (!tableExists(conn, "EVENT_ENGAGEMENT")) {
            st.execute(
                "CREATE TABLE event_engagement (" +
                "  engagementID INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  eventID      INT NOT NULL," +
                "  actorRole    VARCHAR(40)," +
                "  actorId      INT," +
                "  actionType   VARCHAR(40) NOT NULL," +
                "  channel      VARCHAR(40)," +
                "  createdAt    TIMESTAMP NOT NULL," +
                "  FOREIGN KEY (eventID) REFERENCES event(eventID)" +
                ")"
            );
        }

        if (!tableExists(conn, "ACCOUNT_CONTROL")) {
            st.execute(
                "CREATE TABLE account_control (" +
                "  controlID        INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  roleName         VARCHAR(40) NOT NULL," +
                "  userID           INT NOT NULL," +
                "  isLocked         BOOLEAN NOT NULL," +
                "  forceReset       BOOLEAN NOT NULL," +
                "  updatedByAdminID INT," +
                "  updatedAt        TIMESTAMP NOT NULL," +
                "  UNIQUE (roleName, userID)" +
                ")"
            );
        }

        if (!tableExists(conn, "ADMIN_AUDIT_LOG")) {
            st.execute(
                "CREATE TABLE admin_audit_log (" +
                "  adminAuditLogID INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  adminID         INT NOT NULL," +
                "  actionType      VARCHAR(80) NOT NULL," +
                "  targetTable     VARCHAR(80)," +
                "  targetID        VARCHAR(80)," +
                "  details         VARCHAR(1024)," +
                "  createdAt       TIMESTAMP NOT NULL," +
                "  FOREIGN KEY (adminID) REFERENCES admin(adminID)" +
                ")"
            );
        }

        if (!tableExists(conn, "DELETE_REQUEST")) {
            st.execute(
                "CREATE TABLE delete_request (" +
                "  deleteRequestID      INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  requestedByAdminID   INT NOT NULL," +
                "  targetRole           VARCHAR(40) NOT NULL," +
                "  targetUserID         INT NOT NULL," +
                "  reason               VARCHAR(1024)," +
                "  status               VARCHAR(20) NOT NULL," +
                "  requestedAt          TIMESTAMP NOT NULL," +
                "  resolvedByAdminID    INT," +
                "  resolvedAt           TIMESTAMP," +
                "  resolutionNote       VARCHAR(1024)," +
                "  FOREIGN KEY (requestedByAdminID) REFERENCES admin(adminID)," +
                "  FOREIGN KEY (resolvedByAdminID) REFERENCES admin(adminID)" +
                ")"
            );
        }

        if (!tableExists(conn, "SYSTEM_CONFIG")) {
            st.execute(
                "CREATE TABLE system_config (" +
                "  configKey   VARCHAR(80) PRIMARY KEY," +
                "  configValue VARCHAR(1024) NOT NULL," +
                "  updatedAt   TIMESTAMP NOT NULL" +
                ")"
            );
        }

        if (!tableExists(conn, "ADVERT")) {
            st.execute(
                "CREATE TABLE advert (" +
                "  advertID            INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  organizationName    VARCHAR(120) NOT NULL," +
                "  title               VARCHAR(160) NOT NULL," +
                "  details             VARCHAR(1024)," +
                "  venue               VARCHAR(255)," +
                "  eventDate           DATE," +
                "  paidOrganization    BOOLEAN NOT NULL," +
                "  selectedForDisplay  BOOLEAN NOT NULL," +
                "  active              BOOLEAN NOT NULL," +
                "  imageFilename       VARCHAR(255)," +
                "  imageMimeType       VARCHAR(100)," +
                "  imageData           BLOB," +
                "  createdAt           TIMESTAMP NOT NULL" +
                ")"
            );
        }

        if (!tableExists(conn, "ATTENDEE_ORDER")) {
            st.execute(
                "CREATE TABLE attendee_order (" +
                "  orderID          INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  attendeeID       INT NOT NULL," +
                "  transactionRef   VARCHAR(120)," +
                "  totalAmount      DECIMAL(10,2) NOT NULL," +
                "  status           VARCHAR(24) NOT NULL," +
                "  createdAt        TIMESTAMP NOT NULL," +
                "  FOREIGN KEY (attendeeID) REFERENCES attendee(attendeeID)" +
                ")"
            );
        }

        if (!tableExists(conn, "ATTENDEE_ORDER_ITEM")) {
            st.execute(
                "CREATE TABLE attendee_order_item (" +
                "  orderItemID       INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  orderID           INT NOT NULL," +
                "  eventID           INT NOT NULL," +
                "  quantity          INT NOT NULL," +
                "  unitPrice         DECIMAL(10,2) NOT NULL," +
                "  lineTotal         DECIMAL(10,2) NOT NULL," +
                "  FOREIGN KEY (orderID) REFERENCES attendee_order(orderID)," +
                "  FOREIGN KEY (eventID) REFERENCES event(eventID)" +
                ")"
            );
        }

        if (!tableExists(conn, "EVENT_PROPOSAL")) {
            st.execute(
                "CREATE TABLE event_proposal (" +
                "  proposalID         INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  submittedByAdminID INT NOT NULL," +
                "  venueID            INT NOT NULL," +
                "  eventName          VARCHAR(120) NOT NULL," +
                "  eventType          VARCHAR(80) NOT NULL," +
                "  eventDate          TIMESTAMP NOT NULL," +
                "  notes              VARCHAR(1024)," +
                "  status             VARCHAR(20) NOT NULL," +
                "  reviewedByAdminID  INT," +
                "  reviewedAt         TIMESTAMP," +
                "  reviewNote         VARCHAR(1024)," +
                "  createdAt          TIMESTAMP NOT NULL," +
                "  FOREIGN KEY (submittedByAdminID) REFERENCES admin(adminID)," +
                "  FOREIGN KEY (venueID) REFERENCES venue(venueID)," +
                "  FOREIGN KEY (reviewedByAdminID) REFERENCES admin(adminID)" +
                ")"
            );
        }

        if (!tableExists(conn, "ATTENDEE_REFUND_REQUEST")) {
            st.execute(
                "CREATE TABLE attendee_refund_request (" +
                "  refundRequestID     INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  attendeeID          INT NOT NULL," +
                "  orderID             INT," +
                "  eventID             INT," +
                "  requestedByAdminID  INT NOT NULL," +
                "  reason              VARCHAR(1024)," +
                "  status              VARCHAR(20) NOT NULL," +
                "  resolutionNote      VARCHAR(1024)," +
                "  resolvedByAdminID   INT," +
                "  requestedAt         TIMESTAMP NOT NULL," +
                "  resolvedAt          TIMESTAMP," +
                "  FOREIGN KEY (attendeeID) REFERENCES attendee(attendeeID)," +
                "  FOREIGN KEY (orderID) REFERENCES attendee_order(orderID)," +
                "  FOREIGN KEY (eventID) REFERENCES event(eventID)," +
                "  FOREIGN KEY (requestedByAdminID) REFERENCES admin(adminID)," +
                "  FOREIGN KEY (resolvedByAdminID) REFERENCES admin(adminID)" +
                ")"
            );
        }

        if (!tableExists(conn, "PRESENTER_MATERIAL")) {
            st.execute(
                "CREATE TABLE presenter_material (" +
                "  materialID          INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  tertiaryPresenterID INT NOT NULL," +
                "  title               VARCHAR(180) NOT NULL," +
                "  materialUrl         VARCHAR(512)," +
                "  description         VARCHAR(1024)," +
                "  createdAt           TIMESTAMP NOT NULL," +
                "  FOREIGN KEY (tertiaryPresenterID) REFERENCES tertiary_presenter(tertiaryPresenterID)" +
                ")"
            );
        }

        if (!tableExists(conn, "PRESENTER_SCHEDULE_ITEM")) {
            st.execute(
                "CREATE TABLE presenter_schedule_item (" +
                "  scheduleItemID      INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  tertiaryPresenterID INT NOT NULL," +
                "  title               VARCHAR(180) NOT NULL," +
                "  startsAt            TIMESTAMP NOT NULL," +
                "  endsAt              TIMESTAMP," +
                "  room                VARCHAR(120)," +
                "  notes               VARCHAR(1024)," +
                "  createdAt           TIMESTAMP NOT NULL," +
                "  FOREIGN KEY (tertiaryPresenterID) REFERENCES tertiary_presenter(tertiaryPresenterID)" +
                ")"
            );
        }

        if (!tableExists(conn, "PRESENTER_ANNOUNCEMENT")) {
            st.execute(
                "CREATE TABLE presenter_announcement (" +
                "  announcementID      INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY," +
                "  tertiaryPresenterID INT NOT NULL," +
                "  title               VARCHAR(180) NOT NULL," +
                "  body                VARCHAR(1600) NOT NULL," +
                "  createdAt           TIMESTAMP NOT NULL," +
                "  FOREIGN KEY (tertiaryPresenterID) REFERENCES tertiary_presenter(tertiaryPresenterID)" +
                ")"
            );
        }
 
        st.close();
    }

    private static void ensureRootPasswordConfig(Connection conn) throws SQLException {
        String checkSql = "SELECT 1 FROM system_config WHERE configKey = ?";
        try (PreparedStatement check = conn.prepareStatement(checkSql)) {
            check.setString(1, "ROOT_PASSWORD_HASH");
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) {
                    return;
                }
            }
        }

        String insertSql = "INSERT INTO system_config(configKey, configValue, updatedAt) VALUES(?, ?, CURRENT_TIMESTAMP)";
        try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
            insert.setString(1, "ROOT_PASSWORD_HASH");
            insert.setString(2, PasswordUtil.hashPassword("root123"));
            insert.executeUpdate();
        }
    }

    private static void repairRoleCredentials(Connection conn) throws SQLException {
        // Migrate any legacy plaintext credentials to PBKDF2 hashes.
        migrateLegacyPasswords(conn, "admin", "adminID");
        migrateLegacyPasswords(conn, "attendee", "attendeeID");
        migrateLegacyPasswords(conn, "venue_guard", "venueGuardID");
        migrateLegacyPasswords(conn, "event_manager", "eventManagerID");
        migrateLegacyPasswords(conn, "tertiary_presenter", "tertiaryPresenterID");

        // Enforce known seeded credentials so role smoke logins remain reliable.
        enforceSeededPassword(conn, "admin", "admin.tickify@gmail.com", "admin123");
        enforceSeededPassword(conn, "admin", "thabo.tickify@gmail.com", "pass1234");
        enforceSeededPassword(conn, "admin", "lerato.tickify@gmail.com", "pass5678");
        enforceSeededPassword(conn, "admin", "sipho.tickify@gmail.com", "pass9012");
        enforceSeededPassword(conn, "admin", "nomsa.tickify@gmail.com", "pass3456");

        enforceSeededPassword(conn, "attendee", "lekwene@student.dut.ac.za", "att001");
        enforceSeededPassword(conn, "attendee", "ntoampi@student.ukzn.ac.za", "att002");
        enforceSeededPassword(conn, "attendee", "mokoena@student.wits.ac.za", "att003");
        enforceSeededPassword(conn, "attendee", "sosiba@student.uj.ac.za", "att004");
        enforceSeededPassword(conn, "attendee", "mngadi@student.cput.ac.za", "att005");

        enforceSeededPassword(conn, "venue_guard", "guard1.tickify@gmail.com", "guard001");
        enforceSeededPassword(conn, "venue_guard", "guard2.tickify@gmail.com", "guard002");
        enforceSeededPassword(conn, "venue_guard", "guard3.tickify@gmail.com", "guard003");
        enforceSeededPassword(conn, "venue_guard", "guard4.tickify@gmail.com", "guard004");
        enforceSeededPassword(conn, "venue_guard", "guard5.tickify@gmail.com", "guard005");

        enforceSeededPassword(conn, "event_manager", "mgr1.tickify@gmail.com", "mgr001");
        enforceSeededPassword(conn, "event_manager", "mgr2.tickify@gmail.com", "mgr002");
        enforceSeededPassword(conn, "event_manager", "mgr3.tickify@gmail.com", "mgr003");
        enforceSeededPassword(conn, "event_manager", "mgr4.tickify@gmail.com", "mgr004");
        enforceSeededPassword(conn, "event_manager", "mgr5.tickify@gmail.com", "mgr005");

        enforceSeededPassword(conn, "tertiary_presenter", "pzulu.tickify@gmail.com", "pres001");
        enforceSeededPassword(conn, "tertiary_presenter", "dnaidoo.tickify@gmail.com", "pres002");
        enforceSeededPassword(conn, "tertiary_presenter", "psmith.tickify@gmail.com", "pres003");
        enforceSeededPassword(conn, "tertiary_presenter", "dbaloyi.tickify@gmail.com", "pres004");
        enforceSeededPassword(conn, "tertiary_presenter", "pjacobs.tickify@gmail.com", "pres005");
    }

    private static void migrateLegacyPasswords(Connection conn, String tableName, String idColumn) throws SQLException {
        String selectSql = "SELECT " + idColumn + ", password FROM " + tableName;
        String updateSql = "UPDATE " + tableName + " SET password = ? WHERE " + idColumn + " = ?";
        try (PreparedStatement select = conn.prepareStatement(selectSql);
             PreparedStatement update = conn.prepareStatement(updateSql);
             ResultSet rs = select.executeQuery()) {
            while (rs.next()) {
                String stored = rs.getString("password");
                if (stored == null) {
                    continue;
                }
                String trimmed = stored.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("pbkdf2$")) {
                    continue;
                }
                update.setString(1, PasswordUtil.hashPassword(trimmed));
                update.setInt(2, rs.getInt(idColumn));
                update.executeUpdate();
            }
        }
    }

    private static void enforceSeededPassword(Connection conn, String tableName, String email, String rawPassword) throws SQLException {
        String sql = "UPDATE " + tableName + " SET email = ?, password = ? WHERE LOWER(email) = LOWER(?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.toLowerCase());
            ps.setString(2, PasswordUtil.hashPassword(rawPassword));
            ps.setString(3, email);
            ps.executeUpdate();
        }
    }
 
    // ----------------------------------------------------------------
    // SEED DATA  (5+ records per main table)
    // ----------------------------------------------------------------
    private static void seedData(Connection conn) throws SQLException {
        if (rowCount(conn, "VENUE") > 0) return; // already seeded
 
        // venues
        int[] venueIDs = new int[5];
        String[] venueNames = {"DUT Sports Hall", "UKZN Great Hall", "Wits Amphitheatre", "UJ Auditorium", "CPUT Stadium"};
        String[] venueAddrs  = {"1 Steve Biko Rd, Durban", "Howard College, Durban", "1 Jan Smuts Ave, Johannesburg",
                                "55 Kingsway Ave, Auckland Park", "Symphony Way, Bellville"};
        PreparedStatement pv = conn.prepareStatement("INSERT INTO venue(name,address) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS);
        for (int i = 0; i < 5; i++) {
            pv.setString(1, venueNames[i]);
            pv.setString(2, venueAddrs[i]);
            pv.executeUpdate();
            try (ResultSet rs = pv.getGeneratedKeys()) { if (rs.next()) venueIDs[i] = rs.getInt(1); }
        }
        pv.close();
 
        // events
        int[] eventIDs = new int[5];
        String[][] events = {
            {"Tech Summit 2026","Conference","2026-06-15 09:00:00"},
            {"Music Fiesta","Concert","2026-07-20 18:00:00"},
            {"Science Expo","Exhibition","2026-08-05 10:00:00"},
            {"Cultural Day","Festival","2026-09-01 08:00:00"},
            {"Sports Gala","Sports","2026-10-10 14:00:00"}
        };
        PreparedStatement pe = conn.prepareStatement("INSERT INTO event(name,type,date,venueID) VALUES(?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
        for (int i = 0; i < 5; i++) {
            pe.setString(1, events[i][0]); pe.setString(2, events[i][1]);
            pe.setTimestamp(3, Timestamp.valueOf(events[i][2])); pe.setInt(4, venueIDs[i]);
            pe.executeUpdate();
            try (ResultSet rs = pe.getGeneratedKeys()) { if (rs.next()) eventIDs[i] = rs.getInt(1); }
        }
        pe.close();
 
        // qrcodes
        int[] qrIDs = new int[10];
        PreparedStatement pq = conn.prepareStatement("INSERT INTO qrcode(barstring,number) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS);
        for (int i = 0; i < 10; i++) {
            pq.setString(1, "QR-" + System.currentTimeMillis() + "-" + i); pq.setInt(2, 1000 + i);
            pq.executeUpdate();
            try (ResultSet rs = pq.getGeneratedKeys()) { if (rs.next()) qrIDs[i] = rs.getInt(1); }
        }
        pq.close();
 
        // admin
        PreparedStatement pa = conn.prepareStatement("INSERT INTO admin(firstname,lastname,email,password,eventID) VALUES(?,?,?,?,?)");
        String[][] admins = {
            {"System","Admin","admin.tickify@gmail.com","admin123"},
            {"Thabo","Nkosi","thabo.tickify@gmail.com","pass1234"},
            {"Lerato","Dlamini","lerato.tickify@gmail.com","pass5678"},
            {"Sipho","Mokoena","sipho.tickify@gmail.com","pass9012"},
            {"Nomsa","Khumalo","nomsa.tickify@gmail.com","pass3456"}
        };
        for (int i = 0; i < 5; i++) {
            pa.setString(1,admins[i][0]); pa.setString(2,admins[i][1]);
            pa.setString(3,admins[i][2]); pa.setString(4,PasswordUtil.hashPassword(admins[i][3])); pa.setInt(5,eventIDs[i]);
            pa.executeUpdate();
        }
        pa.close();
 
        // attendees
        String[][] attendees = {
            {"DUT","Lekwene","L","lekwene@student.dut.ac.za","att001"},
            {"UKZN","Ntoampi","LP","ntoampi@student.ukzn.ac.za","att002"},
            {"Wits","Mokoena","M","mokoena@student.wits.ac.za","att003"},
            {"UJ","Sosiba","Z","sosiba@student.uj.ac.za","att004"},
            {"CPUT","Mngadi","AM","mngadi@student.cput.ac.za","att005"}
        };
        int[] attIDs = new int[5];
        PreparedStatement patg = conn.prepareStatement("INSERT INTO attendee(username,clientType,tertiaryInstitution,phoneNumber,studentNumber,idPassportNumber,dateOfBirth,biography,firstname,lastname,email,password,qrcode_QRcodeID) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
        for (int i = 0; i < 5; i++) {
            patg.setString(1,("attendee" + (i + 1)));
            patg.setString(2,"STUDENT");
            patg.setString(3,attendees[i][0]);
            patg.setString(4,"000000000" + i);
            patg.setString(5,"STD-2026-" + (100 + i));
            patg.setString(6,"ID-ATT-" + (1000 + i));
            patg.setDate(7,Date.valueOf("2000-01-0" + (i + 1)));
            patg.setString(8,"Seeded attendee account for testing profile traceability.");
            patg.setString(9,attendees[i][1]);
            patg.setString(10,attendees[i][2]);
            patg.setString(11,attendees[i][3]);
            patg.setString(12,PasswordUtil.hashPassword(attendees[i][4]));
            patg.setInt(13,qrIDs[i]);
            patg.executeUpdate();
            try (ResultSet rs = patg.getGeneratedKeys()) { if (rs.next()) attIDs[i] = rs.getInt(1); }
        }
        patg.close();
 
        // venue_guard
        int[] guardIDs = new int[5];
        PreparedStatement pg = conn.prepareStatement(
            "INSERT INTO venue_guard(firstname,lastname,email,password,eventID,venueID,QRcodeID) VALUES(?,?,?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS);
        String[][] guards = {
            {"Guard","Alpha","guard1.tickify@gmail.com","guard001"},
            {"Guard","Bravo","guard2.tickify@gmail.com","guard002"},
            {"Guard","Charlie","guard3.tickify@gmail.com","guard003"},
            {"Guard","Delta","guard4.tickify@gmail.com","guard004"},
            {"Guard","Echo","guard5.tickify@gmail.com","guard005"}
        };
        for (int i = 0; i < 5; i++) {
            pg.setString(1,guards[i][0]); pg.setString(2,guards[i][1]);
            pg.setString(3,guards[i][2]); pg.setString(4,PasswordUtil.hashPassword(guards[i][3]));
            pg.setInt(5,eventIDs[i]); pg.setInt(6,venueIDs[i]); pg.setInt(7,qrIDs[5+i]);
            pg.executeUpdate();
            try (ResultSet rs = pg.getGeneratedKeys()) { if (rs.next()) guardIDs[i] = rs.getInt(1); }
        }
        pg.close();
 
        // event_manager
        int[] managerIDs = new int[5];
        PreparedStatement pm = conn.prepareStatement(
            "INSERT INTO event_manager(firstname,lastname,email,password,venueGuardID) VALUES(?,?,?,?,?)",
            Statement.RETURN_GENERATED_KEYS);
        String[][] managers = {
            {"Manager","One","mgr1.tickify@gmail.com","mgr001"},
            {"Manager","Two","mgr2.tickify@gmail.com","mgr002"},
            {"Manager","Three","mgr3.tickify@gmail.com","mgr003"},
            {"Manager","Four","mgr4.tickify@gmail.com","mgr004"},
            {"Manager","Five","mgr5.tickify@gmail.com","mgr005"}
        };
        for (int i = 0; i < 5; i++) {
            pm.setString(1,managers[i][0]); pm.setString(2,managers[i][1]);
            pm.setString(3,managers[i][2]); pm.setString(4,PasswordUtil.hashPassword(managers[i][3])); pm.setInt(5,guardIDs[i]);
            pm.executeUpdate();
            try (ResultSet rs = pm.getGeneratedKeys()) { if (rs.next()) managerIDs[i] = rs.getInt(1); }
        }
        pm.close();
 
        // tertiary_presenter
        PreparedStatement ptp = conn.prepareStatement(
            "INSERT INTO tertiary_presenter(username,firstname,lastname,email,password,tertiaryInstitution,phoneNumber,biography,eventID,venueID) VALUES(?,?,?,?,?,?,?,?,?,?)");
        String[][] presenters = {
            {"Prof","Zulu","pzulu.tickify@gmail.com","pres001","DUT"},
            {"Dr","Naidoo","dnaidoo.tickify@gmail.com","pres002","UKZN"},
            {"Prof","Smith","psmith.tickify@gmail.com","pres003","Wits"},
            {"Dr","Baloyi","dbaloyi.tickify@gmail.com","pres004","UJ"},
            {"Prof","Jacobs","pjacobs.tickify@gmail.com","pres005","CPUT"}
        };
        for (int i = 0; i < 5; i++) {
            ptp.setString(1,("presenter" + (i + 1)));
            ptp.setString(2,presenters[i][0]);
            ptp.setString(3,presenters[i][1]);
            ptp.setString(4,presenters[i][2]);
            ptp.setString(5,PasswordUtil.hashPassword(presenters[i][3]));
            ptp.setString(6,presenters[i][4]);
            ptp.setString(7,"011000000" + i);
            ptp.setString(8,"Seeded presenter account for testing profile traceability.");
            ptp.setInt(9,eventIDs[i]);
            ptp.setInt(10,venueIDs[i]);
            ptp.executeUpdate();
        }
        ptp.close();
 
        // tickets
        int[] ticketIDs = new int[5];
        PreparedStatement ptk = conn.prepareStatement(
            "INSERT INTO ticket(name,price,QRcodeID) VALUES(?,?,?)", Statement.RETURN_GENERATED_KEYS);
        String[] tNames  = {"General Admission","VIP","Student","Early Bird","Group"};
        double[] tPrices = {150.00, 350.00, 80.00, 120.00, 200.00};
        for (int i = 0; i < 5; i++) {
            ptk.setString(1,tNames[i]); ptk.setBigDecimal(2,new java.math.BigDecimal(tPrices[i])); ptk.setInt(3,qrIDs[i]);
            ptk.executeUpdate();
            try (ResultSet rs = ptk.getGeneratedKeys()) { if (rs.next()) ticketIDs[i] = rs.getInt(1); }
        }
        ptk.close();
 
        // junction rows
        PreparedStatement jahe = conn.prepareStatement("INSERT INTO attendee_has_event(attendeeID,eventID) VALUES(?,?)");
        PreparedStatement jaht = conn.prepareStatement("INSERT INTO attendee_has_ticket(attendeeID,ticketID) VALUES(?,?)");
        PreparedStatement jeht = conn.prepareStatement("INSERT INTO event_has_ticket(eventID,ticketID) VALUES(?,?)");
        PreparedStatement jehm = conn.prepareStatement("INSERT INTO event_has_manager(eventID,eventManagerID) VALUES(?,?)");
        for (int i = 0; i < 5; i++) {
            jahe.setInt(1,attIDs[i]); jahe.setInt(2,eventIDs[i]); jahe.executeUpdate();
            jaht.setInt(1,attIDs[i]); jaht.setInt(2,ticketIDs[i]); jaht.executeUpdate();
            jeht.setInt(1,eventIDs[i]); jeht.setInt(2,ticketIDs[i]); jeht.executeUpdate();
            jehm.setInt(1,eventIDs[i]); jehm.setInt(2,managerIDs[i]); jehm.executeUpdate();
        }
        jahe.close(); jaht.close(); jeht.close(); jehm.close();
 
        System.out.println("Seed data inserted successfully.");
    }

    private static void seedAdverts(Connection conn) throws SQLException {
        if (rowCount(conn, "ADVERT") > 0) {
            return;
        }

        byte[] imageBytes = loadDefaultAdvertImage();
        String mimeType = "image/svg+xml";

        String sql = "INSERT INTO advert(organizationName, title, details, venue, eventDate, paidOrganization, selectedForDisplay, active, imageFilename, imageMimeType, imageData, createdAt) "
                + "VALUES(?,?,?,?,?,?,?,?,?,?,?,CURRENT_TIMESTAMP)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "TUT Campus");
            ps.setString(2, "Freshers Ticket Sales Opening");
            ps.setString(3, "Freshers event ticket sales open on 14 April.");
            ps.setString(4, "TUT Emalahleni Sports Ground");
            ps.setDate(5, Date.valueOf("2026-04-14"));
            ps.setBoolean(6, true);
            ps.setBoolean(7, true);
            ps.setBoolean(8, true);
            ps.setString(9, "tickify-logo.svg");
            ps.setString(10, mimeType);
            ps.setBytes(11, imageBytes);
            ps.executeUpdate();
        }
    }

    private static byte[] loadDefaultAdvertImage() {
        Path path = Paths.get("/home/lintshiwe/Downloads/tickify-logo.svg");
        try {
            if (Files.exists(path)) {
                return Files.readAllBytes(path);
            }
        } catch (Exception ignored) {
        }

        String fallbackSvg = "<svg xmlns='http://www.w3.org/2000/svg' width='600' height='320' viewBox='0 0 600 320'>"
                + "<rect width='600' height='320' fill='#eef6e8'/>"
                + "<text x='40' y='120' font-size='44' fill='#4a5b4f' font-family='Segoe UI, Arial'>TUT Campus Freshers</text>"
                + "<text x='40' y='170' font-size='30' fill='#5da72f' font-family='Segoe UI, Arial'>Ticket sales open 14 April</text>"
                + "<text x='40' y='210' font-size='24' fill='#4a5b4f' font-family='Segoe UI, Arial'>Venue: TUT Emalahleni Sports Ground</text>"
                + "</svg>";
        return fallbackSvg.getBytes(StandardCharsets.UTF_8);
    }
 
    private static boolean tableExists(Connection conn, String tableName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private static boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        try (ResultSet rs = meta.getColumns(null, null, tableName, columnName)) {
            return rs.next();
        }
    }

    private static void ensureEventImageColumns(Connection conn) throws SQLException {
        if (!tableExists(conn, "EVENT")) {
            return;
        }

        try (Statement st = conn.createStatement()) {
            if (!columnExists(conn, "EVENT", "IMAGEFILENAME")) {
                st.execute("ALTER TABLE event ADD COLUMN imageFilename VARCHAR(255)");
            }
            if (!columnExists(conn, "EVENT", "IMAGEMIMETYPE")) {
                st.execute("ALTER TABLE event ADD COLUMN imageMimeType VARCHAR(100)");
            }
            if (!columnExists(conn, "EVENT", "IMAGEDATA")) {
                st.execute("ALTER TABLE event ADD COLUMN imageData BLOB");
            }
        }
    }

    private static void ensureEventMetadataColumns(Connection conn) throws SQLException {
        if (!tableExists(conn, "EVENT")) {
            return;
        }

        try (Statement st = conn.createStatement()) {
            if (!columnExists(conn, "EVENT", "DESCRIPTION")) {
                st.execute("ALTER TABLE event ADD COLUMN description VARCHAR(1200)");
            }
            if (!columnExists(conn, "EVENT", "INFOURL")) {
                st.execute("ALTER TABLE event ADD COLUMN infoUrl VARCHAR(255)");
            }
            if (!columnExists(conn, "EVENT", "STATUS")) {
                st.execute("ALTER TABLE event ADD COLUMN status VARCHAR(20)");
            }
            st.executeUpdate("UPDATE event SET status = 'ACTIVE' WHERE status IS NULL OR TRIM(status) = ''");
        }
    }

    private static void ensureClientProfileColumns(Connection conn) throws SQLException {
        if (tableExists(conn, "ATTENDEE")) {
            try (Statement st = conn.createStatement()) {
                if (!columnExists(conn, "ATTENDEE", "USERNAME")) {
                    st.execute("ALTER TABLE attendee ADD COLUMN username VARCHAR(45)");
                }
                if (!columnExists(conn, "ATTENDEE", "CLIENTTYPE")) {
                    st.execute("ALTER TABLE attendee ADD COLUMN clientType VARCHAR(20)");
                }
                if (!columnExists(conn, "ATTENDEE", "PHONENUMBER")) {
                    st.execute("ALTER TABLE attendee ADD COLUMN phoneNumber VARCHAR(25)");
                }
                if (!columnExists(conn, "ATTENDEE", "STUDENTNUMBER")) {
                    st.execute("ALTER TABLE attendee ADD COLUMN studentNumber VARCHAR(45)");
                }
                if (!columnExists(conn, "ATTENDEE", "IDPASSPORTNUMBER")) {
                    st.execute("ALTER TABLE attendee ADD COLUMN idPassportNumber VARCHAR(45)");
                }
                if (!columnExists(conn, "ATTENDEE", "DATEOFBIRTH")) {
                    st.execute("ALTER TABLE attendee ADD COLUMN dateOfBirth DATE");
                }
                if (!columnExists(conn, "ATTENDEE", "BIOGRAPHY")) {
                    st.execute("ALTER TABLE attendee ADD COLUMN biography VARCHAR(1200)");
                }

                // Backfill profile fields for older rows created before profile enrichment.
                st.executeUpdate("UPDATE attendee SET username = 'attendee' || TRIM(CAST(attendeeID AS CHAR(12))) WHERE username IS NULL OR TRIM(username) = ''");
                st.executeUpdate("UPDATE attendee SET clientType = 'STUDENT' WHERE clientType IS NULL OR TRIM(clientType) = ''");
                st.executeUpdate("UPDATE attendee SET tertiaryInstitution = 'Unspecified' WHERE tertiaryInstitution IS NULL OR TRIM(tertiaryInstitution) = ''");
                st.executeUpdate("UPDATE attendee SET phoneNumber = '0000000000' WHERE phoneNumber IS NULL OR TRIM(phoneNumber) = ''");
                st.executeUpdate("UPDATE attendee SET studentNumber = 'STD-' || TRIM(CAST(attendeeID AS CHAR(12))) WHERE studentNumber IS NULL OR TRIM(studentNumber) = ''");
                st.executeUpdate("UPDATE attendee SET idPassportNumber = 'ID-' || TRIM(CAST(attendeeID AS CHAR(12))) WHERE idPassportNumber IS NULL OR TRIM(idPassportNumber) = ''");
                st.executeUpdate("UPDATE attendee SET dateOfBirth = DATE('2000-01-01') WHERE dateOfBirth IS NULL");
                st.executeUpdate("UPDATE attendee SET biography = 'Auto-filled legacy profile for account completeness.' WHERE biography IS NULL OR TRIM(biography) = ''");
            }
        }

        if (tableExists(conn, "TERTIARY_PRESENTER")) {
            try (Statement st = conn.createStatement()) {
                if (!columnExists(conn, "TERTIARY_PRESENTER", "USERNAME")) {
                    st.execute("ALTER TABLE tertiary_presenter ADD COLUMN username VARCHAR(45)");
                }
                if (!columnExists(conn, "TERTIARY_PRESENTER", "PHONENUMBER")) {
                    st.execute("ALTER TABLE tertiary_presenter ADD COLUMN phoneNumber VARCHAR(25)");
                }
                if (!columnExists(conn, "TERTIARY_PRESENTER", "BIOGRAPHY")) {
                    st.execute("ALTER TABLE tertiary_presenter ADD COLUMN biography VARCHAR(1200)");
                }

                // Backfill profile fields for older rows created before profile enrichment.
                st.executeUpdate("UPDATE tertiary_presenter SET username = 'presenter' || TRIM(CAST(tertiaryPresenterID AS CHAR(12))) WHERE username IS NULL OR TRIM(username) = ''");
                st.executeUpdate("UPDATE tertiary_presenter SET tertiaryInstitution = 'Unspecified' WHERE tertiaryInstitution IS NULL OR TRIM(tertiaryInstitution) = ''");
                st.executeUpdate("UPDATE tertiary_presenter SET phoneNumber = '0000000000' WHERE phoneNumber IS NULL OR TRIM(phoneNumber) = ''");
                st.executeUpdate("UPDATE tertiary_presenter SET biography = 'Auto-filled legacy profile for account completeness.' WHERE biography IS NULL OR TRIM(biography) = ''");
            }
        }
    }

    private static void ensureClientEmailVerificationColumns(Connection conn) throws SQLException {
        if (tableExists(conn, "ATTENDEE")) {
            try (Statement st = conn.createStatement()) {
                if (!columnExists(conn, "ATTENDEE", "EMAILVERIFIED")) {
                    st.execute("ALTER TABLE attendee ADD COLUMN emailVerified BOOLEAN DEFAULT TRUE");
                }
                if (!columnExists(conn, "ATTENDEE", "EMAILVERIFIEDAT")) {
                    st.execute("ALTER TABLE attendee ADD COLUMN emailVerifiedAt TIMESTAMP");
                }
                st.executeUpdate("UPDATE attendee SET emailVerified = TRUE WHERE emailVerified IS NULL");
            }
        }

        if (tableExists(conn, "TERTIARY_PRESENTER")) {
            try (Statement st = conn.createStatement()) {
                if (!columnExists(conn, "TERTIARY_PRESENTER", "EMAILVERIFIED")) {
                    st.execute("ALTER TABLE tertiary_presenter ADD COLUMN emailVerified BOOLEAN DEFAULT TRUE");
                }
                if (!columnExists(conn, "TERTIARY_PRESENTER", "EMAILVERIFIEDAT")) {
                    st.execute("ALTER TABLE tertiary_presenter ADD COLUMN emailVerifiedAt TIMESTAMP");
                }
                st.executeUpdate("UPDATE tertiary_presenter SET emailVerified = TRUE WHERE emailVerified IS NULL");
            }
        }
    }

    private static void ensureUniqueClientUsernameIndexes(Connection conn) throws SQLException {
        if (tableExists(conn, "ATTENDEE") && !hasDuplicateNonBlankValues(conn, "attendee", "username")) {
            createUniqueIndexIfPossible(conn, "uq_attendee_username", "attendee", "username");
        }
        if (tableExists(conn, "TERTIARY_PRESENTER") && !hasDuplicateNonBlankValues(conn, "tertiary_presenter", "username")) {
            createUniqueIndexIfPossible(conn, "uq_presenter_username", "tertiary_presenter", "username");
        }
    }

    private static void ensureEngagementData(Connection conn) throws SQLException {
        if (tableExists(conn, "ATTENDEE_SUBSCRIPTION") && tableExists(conn, "ATTENDEE_BADGE")
                && tableExists(conn, "ATTENDEE_COUPON") && tableExists(conn, "WISHLIST_STOCK_ALERT_LOG")
                && tableExists(conn, "EMAIL_CAMPAIGN_LOG")) {
            return;
        }

        try (Statement st = conn.createStatement()) {
            if (!tableExists(conn, "ATTENDEE_SUBSCRIPTION")) {
                st.execute("CREATE TABLE attendee_subscription ("
                        + " attendeeID INT NOT NULL PRIMARY KEY,"
                        + " email VARCHAR(120) NOT NULL,"
                        + " subscribed BOOLEAN NOT NULL,"
                        + " unsubscribeToken VARCHAR(120) NOT NULL,"
                        + " subscribedAt TIMESTAMP,"
                        + " unsubscribedAt TIMESTAMP,"
                        + " lastCampaignAt TIMESTAMP,"
                        + " FOREIGN KEY (attendeeID) REFERENCES attendee(attendeeID)"
                        + ")");
            }

            if (!tableExists(conn, "ATTENDEE_BADGE")) {
                st.execute("CREATE TABLE attendee_badge ("
                        + " attendeeID INT NOT NULL PRIMARY KEY,"
                        + " badgeLevel VARCHAR(30) NOT NULL,"
                        + " badgeTitle VARCHAR(80) NOT NULL,"
                        + " totalTickets INT NOT NULL,"
                        + " totalSpend DECIMAL(12,2) NOT NULL,"
                        + " lastUpdated TIMESTAMP NOT NULL,"
                        + " FOREIGN KEY (attendeeID) REFERENCES attendee(attendeeID)"
                        + ")");
            }

            if (!tableExists(conn, "ATTENDEE_COUPON")) {
                st.execute("CREATE TABLE attendee_coupon ("
                        + " couponID INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,"
                        + " attendeeID INT NOT NULL,"
                        + " couponCode VARCHAR(40) NOT NULL UNIQUE,"
                        + " discountPercent INT NOT NULL,"
                        + " status VARCHAR(20) NOT NULL,"
                        + " reason VARCHAR(60),"
                        + " createdAt TIMESTAMP NOT NULL,"
                        + " expiresAt TIMESTAMP,"
                        + " FOREIGN KEY (attendeeID) REFERENCES attendee(attendeeID)"
                        + ")");
            }

            if (!tableExists(conn, "WISHLIST_STOCK_ALERT_LOG")) {
                st.execute("CREATE TABLE wishlist_stock_alert_log ("
                        + " alertID INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,"
                        + " attendeeID INT NOT NULL,"
                        + " eventID INT NOT NULL,"
                        + " remainingTickets INT NOT NULL,"
                        + " lastAlertAt TIMESTAMP NOT NULL,"
                        + " UNIQUE (attendeeID, eventID),"
                        + " FOREIGN KEY (attendeeID) REFERENCES attendee(attendeeID),"
                        + " FOREIGN KEY (eventID) REFERENCES event(eventID)"
                        + ")");
            }

            if (!tableExists(conn, "EMAIL_CAMPAIGN_LOG")) {
                st.execute("CREATE TABLE email_campaign_log ("
                        + " campaignLogID INT NOT NULL GENERATED ALWAYS AS IDENTITY PRIMARY KEY,"
                        + " campaignType VARCHAR(40) NOT NULL,"
                        + " attendeeID INT,"
                        + " eventID INT,"
                        + " recipientEmail VARCHAR(120),"
                        + " subject VARCHAR(200),"
                        + " status VARCHAR(20) NOT NULL,"
                        + " sentAt TIMESTAMP NOT NULL,"
                        + " FOREIGN KEY (attendeeID) REFERENCES attendee(attendeeID),"
                        + " FOREIGN KEY (eventID) REFERENCES event(eventID)"
                        + ")");
            }
        }
    }

    private static boolean hasDuplicateNonBlankValues(Connection conn, String table, String column) throws SQLException {
        String sql = "SELECT " + column + " FROM " + table
                + " WHERE " + column + " IS NOT NULL AND TRIM(" + column + ") <> ''"
                + " GROUP BY " + column + " HAVING COUNT(*) > 1";
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            return rs.next();
        }
    }

    private static void createUniqueIndexIfPossible(Connection conn, String indexName, String table, String column) {
        String sql = "CREATE UNIQUE INDEX " + indexName + " ON " + table + "(" + column + ")";
        try (Statement st = conn.createStatement()) {
            st.execute(sql);
        } catch (SQLException ex) {
            String state = ex.getSQLState() == null ? "" : ex.getSQLState();
            String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
            boolean indexExists = "X0Y32".equals(state) || message.contains("already exists") || message.contains("duplicate");
            if (!indexExists) {
                System.err.println("Unable to create unique index " + indexName + ": " + ex.getMessage());
            }
        }
    }
 
    private static int rowCount(Connection conn, String table) throws SQLException {
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM " + table)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private static void ensureAdditionalCampusAttendees(Connection conn, int additionalAttendeeCount) throws SQLException {
        if (additionalAttendeeCount <= 0 || !tableExists(conn, "ATTENDEE") || !tableExists(conn, "EVENT")
                || !tableExists(conn, "QRCODE") || !tableExists(conn, "ATTENDEE_HAS_EVENT")) {
            return;
        }

        int currentAttendees = rowCount(conn, "ATTENDEE");

        String eligibleEventsSql = "SELECT e.eventID, v.name AS campusName "
                + "FROM event e "
                + "JOIN venue v ON e.venueID = v.venueID "
                + "WHERE EXISTS (SELECT 1 FROM admin a WHERE a.eventID = e.eventID) "
                + "AND EXISTS (SELECT 1 FROM event_has_manager ehm WHERE ehm.eventID = e.eventID) "
                + "ORDER BY e.eventID";

        try (PreparedStatement pe = conn.prepareStatement(eligibleEventsSql);
                ResultSet rs = pe.executeQuery()) {

            java.util.List<Integer> eventIDs = new java.util.ArrayList<>();
            java.util.List<String> campusNames = new java.util.ArrayList<>();

            while (rs.next()) {
                eventIDs.add(rs.getInt("eventID"));
                campusNames.add(rs.getString("campusName"));
            }

            if (eventIDs.isEmpty()) {
                return;
            }

            String[] firstNames = {"Anele", "Boitumelo", "Cynthia", "Dineo", "Elvis", "Fikile", "Gugulethu", "Hlumelo", "Imran", "Jabulile"};
            String[] lastNames = {"Maseko", "Nkosi", "Mthembu", "Pillay", "Mokoena", "Dlamini", "Khumalo", "Naidoo", "Mabaso", "Sithole"};

            try (PreparedStatement qrInsert = conn.prepareStatement(
                    "INSERT INTO qrcode(barstring,number) VALUES(?,?)", Statement.RETURN_GENERATED_KEYS);
                    PreparedStatement attendeeInsert = conn.prepareStatement(
                            "INSERT INTO attendee(username,clientType,tertiaryInstitution,phoneNumber,studentNumber,idPassportNumber,dateOfBirth,biography,firstname,lastname,email,password,qrcode_QRcodeID) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                            Statement.RETURN_GENERATED_KEYS);
                    PreparedStatement attendeeEventInsert = conn.prepareStatement(
                            "INSERT INTO attendee_has_event(attendeeID,eventID) VALUES(?,?)");
                    PreparedStatement attendeeTicketInsert = conn.prepareStatement(
                            "INSERT INTO attendee_has_ticket(attendeeID,ticketID) VALUES(?,?)")) {

                long seedTime = System.currentTimeMillis();

                for (int i = 0; i < additionalAttendeeCount; i++) {
                    int eventId = eventIDs.get(i % eventIDs.size());
                    String campusName = campusNames.get(i % campusNames.size());
                    String campusCode = toCampusCode(campusName);

                    String username = "student" + String.format("%03d", currentAttendees + i + 1);
                    String firstName = firstNames[i % firstNames.length];
                    String lastName = lastNames[(i + 3) % lastNames.length];
                    String email = username + "@" + campusCode + ".tickify.ac.za";
                    String studentNumber = "STD-2026-" + String.format("%04d", currentAttendees + i + 1);

                    qrInsert.setString(1, "QR-AUTO-" + seedTime + "-" + i);
                    qrInsert.setInt(2, 3000 + currentAttendees + i);
                    qrInsert.executeUpdate();

                    int qrId;
                    try (ResultSet qrs = qrInsert.getGeneratedKeys()) {
                        if (!qrs.next()) {
                            continue;
                        }
                        qrId = qrs.getInt(1);
                    }

                    attendeeInsert.setString(1, username);
                    attendeeInsert.setString(2, "STUDENT");
                    attendeeInsert.setString(3, campusName != null && !campusName.trim().isEmpty() ? campusName : "Unspecified");
                    attendeeInsert.setString(4, "073" + String.format("%07d", currentAttendees + i));
                    attendeeInsert.setString(5, studentNumber);
                    attendeeInsert.setString(6, "ID-AUTO-" + String.format("%05d", currentAttendees + i + 1));
                    attendeeInsert.setDate(7, Date.valueOf("2001-01-01"));
                    attendeeInsert.setString(8, "Auto-generated attendee profile for campus-aligned test coverage.");
                    attendeeInsert.setString(9, firstName);
                    attendeeInsert.setString(10, lastName);
                    attendeeInsert.setString(11, email);
                    attendeeInsert.setString(12, PasswordUtil.hashPassword("student123"));
                    attendeeInsert.setInt(13, qrId);
                    attendeeInsert.executeUpdate();

                    int attendeeId;
                    try (ResultSet ars = attendeeInsert.getGeneratedKeys()) {
                        if (!ars.next()) {
                            continue;
                        }
                        attendeeId = ars.getInt(1);
                    }

                    attendeeEventInsert.setInt(1, attendeeId);
                    attendeeEventInsert.setInt(2, eventId);
                    attendeeEventInsert.executeUpdate();

                    Integer ticketId = firstTicketForEvent(conn, eventId);
                    if (ticketId != null) {
                        attendeeTicketInsert.setInt(1, attendeeId);
                        attendeeTicketInsert.setInt(2, ticketId);
                        attendeeTicketInsert.executeUpdate();
                    }
                }
            }
        }
    }

    private static Integer firstTicketForEvent(Connection conn, int eventId) throws SQLException {
        String sql = "SELECT ticketID FROM event_has_ticket WHERE eventID = ? ORDER BY ticketID FETCH FIRST ROW ONLY";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, eventId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return null;
    }

    private static String toCampusCode(String campusName) {
        if (campusName == null || campusName.trim().isEmpty()) {
            return "campus";
        }
        String normalized = campusName.toLowerCase().replaceAll("[^a-z0-9]+", "").trim();
        if (normalized.isEmpty()) {
            return "campus";
        }
        return normalized.length() > 12 ? normalized.substring(0, 12) : normalized;
    }
    
}
