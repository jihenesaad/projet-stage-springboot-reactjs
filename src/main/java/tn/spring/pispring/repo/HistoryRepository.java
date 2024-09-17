package tn.spring.pispring.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.spring.pispring.Entities.TicketStatusHistory;

@Repository
public interface HistoryRepository extends JpaRepository<TicketStatusHistory, Long> {
    TicketStatusHistory findTicketStatusHistoriesByTicket(Long id);
}
