import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.json.simple.parser.JSONParser;

public class KeysJsonParser {
	
/*
Consider naming your configuration file something other than "keys".
*/
	
	public static final Path keys = Paths.get("keys.json");
	//public static final Path keys = Paths.get("keys.json");
	public static  final Charset charset = Charset.forName("US-ASCII");

	public static String getText(String jsonText, String keyword) {
		String value = "";
		JSONParser parser = new JSONParser();
		KeyFinder finder = new KeyFinder();
		finder.setMatchKey(keyword);
		try {
			while(!finder.isEnd()){
				parser.parse(jsonText, finder, true);
				if(finder.isFound()){
					finder.setFound(false);
					value = finder.getValue().toString();
				}
			}
		}catch (org.json.simple.parser.ParseException e) {
			e.printStackTrace();
		}
		return value;
	}

	public static String jsonFileTojsonText(Path jsonfile){
		String jsonText = "";
		StringBuilder jsontext = new StringBuilder();
		try (BufferedReader reader = Files.newBufferedReader(keys, charset)) {
			String line = null;
			while ((line = reader.readLine()) != null) {
				jsontext.append(line);
			}
			jsonText = jsontext.toString();
			//System.out.println("Print out the json read: "+jsonText);
		} catch (IOException x) {
			System.err.format("IOException: %s%n", x);
		}
		return jsonText;
	}
}
