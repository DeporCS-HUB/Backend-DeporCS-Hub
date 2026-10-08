package id.csui.depor;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.http.HttpMethod;
import static org.junit.jupiter.api.Assertions.*;
class SupabaseTest {
 HttpServer server;Supabase db;final String uid="00000000-0000-0000-0000-000000000001";
 AtomicReference<String> observed=new AtomicReference<>();
 @BeforeEach void start() throws Exception {server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);db=new Supabase("http://127.0.0.1:"+server.getAddress().getPort(),"test-anon-key");}
 void reply(String path,int code,String body){server.createContext(path,exchange->{observed.set(exchange.getRequestHeaders().getFirst("Authorization"));assertEquals("test-anon-key",exchange.getRequestHeaders().getFirst("apikey"));byte[] bytes=body.getBytes(StandardCharsets.UTF_8);exchange.getResponseHeaders().add("Content-Type","application/json");exchange.sendResponseHeaders(code,bytes.length);exchange.getResponseBody().write(bytes);exchange.close();});}
 @AfterEach void stop(){server.stop(0);}
 @Test void authenticatesRemotelyThenReadsTrustedRole(){reply("/auth/v1/user",200,"{\"id\":\""+uid+"\",\"user_metadata\":{\"role\":\"admin\"}}");reply("/rest/v1/profiles",200,"[{\"id\":\""+uid+"\",\"name\":\"Member\",\"role\":\"member\",\"active\":true}]");server.start();Principal p=db.authenticate("test-access-token");assertEquals("member",p.role);assertEquals("Bearer test-access-token",observed.get());}
 @Test void forgedTokenIsRejectedByAuth(){reply("/auth/v1/user",401,"{\"message\":\"bad secret details\"}");server.start();ApiException e=assertThrows(ApiException.class,()->db.authenticate("forged"));assertEquals(401,e.status);assertFalse(e.getMessage().contains("secret"));}
 @Test void inactiveProfileFailsClosed(){reply("/auth/v1/user",200,"{\"id\":\""+uid+"\"}");reply("/rest/v1/profiles",200,"[{\"active\":false,\"role\":\"admin\"}]");server.start();assertEquals(403,assertThrows(ApiException.class,()->db.authenticate("test-token")).status);}
 @Test void missingProfileFailsClosed(){reply("/auth/v1/user",200,"{\"id\":\""+uid+"\"}");reply("/rest/v1/profiles",200,"[]");server.start();assertEquals(403,assertThrows(ApiException.class,()->db.authenticate("test-token")).status);}
 @Test void foreignKeyConflictIsSanitized(){reply("/rest/v1/programs",409,"{\"code\":\"23503\",\"details\":\"private data\"}");server.start();ApiException e=assertThrows(ApiException.class,()->db.request(HttpMethod.DELETE,"/rest/v1/programs","test-token",null));assertEquals(409,e.status);assertFalse(e.getMessage().contains("private"));}
 @Test void schemaFailureIsNotEmptySuccess(){reply("/rest/v1/inventory",404,"{\"code\":\"PGRST205\"}");server.start();assertEquals(502,assertThrows(ApiException.class,()->db.request(HttpMethod.GET,"/rest/v1/inventory","test-token",null)).status);}
}
