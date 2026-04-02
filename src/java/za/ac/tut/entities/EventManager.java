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
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║  ENTITY : EventManager                                       ║
 * ║  TABLE  : event_manager                                      ║
 * ║  PK     : eventManagerID  (INT, AUTO_INCREMENT)              ║
 * ╠══════════════════════════════════════════════════════════════╣
 * ║  RELATIONSHIPS                                               ║
 * ║  MANY-TO-ONE  → VenueGuard   (manager supervises a guard)    ║
 * ║  MANY-TO-MANY → Event        (event_has_manager)             ║
 * ║  MANY-TO-MANY → Ticket       (eventmanager_has_ticket)       ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * PROBLEMS FIXED FROM ORIGINAL:
 * ──────────────────────────────────────────────────────────────
 * FIX 1  — Entire ERD structure missing: added firstname, lastname,
 *           email, password and all three relationships.
 * FIX 2  — @Access(AccessType.FIELD) — prevents mixed-access error.
 * FIX 3  — id → eventManagerID to match ERD/DB column name.
 * FIX 4  — GenerationType.AUTO → IDENTITY for Derby compatibility.
 * FIX 5  — updatable = false on PK column.
 * FIX 6  — @Column on every attribute with name/length/nullable.
 * FIX 7  — @ManyToOne + @JoinColumn for VenueGuard (owning side).
 * FIX 8  — @ManyToMany + @JoinTable for Event (owning side).
 * FIX 9  — @ManyToMany + @JoinTable for Ticket (owning side).
 * FIX 10 — @Table(name = "event_manager") added.
 * FIX 11 — @NamedQueries added.
 * FIX 12 — equals(), hashCode(), toString() added.
 * FIX 13 — Defensive validation in setters.
 * FIX 14 — null/duplicate guards on add/remove helpers.
 * FIX 15 — getFullName() convenience method added.
 */
@Entity
@Table(name = "event_manager")                // FIX 10
@Access(AccessType.FIELD)                     // FIX 2
@NamedQueries({                               // FIX 11
    @NamedQuery(
        name  = "EventManager.findAll",
        query = "SELECT em FROM EventManager em ORDER BY em.lastname ASC"
    ),
    @NamedQuery(
        name  = "EventManager.findById",
        query = "SELECT em FROM EventManager em WHERE em.eventManagerID = :eventManagerID"
    ),
    @NamedQuery(
        name  = "EventManager.findByEmail",
        query = "SELECT em FROM EventManager em WHERE em.email = :email"
    ),
    @NamedQuery(
        name  = "EventManager.findByVenueGuard",
        query = "SELECT em FROM EventManager em WHERE em.venueGuard.venueGuardID = :venueGuardID"
    ),
    @NamedQuery(
        name  = "EventManager.login",
        query = "SELECT em FROM EventManager em WHERE em.email = :email AND em.password = :password"
    )
})
public class EventManager implements Serializable {

    private static final long serialVersionUID = 2049183756204918375L;  // FIX 12

    // ── Primary Key ──────────────────────────────────────────────
    // FIX 3 — 'id' → 'eventManagerID' matches the ERD column name.
    // FIX 4 — IDENTITY maps to Derby's GENERATED ALWAYS AS IDENTITY.
    // FIX 5 — updatable = false prevents accidental PK overwrites.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "eventManagerID", nullable = false, updatable = false)
    private int eventManagerID;

    // ── Attributes ───────────────────────────────────────────────
    // FIX 6 — @Column on every field: exact DB name, length, nullable.
    @Column(name = "firstname", length = 45, nullable = false)
    private String firstname;

    @Column(name = "lastname", length = 45, nullable = false)
    private String lastname;

    @Column(name = "email", length = 45, nullable = false, unique = true)
    private String email;

    @Column(name = "password", length = 100, nullable = false)
    private String password;

    // ── MANY-TO-ONE : EventManager → VenueGuard (FK: venueGuardID) ──
    // FIX 7 — @ManyToOne was completely missing.
    // EventManager is the OWNING side — it holds the venueGuardID FK.
    // Many managers can each be linked to a different venue guard.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name                 = "venueGuardID",
        nullable             = false,
        referencedColumnName = "venueGuardID"
    )
    private VenueGuard venueGuard;

    // ── MANY-TO-MANY : EventManager ↔ Event (junction: event_has_manager) ──
    // FIX 8 — @ManyToMany + @JoinTable was completely missing.
    // EventManager is the OWNING side — defines the @JoinTable here.
    // Event's inverse side uses mappedBy = "eventManagers".
    // PERSIST + MERGE only — deleting a manager must NOT delete Events.
    @ManyToMany(
        cascade = { CascadeType.PERSIST, CascadeType.MERGE },
        fetch   = FetchType.LAZY
    )
    @JoinTable(
        name               = "event_has_manager",
        joinColumns        = @JoinColumn(name = "eventManagerID"),
        inverseJoinColumns = @JoinColumn(name = "eventID")
    )
    private List<Event> events = new ArrayList<>();

    // ── MANY-TO-MANY : EventManager ↔ Ticket (junction: eventmanager_has_ticket) ──
    // FIX 9 — @ManyToMany + @JoinTable was completely missing.
    // EventManager is the OWNING side — defines the @JoinTable here.
    // Ticket's inverse side uses mappedBy = "eventManagers".
    // PERSIST + MERGE only — deleting a manager must NOT delete Tickets.
    @ManyToMany(
        cascade = { CascadeType.PERSIST, CascadeType.MERGE },
        fetch   = FetchType.LAZY
    )
    @JoinTable(
        name               = "eventmanager_has_ticket",
        joinColumns        = @JoinColumn(name = "eventManagerID"),
        inverseJoinColumns = @JoinColumn(name = "ticketID")
    )
    private List<Ticket> tickets = new ArrayList<>();

    // ── Constructors ─────────────────────────────────────────────

    /** Required by JPA spec. */
    public EventManager() {}

    /**
     * Constructor for a NEW event manager before persisting.
     * eventManagerID is omitted — the DB generates it via IDENTITY.
     */
    public EventManager(String firstname, String lastname,
                        String email, String password, VenueGuard venueGuard) {
        this.firstname = firstname;
        this.lastname  = lastname;
        this.email     = email;
        this.password  = password;
        setVenueGuard(venueGuard);
    }

    /**
     * Full constructor — for reconstructing from DB (JDBC / test fixtures).
     */
    public EventManager(int eventManagerID, String firstname, String lastname,
                        String email, String password, VenueGuard venueGuard) {
        this.eventManagerID = eventManagerID;
        this.firstname      = firstname;
        this.lastname       = lastname;
        this.email          = email;
        this.password       = password;
        setVenueGuard(venueGuard);
    }

    // ── Scalar Getters / Setters ──────────────────────────────────

    // FIX 3 — getId()/setId() renamed to getEventManagerID()/setEventManagerID().
    public int getEventManagerID() {
        return eventManagerID;
    }

    public void setEventManagerID(int eventManagerID) {
        this.eventManagerID = eventManagerID;
    }

    public String getFirstname() {
        return firstname;
    }

    // FIX 13 — Defensive validation and whitespace trim on all required fields.
    public void setFirstname(String firstname) {
        if (firstname == null || firstname.trim().isEmpty()) {
            throw new IllegalArgumentException("EventManager firstname must not be blank.");
        }
        this.firstname = firstname.trim();
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        if (lastname == null || lastname.trim().isEmpty()) {
            throw new IllegalArgumentException("EventManager lastname must not be blank.");
        }
        this.lastname = lastname.trim();
    }

    public String getEmail() {
        return email;
    }

    // FIX 13 — Lowercase + trim so "Manager@Tickify.AC.ZA" and
    //           "manager@tickify.ac.za" match consistently on login.
    public void setEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("EventManager email must not be blank.");
        }
        this.email = email.trim().toLowerCase();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("EventManager password must not be blank.");
        }
        this.password = password;
        // Note: Hash password in production: this.password = PasswordUtil.hash(password);
    }

    // FIX 15 — Convenience method for JSP/EL: ${eventManager.fullName}
    public String getFullName() {
        return firstname + " " + lastname;
    }

    // ── Relationship Getters / Setters ────────────────────────────

    public VenueGuard getVenueGuard() {
        return venueGuard;
    }

    /**
     * Bidirectional helper — also registers this manager with the
     * VenueGuard's eventManagers list, keeping the ONE-TO-MANY
     * relationship consistent on both sides in memory.
     */
    public void setVenueGuard(VenueGuard venueGuard) {
        if (Objects.equals(this.venueGuard, venueGuard)) return;  // prevent infinite loop
        this.venueGuard = venueGuard;
        if (venueGuard != null && !venueGuard.getEventManagers().contains(this)) {
            venueGuard.addEventManager(this);
        }
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    // FIX 14 — null-check + duplicate guard on all add/remove helpers.
    // Without contains(), adding the same Event twice inserts a duplicate
    // row into event_has_manager, violating its composite PK constraint.
    public void addEvent(Event e) {
        if (e != null && !events.contains(e)) {
            events.add(e);
        }
    }

    public void removeEvent(Event e) {
        if (e != null) {
            events.remove(e);
        }
    }

    public List<Ticket> getTickets() {
        return tickets;
    }

    public void setTickets(List<Ticket> tickets) {
        this.tickets = tickets;
    }

    public void addTicket(Ticket t) {
        if (t != null && !tickets.contains(t)) {
            tickets.add(t);
        }
    }

    public void removeTicket(Ticket t) {
        if (t != null) {
            tickets.remove(t);
        }
    }

    // ── equals & hashCode ─────────────────────────────────────────
    // FIX 12 — Both were completely absent in the original.
    // eventManagerID != 0 guards the transient (not-yet-persisted) state.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventManager)) return false;
        EventManager other = (EventManager) o;
        return eventManagerID != 0 && eventManagerID == other.eventManagerID;
    }

    @Override
    public int hashCode() {
        return eventManagerID != 0
                ? Objects.hash(eventManagerID)
                : System.identityHashCode(this);
    }

    // FIX 12 — toString() added. Lazy collections excluded to prevent
    //           LazyInitializationException outside an active transaction.
    @Override
    public String toString() {
        return "EventManager{"
             + "eventManagerID=" + eventManagerID
             + ", firstname='"   + firstname + '\''
             + ", lastname='"    + lastname  + '\''
             + ", email='"       + email     + '\''
             + '}';
    }
}