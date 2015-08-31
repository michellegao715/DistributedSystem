package src;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;

import src.HTTPConstants.HTTPMethod;


public class HTTPRequestLineParserTest {
	
	@Test
	public void testValidParametersWithPoundSign() {
	    String s = "GET /restaurantmashup?city=a&pic=c&#frag1#frag2 HTTP/1.1";
	    HashMap<String,String> params = new HashMap<String,String>();
	    params.put("city", "a");
	    params.put("pic", "c");
	    HTTPRequestLine expected = new HTTPRequestLine(HTTPMethod.GET,"/restaurantmashup",params,"HTTP/1.1");  
	    try {
			HTTPRequestLine test = HTTPRequestLineParser.parse(s);
			assertTrue(test.equals(expected)); 
	    	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Test
	public void testValidWithNoParameters() {
	    String s = "POST /tweets HTTP/1.1";
	    HashMap<String,String> params = new HashMap<String,String>();
	    HTTPRequestLine expected = new HTTPRequestLine(HTTPMethod.POST,"/tweets",params,"HTTP/1.1");  
	    try {
			HTTPRequestLine test = HTTPRequestLineParser.parse(s);
			assertTrue(test.equals(expected));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Test
	public void testInvalidWithTooManyEqualsInParameter() {
	    String s = "GET /test?q=a=c&q=b HTTP/1.1";
	    try {
			HTTPRequestLine test = HTTPRequestLineParser.parse(s);
			Assert.assertNull(test);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Test
	public void testValidForAnotherHttpVersion() {
		String s="GET /tweets?q=searchterm&v=versionnum HTTP/1.0";   //valid
	    HashMap<String,String> params = new HashMap<String,String>();
	    params.put("q", "searchterm");
	    params.put("v", "versionnum");
	    HTTPRequestLine expected = new HTTPRequestLine(HTTPMethod.GET,"/tweets",params,"HTTP/1.0");  
	    try {
			HTTPRequestLine test = HTTPRequestLineParser.parse(s);
			assertTrue(expected.equals(test));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}	
	
	@Test
	public void testInvalidMethod() {
	    String s = "GETT /test?q=h HTTP/1.1";  
		try {
			HTTPRequestLine test=HTTPRequestLineParser.parse(s);
			Assert.assertNull(test);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}
	
	@Test
	public void testNotExistedMethod() {
	    String s = "BOB /test HTTP/1.1";
	    try {
			HTTPRequestLine test = HTTPRequestLineParser.parse(s);
			Assert.assertNull(test);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Test
	public void testInvalidForNoHttpVersion() {
	    String s = "GET /test";  
	    try {
			HTTPRequestLine test = HTTPRequestLineParser.parse(s);
			Assert.assertNull(test);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Test  //valid
	public void testForParameterWithPlace() {
	    String s = "GET /test?q=san%20francisco HTTP/1.1";  
	    HashMap<String,String> params = new HashMap<String,String>();
	    params.put("q", "san%20francisco");
	    HTTPRequestLine expected = new HTTPRequestLine(HTTPMethod.GET,"/test",params,"HTTP/1.1"); 

	    try {
			HTTPRequestLine test = HTTPRequestLineParser.parse(s);
			assertTrue(expected.equals(test));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Test
	public void testInvalidForWrongQuery() {
	    String s = "GET /restaurant?mashup?city& HTTP/1.1";
	    //invalid since wrong format of query "key=value"
	    try {
			HTTPRequestLine test = HTTPRequestLineParser.parse(s);
			Assert.assertNull(test);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Test
	public void testValid() {
		String s = "DELETE /twitter?a=1&b=2&c=3 HTTP/1.1";
		HashMap<String,String> params = new HashMap<String,String>();
	    params.put("a", "1");
	    params.put("b", "2");
	    params.put("c", "3");
	    HTTPRequestLine expected = new HTTPRequestLine(HTTPMethod.DELETE,"/twitter",params,"HTTP/1.1"); 
	    try {
			HTTPRequestLine test = HTTPRequestLineParser.parse(s);
			assertTrue(test.equals(expected));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
