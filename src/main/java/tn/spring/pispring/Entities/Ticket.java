package tn.spring.pispring.Entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.*;
import tn.spring.pispring.Entities.TicketStatusHistory;
import tn.spring.pispring.Entities.User;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
public class Ticket {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String assetId;
    private String description;
    private String severity;
    private String remediation;
    private String siteName;


    @Enumerated(EnumType.STRING)
    private TicketStatus status;
    private boolean archived = false;
    private LocalDateTime slaDeadline;
    private boolean slaNotificationSent = false;

    // Relation OneToMany avec TicketStatusHistory
    @OneToMany(mappedBy = "ticket", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<TicketStatusHistory> statusHistory = new ArrayList<>();

    @JsonIgnore
    @OneToOne
    Vulnerability vulnerability;

    @ManyToOne
    User user;

    public enum TicketStatus {
        OPEN,
        IN_PROGRESS,
        RESOLVED,
        CLOSED,

    }

    public Ticket(String ip, String severity, String description,String remidiation) {
        this.assetId = ip;
        this.severity = severity;
        this.description = description;
        this.remediation=remidiation;
        this.status = TicketStatus.OPEN;
        this.slaDeadline = calculateSlaDeadline(severity);
    }

    private LocalDateTime calculateSlaDeadline(String severity) {
        switch (severity) {
            case "Critical":
                return LocalDateTime.now().plusMinutes(2);
            case "Severe":
                return LocalDateTime.now().plusMinutes(5);
            case "Moderate":
                return LocalDateTime.now().plusMinutes(7);
            default:
                return LocalDateTime.now().plusMinutes(10);
        }
    }
    public void addStatusChange(Ticket.TicketStatus status) {
        TicketStatusHistory history = new TicketStatusHistory(this, status, LocalDateTime.now());
        this.statusHistory.add(history);
    }
    public void setStatus(TicketStatus status) {
        this.status = status;
        if (status == TicketStatus.CLOSED) {
            this.archived = true;
        }
    }


}