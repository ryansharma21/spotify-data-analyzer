//import dotenv here: 
import io.github.cdimascio.dotenv.Dotenv;

public class Main {
	public static void main(String[] args) {
		try {
			//load my .env file using dotenv	
			Dotenv dotenv = Dotenv.load();
			//print out the values of my .env file to the console
			System.out.println("SPOTIFY_CLIENT_ID: " + dotenv.get("SPOTIFY_CLIENT_ID"));
			System.out.println("SPOTIFY_CLIENT_SECRET: " + dotenv.get("SPOTIFY_CLIENT_SECRET"));
			System.out.println("SPOTIFY_REDIRECT_URI: " + dotenv.get("SPOTIFY_REDIRECT_URI"));
			System.out.println("SPOTIFY_REFRESH_TOKEN: " + dotenv.get("SPOTIFY_REFRESH_TOKEN"));
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}