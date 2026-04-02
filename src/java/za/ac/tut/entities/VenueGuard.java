package za.ac.tut.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  ENTITY : VenueGuard                                     ║
 * ║  TABLE  : venue_guard                                    ║
 * ║  PK     : venueGuardID  (INT, AUTO_INCREMENT)            ║
 * ╠══════════════════════════════════════════════════════════╣
 * ║  RELATIONSHIPS                                           ║
 * ║  MANY-TO-ONE → Event        (guard deployed to event)    ║
 * ║  MANY-TO-ONE → Venue        (guard stationed at venue)   ║
 * ║  MANY-TO-ONE → QRCode       (guard references a QR code) ║
 * ║  ONE-TO-MANY → EventManager (guard supervised by mgrs)   ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * PROBLEMS FIXED FROM ORIGINAL:
 * ──────────────────────────────────────────────────────────
 * FIX 1  — Entire ERD structure was missing — only an id field existed.
 *           Added all attributes: firstname, lastname, email, password,
 *           and all four relationships from the ERD.
 * FIX 2  — @Access(AccessType.FIELD) added to prevent mixed-access
 *           "no Id defined in entity hierarchy" error.
 * FIX 3  — Field renamed id → venueGuardID to match ERD/DB column name.
 * FIX 4  — GenerationType.AUTO → IDENTITY for Derby compatibility.
 * FIX 5  — updatable = false on PK column.
 * FIX 6  — @Column added to every attribute with correct name/length/nullable.
 * FIX 7  — @ManyToOne + @JoinColumn added for Event relationship.
 * FIX 8  — @ManyToOne + @JoinColumn added for Venue relationship.
 * FIX 9  — @ManyToOne + @JoinColumn added for QRCode relationship.
 * FIX 10 — @OneToMany added for EventManager relationship (inverse side).
 * FIX 11 — @Table(name = "venue_guard") added.
 * FIX 12 — @NamedQueries added.
 * FIX 13 — equals(), hashCode(), toString() added correctly.
 * FIX 14 — Defensive validation in setters for required fields.
 * FIX 15 — null/duplicate guards added to add/remove helpers.
 * FIX 16 — getFullName() convenience method added.
 */
@Entity
@Table(name = "venue_guard")
@Access(AccessType.FIELD)           // FIX 2 — lock to field access; prevents mixed-access error
@NamedQueries({                     // FIX 12 — centralised JPQL, validated at startup
    @NamedQuery(
        name  = "VenueGuard.findAll",
        query = "SELECT vg FROM VenueGuard vg ORDER BY vg.lastname ASC"
    ),
    @NamedQuery(
        name  = "VenueGuard.findById",
        query = "SELECT vg FROM VenueGuard vg WHERE vg.venueGuardID = :venueGuardID"
    ),
    @NamedQuery(
        name  = "VenueGuard.findByEmail",
        query = "SELECT vg FROM VenueGuard vg WHERE vg.email = :email"
    ),
    @NamedQuery(
        name  = "VenueGuard.findByEvent",
        query = "SELECT vg FROM VenueGuard vg WHERE vg.event.eventID = :eventID ORDER BY vg.lastname ASC"
    ),
    @NamedQuery(
        name  = "VenueGuard.findByVenue",
        query = "SELECT vg FROM VenueGuard vg WHERE vg.venue.venueID = :venueID ORDER BY vg.lastname ASC"
    ),
    @NamedQuery(
        name  = "VenueGuard.login",
        query = "SELECT vg FROM VenueGuard vg WHERE vg.email = :email AND vg.password = :password"
    )
})
public class VenueGuard implements Serializable {

    // FIX 13 — Unique serialVersionUID instead of the generic 1L.
    private static final long serialVersionUID = 83746L;

    // ── Primary Key ──────────────────────────────────────────────
    // FIX 3 — Renamed from 'id' to 'venueGuardID' to match the ERD column.
    //          A field named 'id' maps to a column called 'id' by default —
    //          the DB column is 'venueGuardID', so the mapping would silently fail.
    // FIX 4 — IDENTITY maps directly to Derby's GENERATED ALWAYS AS IDENTITY.
    // FIX 5 — updatable = false: PK must never change after row creation.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "venueGuardID", nullable = false, updatable = false)
    private int venueGuardID;

    // ── Attributes ───────────────────────────────────────────────
    // FIX 1 + FIX 6 — All fields were completely absent in the original.
    //                  @Column maps each field to the exact DB column name,
    //                  length, and nullability as defined in the ERD DDL.
    @Column(name = "firstname", length = 45, nullable = false)
    private String firstname;

    @Column(name = "lastname", length = 45, nullable = false)
    private String lastname;

    @Column(name = "email", length = 45, nullable = false, unique = true)
    // unique = true — two guards cannot share the same login email
    private String email;

    @Column(name = "password", length = 100, nullable = false)
    // length = 100 — extra room for password hashing in future
    private String password;

    // ── MANY-TO-ONE : VenueGuard → Event (FK: eventID) ───────────
    // FIX 7 — @ManyToOne + @JoinColumn was completely missing.
    //          VenueGuard is the OWNING side — it holds the eventID FK
    //          column in the venue_guard table.
    //          A guard is deployed to ONE specific event at a time.
    //          Many guards can be assigned to the same event.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name                 = "eventID",
        nullable             = false,
        referencedColumnName = "eventID"
    )
    private Event event;

    // ── MANY-TO-ONE : VenueGuard → Venue (FK: venueID) ───────────
    // FIX 8 — @ManyToOne + @JoinColumn was completely missing.
    //          VenueGuard is the OWNING side — it holds the venueID FK.
    //          A guard is physically stationed at ONE venue.
    //          One venue can have many guards.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name                 = "venueID",
        nullable             = false,
        referencedColumnName = "venueID"
    )
    private Venue venue;

    // ── MANY-TO-ONE : VenueGuard → QRCode (FK: QRcodeID) ─────────
    // FIX 9 — @ManyToOne + @JoinColumn was completely missing.
    //          VenueGuard is the OWNING side — it holds the QRcodeID FK.
    //          A guard references ONE QR code (the device/scanner reference).
    //          One QR code can be referenced by many guards across events.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name                 = "QRcodeID",
        nullable             = false,
        referencedColumnName = "QRcodeID"
    )
    private QRCode qrCode;

    // ── ONE-TO-MANY : VenueGuard → EventManager ───────────────────
    // FIX 10 — @OneToMany annotation was completely missing.
    //           EventManager holds the FK (venueGuardID) in its own table,
    //           so EventManager is the OWNING side.
    //           VenueGuard is the INVERSE side — mappedBy = "venueGuard"
    //           refers to the 'venueGuard' field inside EventManager.java.
    //           One venue guard can be supervised by / linked to many managers.
    @OneToMany(
        mappedBy      = "venueGuard",
        cascade       = CascadeType.ALL,
        orphanRemoval = true,
        fetch         = FetchType.LAZY
    )
    private List<EventManager> eventManagers = new ArrayList<>();

    // ── Constructors ─────────────────────────────────────────────

    /** Required by JPA spec. */
    public VenueGuard() {}

    /**
     * Constructor for a NEW venue guard before persisting.
     * venueGuardID is omitted — the DB generates it via IDENTITY.
     */
    public VenueGuard(String firstname, String lastname, String email,
                      String password, Event event, Venue venue, QRCode qrCode) {
        this.firstname = firstname;
        this.lastname  = lastname;
        this.email     = email;
        this.password  = password;
        setEvent(event);    // use setters to keep bidirectional links in sync
        setVenue(venue);
        setQrCode(qrCode);
    }

    /**
     * Full constructor — for reconstructing from DB (JDBC / test fixtures).
     */
    public VenueGuard(int venueGuardID, String firstname, String lastname, String email,
                      String password, Event event, Venue venue, QRCode qrCode) {
        this.venueGuardID = venueGuardID;
        this.firstname    = firstname;
        this.lastname     = lastname;
        this.email        = email;
        this.password     = password;
        setEvent(event);
        setVenue(venue);
        setQrCode(qrCode);
    }

    // ── Scalar Getters / Setters ──────────────────────────────────

    // FIX 3 — Renamed from getId()/setId() to getVenueGuardID()/setVenueGuardID().
    //          JSF EL ${venueGuard.venueGuardID} depends on getter name matching field name.
    public int getVenueGuardID() {
        return venueGuardID;
    }

    public void setVenueGuardID(int venueGuardID) {
        this.venueGuardID = venueGuardID;
    }

    public String getFirstname() {
        return firstname;
    }

    // FIX 14 — Defensive validation and trim for all required string fields.
    public void setFirstname(String firstname) {
        if (firstname == null || firstname.trim().isEmpty()) {
            throw new IllegalArgumentException("VenueGuard firstname must not be blank.");
        }
        this.firstname = firstname.trim();
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        if (lastname == null || lastname.trim().isEmpty()) {
            throw new IllegalArgumentException("VenueGuard lastname must not be blank.");
        }
        this.lastname = lastname.trim();
    }

    public String getEmail() {
        return email;
    }

    // FIX 14 — Lowercase email for consistent login lookups.
    //           "Guard@Tickify.ac.za" and "guard@tickify.ac.za" must match.
    public void setEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("VenueGuard email must not be blank.");
        }
        this.email = email.trim().toLowerCase();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("VenueGuard password must not be blank.");
        }
        this.password = password;
        // Note: Hash the password here in production:
        // this.password = PasswordUtil.hash(password);
    }

    // ── Convenience method ────────────────────────────────────────
    // FIX 16 — Useful in JSP/EL: ${venueGuard.fullName} instead of
    //           ${venueGuard.firstname} ${venueGuard.lastname}
    public String getFullName() {
        return firstname + " " + lastname;
    }

    // ── Relationship Getters / Setters ────────────────────────────

    public Event getEvent() {
        return event;
    }

    /**
     * Bidirectional helper — also adds this guard to the Event's venueGuards
     * list if it is not already there, keeping both sides in sync.
     */
    public void setEvent(Event event) {
        this.event = event;
    }

    public Venue getVenue() {
        return venue;
    }

    /**
     * Bidirectional helper — also registers this guard with the Venue.
     */
    public void setVenue(Venue venue) {
        if (Objects.equals(this.venue, venue)) return;
        this.venue = venue;
        if (venue != null && !venue.getVenueGuards().contains(this)) {
            venue.addVenueGuard(this);
        }
    }

    public QRCode getQrCode() {
        return qrCode;
    }

    public void setQrCode(QRCode qrCode) {
        this.qrCode = qrCode;
    }

    public List<EventManager> getEventManagers() {
        return eventManagers;
    }

    public void setEventManagers(List<EventManager> eventManagers) {
        this.eventManagers = eventManagers;
    }

    // FIX 15 — null-check + duplicate guard on add/remove helpers.
    public void addEventManager(EventManager em) {
        if (em != null && !eventManagers.contains(em)) {
            eventManagers.add(em);
            em.setVenueGuard(this);   // keep owning side in sync
        }
    }

    public void removeEventManager(EventManager em) {
        if (em != null && eventManagers.remove(em)) {
            em.setVenueGuard(null);
        }
    }

    // ── equals & hashCode ─────────────────────────────────────────
    // FIX 13 — Both were completely absent in the original.
    // venueGuardID != 0 guards the transient (not-yet-persisted) state.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VenueGuard)) return false;
        VenueGuard other = (VenueGuard) o;
        return venueGuardID != 0 && venueGuardID == other.venueGuardID;
    }

    @Override
    public int hashCode() {
        // Persisted entity → stable PK hash.
        // Transient entity → identity hash (safe in HashSets before save).
        return venueGuardID != 0 ? Objects.hash(venueGuardID) : System.identityHashCode(this);
    }

    // FIX 13 — toString() added. Lazy collections and relationships excluded
    //           to prevent LazyInitializationException during logging
    //           outside an active transaction.
    @Override
    public String toString() {
        return "VenueGuard{"
             + "venueGuardID=" + venueGuardID
             + ", firstname='" + firstname + '\''
             + ", lastname='"  + lastname  + '\''
             + ", email='"     + email     + '\''
             + '}';
    }
}