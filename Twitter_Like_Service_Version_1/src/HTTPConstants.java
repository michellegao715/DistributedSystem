
public class HTTPConstants {

	public enum HTTPMethod {
		GET, POST;
		
		@Override
		public String toString(){
	        switch(this){
	        case GET:
	            return "GET";
	        case POST:
	            return "POST";
	        }
	        return null;
	    }
	};
}
