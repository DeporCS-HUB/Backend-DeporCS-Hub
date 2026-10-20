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
 private final RestClient client;private final String baseUrl;
 public Supabase(String url,String key) {this(url,key,5000,10000);}
 @org.springframework.beans.factory.annotation.Autowired
 public Supabase(@Value("${app.supabase-url}") String url,@Value("${app.supabase-anon-key}") String key,
                 @Value("${app.supabase-connect-timeout-ms:5000}") int connectTimeoutMs,
                 @Value("${app.supabase-read-timeout-ms:10000}") int readTimeoutMs) {
  if(url.isBlank() || key.isBlank()) throw new IllegalArgumentException("Supabase environment is required");
  if(connectTimeoutMs<1 || connectTimeoutMs>120000 || readTimeoutMs<1 || readTimeoutMs>120000) throw new IllegalArgumentException("Supabase timeouts must be 1–120000 milliseconds");
  var httpClient=java.net.http.HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectTimeoutMs)).build();
  var factory=new JdkClientHttpRequestFactory(httpClient);factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
  baseUrl=url.replaceAll("/+$","");
  client=RestClient.builder().baseUrl(url).defaultHeader("apikey",key).requestFactory(factory).build();
 }
 public JsonNode uploadAvatar(String path,String token,byte[] image) {return request(HttpMethod.POST,"/storage/v1/object/depor-avatars/"+path,token,image,MediaType.IMAGE_JPEG);}
 public String signedAvatar(String path,String token) {
  JsonNode result=request(HttpMethod.POST,"/storage/v1/object/sign/depor-avatars/"+path,token,Map.of("expiresIn",900));
  String signed=result.path("signedURL").asText();
  if(!signed.startsWith("/object/sign/depor-avatars/"+path+"?token="))throw new ApiException(502,"STORAGE_UNAVAILABLE","Foto profil belum dapat dimuat.");
  return baseUrl+"/storage/v1"+signed;
 }
 // Batch signing keeps directory/assignment rendering to one Storage request per 100 photos.
 public Map<String,String> signedAvatars(List<String> paths,String token) {
  if(paths.isEmpty())return Map.of();
  JsonNode rows=request(HttpMethod.POST,"/storage/v1/object/sign/depor-avatars",token,Map.of("expiresIn",900,"paths",paths));
  if(!rows.isArray())throw new ApiException(502,"STORAGE_UNAVAILABLE","Foto tim belum dapat dimuat.");
  var urls=new LinkedHashMap<String,String>();
  for(JsonNode row:rows) {
   String path=row.path("path").asText(),signed=row.path("signedURL").asText();
   if(paths.contains(path)&&(row.path("error").isNull()||row.path("error").isMissingNode())&&signed.startsWith("/object/sign/depor-avatars/"+path+"?token="))urls.put(path,baseUrl+"/storage/v1"+signed);
  }
  return urls;
 }
 public JsonNode request(HttpMethod method,String path,String token,Object body) {return request(method,path,token,body,MediaType.APPLICATION_JSON);}
 private JsonNode request(HttpMethod method,String path,String token,Object body,MediaType contentType) {
  try {
   var req=client.method(method).uri(java.net.URI.create(baseUrl+path)).contentType(contentType).header("Prefer","return=representation");
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
   if(path.equals("/auth/v1/user")&&method==HttpMethod.PUT&&(s==400||s==422)) {
    String code="";try{code=new com.fasterxml.jackson.databind.ObjectMapper().readTree(detail).path("error_code").asText();}catch(Exception ignored){}
    if(code.equals("reauthentication_needed")||code.equals("reauthentication_not_valid"))throw new ApiException(400,"REAUTH_REQUIRED","Silakan login ulang sebelum mengganti password.");
    if(code.equals("current_password_mismatch")||code.equals("current_password_required"))throw new ApiException(400,"PASSWORD_MISMATCH","Password saat ini tidak sesuai.");
    throw new ApiException(400,"PASSWORD_POLICY","Password baru belum memenuhi ketentuan akun atau sama dengan password sebelumnya.");
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
  JsonNode rows=request(HttpMethod.GET,"/rest/v1/profiles?id=eq."+id+"&select=id,name,role,active,avatar_path",token,null);
  if(!rows.isArray() || rows.size()!=1 || !rows.get(0).path("active").asBoolean()) throw new ApiException(403,"INACTIVE_PROFILE","Profil belum aktif. Hubungi BPH.");
  JsonNode profile=rows.get(0);String role=profile.path("role").asText();
  if(!Set.of("member","staff","admin").contains(role)) throw new ApiException(403,"FORBIDDEN","Role tidak valid.");
  return new Principal(id,role,profile.path("name").asText(),token,profile.path("avatar_path").isTextual()?profile.path("avatar_path").asText():null);
 }
}
