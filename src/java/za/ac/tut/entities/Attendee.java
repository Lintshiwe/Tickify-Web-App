package za.ac.tut.entities;

import java.io.Serializable;
import java.util.Date;
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
import javax.persistence.Temporal;
import javax.persistence.TemporalType;


@Entity
@Table(name = "attendee")
@Access(AccessType.FIELD)           // FIX 2 — lock to field access; prevents mixed-access error
@NamedQueries({ // FIX 11 — centralised JPQL, validated at startup
    
    @NamedQuery(
            name = "Attendee.findAll",
            query = "SELECT a FROM Attendee a ORDER BY a.lastname ASC"
    ),
    @NamedQuery(
            name = "Attendee.findById",
            query = "SELECT a FROM Attendee a WHERE a.attendeeID = :attendeeID"
    ),
    @NamedQuery(
            name = "Attendee.findByEmail",
            query = "SELECT a FROM Attendee a WHERE a.email = :email"
    ),
    @NamedQuery(
            name = "Attendee.findByInstitution",
            query = "SELECT a FROM Attendee a WHERE a.tertiaryInstitution = :tertiaryInstitution ORDER BY a.lastname ASC"
    ),
    @NamedQuery(
            name = "Attendee.login",
            query = "SELECT a FROM Attendee a WHERE a.email = :email AND a.password = :password"
    )
})
public class Attendee implements Serializable {

    // FIX 12 — Unique serialVersionUID per class instead of the generic 1L.
    private static final long serialVersionUID = 51928L;

   
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attendeeID", nullable = false, updatable = false)
    private int attendeeID;

    // ── Attributes ───────────────────────────────────────────────
    // FIX 1 + FIX 6 — All fields were completely absent in the original.
    //                  @Column maps each field to the exact DB column name,
    //                  length, and nullability from the ERD DDL.
    @Column(name = "tertiaryInstitution", length = 45, nullable = true)
    // nullable = true — not every attendee may be from a tertiary institution
    private String tertiaryInstitution;

    @Column(name = "username", length = 45, nullable = true)
    private String username;

    @Column(name = "clientType", length = 20, nullable = true)
    private String clientType;

    @Column(name = "phoneNumber", length = 25, nullable = true)
    private String phoneNumber;

    @Column(name = "studentNumber", length = 45, nullable = true)
    private String studentNumber;

    @Column(name = "idPassportNumber", length = 45, nullable = true)
    private String idPassportNumber;

    @Temporal(TemporalType.DATE)
    @Column(name = "dateOfBirth", nullable = true)
    private Date dateOfBirth;

    @Column(name = "biography", length = 1200, nullable = true)
    private String biography;

    @Column(name = "firstname", length = 45, nullable = false)
    private String firstname;

    @Column(name = "lastname", length = 45, nullable = false)
    private String lastname;

    @Column(name = "email", length = 45, nullable = true, unique = true)
    // unique = true — two attendees cannot share the same email address (login credential)
    private String email;

    @Column(name = "password", length = 100, nullable = false)
    // length = 100 — longer than 45 to accommodate hashed passwords in future
    private String password;

  
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "qrcode_QRcodeID", // FK column in the attendee table
            nullable = false,
            referencedColumnName = "QRcodeID" // PK column in the qrcode table
    )
    private QRCode qrCode;

  
    @ManyToMany(
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY
    )
    @JoinTable(
            name = "attendee_has_event",
            joinColumns = @JoinColumn(name = "attendeeID"),
            inverseJoinColumns = @JoinColumn(name = "eventID")
    )
    private List<Event> events = new ArrayList<>();

   
    @ManyToMany(
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            fetch = FetchType.LAZY
    )
    @JoinTable(
            name = "attendee_has_ticket",
            joinColumns = @JoinColumn(name = "attendeeID"),
            inverseJoinColumns = @JoinColumn(name = "ticketID")
    )
    private List<Ticket> tickets = new ArrayList<>();

    // ── Constructors ─────────────────────────────────────────────
    /**
     * Required by JPA spec.
     */
    public Attendee() {
    }

    /**
     * Constructor for a NEW attendee before persisting. attendeeID is omitted —
     * the DB generates it via IDENTITY.
     */
    public Attendee(String tertiaryInstitution, String firstname, String lastname,
            String email, String password, QRCode qrCode) {
        this.tertiaryInstitution = tertiaryInstitution;
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.password = password;
        setQrCode(qrCode);   // use setter to keep bidirectional link in sync
    }

    /**
     * Full constructor — for reconstructing from DB (JDBC / test fixtures).
     */
    public Attendee(int attendeeID, String tertiaryInstitution, String firstname,
            String lastname, String email, String password, QRCode qrCode) {
        this.attendeeID = attendeeID;
        this.tertiaryInstitution = tertiaryInstitution;
        this.firstname = firstname;
        this.lastname = lastname;
        this.email = email;
        this.password = password;
        setQrCode(qrCode);
    }

    // ── Scalar Getters / Setters ──────────────────────────────────
  
    public int getAttendeeID() {
        return attendeeID;
    }

    public void setAttendeeID(int attendeeID) {
        this.attendeeID = attendeeID;
    }

    public String getTertiaryInstitution() {
        return tertiaryInstitution;
    }

    public void setTertiaryInstitution(String tertiaryInstitution) {
        // FIX 13 — Trim input for consistent storage.
        this.tertiaryInstitution = (tertiaryInstitution != null)
                ? tertiaryInstitution.trim()
                : null;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username == null ? null : username.trim().toLowerCase();
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType == null ? null : clientType.trim().toUpperCase();
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber == null ? null : phoneNumber.trim();
    }

    public String getStudentNumber() {
        return studentNumber;
    }

    public void setStudentNumber(String studentNumber) {
        this.studentNumber = studentNumber == null ? null : studentNumber.trim();
    }

    public String getIdPassportNumber() {
        return idPassportNumber;
    }

    public void setIdPassportNumber(String idPassportNumber) {
        this.idPassportNumber = idPassportNumber == null ? null : idPassportNumber.trim();
    }

    public Date getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(Date dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getBiography() {
        return biography;
    }

    public void setBiography(String biography) {
        this.biography = biography == null ? null : biography.trim();
    }

    public String getFirstname() {
        return firstname;
    }

    // FIX 13 — Defensive validation: firstname is required.
    public void setFirstname(String firstname) {
        if (firstname == null || firstname.isEmpty()) {
            throw new IllegalArgumentException("Attendee firstname must not be blank.");
        }
        this.firstname = firstname.trim();
    }

    public String getLastname() {
        return lastname;
    }

    // FIX 13 — Defensive validation: lastname is required.
    public void setLastname(String lastname) {
        if (lastname == null || lastname.isEmpty()) {
            throw new IllegalArgumentException("Attendee lastname must not be blank.");
        }
        this.lastname = lastname.trim();
    }

    public String getEmail() {
        return email;
    }

    // Email is optional for attendee registration when username is provided.
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

    // FIX 13 — Reject blank passwords.
    public void setPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Attendee password must not be blank.");
        }
        this.password = password;
        // Note: In production, hash the password here before storing:
        // this.password = PasswordUtil.hash(password);
    }

    // ── Convenience method ────────────────────────────────────────
    // FIX 15 — Useful in JSP/EL: ${attendee.fullName} instead of
    //           ${attendee.firstname} ${attendee.lastname}
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
    public QRCode getQrCode() {
        return qrCode;
    }


    public void setQrCode(QRCode qrCode) {
        if (Objects.equals(this.qrCode, qrCode)) {
            return;
        }
        this.qrCode = qrCode;
        if (qrCode != null && qrCode.getAttendee() != this) {
            qrCode.setAttendee(this);
        }
    }

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    // FIX 14 — null-check + duplicate guard on all add/remove helpers.
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

   
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Attendee)) {
            return false;
        }
        Attendee other = (Attendee) o;
        return attendeeID != 0 && attendeeID == other.attendeeID;
    }

    @Override
    public int hashCode() {
        // Persisted entity → stable PK hash.
        // Transient entity → identity hash (safe in HashSets before save).
        return attendeeID != 0 ? Objects.hash(attendeeID) : System.identityHashCode(this);
    }

    // FIX 12 — toString() added. Lazy collections intentionally excluded
    //           to prevent LazyInitializationException during logging
    //           outside an active transaction.
    @Override
    public String toString() {
        return "Attendee{"
                + "attendeeID=" + attendeeID
            + ", username='" + username + '\''
                + ", firstname='" + firstname + '\''
                + ", lastname='" + lastname + '\''
                + ", email='" + email + '\''
                + ", tertiaryInstitution='" + tertiaryInstitution + '\''
                + '}';
    }
}
