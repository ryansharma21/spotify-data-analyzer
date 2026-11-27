/*
 * Use these commands to run the program:
 * javac -cp "lib/*" -d bin src/Main.java
 * java -cp "bin:lib/*" Main
 */
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
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
import com.google.gson.JsonArray;
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
		//System.out.println("Status: " + response.statusCode());
		//System.out.println("Body: " + response.body());

		JsonObject jsonObject = JsonParser.parseString(response.body()).getAsJsonObject();
		String token = jsonObject.get("access_token").getAsString();
		
		getTopArtists(token);
		getTopTracks(token);
	}
	//help finish this method taking the code from the main method and basically putting it in here
	public static void getTopArtists(String token) throws Exception {
		HttpRequest userRequest = HttpRequest.newBuilder()
				.uri(URI.create("https://api.spotify.com/v1/me/top/artists?limit=20"))
				.header("Authorization", "Bearer " + token)
				.GET()
				.build();

		HttpClient userClient = HttpClient.newHttpClient();
		HttpResponse<String> userResponse = userClient.send(
				userRequest, HttpResponse.BodyHandlers.ofString());

		JsonObject topArtistsJson = JsonParser.parseString(userResponse.body())
				.getAsJsonObject();
		JsonArray itemsArray = topArtistsJson.getAsJsonArray("items");

		System.out.println("Your Top Artists:");
		for (int i = 0; i < itemsArray.size(); i++) {
			JsonObject artist = itemsArray.get(i).getAsJsonObject();
			String name = artist.get("name").getAsString();
			System.out.println((i + 1) + ". " + name);
		}
	}

	public static void getTopTracks(String token) throws Exception {
		HttpRequest userRequest = HttpRequest.newBuilder()
				.uri(URI.create("https://api.spotify.com/v1/me/top/tracks?limit=20"))
				.header("Authorization", "Bearer " + token)
				.GET()
				.build();
		HttpClient userClient = HttpClient.newHttpClient();
		HttpResponse<String> userResponse = userClient.send(
				userRequest, HttpResponse.BodyHandlers.ofString());

		JsonObject topTracksJson = JsonParser.parseString(userResponse.body())
				.getAsJsonObject();
		JsonArray itemsArray = topTracksJson.getAsJsonArray("items");

		//print all the keys that are in the arraything
		
		System.out.println("Your Top Tracks:");
		for (int i = 0; i < itemsArray.size(); i++) {
			JsonObject track = itemsArray.get(i).getAsJsonObject();
			String name = track.get("name").getAsString();

			JsonArray artists = track.getAsJsonArray("artists");
			String artistNames = "";
			for (int j = 0; j < artists.size(); j++) {
				JsonObject artist = artists.get(j).getAsJsonObject();
				artistNames += artist.get("name").getAsString();
				if (j != artists.size() - 1) {
					artistNames += ", ";
				}
			}
			
			long duration = track.get("duration_ms").getAsLong();
			long minutes = duration / 60000;
			long seconds = (duration % 60000) / 1000;

			String trackLength = ""; 
			trackLength += minutes + ":"; 
			if (seconds < 10) {
				trackLength += "0"; 
			}
			trackLength += seconds;
			System.out.println((i + 1) + ". " + name + " by " + artistNames + " (" + trackLength + ")");
			
		}
	}
}