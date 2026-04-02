package za.ac.tut.entities;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * ╔══════════════════════════════════════════════════════════╗
 * ║  ENTITY : Venue                                          ║
 * ║  TABLE  : venue                                          ║
 * ║  PK     : venueID  (INT, AUTO_INCREMENT)                 ║
 * ╠══════════════════════════════════════════════════════════╣
 * ║  RELATIONSHIPS                                           ║
 * ║  ONE-TO-MANY → Event              (venue hosts events)   ║
 * ║  ONE-TO-MANY → VenueGuard         (venue has guards)     ║
 * ║  ONE-TO-MANY → TertiaryPresenter  (venue has presenters) ║
 * ╚══════════════════════════════════════════════════════════╝
 */
@Entity
@Table(name = "venue")
public class Venue implements Serializable {

    private static final long serialVersionUID = 1L;

    // ── Primary Key ──────────────────────────────────────────────
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)   // FIX: IDENTITY maps to AUTO_INCREMENT in Derby/MySQL
    @Column(name = "venueID", nullable = false)
    private int venueID;

    // ── Attributes ───────────────────────────────────────────────
    @Column(name = "name", length = 45)
    private String name;

    @Column(name = "address", length = 205)
    private String address;

    // ── ONE-TO-MANY : Venue → Event ───────────────────────────────
    // mappedBy = "venue" refers to the 'venue' field inside Event.java
    // CascadeType.ALL  : persist/merge/remove events when venue changes
    // orphanRemoval    : if an Event is removed from this list, delete it from DB
    // FetchType.LAZY   : events are only loaded when getEvents() is called (performance)
    @OneToMany(
        mappedBy     = "venue",
        cascade      = CascadeType.ALL,
        orphanRemoval = true,
        fetch        = FetchType.LAZY
    )
    private List<Event> events = new ArrayList<>();

    // ── ONE-TO-MANY : Venue → VenueGuard ─────────────────────────
    @OneToMany(
        mappedBy     = "venue",
        cascade      = CascadeType.ALL,
        orphanRemoval = true,
        fetch        = FetchType.LAZY
    )
    private List<VenueGuard> venueGuards = new ArrayList<>();

    // ── ONE-TO-MANY : Venue → TertiaryPresenter ──────────────────
    @OneToMany(
        mappedBy     = "venue",
        cascade      = CascadeType.ALL,
        orphanRemoval = true,
        fetch        = FetchType.LAZY
    )
    private List<TertiaryPresenter> tertiaryPresenters = new ArrayList<>();

    // ── Constructors ─────────────────────────────────────────────
    public Venue() {}

    /**
     * Use this constructor when creating a NEW venue (before persisting).
     * Do NOT pass venueID — the database generates it via AUTO_INCREMENT.
     */
    public Venue(String name, String address) {
        this.name    = name;
        this.address = address;
    }

    /**
     * Full constructor — use when reconstructing from DB results.
     */
    public Venue(int venueID, String name, String address) {
        this.venueID = venueID;
        this.name    = name;
        this.address = address;
    }

    // ── Scalar Getters / Setters ──────────────────────────────────
    public int getVenueID() {
        return venueID;
    }

    // FIX: was setId() — must match field name so JSF/EL and frameworks resolve it
    public void setVenueID(int venueID) {
        this.venueID = venueID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    // ── Relationship Getters / Setters ────────────────────────────

    public List<Event> getEvents() {
        return events;
    }

    public void setEvents(List<Event> events) {
        this.events = events;
    }

    /**
     * Bidirectional helper — always use this instead of getEvents().add().
     * Sets BOTH sides of the relationship so the object graph stays consistent.
     */
    public void addEvent(Event e) {
        events.add(e);
        e.setVenue(this);   // keeps the owning side (Event) in sync
    }

    public void removeEvent(Event e) {
        events.remove(e);
        e.setVenue(null);
    }

    public List<VenueGuard> getVenueGuards() {
        return venueGuards;
    }

    public void setVenueGuards(List<VenueGuard> venueGuards) {
        this.venueGuards = venueGuards;
    }

    public void addVenueGuard(VenueGuard g) {
        venueGuards.add(g);
        g.setVenue(this);
    }

    public void removeVenueGuard(VenueGuard g) {
        venueGuards.remove(g);
        g.setVenue(null);
    }

    public List<TertiaryPresenter> getTertiaryPresenters() {
        return tertiaryPresenters;
    }

    public void setTertiaryPresenters(List<TertiaryPresenter> tertiaryPresenters) {
        this.tertiaryPresenters = tertiaryPresenters;
    }

    public void addTertiaryPresenter(TertiaryPresenter tp) {
        tertiaryPresenters.add(tp);
        tp.setVenue(this);
    }

    public void removeTertiaryPresenter(TertiaryPresenter tp) {
        tertiaryPresenters.remove(tp);
        tp.setVenue(null);
    }



   
}