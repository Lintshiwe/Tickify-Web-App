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


@Entity
@Table(name = "admin")              
@Access(AccessType.FIELD)           
@NamedQueries({                     
    @NamedQuery(
        name  = "Admin.findAll",
        query = "SELECT a FROM Admin a ORDER BY a.lastname ASC"
    ),
    @NamedQuery(
        name  = "Admin.findById",
        query = "SELECT a FROM Admin a WHERE a.adminID = :adminID"
    ),
    @NamedQuery(
        name  = "Admin.findByEmail",
        query = "SELECT a FROM Admin a WHERE a.email = :email"
    ),
    @NamedQuery(
        name  = "Admin.findByEvent",
        query = "SELECT a FROM Admin a WHERE a.event.eventID = :eventID ORDER BY a.lastname ASC"
    ),
    @NamedQuery(
        name  = "Admin.login",
        query = "SELECT a FROM Admin a WHERE a.email = :email AND a.password = :password"
    )
})
public class Admin implements Serializable {

    
    private static final long serialVersionUID = 1047392L;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "adminID", nullable = false, updatable = false)
    private int adminID;

  

    @Column(name = "firstname", length = 45, nullable = false)
    private String firstname;

    @Column(name = "lastname", length = 45, nullable = false)
    private String lastname;

    @Column(name = "email", length = 45, nullable = false, unique = true)
    
    private String email;

    @Column(name = "password", length = 15, nullable = false)
  
    private String password;

  
    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(
        name                 = "eventID",    // FK column in the admin table
        nullable             = true,
        referencedColumnName = "eventID"     // PK column in the event table
    )
    private Event event;

    // ── Constructors ─────────────────────────────────────────────

    public Admin() {}

    public Admin(String firstname, String lastname,
                 String email, String password, Event event) {
        this.firstname = firstname;
        this.lastname  = lastname;
        this.email     = email;
        this.password  = password;
        setEvent(event);   // use setter to keep bidirectional link in sync
    }

   
    public Admin(int adminID, String firstname, String lastname,
                 String email, String password, Event event) {
        this.adminID   = adminID;
        this.firstname = firstname;
        this.lastname  = lastname;
        this.email     = email;
        this.password  = password;
        setEvent(event);
    }


    public int getAdminID() {
        return adminID;
    }

    public void setAdminID(int adminID) {
        this.adminID = adminID;
    }

    public String getFirstname() {
        return firstname;
    }

 
    public void setFirstname(String firstname) {
        if (firstname == null || firstname.trim().isEmpty()) {
            throw new IllegalArgumentException("Admin firstname must not be blank.");
        }
        this.firstname = firstname.trim();
    }

    public String getLastname() {
        return lastname;
    }

    public void setLastname(String lastname) {
        if (lastname == null || lastname.trim().isEmpty()) {
            throw new IllegalArgumentException("Admin lastname must not be blank.");
        }
        this.lastname = lastname.trim();
    }

    public String getEmail() {
        return email;
    }

    // FIX 11 — Lowercase and trim email for consistent login lookups.
    //           "Admin@Tickify.ac.za" and "admin@tickify.ac.za" must match.
    public void setEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Admin email must not be blank.");
        }
        this.email = email.trim().toLowerCase();
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Admin password must not be blank.");
        }
        this.password = password;
        
    }

 
    public String getFullName() {
        return firstname + " " + lastname;
    }

    // ── Relationship Getters / Setters ────────────────────────────

    public Event getEvent() {
        return event;
    }

  
    public void setEvent(Event event) {
        if (Objects.equals(this.event, event)) return;
        this.event = event;
    }

  
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Admin)) return false;
        Admin other = (Admin) o;
        return adminID != 0 && adminID == other.adminID;
    }

    @Override
    public int hashCode() {
        // Persisted entity → stable PK hash.
        // Transient entity → identity hash (safe in HashSets before save).
        return adminID != 0
                ? Objects.hash(adminID)
                : System.identityHashCode(this);
    }

   
    @Override
    public String toString() {
        return "Admin{"
             + "adminID="     + adminID
             + ", firstname='" + firstname + '\''
             + ", lastname='"  + lastname  + '\''
             + ", email='"     + email     + '\''
             + '}';
    }
}