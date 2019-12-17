package httprequest;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.google.gson.Gson;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

//import com.amazonaws.services.lambda.runtime.RequestHandler;

/**
 * Handler for requests to Lambda function.
 */
public class App implements RequestStreamHandler {
    // Initialize the Log4j logger.
    static final Logger logger = LogManager.getLogger(App.class);

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        Reader reader = new InputStreamReader(inputStream, "UTF-8");
        Gson gson = new Gson();
        Map<String, Object> map = gson.fromJson(reader, Map.class);
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("X-Custom-Header", "application/json");

        try (CloseableHttpClient client = createAcceptSelfSignedCertificateClient()) {
            URIBuilder uriBuilder = new URIBuilder(map.get("url").toString());
            //Set parameter
            if (map.get("parameters") != null){
                for (Map.Entry<String, String> entry: ((Map<String, String>)map.get("parameters")).entrySet()){
                    uriBuilder.setParameter(entry.getKey(), entry.getValue());
                }
            }

            HttpGet request = new HttpGet(uriBuilder.build());
            //Set header
            if (map.get("headers") != null){
                for (Map.Entry<String, String> entry: ((Map<String, String>)map.get("headers")).entrySet()){
                    request.setHeader(entry.getKey(), entry.getValue());
                }
            }

            HttpResponse response = client.execute(request);

            BufferedReader bufReader = new BufferedReader(new InputStreamReader(
                    response.getEntity().getContent()));

            StringBuilder builder = new StringBuilder();

            String line;

            while ((line = bufReader.readLine()) != null) {
                builder.append(line);
                builder.append(System.lineSeparator());
            }
            logger.info("Response: {}", builder.toString());
            outputStream.write(gson.toJson(new GatewayResponse(builder.toString(), headers, 200)).getBytes());
        } catch (URISyntaxException | KeyManagementException | NoSuchAlgorithmException | KeyStoreException e) {
            logger.error(e.getMessage(), e);
            outputStream.write(gson.toJson(new GatewayResponse(e.getMessage(), headers, 500)).getBytes());
        } catch (IOException e) {
            //return new GatewayResponse("{}", headers, 500);
            logger.error(e.getMessage(), e);
            outputStream.write(gson.toJson(new GatewayResponse("{}", headers, 500)).getBytes());
        }
    }

    private CloseableHttpClient createAcceptSelfSignedCertificateClient()
            throws KeyManagementException, NoSuchAlgorithmException, KeyStoreException {

        // use the TrustSelfSignedStrategy to allow Self Signed Certificates
        SSLContext sslContext = SSLContextBuilder
                .create()
                .loadTrustMaterial(new TrustSelfSignedStrategy())
                .build();

        // we can optionally disable hostname verification.
        // if you don't want to further weaken the security, you don't have to include this.
        HostnameVerifier allowAllHosts = new NoopHostnameVerifier();

        // create an SSL Socket Factory to use the SSLContext with the trust self signed certificate strategy
        // and allow all hosts verifier.
        SSLConnectionSocketFactory connectionFactory = new SSLConnectionSocketFactory(sslContext, allowAllHosts);

        // finally create the HttpClient using HttpClient factory methods and assign the ssl socket factory
        return HttpClients
                .custom()
                .setSSLSocketFactory(connectionFactory)
                .build();
    }
}
