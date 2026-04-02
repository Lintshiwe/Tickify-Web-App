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
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  ENTITY : QRCode                                         ║
 * ║  TABLE  : qrcode                                         ║
 * ║  PK     : qrCodeID  (INT, AUTO_INCREMENT)                ║
 * ╠══════════════════════════════════════════════════════════╣
 * ║  RELATIONSHIPS                                           ║
 * ║  ONE-TO-ONE  → Attendee   (each attendee has one QR)     ║
 * ║  ONE-TO-ONE  → Ticket     (each ticket has one QR)       ║
 * ║  ONE-TO-MANY → VenueGuard (guards scan QR codes)         ║
 * ╚══════════════════════════════════════════════════════════╝
 *
 * PROBLEMS FIXED FROM ORIGINAL:
 * ──────────────────────────────────────────────────────────
 * FIX 1 — @Access(AccessType.FIELD) added — prevents the mixed-access
 *          "no Id defined in entity hierarchy" error seen in Event.
 * FIX 2 — Field renamed id → qrCodeID to match the ERD/DB column name.
 * FIX 3 — GenerationType.AUTO → IDENTITY (maps to Derby IDENTITY column).
 * FIX 4 — updatable = false on PK column.
 * FIX 5 — @Column added to every attribute.
 * FIX 6 — @OneToOne annotations added to attendee and ticket fields.
 * FIX 7 — @OneToMany annotation added to venueGuards list.
 * FIX 8 — @Table(name = "qrcode") added.
 * FIX 9 — @NamedQueries added.
 * FIX 10 — null/duplicate guards on add/remove helpers.
 * FIX 11 — equals(), hashCode(), toString() corrected.
 * FIX 12 — Duplicate/orphaned comment block removed.
 */
@Entity
@Table(name = "qrcode")
@Access(AccessType.FIELD)         // FIX 1 — lock to field access, prevents mixed-access error
@NamedQueries({                   // FIX 9 — centralised JPQL
    @NamedQuery(
        name  = "QRCode.findAll",
        query = "SELECT q FROM QRCode q ORDER BY q.qrCodeID ASC"
    ),
    @NamedQuery(
        name  = "QRCode.findById",
        query = "SELECT q FROM QRCode q WHERE q.qrCodeID = :qrCodeID"
    ),
    @NamedQuery(
        name  = "QRCode.findByBarstring",
        query = "SELECT q FROM QRCode q WHERE q.barstring = :barstring"
    )
})
public class QRCode implements Serializable {

    // FIX 2 — Unique serialVersionUID. 54L is arbitrary; use a specific value
    //          per class to avoid version clashes during serialisation.
    private static final long serialVersionUID = 394L;

    // ── Primary Key ──────────────────────────────────────────────
    // FIX 2 — Renamed from 'id' to 'qrCodeID' to match the ERD column name.
    //          A field named 'id' would map to a column called 'id' by default,
    //          but the DB column is 'QRcodeID'. Explicit naming prevents mismatch.
    // FIX 3 — IDENTITY instead of AUTO (maps to Derby GENERATED ALWAYS AS IDENTITY).
    // FIX 4 — updatable = false: the PK must never change after row creation.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "QRcodeID", nullable = false, updatable = false)
    private int qrCodeID;

    // ── Attributes ───────────────────────────────────────────────
    // FIX 5 — @Column on every field: exact DB column name, length, nullability.
    @Column(name = "barstring", length = 255, nullable = false, unique = true)
    // unique = true: two QR codes with the same barstring would be a data error.
    private String barstring;

    @Column(name = "number", nullable = false)
    private int number;

    // ── ONE-TO-ONE : QRCode → Attendee ───────────────────────────
    // FIX 6 — @OneToOne annotation was completely missing in the original.
    //          Without it, JPA tried to persist 'attendee' as a plain column
    //          (looking for a column named "ATTENDEE" in the qrcode table),
    //          which does not exist, causing a startup mapping error.
    //
    // mappedBy = "qrCode" → the FK (qrcode_QRcodeID) lives in the ATTENDEE
    //            table, not here. Attendee is the owning side.
    //            QRCode is the INVERSE side of this one-to-one.
    //
    // CascadeType.ALL    → if this QR code is deleted, delete the attendee too.
    // optional = true    → a QRCode can exist before an attendee is assigned.
    // FetchType.LAZY     → don't load the attendee unless explicitly needed.
    @OneToOne(
        mappedBy  = "qrCode",
        cascade   = CascadeType.ALL,
        optional  = true,
        fetch     = FetchType.LAZY
    )
    private Attendee attendee;

    // ── ONE-TO-ONE : QRCode → Ticket ─────────────────────────────
    // FIX 6 — Same as above — @OneToOne was missing.
    // mappedBy = "qrCode" → the FK (QRcodeID) lives in the TICKET table.
    // Ticket is the owning side; QRCode is the inverse side.
    @OneToOne(
        mappedBy  = "qrCode",
        cascade   = CascadeType.ALL,
        optional  = true,
        fetch     = FetchType.LAZY
    )
    private Ticket ticket;

    // ── ONE-TO-MANY : QRCode → VenueGuard ────────────────────────
    // FIX 7 — @OneToMany annotation was completely missing in the original.
    //          Without it, JPA saw a List<VenueGuard> and tried to map it as
    //          an @ElementCollection of basic types, which fails because
    //          VenueGuard is an entity, not a basic type.
    //
    // mappedBy = "qrCode" → the FK (QRcodeID) lives in the VENUE_GUARD table.
    // VenueGuard is the owning side.
    @OneToMany(
        mappedBy      = "qrCode",
        cascade       = CascadeType.ALL,
        orphanRemoval = true,
        fetch         = FetchType.LAZY
    )
    private List<VenueGuard> venueGuards = new ArrayList<>();

    // ── Constructors ─────────────────────────────────────────────

    /** Required by JPA spec. */
    public QRCode() {}

    /**
     * Constructor for a NEW QR code before persisting.
     * qrCodeID is omitted — the DB generates it via IDENTITY.
     */
    public QRCode(String barstring, int number) {
        this.barstring = barstring;
        this.number    = number;
    }

    /**
     * Full constructor — used when reconstructing from DB (JDBC / test fixtures).
     * Attendee and Ticket are set separately via setAttendee() / setTicket()
     * because they are lazy-loaded relationships, not scalar values.
     */
    public QRCode(int qrCodeID, String barstring, int number) {
        this.qrCodeID  = qrCodeID;
        this.barstring = barstring;
        this.number    = number;
    }

    // ── Scalar Getters / Setters ──────────────────────────────────

    // FIX 2 — Renamed from getId()/setId() to getQrCodeID()/setQrCodeID()
    //          to match the field name. getId() on a field named qrCodeID
    //          breaks JSF EL (${qrCode.qrCodeID}) and framework reflection.
    public int getQrCodeID() {
        return qrCodeID;
    }

    public void setQrCodeID(int qrCodeID) {
        this.qrCodeID = qrCodeID;
    }

    public String getBarstring() {
        return barstring;
    }

    // Trim input and reject blank barstrings — the encoded value is required.
    public void setBarstring(String barstring) {
        if (barstring == null || barstring.trim().isEmpty()) {
            throw new IllegalArgumentException("QR barstring must not be blank.");
        }
        this.barstring = barstring.trim();
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    // ── Relationship Getters / Setters ────────────────────────────

    public Attendee getAttendee() {
        return attendee;
    }

    /**
     * Bidirectional helper for the ONE-TO-ONE with Attendee.
     * Sets both sides so the object graph stays consistent:
     *   qrCode.setAttendee(a) also sets a.setQrCode(qrCode).
     */
    public void setAttendee(Attendee a) {
        // avoid infinite loop: only update if the value is actually changing
        if (Objects.equals(this.attendee, a)) return;
        this.attendee = a;
        if (a != null && a.getQrCode() != this) {
            a.setQrCode(this);
        }
    }

    public Ticket getTicket() {
        return ticket;
    }

    /**
     * Bidirectional helper for the ONE-TO-ONE with Ticket.
     */
    public void setTicket(Ticket t) {
        if (Objects.equals(this.ticket, t)) return;
        this.ticket = t;
        if (t != null && t.getQrCode() != this) {
            t.setQrCode(this);
        }
    }

    public List<VenueGuard> getVenueGuards() {
        return venueGuards;
    }

    public void setVenueGuards(List<VenueGuard> venueGuards) {
        this.venueGuards = venueGuards;
    }

    // FIX 10 — null-check + duplicate guard.
    public void addVenueGuard(VenueGuard g) {
        if (g != null && !venueGuards.contains(g)) {
            venueGuards.add(g);
            g.setQrCode(this);   // keep the owning side in sync
        }
    }

    public void removeVenueGuard(VenueGuard g) {
        if (g != null && venueGuards.remove(g)) {
            g.setQrCode(null);
        }
    }

    // ── equals & hashCode ─────────────────────────────────────────
    // FIX 11 — Missing in original. Required for correct behaviour in
    //           collections and for bidirectional relationship helpers.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QRCode)) return false;
        QRCode other = (QRCode) o;
        // qrCodeID != 0 guards the transient (not-yet-persisted) state.
        return qrCodeID != 0 && qrCodeID == other.qrCodeID;
    }

    @Override
    public int hashCode() {
        return qrCodeID != 0 ? Objects.hash(qrCodeID) : System.identityHashCode(this);
    }

    // FIX 11 — Lazy relationships excluded from toString() to prevent
    //           LazyInitializationException during logging outside a transaction.
    // FIX 12 — Corrected field reference from 'id' to 'qrCodeID'.
    @Override
    public String toString() {
        return "QRCode{"
             + "qrCodeID="   + qrCodeID
             + ", barstring='" + barstring + '\''
             + ", number="   + number
             + '}';
    }
}