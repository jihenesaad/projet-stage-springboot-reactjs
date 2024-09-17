package tn.spring.pispring.ServiceIMP;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tn.spring.pispring.Entities.MailStructure;
import tn.spring.pispring.Entities.Ticket;
import tn.spring.pispring.Entities.User;
import tn.spring.pispring.repo.TicketRepository;
import tn.spring.pispring.repo.UserRepository;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private TicketRepository ticketRepo;

    @Value("$(SecureFlow)")
    private String fromMail;

    public void sendEmailWithSLA(Long ticketId, Long userId, MailStructure mailStructure) throws MessagingException {
        User user = userRepo.findUserById(userId);
        Ticket ticket = ticketRepo.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket not found"));

        int slaMinutes = getSLAMinutes(ticket.getSeverity());
        long slaTimeMillis = slaMinutes * 60 * 1000;

        Timer timer = new Timer();
        int finalSlaMinutes = slaMinutes;
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    Ticket ticket = ticketRepo.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket not found"));
                    if (isTicketNotClosed(ticket)) {
                        MimeMessage message = mailSender.createMimeMessage();
                        MimeMessageHelper helper = new MimeMessageHelper(message, true);

                        helper.setFrom(fromMail);
                        helper.setTo(user.getEmail());
                        helper.setSubject("SLA Expiry Notification for Ticket " + ticketId);
                        helper.setText("The SLA for ticket " + ticketId + " has expired after " + finalSlaMinutes + " minutes. Please CLOSE your ticket.", true);

                        mailSender.send(message);
                        System.out.println("Follow-up email sent to " + user.getEmail() + " after SLA time of " + finalSlaMinutes + " minutes.");
                    }
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        }, slaTimeMillis);
    }


    public void sendEmailOnTicketAssignment(Long ticketId, Long userId) {
        try {
            // Fetch the user and ticket details
            User user = userRepo.findUserById(userId);
            Ticket ticket = ticketRepo.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket not found"));

            // Determine the SLA based on ticket severity
            String severity = ticket.getSeverity();
            MailStructure mailStructure = new MailStructure();
            mailStructure.setSubject("Ticket Assigned: " + severity + " Severity");
            mailStructure.setMessage("A ticket with severity " + severity + " has been assigned to you. Please review it within the next " + getSLAMinutes(severity) + " minutes.");

            // Send the initial email immediately
            sendEmailImmediately(user.getEmail(), mailStructure);

            // Schedule the follow-up email based on SLA time
            sendEmailWithSLA(ticketId, userId, mailStructure);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }



    public void sendEmailImmediately(String toEmail, MailStructure mailStructure) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true);

        // Assurez-vous que 'fromMail' est correctement défini
        helper.setFrom(fromMail);
        helper.setTo(toEmail);
        helper.setSubject(mailStructure.getSubject());
        helper.setText(mailStructure.getMessage(), true);

        mailSender.send(message);
        System.out.println("Immediate email sent to " + toEmail);
    }


    // Check if the ticket is not closed
    private boolean isTicketNotClosed(Ticket ticket) {
        return ticket.getStatus() != Ticket.TicketStatus.CLOSED;
    }
    // Example method to set SLA deadline based on severity
    public void setSLADeadline(Ticket ticket) {
        int slaMinutes = getSLAMinutes(ticket.getSeverity());
        LocalDateTime slaDeadline = LocalDateTime.now().plusMinutes(slaMinutes);
        ticket.setSlaDeadline(slaDeadline);
        ticketRepo.save(ticket);
    }


    // Scheduled task to check for tickets with expired SLA and status not closed
    @Scheduled(fixedRate = 60000) // Check every minute
    public void checkTicketsSLA() {
        List<Ticket> tickets = ticketRepo.findAll();
        LocalDateTime now = LocalDateTime.now();

        for (Ticket ticket : tickets) {
            if (ticket.getSlaDeadline() != null && ticket.getSlaDeadline().isBefore(now) && isTicketNotClosed(ticket) && !ticket.isSlaNotificationSent()) {
                try {
                    // Prepare the mail structure
                    MailStructure mailStructure = new MailStructure();
                    mailStructure.setSubject("SLA Expiry: Ticket " + ticket.getId());
                    mailStructure.setMessage("The SLA for ticket " + ticket.getId() + " has expired. Please review the ticket.");

                    // Send email
                    sendEmailImmediately(ticket.getUser().getEmail(), mailStructure);

                    // Mark SLA notification as sent
                    ticket.setSlaNotificationSent(true);
                    ticketRepo.save(ticket); // Save the updated ticket
                } catch (MessagingException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private int getSLAMinutes(String severity) {
        switch (severity) {
            case "Critical":
                return 2; // 2 minutes for Critical severity
            case "Severe":
                return 5; // 5 minutes for Severe severity
            case "Moderate":
                return 2; // 7 minutes for Moderate severity
            default:
                return 10; // Default: 10 minutes for other severities
        }
    }}

