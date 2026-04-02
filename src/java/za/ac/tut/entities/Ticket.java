package za.ac.tut.entities;

import java.io.Serializable;
import java.math.BigDecimal;
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
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  ENTITY : Ticket                                         ║
 * ║  TABLE  : ticket                                         ║
 * ║  PK     : ticketID  (INT, AUTO_INCREMENT)                ║
 * ╠══════════════════════════════════════════════════════════╣
 * ║  RELATIONSHIPS                                           ║
 * ║  MANY-TO-ONE  → QRCode       (ticket linked to QR code)  ║
 * ║  ONE-TO-ONE   ← QRCode       (inverse side)              ║
 * ║  MANY-TO-MANY → Event        (event_has_ticket)          ║
 * ║  MANY-TO-MANY → Attendee     (attendee_has_ticket)       ║
 * ║  MANY-TO-MANY → EventManager (eventmanager_has_ticket)   ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * PROBLEMS FIXED FROM ORIGINAL:
 * ──────────────────────────────────────────────────────────
 * FIX 1  — The original was an empty skeleton — all attributes and
 *           relationships from the ERD were completely missing.
 * FIX 2  — @Access(AccessType.FIELD) added to prevent mixed-access error.
 * FIX 3  — Field renamed id → ticketID to match ERD/DB column name.
 * FIX 4  — GenerationType.AUTO → IDENTITY for Derby compatibility.
 * FIX 5  — updatable = false on PK column.
 * FIX 6  — @Column added to every attribute with correct name/length/nullable.
 * FIX 7  — @ManyToOne + @JoinColumn added for QRCode relationship.
 * FIX 8  — @OneToOne (inverse) added so QRCode can navigate to Ticket.
 * FIX 9  — @ManyToMany + @JoinTable added for Event, Attendee, EventManager.
 * FIX 10 — @Table(name = "ticket") added.
 * FIX 11 — @NamedQueries added.
 * FIX 12 — hashCode() fixed — original used (int) cast which loses precision
 *           and always returns 0 for id = 0 (transient state collision).
 * FIX 13 — equals() fixed — original TODO warning acknowledged the bug;
 *           now correctly handles transient state with id != 0 guard.
 * FIX 14 — toString() improved — no longer exposes full package path.
 * FIX 15 — null/duplicate guards added to all add/remove helpers.
 */
@Entity
@Table(name = "ticket")
@Access(AccessType.FIELD)          // FIX 2 — lock to field access throughout
@NamedQueries({                    // FIX 11 — centralised JPQL
    @NamedQuery(
        name  = "Ticket.findAll",
        query = "SELECT t FROM Ticket t ORDER BY t.price ASC"
    ),
    @NamedQuery(
        name  = "Ticket.findById",
        query = "SELECT t FROM Ticket t WHERE t.ticketID = :ticketID"
    ),
    @NamedQuery(
        name  = "Ticket.findByName",
        query = "SELECT t FROM Ticket t WHERE LOWER(t.name) = LOWER(:name)"
    ),
    @NamedQuery(
        name  = "Ticket.findByMaxPrice",
        query = "SELECT t FROM Ticket t WHERE t.price <= :maxPrice ORDER BY t.price ASC"
    )
})
public class Ticket implements Serializable {

    // FIX 12 — Unique serialVersionUID per class.
    private static final long serialVersionUID = 73419L;

    // ── Primary Key ──────────────────────────────────────────────
    // FIX 3 — Renamed from 'id' to 'ticketID' to match the ERD column name.
    //          A field named 'id' maps to a column called 'id' by default —
    //          the DB column is 'ticketID', so the mapping would silently fail.
    // FIX 4 — IDENTITY maps directly to Derby's GENERATED ALWAYS AS IDENTITY.
    // FIX 5 — updatable = false: the PK must never change after row creation.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticketID", nullable = false, updatable = false)
    private int ticketID;

    // ── Attributes ───────────────────────────────────────────────
    // FIX 1 + FIX 6 — These fields were completely absent in the original.
    //                  @Column maps each field to the exact DB column.
    @Column(name = "name", length = 45, nullable = false)
    private String name;           // e.g. "General Admission", "VIP", "Student"

    @Column(name = "price", precision = 6, scale = 2, nullable = false)
    private BigDecimal price;      // DECIMAL(6,2) — matches ERD exactly.
                                   // BigDecimal is required for monetary values;
                                   // double/float cause rounding errors with currency.

    // ── MANY-TO-ONE : Ticket → QRCode (FK: QRcodeID) ─────────────
    // FIX 7 — @ManyToOne + @JoinColumn was completely missing in original.
    //          Ticket is the OWNING side — it holds the FK column (QRcodeID).
    //          Without this annotation JPA tries to map 'qrCode' as a basic
    //          column type, which fails because QRCode is an entity.
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name                 = "QRcodeID",    // FK column in the ticket table
        nullable             = false,
        referencedColumnName = "QRcodeID"     // PK column in the qrcode table
    )
    private QRCode qrCode;

    // ── ONE-TO-ONE INVERSE : Ticket ← QRCode ─────────────────────
    // FIX 8 — QRCode declares a @OneToOne(mappedBy = "qrCode") back to Ticket.
    //          This field allows QRCode to navigate to its Ticket.
    //          'mappedBy = "qrCode"' refers to the qrCode field above —
    //          the FK lives here in Ticket, so Ticket is the owning side.
    //          This field is the INVERSE side — no extra column is created.
    @OneToOne(mappedBy = "qrCode", fetch = FetchType.LAZY)
    private QRCode qrCodeInverse;

    // ── MANY-TO-MANY : Ticket ↔ Event ─────────────────────────────
    // FIX 9 — @ManyToMany + @JoinTable was completely missing in original.
    //          Event is the owning side and defines the @JoinTable there.
    //          Ticket uses mappedBy = "tickets" (the list name in Event).
    //          Do NOT redeclare @JoinTable here — only one side owns it.
    @ManyToMany(
        mappedBy = "tickets",
        fetch    = FetchType.LAZY
    )
    private List<Event> events = new ArrayList<>();

    // ── MANY-TO-MANY : Ticket ↔ Attendee ──────────────────────────
    // Attendee is the owning side (defines @JoinTable in Attendee.java).
    // Ticket uses mappedBy = "tickets".
    @ManyToMany(
        mappedBy = "tickets",
        fetch    = FetchType.LAZY
    )
    private List<Attendee> attendees = new ArrayList<>();

    // ── MANY-TO-MANY : Ticket ↔ EventManager ──────────────────────
    // EventManager is the owning side (defines @JoinTable in EventManager.java).
    // Ticket uses mappedBy = "tickets".
    @ManyToMany(
        mappedBy = "tickets",
        fetch    = FetchType.LAZY
    )
    private List<EventManager> eventManagers = new ArrayList<>();

    // ── Constructors ─────────────────────────────────────────────

    /** Required by JPA spec. */
    public Ticket() {}

    /**
     * Constructor for a NEW ticket before persisting.
     * ticketID is omitted — the DB generates it via IDENTITY.
     */
    public Ticket(String name, BigDecimal price, QRCode qrCode) {
        this.name   = name;
        this.price  = price;
        setQrCode(qrCode);    // use setter to keep bidirectional link in sync
    }

    /**
     * Full constructor — for reconstructing from DB (JDBC / test fixtures).
     */
    public Ticket(int ticketID, String name, BigDecimal price, QRCode qrCode) {
        this.ticketID = ticketID;
        this.name     = name;
        this.price    = price;
        setQrCode(qrCode);
    }

    // ── Scalar Getters / Setters ──────────────────────────────────

    // FIX 3 — Renamed from getId()/setId() to getTicketID()/setTicketID().
    public int getTicketID() {
        return ticketID;
    }

    public void setTicketID(int ticketID) {
        this.ticketID = ticketID;
    }

    public String getName() {
        return name;
    }

    // Defensive validation — ticket name is required.
    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Ticket name must not be blank.");
        }
        this.name = name.trim();
    }

    public BigDecimal getPrice() {
        return price;
    }

    // Defensive validation — price must be non-negative.
    public void setPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Ticket price must be zero or greater.");
        }
        this.price = price;
    }

    // ── Relationship Getters / Setters ────────────────────────────

    public QRCode getQrCode() {
        return qrCode;
    }

    /**
     * Bidirectional helper — sets BOTH sides of the Ticket ↔ QRCode link.
     * Calling ticket.setQrCode(qr) also calls qr.setTicket(ticket) so
     * the in-memory object graph stays consistent with what JPA persists.
     */
    public void setQrCode(QRCode qrCode) {
        if (Objects.equals(this.qrCode, qrCode)) return;  // avoid infinite loop
        this.qrCode = qrCode;
        if (qrCode != null && qrCode.getTicket() != this) {
            qrCode.setTicket(this);
        }
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    // FIX 15 — null-check + duplicate guard on all add/remove helpers.
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

    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ticket)) return false;
        Ticket other = (Ticket) o;
        // ticketID != 0 handles the transient (not-yet-persisted) state.
        return ticketID != 0 && ticketID == other.ticketID;
    }

    @Override
    public int hashCode() {
        // Persisted entity → stable PK hash.
        // Transient entity → identity hash (safe in HashSets before save).
        return ticketID != 0 ? Objects.hash(ticketID) : System.identityHashCode(this);
    }

    // FIX 14 — Removed full package path from toString().
    //           Lazy collections excluded to prevent LazyInitializationException.
    @Override
    public String toString() {
        return "Ticket{"
             + "ticketID=" + ticketID
             + ", name='"  + name  + '\''
             + ", price="  + price
             + '}';
    }
}