package tn.spring.pispring.repo;

import tn.spring.pispring.Entities.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.spring.pispring.Entities.User;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket,Long> {
    List<Ticket> findTicketByUser(User user);
    List<Ticket> findTicketsByAssetIdIn(List<String> assetIds);
    Ticket findByAssetId(String assetId);
    List<Ticket> findByStatus(Ticket.TicketStatus status);
    List<Ticket> findBySiteName(String siteName);
    Ticket findTicketById(Long Id);

}
