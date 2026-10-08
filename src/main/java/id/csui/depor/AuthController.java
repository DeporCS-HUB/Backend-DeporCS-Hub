package id.csui.depor;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {
 record Login(@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(max=1024) String password) {}
 private final Supabase db;private final boolean secure;private final String sameSite;
 AuthController(Supabase db,@Value("${app.cookie-secure}") boolean secure,@Value("${app.cookie-same-site}") String sameSite) {
  this.db=db;this.secure=secure;this.sameSite=sameSite;
  if(!Set.of("Lax","Strict","None").contains(sameSite) || (sameSite.equals("None")&&!secure)) throw new IllegalArgumentException("Invalid cookie configuration");
 }
 private String cookie(String refresh,long age) {return ResponseCookie.from("depor_refresh",refresh).httpOnly(true).secure(secure).sameSite(sameSite).path("/api/auth").maxAge(age).build().toString();}
 private ResponseEntity<?> session(JsonNode result) {
  String token=result.path("access_token").asText();String refresh=result.path("refresh_token").asText();
  if(token.isBlank()||refresh.isBlank()) throw new ApiException(502,"AUTH_UNAVAILABLE","Respons autentikasi tidak valid.");
  try {
   Principal p=db.authenticate(token);
   return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,cookie(refresh,604800)).body(Map.of("data",Map.of("accessToken",token,"expiresIn",result.path("expires_in").asLong(),"user",p.profile())));
  } catch(ApiException e) {return ResponseEntity.status(e.status).header(HttpHeaders.SET_COOKIE,cookie("",0)).body(ApiErrors.body(e.code,e.getMessage()));}
 }
 @PostMapping("/login") ResponseEntity<?> login(@Valid @RequestBody Login input) {
  return session(db.request(HttpMethod.POST,"/auth/v1/token?grant_type=password",null,Map.of("email",input.email(),"password",input.password())));
 }
 @PostMapping("/refresh") ResponseEntity<?> refresh(@CookieValue(value="depor_refresh",defaultValue="") String refresh) {
  if(refresh.isBlank()) return ResponseEntity.status(401).header(HttpHeaders.SET_COOKIE,cookie("",0)).body(ApiErrors.body("UNAUTHENTICATED","Session berakhir."));
  try {return session(db.request(HttpMethod.POST,"/auth/v1/token?grant_type=refresh_token",null,Map.of("refresh_token",refresh)));}
  catch(ApiException e) {return ResponseEntity.status(e.status).header(HttpHeaders.SET_COOKIE,cookie("",0)).body(ApiErrors.body(e.code,e.getMessage()));}
 }
 @GetMapping("/session") Map<String,Object> me(@AuthenticationPrincipal Principal p) {return Map.of("data",p.profile());}
 @PostMapping("/logout") ResponseEntity<?> logout(@AuthenticationPrincipal Principal p) {
  try {db.request(HttpMethod.POST,"/auth/v1/logout?scope=local",p.token,null);return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE,cookie("",0)).body(Map.of("data",Map.of("loggedOut",true)));}
  catch(ApiException e) {return ResponseEntity.status(e.status).header(HttpHeaders.SET_COOKIE,cookie("",0)).body(ApiErrors.body(e.code,e.getMessage()));}
 }
}
