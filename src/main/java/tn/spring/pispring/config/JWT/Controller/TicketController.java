package tn.spring.pispring.config.JWT.Controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import tn.spring.pispring.Entities.Ticket;
import tn.spring.pispring.Entities.TicketStatusHistory;
import tn.spring.pispring.ServiceIMP.TicketService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@CrossOrigin("*")
public class TicketController {
    @Autowired
    TicketService ticketService;
    @PostMapping("/addTicket")
    public Ticket addTicket(@RequestBody Ticket ticket) {
        return ticketService.addTicket(ticket);
    }
    @PutMapping("/UpdateTicket/{id}")
    public Ticket UpdateTicket(@PathVariable("id") Long id, Ticket updatedTicket) {
        return ticketService.UpdateTicket(id, updatedTicket);
    }
    @DeleteMapping("/deleteTicket/{id}")
    public void deleteTicket(@PathVariable("id") long id) {
        ticketService.deleteTicket(id);
    }
    @GetMapping("/findAllTickets")
    public List<Ticket> findAllTickets() {
        return ticketService.findAllTickets();
    }
    @GetMapping("/findTicketById/{id}")
    public Ticket findTicketById(@PathVariable("id") long id) {
        return ticketService.findTicketById(id);
    }
    @PutMapping("/status/{ticketid}")
    public ResponseEntity<?> updateTicketStatusByAssetId(
            @PathVariable Long ticketid,
            @RequestParam Ticket.TicketStatus status) {
        Optional<Ticket> updatedTicket = ticketService.updateTicketStatusByticketId(ticketid, status);

        if (updatedTicket.isPresent()) {
            return ResponseEntity.ok(updatedTicket.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Ticket not found");
        }
    }
    @PutMapping("/{ticketId}/status/{newStatus}")
    public ResponseEntity<String> updateTicketStatus(@PathVariable Long ticketId, @PathVariable Ticket.TicketStatus newStatus) {
        try {
            ticketService. updateTicketStatusByticketId(ticketId, newStatus);
            return ResponseEntity.ok("Ticket status updated successfully");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    @GetMapping("/asset/{ticketId}/history")
    public ResponseEntity<List<TicketStatusHistory>> getTicketStatusHistoryByAssetId(@PathVariable Long ticketId) {
        List<TicketStatusHistory> statusHistory = ticketService.getTicketStatusHistoryByticketId(ticketId);
        return ResponseEntity.ok(statusHistory);
    }
    @GetMapping("/closed")
    public ResponseEntity<List<Ticket>> getClosedTickets() {
        List<Ticket> closedTickets = ticketService.findClosedTickets();
        return ResponseEntity.ok(closedTickets);
    }




}
