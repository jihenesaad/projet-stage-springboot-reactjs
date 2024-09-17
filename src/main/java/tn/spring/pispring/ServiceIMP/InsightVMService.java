package tn.spring.pispring.ServiceIMP;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import tn.spring.pispring.Entities.Ticket;
import tn.spring.pispring.Entities.User;
import tn.spring.pispring.Entities.Vulnerability;
import tn.spring.pispring.Entities.VulnerabilityResponse;
import tn.spring.pispring.config.JWT.RestTemplateConfig;
import tn.spring.pispring.repo.TicketRepository;
import tn.spring.pispring.repo.UserRepository;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.hibernate.tool.schema.SchemaToolingLogging.LOGGER;

@Service
public class InsightVMService {
    @Autowired
    TicketRepository ticketRepository;
    @Autowired
    UserRepository userRepository;
    private final RestTemplate restTemplate;

    @Value("${insightvm.api.url}")
    private String apiUrl;

    @Value("${insightvm.api.user}")
    private String apiUser;

    @Value("${insightvm.api.password}")
    private String apiPassword;
    private static final Logger LOGGER = Logger.getLogger(InsightVMService.class.getName());
    private final ObjectMapper mapper = new ObjectMapper();



    @Autowired
    public InsightVMService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }


    @Scheduled(fixedRate = 30000)
    public void scheduledScanAndSave() {
        generateTicketsForAllSites();
        getSiteInformation();

    }
    public List<Ticket> generateTicketsFromJson(String json) {
        ObjectMapper mapper = new ObjectMapper();
        List<Ticket> tickets = new ArrayList<>();

        try {
            JsonNode rootNode = mapper.readTree(json);
            JsonNode resourcesNode = rootNode.path("resources");

            if (resourcesNode.isArray()) {
                for (JsonNode node : resourcesNode) {
                    JsonNode vulnerabilitiesNode = node.path("vulnerabilities");

                    for (JsonNode vulnerabilityNode : vulnerabilitiesNode) {
                        String vulnerabilityId = vulnerabilityNode.path("id").asText();
                        String assetId = vulnerabilityId; // Utiliser l'id de la vulnérabilité comme identifiant principal

                        try {
                            String vulnerabilityUrl = apiUrl + "/vulnerabilities/" + vulnerabilityId;
                            JsonNode vulnerabilityDetails = restTemplate.getForObject(vulnerabilityUrl, JsonNode.class);
                            if (vulnerabilityDetails == null) {
                                System.err.println("No details found for vulnerability ID: " + vulnerabilityId);
                                continue;
                            }

                            String severity = vulnerabilityDetails.path("severity").asText("None");
                            String vector = vulnerabilityDetails.path("cvss").path("v2").path("vector").asText("N/A");

                            String remediationUrl = apiUrl + "/assets/" + assetId + "/vulnerabilities/" + vulnerabilityId + "/solution";
                            JsonNode remediationDetails = restTemplate.getForObject(remediationUrl, JsonNode.class);
                            String remediationSteps = remediationDetails.path("resources").get(0).path("steps").path("text").asText("No remediation steps provided.");

                            Optional<Ticket> existingTicketOptional = Optional.ofNullable(ticketRepository.findByAssetId(assetId));
                            if (existingTicketOptional.isPresent()) {
                                Ticket existingTicket = existingTicketOptional.get();
                                if (!existingTicket.getSeverity().equals(severity)) {
                                    existingTicket.setSeverity(severity);
                                    existingTicket.setDescription("Vulnerability Vector: " + vector);
                                    existingTicket.setRemediation(remediationSteps);
                                    ticketRepository.save(existingTicket);
                                }
                            } else {
                                String description = "Vulnerability Vector: " + vector;
                                Ticket newTicket = new Ticket(assetId, severity, description, remediationSteps);
                                newTicket.addStatusChange(newTicket.getStatus());
                                tickets.add(newTicket);
                                ticketRepository.save(newTicket);
                            }
                        } catch (HttpServerErrorException e) {
                            System.err.println("Error fetching vulnerability details: " + e.getStatusCode() + " " + e.getResponseBodyAsString());
                        } catch (Exception e) {
                            System.err.println("Error processing vulnerability ID " + vulnerabilityId + ": " + e.getMessage());
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading JSON: " + e.getMessage());
        }

        return tickets;
    }

    public Ticket getTicketDetailsById(Long ticketId) { Optional<Ticket> ticketOptional = ticketRepository.findById(ticketId); return ticketOptional.orElse(null); }



    private String getSites() {
        String url = apiUrl + "/sites";
        HttpHeaders headers = createHeaders(apiUser, apiPassword);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        String responseBody = response.getBody();
        System.out.println("Response from getSites: " + responseBody); // For debugging

        return responseBody;
    }

    private String getAssetsForSite(String siteId) {
        String url = apiUrl + "/sites/" + siteId + "/assets";
        HttpHeaders headers = createHeaders(apiUser, apiPassword);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        String responseBody = response.getBody();
        System.out.println("Response from getAssetsForSite: " + responseBody); // For debugging

        return responseBody;
    }

    public List<String> getAssetIdsForSite(String siteId) {
        String url = apiUrl + "/sites/" + siteId + "/assets";
        HttpHeaders headers = createHeaders(apiUser, apiPassword);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        String responseBody = response.getBody();
        System.out.println("Response from getAssetsForSite: " + responseBody); // For debugging

        List<String> assetIds = new ArrayList<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);
            JsonNode resourcesNode = rootNode.path("resources");

            if (resourcesNode.isArray()) {
                for (JsonNode assetNode : resourcesNode) {
                    String assetId = assetNode.path("id").asText();
                    assetIds.add(assetId);
                }
            }
        } catch (IOException e) {
            System.err.println("Error parsing asset IDs: " + e.getMessage());
        }

        return assetIds;
    }
    private String getVulnerabilitiesForAsset(String assetId) {
        String url = apiUrl + "/assets/" + assetId + "/vulnerabilities";
        HttpHeaders headers = createHeaders(apiUser, apiPassword);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        String responseBody = response.getBody();
        System.out.println("Response from getVulnerabilitiesForAsset: " + responseBody); // For debugging

        return responseBody;
    }
    public String getVulnerabilityDetails(String vulnId) {
        // URL de l'API pour récupérer les détails de la vulnérabilité par ID
        String url = String.format("%s/vulnerabilities/%s", apiUrl, vulnId);
        HttpHeaders headers = createHeaders(apiUser, apiPassword);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                return response.getBody(); // Return the details of the vulnerability
            } else {
                System.err.println("Failed to fetch vulnerability details: " + response.getStatusCode() + " - " + response.getBody());
            }
        } catch (Exception e) {
            System.err.println("Error fetching vulnerability details: " + e.getMessage());
        }

        return null; // Return null if there was an error
    }

    private Map<String, String> extractVulnerabilityDetails(String responseBody) {
        Map<String, String> details = new HashMap<>();
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(responseBody);

            // Extraire les informations spécifiques
            String vector = rootNode.path("vector").asText();
            String severity = rootNode.path("severity").asText();
            String title = rootNode.path("title").asText();

            details.put("vector", vector);
            details.put("severity", severity);
            details.put("title", title);
        } catch (IOException e) {
            System.err.println("Error parsing vulnerability details: " + e.getMessage());
        }
        return details;
    }
    private List<String> getVulnerabilityIdsForAsset(String assetId) {
        String url = String.format("%s/assets/%s/vulnerabilities", apiUrl, assetId);
        HttpHeaders headers = createHeaders(apiUser, apiPassword);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            String responseBody = response.getBody();
            List<String> vulnerabilityIds = new ArrayList<>();
            JsonNode rootNode = mapper.readTree(responseBody);
            JsonNode resourcesNode = rootNode.path("resources");

            if (resourcesNode.isArray()) {
                for (JsonNode vulnNode : resourcesNode) {
                    String vulnId = vulnNode.path("id").asText();
                    vulnerabilityIds.add(vulnId);
                }
            }
            return vulnerabilityIds;
        } catch (Exception e) {
            System.err.println("Error fetching vulnerability IDs for asset: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    public String getRemediationDetails(String assetId, String vulnId) {
        String url = String.format("%s/assets/%s/vulnerabilities/%s/solution", apiUrl, assetId, vulnId);
        HttpHeaders headers = createHeaders(apiUser, apiPassword);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            if (response.getStatusCode() == HttpStatus.OK) {
                String remediationJson = response.getBody();
                System.out.println("Remediation JSON: " + remediationJson); // Log the JSON
                return remediationJson;
            } else {
                System.err.println("Failed to fetch remediation details: " + response.getStatusCode() + " - " + response.getBody());
            }
        } catch (Exception e) {
            System.err.println("Error fetching remediation details: " + e.getMessage());
        }
        return null;
    }
    private Ticket createTicketWithDetails(String assetId, String vulnerabilityId, String siteName) {
        try {
            // Récupération des détails de la vulnérabilité
            String vulnerabilityJson = getVulnerabilityDetails(vulnerabilityId);
            if (vulnerabilityJson == null) {
                LOGGER.severe("Aucun détail de vulnérabilité trouvé pour l'ID : " + vulnerabilityId);
                return null;
            }

            JsonNode vulnerabilityNode = mapper.readTree(vulnerabilityJson);
            String ip = vulnerabilityNode.path("id").asText();
            String description = vulnerabilityNode.path("title").asText();
            String severity = vulnerabilityNode.path("severity").asText();

            // Récupération des détails de la remédiation
            String remediationJson = getRemediationDetails(assetId, vulnerabilityId);
            if (remediationJson == null) {
                LOGGER.severe("Aucun détail de remédiation trouvé pour l'asset ID : " + assetId + " et la vulnérabilité ID : " + vulnerabilityId);
                return null;
            }

            JsonNode remediationNode = mapper.readTree(remediationJson);
            JsonNode resourcesNode = remediationNode.path("resources").get(0);
            JsonNode stepsNode = resourcesNode.path("steps");
            String remediationHtml = stepsNode.path("html").asText();

            String remediationText = Jsoup.parse(remediationHtml).text();

            Optional<Ticket> existingTicketOpt = Optional.ofNullable(ticketRepository.findByAssetId(assetId));
            Ticket ticket;

            if (existingTicketOpt.isPresent()) {
                ticket = existingTicketOpt.get();
                updateTicket(ticket, severity, description, remediationText);
            } else {
                Ticket newTicket = new Ticket(ip, severity, description, remediationText);
                newTicket.setSiteName(siteName); // Assigner le siteName au ticket
                ticketRepository.save(newTicket);
                return newTicket;
            }

        } catch (IOException e) {
            LOGGER.severe("Erreur lors de la création du ticket avec les détails : " + e.getMessage());
            throw new RuntimeException("Erreur lors de la création du ticket avec les détails", e);
        }
        return null;
    }
    public List<Ticket> createTicketsForSite(String siteId) {
        List<Ticket> tickets = new ArrayList<>();
        List<String> assetIds = getAssetIdsForSite(siteId);

        // Vérifier si le site a déjà été traité
        if (processedSiteIds.contains(siteId)) {
            // Si oui, ne pas recréer les tickets pour ce site
            System.out.println("Tickets for siteId " + siteId + " have already been created.");
            return null;
        }

        // Récupérer la liste des sites à partir de l'API
        String sitesJson = getSites(); // Méthode qui récupère les données de l'API
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode rootNode = mapper.readTree(sitesJson);
            JsonNode sitesNode = rootNode.path("resources");

            String siteName = null;

            // Trouver le nom du site basé sur l'ID
            if (sitesNode.isArray()) {
                for (JsonNode siteNode : sitesNode) {
                    String currentSiteId = siteNode.path("id").asText();
                    if (siteId.equals(currentSiteId)) {
                        siteName = siteNode.path("name").asText(); // Récupérer le nom du site
                        break;
                    }
                }
            }

            // Vérifier si le nom du site a été trouvé
            if (siteName == null) {
                throw new RuntimeException("Site name not found for siteId: " + siteId);
            }

            // Vérifier si des tickets existent déjà pour ce site
            List<Ticket> existingTickets = ticketRepository.findBySiteName(siteName);
            if (!existingTickets.isEmpty()) {
                System.out.println("Tickets already exist for siteName " + siteName);
                return null; // Si des tickets existent déjà, ne pas en créer de nouveaux
            }

            // Créer les tickets pour chaque asset et chaque vulnérabilité
            for (String assetId : assetIds) {
                List<String> vulnerabilityIds = getVulnerabilityIdsForAsset(assetId);
                for (String vulnerabilityId : vulnerabilityIds) {
                    Ticket ticket = createTicketWithDetails(assetId, vulnerabilityId,siteName);
                    if (ticket != null) {
                        ticket.setSiteName(siteName); // Affecter le nom du site au ticket
                        ticket.addStatusChange(ticket.getStatus());
                        ticketRepository.save(ticket); // Sauvegarder le ticket
                        tickets.add(ticket);
                    }
                }
            }

            // Ajouter l'ID du site au Set des sites traités pour éviter la duplication
            processedSiteIds.add(siteId);

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Error processing site information", e);
        }

        return tickets;
    }

    private void updateTicket(Ticket ticket, String severity, String description, String remediation) {
        boolean updated = false;

        if (!ticket.getSeverity().equals(severity)) {
            ticket.setSeverity(severity);
            updated = true;
        }

        if (!ticket.getDescription().equals(description)) {
            ticket.setDescription(description);
            updated = true;
        }

        if (!ticket.getRemediation().equals(remediation)) {
            ticket.setRemediation(remediation);
            updated = true;
        }

        if (updated) {
            ticketRepository.save(ticket);
        }
    }
    private Set<String> processedSiteIds = new HashSet<>();
    public List<Ticket> generateTicketsForAllSites() {
        String sitesJson = getSites(); // Récupérer la liste des sites
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode rootNode = mapper.readTree(sitesJson);
            JsonNode sitesNode = rootNode.path("resources");

            if (sitesNode.isArray()) {
                for (JsonNode siteNode : sitesNode) {
                    String siteId = siteNode.path("id").asText();

                    // Vérifier si le site a déjà été traité
                    if (!processedSiteIds.contains(siteId)) {
                        // Créer des tickets pour le site
                        createTicketsForSite(siteId);

                        // Ajouter l'ID du site au Set des sites traités
                        processedSiteIds.add(siteId);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }



    public List<Map<String, Object>> getSiteInformation() {
        List<Map<String, Object>> siteInfos = new ArrayList<>();
        String sitesJson = getSites();

        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode rootNode = mapper.readTree(sitesJson);
            JsonNode resourcesNode = rootNode.path("resources");

            if (resourcesNode.isArray()) {
                for (JsonNode siteNode : resourcesNode) {
                    Map<String, Object> siteInfo = new HashMap<>();
                    siteInfo.put("assets", siteNode.path("assets").asInt());
                    siteInfo.put("description", siteNode.path("description").asText(null));
                    siteInfo.put("importance", siteNode.path("importance").asText(null));
                    siteInfo.put("lastScanTime", siteNode.path("lastScanTime").asText(null));
                    siteInfo.put("name", siteNode.path("name").asText(null));
                    siteInfos.add(siteInfo);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            // Log and handle the exception appropriately
        }

        return siteInfos;
    }

    public List<Ticket> getTicketsBySiteId(String siteId) {
        // Fetch assets for the site
        String assetsJson = getAssetsForSite(siteId);
        ObjectMapper mapper = new ObjectMapper();
        List<String> assetIds = new ArrayList<>();

        try {
            JsonNode rootNode = mapper.readTree(assetsJson);
            JsonNode resourcesNode = rootNode.path("resources");

            if (resourcesNode.isArray()) {
                for (JsonNode resource : resourcesNode) {
                    JsonNode addressesNode = resource.path("addresses");
                    if (addressesNode.isArray() && !addressesNode.isEmpty()) {
                        JsonNode addressNode = addressesNode.get(0); // Assuming there is at least one address
                        String ip = addressNode.path("ip").asText(null); // Default to null if not present
                        if (ip != null && !ip.isEmpty()) {
                            assetIds.add(ip);
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Fetch tickets based on asset IDs
        return ticketRepository.findTicketsByAssetIdIn(assetIds);
    }

    public String getSiteIdByName(String siteName) {
        String sitesJson = getSites(); // Récupère la liste des sites
        ObjectMapper mapper = new ObjectMapper();

        try {
            JsonNode rootNode = mapper.readTree(sitesJson);
            JsonNode sitesNode = rootNode.path("resources");

            if (sitesNode.isArray()) {
                for (JsonNode siteNode : sitesNode) {
                    if (siteNode.path("name").asText().equals(siteName)) {
                        return siteNode.path("id").asText(); // Retourne l'ID du site si le nom correspond
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null; // Retourne null si le site avec ce nom n'a pas été trouvé
    }

    public List<Ticket> getTicketsBySiteName(String siteName) { return ticketRepository.findBySiteName(siteName); }


    private HttpHeaders createHeaders(String username, String password) {
        String auth = username + ":" + password;
        byte[] encodedAuth = Base64.getEncoder().encode(auth.getBytes());
        String authHeader = "Basic " + new String(encodedAuth);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", authHeader);
        return headers;
    }

    public Ticket assignTicketToUser(Long ticketId, Long userId) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new RuntimeException("Ticket not found"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        ticket.setUser(user);
        return ticketRepository.save(ticket);



    }
    public List<User>getUsers(){
        return userRepository.findAll();
    }

    public List<Ticket> getTicketsByUserId(Long userId) {
        User user = userRepository.findUserById(userId);
        return ticketRepository.findTicketByUser(user);
    }
}
