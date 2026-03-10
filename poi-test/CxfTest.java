import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class CxfTest {
    public static void main(String[] args) throws Exception {
        String urlExact = "https://services.nvd.nist.gov/rest/json/cves/2.0?cpeName=cpe:2.3:a:apache:cxf:4.1.1:*:*:*:*:*:*:*";
        String urlKeyword = "https://services.nvd.nist.gov/rest/json/cves/2.0?keywordSearch=apache%20cxf%204.1.1";
        String apiKey = "YOUR_API_KEY";

        HttpClient client = HttpClient.newHttpClient();
        
        System.out.println("---- EXACT CPE ----");
        HttpRequest req1 = HttpRequest.newBuilder()
            .uri(URI.create(urlExact))
            .header("apiKey", apiKey)
            .GET().build();
        HttpResponse<String> res1 = client.send(req1, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status: " + res1.statusCode());
        System.out.println(res1.body());

        System.out.println("\n---- KEYWORD SEARCH ----");
        HttpRequest req2 = HttpRequest.newBuilder()
            .uri(URI.create(urlKeyword))
            .header("apiKey", apiKey)
            .GET().build();
        HttpResponse<String> res2 = client.send(req2, HttpResponse.BodyHandlers.ofString());
        System.out.println("Status: " + res2.statusCode());
        System.out.println(res2.body());
    }
}
