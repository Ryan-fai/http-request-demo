package httprequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import java.io.*;
import java.util.Map;

public class AppTest {
  static final Logger logger = LogManager.getLogger(App.class);

  @Test
  public void successfulResponse() throws IOException {
    logger.info("=======Start=======");
    App app = new App();
    File event = new File("src/test/resources/request.json");
    InputStream inputStream = new FileInputStream(event);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    app.handleRequest(inputStream, outputStream, null);
    String output = outputStream.toString();
    Gson gson = new Gson();
    Map<String, Object> map = gson.fromJson(output, Map.class);

    assertEquals(200, Double.valueOf(map.get("statusCode").toString()).intValue());
    assertEquals("application/json", ((Map)map.get("headers")).get("Content-Type"));
    String content = map.get("body").toString();
    assertNotNull(content);
    logger.info("=======End=======");
//    assertTrue(content.contains("\"message\""));
//    assertTrue(content.contains("\"hello world\""));
//    assertTrue(content.contains("\"location\""));

  }
}
