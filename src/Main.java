/*
 * Use these commands to run the program:
 * javac -cp "lib/*" -d bin src/Main.java
 * java -cp "bin:lib/*" Main
 */
import io.github.cdimascio.dotenv.Dotenv;
import java.io.OutputStream;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class Main {
	public static void main(String[] args) throws Exception {
		Dotenv dotenv = Dotenv.load();

		String baseURL = "https://accounts.spotify.com/authorize";
		String clientId = URLEncoder.encode(dotenv.get("SPOTIFY_CLIENT_ID"), StandardCharsets.UTF_8);
		String redirectUri = URLEncoder.encode(dotenv.get("SPOTIFY_REDIRECT_URI"), StandardCharsets.UTF_8);
		String scopes = URLEncoder.encode("user-top-read user-read-recently-played", StandardCharsets.UTF_8);

		String fullURL = baseURL + "?client_id=" + clientId + "&response_type=code&redirect_uri=" + redirectUri + "&scope=" + scopes;

		CompletableFuture<String> resultFuture = new CompletableFuture<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 8888), 0);

        server.createContext("/callback", exchange -> {
            String query = exchange.getRequestURI().getQuery();
            String response = "Got it! You can close this tab.";
            exchange.sendResponseHeaders(200, response.length());
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(response.getBytes());
            }
            resultFuture.complete(query);
        });

        server.start();

        System.out.println("Full URL: " + fullURL);
        String capturedQuery = resultFuture.get();
        System.out.println("Captured query: " + capturedQuery);
		String code = null;
		if (capturedQuery != null) {
			for (String pair : capturedQuery.split("&")) {
				String[] parts = pair.split("=", 2);
				if (parts.length == 2 && parts[0].equals("code")) {
					code = parts[1];
					break;
				}
			}
		}
		System.out.println("Authorization code: " + code);
		
        server.stop(0);

		String apiKey = dotenv.get("SPOTIFY_CLIENT_ID");
		String apiSecret = dotenv.get("SPOTIFY_CLIENT_SECRET");

		String credentials = apiKey + ":" + apiSecret;
		String encodedAuth = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
		String formBody = "grant_type=authorization_code&code=" + code + "&redirect_uri=" + redirectUri;
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create("https://accounts.spotify.com/api/token"))
				.header("Authorization", "Basic " + encodedAuth)
				.header("Content-Type", "application/x-www-form-urlencoded")
				.POST(HttpRequest.BodyPublishers.ofString(formBody))
				.build();
		
		HttpClient client = HttpClient.newHttpClient();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		System.out.println("Status: " + response.statusCode());
		System.out.println("Body: " + response.body());
	}
}