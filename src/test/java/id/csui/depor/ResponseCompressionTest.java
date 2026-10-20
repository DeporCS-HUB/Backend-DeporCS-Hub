package id.csui.depor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.util.*;
import java.util.zip.GZIPInputStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"app.supabase-url=http://localhost:54321","app.supabase-anon-key=test-key","app.allowed-origins=http://localhost:3000","app.cookie-secure=false"})
class ResponseCompressionTest {
 @LocalServerPort int port; @MockitoBean Supabase db; @Autowired ObjectMapper mapper;
 @Test void largeAuthenticatedJsonIsCompressedButNeverPubliclyCached()throws Exception {
  when(db.authenticate("fixture-token")).thenReturn(new Principal(UUID.fromString("00000000-0000-0000-0000-000000000001"),"member","PJ","fixture-token"));
  var node=mapper.createObjectNode().put("fixture","a".repeat(6000));
  when(db.request(eq(org.springframework.http.HttpMethod.POST),eq("/rest/v1/rpc/dashboard_overview"),eq("fixture-token"),any())).thenReturn(node);
  var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/dashboard/overview")).header("Authorization","Bearer fixture-token").header("Accept-Encoding","gzip").build();
  var response=HttpClient.newHttpClient().send(request,HttpResponse.BodyHandlers.ofByteArray());
  assertEquals(200,response.statusCode());assertEquals("gzip",response.headers().firstValue("Content-Encoding").orElse(""));assertTrue(response.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
  try(var decoded=new GZIPInputStream(new ByteArrayInputStream(response.body()))){assertEquals(6000,mapper.readTree(decoded).path("data").path("fixture").asText().length());}
  assertTrue(response.body().length<1000);
 }
}
