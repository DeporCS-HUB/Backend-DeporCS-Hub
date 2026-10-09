package id.csui.depor;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.http.*;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.*;
@Component
public class Supabase {
 private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger(Supabase.class);
 private final RestClient client;
 public Supabase(String url,String key) {this(url,key,5000,10000);}
 @org.springframework.beans.factory.annotation.Autowired
 public Supabase(@Value("${app.supabase-url}") String url,@Value("${app.supabase-anon-key}") String key,
                 @Value("${app.supabase-connect-timeout-ms:5000}") int connectTimeoutMs,
                 @Value("${app.supabase-read-timeout-ms:10000}") int readTimeoutMs) {
  if(url.isBlank() || key.isBlank()) throw new IllegalArgumentException("Supabase environment is required");
  if(connectTimeoutMs<1 || connectTimeoutMs>120000 || readTimeoutMs<1 || readTimeoutMs>120000) throw new IllegalArgumentException("Supabase timeouts must be 1–120000 milliseconds");
  var httpClient=java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectTimeoutMs)).build();
  var factory=new JdkClientHttpRequestFactory(httpClient);factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
  client=RestClient.builder().baseUrl(url).defaultHeader("apikey",key).requestFactory(factory).build();
 }
 public JsonNode request(HttpMethod method,String path,String token,Object body) {
  try {
   var req=client.method(method).uri(path).contentType(MediaType.APPLICATION_JSON).header("Prefer","return=representation");
   if(token!=null) req.header("Authorization","Bearer "+token);
   if(body!=null) req.body(body);
   JsonNode result=req.retrieve().body(JsonNode.class);
   return result==null?com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.nullNode():result;
  } catch(RestClientResponseException e) {
   int s=e.getStatusCode().value();String detail=e.getResponseBodyAsString();
   boolean invalidJwt=false;
   if(s==403 && path.startsWith("/auth/")) {
    try {JsonNode error=new com.fasterxml.jackson.databind.ObjectMapper().readTree(detail);invalidJwt=error!=null && "bad_jwt".equals(error.path("error_code").asText());}
    catch(com.fasterxml.jackson.core.JsonProcessingException ignored) { /* Preserve ordinary forbidden responses. */ }
   }
   if(s==401 || invalidJwt || (path.startsWith("/auth/") && (s==400 || s==422))) throw new ApiException(401,"UNAUTHENTICATED","Login atau session tidak valid.");
   if(s==403) throw new ApiException(403,"FORBIDDEN","Anda tidak memiliki hak untuk operasi ini.");
   if(s==429) throw new ApiException(429,"RATE_LIMITED","Terlalu banyak percobaan. Coba kembali nanti.");
   if(detail.contains("23505") || (detail.contains("23503") || detail.contains("23001"))) throw new ApiException(409,"CONFLICT","Data duplikat atau masih digunakan oleh data lain.");
   if(detail.contains("42501")) throw new ApiException(403,"FORBIDDEN","Anda tidak memiliki hak untuk operasi ini.");
   if(detail.contains("23514") || detail.contains("22P02")) throw new ApiException(400,"VALIDATION_ERROR","Data tidak sesuai constraint database.");
   throw new ApiException(502,"DATABASE_UNAVAILABLE","Layanan data belum siap atau sedang tidak tersedia.");
  } catch(ResourceAccessException e) {LOG.debug("Supabase transport failure: {}",e.getMostSpecificCause().getClass().getSimpleName());throw new ApiException(503,"SUPABASE_UNAVAILABLE","Supabase tidak dapat dihubungi.");}
 }
 public Principal authenticate(String token) {
  JsonNode user=request(HttpMethod.GET,"/auth/v1/user",token,null);
  UUID id;
  try { id=UUID.fromString(user.path("id").asText()); } catch(IllegalArgumentException e) { throw new ApiException(401,"UNAUTHENTICATED","Token tidak valid."); }
  JsonNode rows=request(HttpMethod.GET,"/rest/v1/profiles?id=eq."+id+"&select=id,name,role,active",token,null);
  if(!rows.isArray() || rows.size()!=1 || !rows.get(0).path("active").asBoolean()) throw new ApiException(403,"INACTIVE_PROFILE","Profil belum aktif. Hubungi BPH.");
  JsonNode profile=rows.get(0);String role=profile.path("role").asText();
  if(!Set.of("member","staff","admin").contains(role)) throw new ApiException(403,"FORBIDDEN","Role tidak valid.");
  return new Principal(id,role,profile.path("name").asText(),token);
 }
}
