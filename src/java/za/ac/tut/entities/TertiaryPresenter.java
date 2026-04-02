package za.ac.tut.entities;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Access;
import javax.persistence.AccessType;
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
import javax.persistence.Table;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  ENTITY : TertiaryPresenter                              ║
 * ║  TABLE  : tertiary_presenter                             ║
 * ║  PK     : tertiaryPresenterID  (INT, AUTO_INCREMENT)     ║
 * ╠══════════════════════════════════════════════════════════╣
 * ║  RELATIONSHIPS                                           ║
 * ║  MANY-TO-ONE → Event  (presenter presents at an event)   ║
 * ║  MANY-TO-ONE → Venue  (presenter presents at a venue)    ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * Role in the system:
 *   A TertiaryPresenter is a university/college lecturer or department
 *   representative who books events, browses available event slots,
 *   and can cancel or request updates to their booked events.
 *
 * PROBLEMS FIXED FROM ORIGINAL:
 * ──────────────────────────────────────────────────────────
 * FIX 1  — Entire ERD structure was missing — only an id field existed.
 *           Added all attributes: firstname, lastname, email, password,
 *           tertiaryInstitution, and both relationships from the ERD.
 * FIX 2  — @Access(AccessType.FIELD) added to prevent the mixed-access
 *           "no Id defined in entity hierarchy" error.
 * FIX 3  — Field renamed id → tertiaryPresenterID to match ERD/DB column.
 * FIX 4  — GenerationType.AUTO → IDENTITY for Derby compatibility.
 * FIX 5  — updatable = false on PK column.
 * FIX 6  — @Column added to every attribute with correct name/length/nullable.
 * FIX 7  — @ManyToOne + @JoinColumn added for Event relationship (owning side).
 * FIX 8  — @ManyToOne + @JoinColumn added for Venue relationship (owning side).
 * FIX 9  — @Table(name = "tertiary_presenter") added — table has underscore.
 * FIX 10 — @NamedQueries added.
 * FIX 11 — equals(), hashCode(), toString() added correctly.
 * FIX 12 — Defensive validation in setters for all required fields.
 * FIX 13 — getFullName() convenience method added.
 */
@Entity
@Table(name = "tertiary_presenter")  // FIX 9 — explicit table name with underscore;
                                     //          without this JPA looks for TERTIARYPRESENTER
                                     //          (no underscore) and fails to find it.
@Access(AccessType.FIELD)            // FIX 2 — lock to field access; prevents mixed-access error
@NamedQueries({                      // FIX 10 — centralised JPQL, validated at startup
    @NamedQuery(
        name  = "TertiaryPresenter.findAll",
        query = "SELECT tp FROM TertiaryPresenter tp ORDER BY tp.lastname ASC"
    ),
    @NamedQuery(
        name  = "TertiaryPresenter.findById",
        query = "SELECT tp FROM TertiaryPresenter tp WHERE tp.tertiaryPresenterID = :tertiaryPresenterID"
    ),
    @NamedQuery(
        name  = "TertiaryPresenter.findByEmail",
        query = "SELECT tp FROM TertiaryPresenter tp WHERE tp.email = :email"
    ),
    @NamedQuery(
        name  = "TertiaryPresenter.findByInstitution",
        query = "SELECT tp FROM TertiaryPresenter tp "
              + "WHERE tp.tertiaryInstitution = :tertiaryInstitution "
              + "ORDER BY tp.lastname ASC"
    ),
    @NamedQuery(
        name  = "TertiaryPresenter.findByEvent",
        query = "SELECT tp FROM TertiaryPresenter tp WHERE tp.event.eventID = :eventID ORDER BY tp.lastname ASC"
    ),
    @NamedQuery(
        name  = "TertiaryPresenter.findByVenue",
        query = "SELECT tp FROM TertiaryPresenter tp WHERE tp.venue.venueID = :venueID ORDER BY tp.lastname ASC"
    ),
    @NamedQuery(
        name  = "TertiaryPresenter.login",
        query = "SELECT tp FROM TertiaryPresenter tp WHERE tp.email = :email AND tp.password = :password"
    )
})
public class TertiaryPresenter implements Serializable {

    // FIX 11 — Unique serialVersionUID per class instead of the generic 1L.
    private static final long serialVersionUID = 4719283650174928365L;

    // ── Primary Key ──────────────────────────────────────────────
    // FIX 3 — Renamed from 'id' to 'tertiaryPresenterID' to match the ERD.
    //          A field named 'id' maps to a column called 'id' by default —
    //          the DB column is 'tertiaryPresenterID', causing a silent mismatch.
    // FIX 4 — IDENTITY maps directly to Derby's GENERATED ALWAYS AS IDENTITY
    //          instead of creating an unwanted separate SEQUENCE table.
    // FIX 5 — updatable = false: the PK must never change after row creation.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tertiaryPresenterID", nullable = false, updatable = false)
    private int tertiaryPresenterID;

    // ── Attributes ───────────────────────────────────────────────
    // FIX 1 + FIX 6 — All fields were completely absent in the original.
    //                  @Column maps each field to the exact DB column name,
    //                  length, and nullability as defined in the ERD DDL.

    @Column(name = "firstname", length = 45, nullable = false)
    private String firstname;

    @Column(name = "username", length = 45, nullable = true)
    private String username;

    @Column(name = "lastname", length = 45, nullable = false)
    private String lastname;

    @Column(name = "email", length = 45, nullable = true, unique = true)
    // unique = true — two presenters cannot share the same login email
    private String email;

    @Column(name = "password", length = 100, nullable = false)
    // length = 100 — extra room to accommodate hashed passwords in future
    private String password;

    @Column(name = "tertiaryInstitution", length = 45, nullable = false)
    // nullable = false — a presenter must always belong to an institution;
    // this is the defining characteristic of this user role.
    private String tertiaryInstitution;

    @Column(name = "phoneNumber", length = 25, nullable = true)
    private String phoneNumber;

    @Column(name = "biography", length = 1200, nullable = true)
    private String biography;

    // ── MANY-TO-ONE : TertiaryPresenter → Event (FK: eventID) ────
    // FIX 7 — @ManyToOne + @JoinColumn was completely missing in original.
    //          TertiaryPresenter is the OWNING side — it holds the eventID FK
    //          column in the tertiary_presenter table.
    //          Without this annotation JPA tries to persist 'event' as a
    //          basic column called 'EVENT', which does not exist and causes
    //          a deployment mapping error.
    //
    //          Per the ERD: a presenter is linked to one specific event they
    //          are booking or presenting at. Many presenters can book the same event.
    //
    //          optional = true — a presenter might exist in the system before
    //          they have been assigned to a specific event (e.g. pending approval).
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
        name                 = "eventID",    // FK column in the tertiary_presenter table
        nullable             = true,
        referencedColumnName = "eventID"     // PK column in the event table
    )
    private Event event;

    // ── MANY-TO-ONE : TertiaryPresenter → Venue (FK: venueID) ────
    // FIX 8 — @ManyToOne + @JoinColumn was completely missing in original.
    //          TertiaryPresenter is the OWNING side — it holds the venueID FK.
    //          Without this annotation JPA tries to persist 'venue' as a basic
    //          column called 'VENUE', which does not exist.
    //
    //          Per the ERD: a presenter is associated with the venue where
    //          their event will take place. Many presenters can be at the same venue.
    //
    //          optional = true — same reasoning as event above.
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
        name                 = "venueID",    // FK column in the tertiary_presenter table
        nullable             = true,
        referencedColumnName = "venueID"     // PK column in the venue table
    )
    private Venue venue;

    // ── Constructors ─────────────────────────────────────────────

    /** Required by JPA spec. */
    public TertiaryPresenter() {}

    /**
     * Constructor for a NEW presenter before persisting.
     * tertiaryPresenterID is omitted — the DB generates it via IDENTITY.
     * event and venue can be null if not yet assigned.
     */
    public TertiaryPresenter(String firstname, String lastname, String email,
                              String password, String tertiaryInstitution,
                              Event event, Venue venue) {
        this.firstname           = firstname;
        this.lastname            = lastname;
        this.email               = email;
        this.password            = password;
        this.tertiaryInstitution = tertiaryInstitution;
        setEvent(event);     // use setters to keep bidirectional links in sync
        setVenue(venue);
    }

    /**
     * Full constructor — for reconstructing from DB (JDBC / test fixtures).
     */
    public TertiaryPresenter(int tertiaryPresenterID, String firstname, String lastname,
                              String email, String password, String tertiaryInstitution,
                              Event event, Venue venue) {
        this.tertiaryPresenterID = tertiaryPresenterID;
        this.firstname           = firstname;
        this.lastname            = lastname;
        this.email               = email;
        this.password            = password;
        this.tertiaryInstitution = tertiaryInstitution;
        setEvent(event);
        setVenue(venue);
    }

    // ── Scalar Getters / Setters ──────────────────────────────────

    // FIX 3 — Renamed from getId()/setId() to getTertiaryPresenterID()/setTertiaryPresenterID().
    //          JSF EL ${tertiaryPresenter.tertiaryPresenterID} depends on the
    //          getter name matching the field name exactly.
    public int getTertiaryPresenterID() {
        return tertiaryPresenterID;
    }

    public void setTertiaryPresenterID(int tertiaryPresenterID) {
        this.tertiaryPresenterID = tertiaryPresenterID;
    }

    public String getFirstname() {
        return firstname;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username == null ? null : username.trim().toLowerCase();
    }

    // FIX 12 — Defensive validation and trim for all required string fields.
    public void setFirstname(String firstname) {
        if (firstname == null || firstname.trim().isEmpty()) {
            throw new IllegalArgumentException("TertiaryPresenter firstname must not be blank.");
        }
        this.firstname = firstname.trim();
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        if (lastname == null || lastname.trim().isEmpty()) {
            throw new IllegalArgumentException("TertiaryPresenter lastname must not be blank.");
        }
        this.lastname = lastname.trim();
    }

    public String getEmail() {
        return email;
    }

    // Email is optional when username is used for login.
    public void setEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            this.email = null;
            return;
        }
        this.email = email.trim().toLowerCase();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("TertiaryPresenter password must not be blank.");
        }
        this.password = password;
        // Note: Hash the password here in production:
        // this.password = PasswordUtil.hash(password);
    }

    public String getTertiaryInstitution() {
        return tertiaryInstitution;
    }

    // FIX 12 — Trim institution name for consistent storage and lookups.
    //           "DUT " and "DUT" must resolve to the same institution.
    public void setTertiaryInstitution(String tertiaryInstitution) {
        if (tertiaryInstitution == null || tertiaryInstitution.trim().isEmpty()) {
            throw new IllegalArgumentException("TertiaryPresenter institution must not be blank.");
        }
        this.tertiaryInstitution = tertiaryInstitution.trim();
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber == null ? null : phoneNumber.trim();
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography == null ? null : biography.trim();
    }

    // ── Convenience method ────────────────────────────────────────
    // FIX 13 — Useful in JSP/EL: ${tertiaryPresenter.fullName} instead of
    //           ${tertiaryPresenter.firstname} ${tertiaryPresenter.lastname}
    public String getFullName() {
        return firstname + " " + lastname;
    }

    public String getDisplayName() {
        if (username != null && !username.trim().isEmpty()) {
            return username;
        }
        return getFullName();
    }

    // ── Relationship Getters / Setters ────────────────────────────

    public Event getEvent() {
        return event;
    }

    /**
     * Bidirectional helper — setting the event also registers this presenter
     * with Event's tertiaryPresenters list, keeping both sides in sync.
     */
    public void setEvent(Event event) {
        if (Objects.equals(this.event, event)) return;   // avoid infinite loop
        this.event = event;
        if (event != null && !event.getTertiaryPresenters().contains(this)) {
            event.addTertiaryPresenter(this);
        }
    }

    public Venue getVenue() {
        return venue;
    }

    /**
     * Bidirectional helper — setting the venue also registers this presenter
     * with Venue's tertiaryPresenters list, keeping both sides in sync.
     */
    public void setVenue(Venue venue) {
        if (Objects.equals(this.venue, venue)) return;   // avoid infinite loop
        this.venue = venue;
        if (venue != null && !venue.getTertiaryPresenters().contains(this)) {
            venue.addTertiaryPresenter(this);
        }
    }

    // ── equals & hashCode ─────────────────────────────────────────
    // FIX 11 — Both were completely absent in the original.
    // tertiaryPresenterID != 0 guards the transient (not-yet-persisted) state.
    // Two unsaved TertiaryPresenter objects must NOT be considered equal just
    // because they both have tertiaryPresenterID = 0 (the Java int default).
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TertiaryPresenter)) return false;
        TertiaryPresenter other = (TertiaryPresenter) o;
        return tertiaryPresenterID != 0 && tertiaryPresenterID == other.tertiaryPresenterID;
    }

    @Override
    public int hashCode() {
        // Persisted entity → stable PK hash.
        // Transient entity → identity hash (safe in HashSets before save).
        return tertiaryPresenterID != 0
                ? Objects.hash(tertiaryPresenterID)
                : System.identityHashCode(this);
    }

    // FIX 11 — toString() added. Lazy relationships intentionally excluded
    //           to prevent LazyInitializationException during logging
    //           outside an active transaction.
    @Override
    public String toString() {
        return "TertiaryPresenter{"
             + "tertiaryPresenterID="  + tertiaryPresenterID
             + ", firstname='"         + firstname           + '\''
             + ", lastname='"          + lastname            + '\''
             + ", email='"             + email               + '\''
             + ", institution='"       + tertiaryInstitution + '\''
             + '}';
    }
}