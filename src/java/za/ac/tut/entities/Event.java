package za.ac.tut.entities;

import java.io.Serializable;
import java.sql.Timestamp;
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
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  ENTITY : Event                                          ║
 * ║  TABLE  : event                                          ║
 * ║  PK     : eventID  (INT, AUTO_INCREMENT)                 ║
 * ╠══════════════════════════════════════════════════════════╣
 * ║  RELATIONSHIPS                                           ║
 * ║  MANY-TO-ONE  → Venue             (event held at venue)  ║
 * ║  MANY-TO-MANY → Attendee          (attendee_has_event)   ║
 * ║  MANY-TO-MANY → Ticket            (event_has_ticket)     ║
 * ║  MANY-TO-MANY → EventManager      (event_has_manager)    ║
 * ║  ONE-TO-MANY  → TertiaryPresenter (presenters at event)  ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * ROOT CAUSE OF "no Id defined in entity hierarchy" ERROR:
 * ─────────────────────────────────────────────────────────
 * The previous version had @Transient placed on the getVenueID() METHOD.
 * JPA has two access strategies:
 *   • FIELD access  — JPA reads annotations on private fields.
 *   • PROPERTY access — JPA reads annotations on getter methods.
 * When @Id is on a FIELD but ANY other JPA annotation (@Transient in this
 * case) is on a GETTER METHOD, EclipseLink switches to PROPERTY access for
 * that annotation and gets confused about which access mode the whole class
 * uses. It then fails to locate the @Id on the field, throwing:
 *   "Entity class [Event] has no primary key specified."
 *
 * FIX:
 *   1. Add @Access(AccessType.FIELD) at class level — forces FIELD access
 *      for the entire entity so there is zero ambiguity.
 *   2. Move @Transient from the getter to the FIELD declaration.
 *   3. The getter becomes a plain helper method with no JPA annotation.
 */
@Entity
@Table(name = "event")
@Access(AccessType.FIELD)          // ← THE CRITICAL FIX: lock access to FIELD mode
@NamedQueries({
    @NamedQuery(
        name  = "Event.findAll",
        query = "SELECT e FROM Event e ORDER BY e.date ASC"
    ),
    @NamedQuery(
        name  = "Event.findById",
        query = "SELECT e FROM Event e WHERE e.eventID = :eventID"
    ),
    @NamedQuery(
        name  = "Event.findByType",
        query = "SELECT e FROM Event e WHERE e.type = :type ORDER BY e.date ASC"
    ),
    @NamedQuery(
        name  = "Event.findByVenue",
        query = "SELECT e FROM Event e WHERE e.venue.venueID = :venueID ORDER BY e.date ASC"
    )
})
public class Event implements Serializable {

    private static final long serialVersionUID = 68L;

    // ── Primary Key ──────────────────────────────────────────────
    // @Id is on the FIELD — consistent with @Access(AccessType.FIELD) above.
    // updatable = false: the PK must never change after the row is created.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "eventID", nullable = false, updatable = false)
    private int eventID;

    // ── Attributes ───────────────────────────────────────────────
    @Column(name = "name", length = 45, nullable = false)
    private String name;

    @Column(name = "type", length = 45, nullable = false)
    private String type;

    @Column(name = "date", nullable = false)
    private Timestamp date;

    // ── MANY-TO-ONE : Event → Venue ───────────────────────────────
    // Event is the OWNING side — it holds the venueID FK column.
    // @ManyToOne + @JoinColumn is the correct JPA mapping; a raw int venueID
    // field alongside this would create a "repeated column" mapping error.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name                 = "venueID",
        nullable             = false,
        referencedColumnName = "venueID"
    )
    private Venue venue;

    // ── @Transient fields ─────────────────────────────────────────
    // FIX: @Transient must be on the FIELD, not the getter.
    // With @Access(FIELD) in effect, JPA only scans fields for annotations.
    // Putting @Transient on a getter does nothing in FIELD mode — JPA ignores
    // getter annotations entirely and tries to persist "venueName" as a column,
    // which then crashes because no such column exists in the table.
    @Transient
    private int venueID;   // populated from Venue.name after a JOIN

    // ── MANY-TO-MANY : Event ↔ Attendee ──────────────────────────
    // Event is the owning side — it defines @JoinTable.
    // Attendee's side will use mappedBy = "events".
    // CascadeType.PERSIST + MERGE only — do NOT cascade REMOVE here because
    // deleting an Event should NOT delete the Attendee records.
    @ManyToMany(
        cascade = { CascadeType.PERSIST, CascadeType.MERGE },
        fetch   = FetchType.LAZY
    )
    @JoinTable(
        name               = "attendee_has_event",
        joinColumns        = @JoinColumn(name = "eventID"),
        inverseJoinColumns = @JoinColumn(name = "attendeeID")
    )
    private List<Attendee> attendees = new ArrayList<>();

    // ── MANY-TO-MANY : Event ↔ Ticket ─────────────────────────────
    @ManyToMany(
        cascade = { CascadeType.PERSIST, CascadeType.MERGE },
        fetch   = FetchType.LAZY
    )
    @JoinTable(
        name               = "event_has_ticket",
        joinColumns        = @JoinColumn(name = "eventID"),
        inverseJoinColumns = @JoinColumn(name = "ticketID")
    )
    private List<Ticket> tickets = new ArrayList<>();

    // ── MANY-TO-MANY : Event ↔ EventManager ───────────────────────
    @ManyToMany(
        cascade = { CascadeType.PERSIST, CascadeType.MERGE },
        fetch   = FetchType.LAZY
    )
    @JoinTable(
        name               = "event_has_manager",
        joinColumns        = @JoinColumn(name = "eventID"),
        inverseJoinColumns = @JoinColumn(name = "eventManagerID")
    )
    private List<EventManager> eventManagers = new ArrayList<>();

    // ── ONE-TO-MANY : Event → TertiaryPresenter ───────────────────
    // Event is the INVERSE side — TertiaryPresenter owns the FK column.
    // mappedBy = "event" matches the field name in TertiaryPresenter.
    @OneToMany(
        mappedBy      = "event",
        cascade       = CascadeType.ALL,
        orphanRemoval = true,
        fetch         = FetchType.LAZY
    )
    private List<TertiaryPresenter> tertiaryPresenters = new ArrayList<>();

    // ── Constructors ─────────────────────────────────────────────

    /** Required by JPA spec. */
    public Event() {}

    /**
     * Constructor for a NEW event (before persisting).
     * eventID is omitted — the DB generates it via IDENTITY.
     */
    public Event(String name, String type, Timestamp date, Venue venue) {
        this.name = name;
        this.type = type;
        this.date = date;
        setVenue(venue);
    }

    /**
     * Full constructor — used when reconstructing from DB (JDBC/test fixtures).
     */
    public Event(int eventID, String name, String type, Timestamp date, Venue venue) {
        this.eventID = eventID;
        this.name    = name;
        this.type    = type;
        this.date    = date;
        setVenue(venue);
    }

    // ── Scalar Getters / Setters ──────────────────────────────────

    public int getEventID() {
        return eventID;
    }

    public void setEventID(int eventID) {
        this.eventID = eventID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Event name must not be blank.");
        }
        this.name = name.trim();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        if (type == null || type.trim().isEmpty()) {
            throw new IllegalArgumentException("Event type must not be blank.");
        }
        this.type = type.trim();
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    /**
     * Convenience getter — returns the venueID FK value without needing
     * to load the full Venue object. Safe to call even before venue is loaded.
     * No @Transient here — this is a plain Java method, not a JPA mapping.
     */
    public int getVenueID() {
        return (venue != null) ? venue.getVenueID() : 0;
    }

    public void setVenueID(int venueID) {
        this.venueID = venueID;
    }

    
    
    

    // ── Relationship Getters / Setters ────────────────────────────

    public Venue getVenue() {
        return venue;
    }

    /**
     * Bidirectional helper — setting the venue also caches the name so
     * JSPs can call event.getVenueName() without triggering a lazy load.
     */
    public void setVenue(Venue venue) {
        this.venue     = venue;
        this.venueID = (venue != null) ? venue.getVenueID() : 0;
    }

    public List<Attendee> getAttendees() {
        return attendees;
    }

    public void setAttendees(List<Attendee> attendees) {
        this.attendees = attendees;
    }

    public void addAttendee(Attendee a) {
        if (a != null && !attendees.contains(a)) {
            attendees.add(a);
        }
    }

    public void removeAttendee(Attendee a) {
        if (a != null) {
            attendees.remove(a);
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

    public List<EventManager> getEventManagers() {
        return eventManagers;
    }

    public void setEventManagers(List<EventManager> eventManagers) {
        this.eventManagers = eventManagers;
    }

    public void addEventManager(EventManager em) {
        if (em != null && !eventManagers.contains(em)) {
            eventManagers.add(em);
        }
    }

    public void removeEventManager(EventManager em) {
        if (em != null) {
            eventManagers.remove(em);
        }
    }

    public List<TertiaryPresenter> getTertiaryPresenters() {
        return tertiaryPresenters;
    }

    public void setTertiaryPresenters(List<TertiaryPresenter> tertiaryPresenters) {
        this.tertiaryPresenters = tertiaryPresenters;
    }

    public void addTertiaryPresenter(TertiaryPresenter tp) {
        if (tp != null && !tertiaryPresenters.contains(tp)) {
            tertiaryPresenters.add(tp);
            tp.setEvent(this);
        }
    }

    public void removeTertiaryPresenter(TertiaryPresenter tp) {
        if (tp != null && tertiaryPresenters.remove(tp)) {
            tp.setEvent(null);
        }
    }

    // ── equals & hashCode ─────────────────────────────────────────
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Event)) return false;
        Event other = (Event) o;
        return eventID != 0 && eventID == other.eventID;
    }

    @Override
    public int hashCode() {
        return eventID != 0 ? Objects.hash(eventID) : System.identityHashCode(this);
    }

    // Lazy collections intentionally excluded — would trigger
    // LazyInitializationException outside a transaction.
    @Override
    public String toString() {
        return "Event{"
             + "eventID="  + eventID
             + ", name='"  + name   + '\''
             + ", type='"  + type   + '\''
             + ", date="   + date
             + ", venueID=" + getVenueID()
             + '}';
    }
}