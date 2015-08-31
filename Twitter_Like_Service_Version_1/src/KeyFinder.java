// Reference: https://code.google.com/p/json-simple/wiki/DecodingExamples
import java.io.IOException;

import org.json.simple.parser.ContentHandler;


class KeyFinder implements ContentHandler{
	private Object value;
	private boolean found = false;
	private boolean end = false;
	private String key;
	private String matchKey;

	public void setMatchKey(String matchKey){
		this.matchKey = matchKey;
	}

	public Object getValue(){
		return value;
	}

	public boolean isEnd(){
		return end;
	}

	public void setFound(boolean found){
		this.found = found;
	}

	public boolean isFound(){
		return found;
	}

	@Override
	public void startJSON() throws IOException {
		found = false;
		end = false;
	}

	@Override
	public void endJSON() throws IOException {
		end = true;
	}

	@Override
	public boolean primitive(Object value) throws  IOException {
		if(key != null){
			if(key.equals(matchKey)){
				found = true;
				this.value = value;
				key = null;
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean startArray() throws IOException {
		return true;
	}


	@Override
	public boolean startObject() throws  IOException {
		return true;
	}

	@Override
	public boolean startObjectEntry(String key) throws IOException {
		this.key = key;
		return true;
	}

	@Override
	public boolean endArray() throws  IOException {
		return false;
	}

	@Override
	public boolean endObject() throws  IOException {
		return true;
	}

	@Override
	public boolean endObjectEntry() throws  IOException {
		return true;
	}
}
