package tn.spring.pispring.ServiceIMP;

import org.springframework.http.ResponseEntity;
import tn.spring.pispring.Entities.Ticket;
import tn.spring.pispring.Entities.TicketStatusHistory;
import tn.spring.pispring.Interfaces.TicketInterface;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.spring.pispring.repo.HistoryRepository;
import tn.spring.pispring.repo.TicketRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TicketService implements TicketInterface {
    @Autowired
    TicketRepository ticketRepository;
    @Autowired
    private HistoryRepository historyRepository;
    @Override
    public Ticket addTicket(Ticket ticket) {
        return ticketRepository.save(ticket);
    }

    @Override
    public Ticket UpdateTicket(Long id, Ticket updatedTicket) {
      /*  Optional<Ticket> optionalTicket = ticketRepository.findById(id);

        if (optionalTicket.isPresent()) {
            Ticket existingTicket = optionalTicket.get();
            existingTicket.setTitre(updatedTicket.getTitre());
            existingTicket.setDescription(updatedTicket.getDescription());
            existingTicket.setDate(updatedTicket.getDate());
            existingTicket.setStatut(updatedTicket.getStatut());
            existingTicket.setDegre(updatedTicket.getDegre());
            return ticketRepository.save(existingTicket);
        } else {*/
            return null;

    }

    @Override
    public void deleteTicket(long id) {
        ticketRepository.deleteById(id);

    }

    @Override
    public List<Ticket> findAllTickets() {
        return ticketRepository.findAll();
    }

    @Override
    public Ticket findTicketById(long id) {
        return ticketRepository.findById(id).get();
    }
    public Optional<Ticket> updateTicketStatusByticketId(Long ticketid, Ticket.TicketStatus status) {
        Ticket ticket = ticketRepository.findTicketById(ticketid);
        if (ticket != null) {
            try {
                ticket.addStatusChange(status); // Ajouter le changement de statut à l'historique
                ticket.setStatus(status);
                ticketRepository.save(ticket);  // Enregistre les modifications dans la base de données
            } catch (Exception e) {
                // Loggez l'erreur pour en savoir plus
                System.err.println("Erreur lors de la mise à jour du statut du ticket: " + e.getMessage());
                e.printStackTrace();
            }
            return Optional.of(ticket);
        }
        return Optional.empty();
    }

    public List<TicketStatusHistory> getTicketStatusHistoryByticketId(Long ticketId) {
        Ticket ticket = ticketRepository.findTicketById(ticketId);
        if (ticket == null) {
            throw new RuntimeException("Ticket not found with assetId " + ticketId);
        }
        return ticket.getStatusHistory();
    }
    public List<Ticket> findClosedTickets() {
        return ticketRepository.findByStatus(Ticket.TicketStatus.CLOSED);
    }






}
