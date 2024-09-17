package tn.spring.pispring.config.JWT.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.spring.pispring.Entities.MailStructure;
import tn.spring.pispring.Entities.Ticket;
import tn.spring.pispring.Entities.User;
import tn.spring.pispring.Entities.Vulnerability;
import tn.spring.pispring.ServiceIMP.EmailService;
import tn.spring.pispring.ServiceIMP.InsightVMService;

import javax.mail.MessagingException;
import java.util.List;
import java.util.Map;

@RestController
public class InsightVMController {

    @Autowired
    private InsightVMService insightVMService;
    @Autowired
    private EmailService emailService;

    @PostMapping("/generate-tickets-from-json")
    public List<Ticket> generateTicketsFromJson(@RequestBody String json) {
        // Call the service method to generate tickets from the provided JSON
        return insightVMService.generateTicketsFromJson(json);
    }
    @GetMapping("/sites/info")
    public List<Map<String, Object>> getSiteInformation() {
        return insightVMService.getSiteInformation();
    }
    @GetMapping("/by-site-name/{siteName}")
    public List<Ticket> getTicketsBySiteName(@PathVariable String siteName)
    { return insightVMService.getTicketsBySiteName(siteName); }

    @GetMapping("/{siteId}/tickets")
    public List<Ticket> getTicketsBySite(@PathVariable String siteId) {
        return insightVMService.getTicketsBySiteId(siteId);
    }
    @GetMapping("/tickets/details/by-ip/{ticketId}")
    public ResponseEntity<Ticket> getTicketById(@PathVariable Long ticketId) {
        Ticket ticket = insightVMService.getTicketDetailsById(ticketId);
        if (ticket != null) {
            return ResponseEntity.ok(ticket);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    @PutMapping("/{ticketId}/assign/{userId}")
    public Ticket assignTicketToUser(@PathVariable Long ticketId, @PathVariable Long userId) {
        emailService.sendEmailOnTicketAssignment(ticketId, userId);
        return insightVMService.assignTicketToUser(ticketId, userId);

    }
    @GetMapping("/getusers")
    public List<User> getUsers() {
        return insightVMService.getUsers();
    }

    @GetMapping("/tickets-user/{userId}")
    public List<Ticket> getTicketsByUserId(@PathVariable Long userId) {
        return insightVMService.getTicketsByUserId(userId);
    }
    @PostMapping("/send/{idUser}/{severity}")
    public ResponseEntity<String> sendMailWithSLA(@PathVariable Long ticketid, @PathVariable Long userId, @RequestBody MailStructure mailStructure) throws MessagingException {
        emailService.sendEmailWithSLA(ticketid, userId, mailStructure);
        return ResponseEntity.ok().body("{\"message\": \"Email will be sent based on SLA!\"}");
    }

    @GetMapping("/assets/{assetId}/vulnerabilities/{vulnId}/solution")
    public ResponseEntity<String> getRemediationDetails(
            @PathVariable String assetId,
            @PathVariable String vulnId) {

        String remediationDetails = insightVMService.getRemediationDetails(assetId, vulnId);

        if (remediationDetails != null) {
            return ResponseEntity.ok(remediationDetails);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Remediation details not found");
        }
    }
    @GetMapping("/vulnerabilities/{vulnId}")
    public ResponseEntity<String> getVulnerabilityDetails(@PathVariable String vulnId) {
        String vulnerabilityDetails = insightVMService.getVulnerabilityDetails(vulnId);
        if (vulnerabilityDetails != null) {
            return ResponseEntity.ok(vulnerabilityDetails);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Vulnerability details not found");
        }
    }
    @PostMapping("/create")
    public ResponseEntity<Ticket> createTicketsForSite(
            @RequestParam String siteId
    ) {
        try {
            Ticket ticket = (Ticket) insightVMService.createTicketsForSite(siteId);
            if (ticket != null) {
                return ResponseEntity.ok(ticket);
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }


}
