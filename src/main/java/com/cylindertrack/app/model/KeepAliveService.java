@Service
public class KeepAliveService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String url = "https://cylinderservice.onrender.com/newhome";

    // Cron: 0 (sec) */10 (every 10 min) 9-18 (9 AM through 6:59 PM)
    @Scheduled(cron = "0 */10 9-18 * * *", zone = "America/Chicago")
    public void pingSelf() {
        try {
            // Using a simple head request to save bandwidth
            System.out.println("Keep-Alive: Pinging at " + LocalDateTime.now());
            restTemplate.execute(url, HttpMethod.GET, null, null);
        } catch (Exception e) {
            System.err.println("Keep-Alive failed: " + e.getMessage());
        }
    }
}