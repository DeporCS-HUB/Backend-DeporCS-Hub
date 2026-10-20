package id.csui.depor;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"app.supabase-url=http://localhost:54321","app.supabase-anon-key=test-key","app.allowed-origins=http://localhost:3000","app.cookie-secure=false"})
@AutoConfigureMockMvc
class AccountApiTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @MockitoBean Supabase db;
 final UUID uid=UUID.fromString("00000000-0000-0000-0000-000000000001");
 final String old=uid+"/00000000-0000-0000-0000-000000000002.jpg";
 @BeforeEach void auth(){when(db.authenticate("user-token")).thenReturn(new Principal(uid,"member","User","user-token",old));}
 String jpeg(int w,int h)throws Exception{var out=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB),"jpg",out);return Base64.getEncoder().encodeToString(out.toByteArray());}
 void freshLogin()throws Exception{
  when(db.request(eq(HttpMethod.GET),eq("/auth/v1/user"),eq("user-token"),isNull())).thenReturn(mapper.readTree("{\"id\":\""+uid+"\",\"email\":\"owner@example.invalid\"}"));
  when(db.request(eq(HttpMethod.POST),eq("/auth/v1/token?grant_type=password"),isNull(),any())).thenReturn(mapper.readTree("{\"access_token\":\"fresh-token\",\"refresh_token\":\"fresh-refresh\",\"expires_in\":3600,\"user\":{\"id\":\""+uid+"\"}}"));
 }
 @Test void passwordChangeRequiresTrustedOriginAndCurrentSession()throws Exception{
  mvc.perform(post("/api/auth/password").header("Origin","https://other.invalid").header("Authorization","Bearer user-token").contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"old-test\",\"newPassword\":\"new-test-value\"}")).andExpect(status().isForbidden());
  mvc.perform(post("/api/auth/password").header("Origin","http://localhost:3000").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isUnauthorized());
  verify(db,never()).request(any(),any(),any(),any());
 }
 @Test void passwordChangesOnlyTheVerifiedUserAndRotatesRefreshCookie()throws Exception{
  freshLogin();when(db.request(eq(HttpMethod.PUT),eq("/auth/v1/user"),eq("fresh-token"),any())).thenReturn(mapper.readTree("{\"id\":\""+uid+"\"}"));
  mvc.perform(post("/api/auth/password").header("Origin","http://localhost:3000").header("Authorization","Bearer user-token").contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"old-test-value\",\"newPassword\":\"new-test-value\"}")).andExpect(status().isOk()).andExpect(header().string("Set-Cookie",org.hamcrest.Matchers.containsString("HttpOnly"))).andExpect(jsonPath("$.data.accessToken").value("fresh-token")).andExpect(jsonPath("$.data.passwordChanged").value(true)).andExpect(jsonPath("$.data.refresh_token").doesNotExist());
  verify(db).request(HttpMethod.POST,"/auth/v1/token?grant_type=password",null,Map.of("email","owner@example.invalid","password","old-test-value"));
  verify(db).request(HttpMethod.PUT,"/auth/v1/user","fresh-token",Map.of("password","new-test-value","current_password","old-test-value"));
 }
 @Test void wrongCurrentPasswordDoesNotLogOutTheBrowserOrChangePassword()throws Exception{
  freshLogin();when(db.request(eq(HttpMethod.POST),eq("/auth/v1/token?grant_type=password"),isNull(),any())).thenThrow(new ApiException(401,"UNAUTHENTICATED","Login invalid"));
  mvc.perform(post("/api/auth/password").header("Origin","http://localhost:3000").header("Authorization","Bearer user-token").contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"wrong-test-value\",\"newPassword\":\"new-test-value\"}")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("PASSWORD_MISMATCH")).andExpect(header().doesNotExist("Set-Cookie"));
  verify(db,never()).request(eq(HttpMethod.PUT),any(),any(),any());
 }
 @Test void passwordPolicyFailureCleansOnlyTemporaryVerificationSession()throws Exception{
  freshLogin();when(db.request(eq(HttpMethod.PUT),eq("/auth/v1/user"),eq("fresh-token"),any())).thenThrow(new ApiException(400,"PASSWORD_POLICY","Rejected"));
  mvc.perform(post("/api/auth/password").header("Origin","http://localhost:3000").header("Authorization","Bearer user-token").contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"old-test-value\",\"newPassword\":\"new-test-value\"}")).andExpect(status().isBadRequest());
  verify(db).request(HttpMethod.POST,"/auth/v1/logout?scope=local","fresh-token",null);
 }
 @Test void passwordRejectsIdentityInputShortAndSameValues()throws Exception{
  for(String body:List.of("{\"currentPassword\":\"old-test-value\",\"newPassword\":\"short\"}","{\"currentPassword\":\"old-test-value\",\"newPassword\":\"old-test-value\"}","{\"currentPassword\":\"old-test-value\",\"newPassword\":\"new-test-value\",\"userId\":\""+uid+"\"}"))mvc.perform(post("/api/auth/password").header("Origin","http://localhost:3000").header("Authorization","Bearer user-token").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
  verify(db,never()).request(any(),any(),any(),any());
 }
 @Test void passwordVerificationForDifferentUserCannotUpdateThatAccount()throws Exception{
  freshLogin();when(db.request(eq(HttpMethod.POST),eq("/auth/v1/token?grant_type=password"),isNull(),any())).thenReturn(mapper.readTree("{\"access_token\":\"mismatch-token\",\"refresh_token\":\"test-refresh\",\"user\":{\"id\":\"00000000-0000-0000-0000-000000000099\"}}"));
  mvc.perform(post("/api/auth/password").header("Origin","http://localhost:3000").header("Authorization","Bearer user-token").contentType(MediaType.APPLICATION_JSON).content("{\"currentPassword\":\"old-test-value\",\"newPassword\":\"new-test-value\"}")).andExpect(status().isBadGateway());
  verify(db,never()).request(eq(HttpMethod.PUT),any(),any(),any());verify(db).request(HttpMethod.POST,"/auth/v1/logout?scope=local","mismatch-token",null);
 }
 @Test void avatarCannotSignAPathFromAnotherUserEvenForBph()throws Exception{
  when(db.authenticate("user-token")).thenReturn(new Principal(uid,"staff","BPH","user-token","00000000-0000-0000-0000-000000000099/00000000-0000-0000-0000-000000000002.jpg"));
  mvc.perform(get("/api/profiles/me/avatar").header("Authorization","Bearer user-token")).andExpect(status().isNotFound());verify(db,never()).signedAvatar(any(),any());
 }
 @Test void avatarUploadDecodesJpegAndUsesOwnGeneratedPathBeforeCleanup()throws Exception{
  when(db.request(eq(HttpMethod.PATCH),contains("/rest/v1/profiles?id=eq."),eq("user-token"),any())).thenReturn(mapper.readTree("[{\"id\":\""+uid+"\"}]"));
  mvc.perform(put("/api/profiles/me/avatar").header("Authorization","Bearer user-token").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("image",jpeg(20,20))))).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(uid.toString())).andExpect(jsonPath("$.data.avatar_path").value(org.hamcrest.Matchers.startsWith(uid+"/")));
  var bytes=org.mockito.ArgumentCaptor.forClass(byte[].class);var path=org.mockito.ArgumentCaptor.forClass(String.class);
  var ordered=inOrder(db);ordered.verify(db).uploadAvatar(path.capture(),eq("user-token"),bytes.capture());ordered.verify(db).request(eq(HttpMethod.PATCH),contains("&avatar_path=eq."),eq("user-token"),any());ordered.verify(db).request(HttpMethod.DELETE,"/storage/v1/object/depor-avatars","user-token",Map.of("prefixes",List.of(old)));
  assertNotEquals(old,path.getValue());assertEquals(20,ImageIO.read(new java.io.ByteArrayInputStream(bytes.getValue())).getWidth());
 }
 @Test void avatarRejectsNonImageOversizedDimensionsAndSpoofedPath()throws Exception{
  for(String body:List.of(mapper.writeValueAsString(Map.of("image",Base64.getEncoder().encodeToString("<svg/>".getBytes()))),mapper.writeValueAsString(Map.of("image",jpeg(1025,1))),"{\"image\":\"%%%\"}",mapper.writeValueAsString(Map.of("image",jpeg(1,1),"path","other/user.jpg"))))mvc.perform(put("/api/profiles/me/avatar").header("Authorization","Bearer user-token").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
  verify(db,never()).uploadAvatar(any(),any(),any());
 }
 @Test void avatarFailedPointerDoesNotDeletePreviousPhotoOrPretendSuccess()throws Exception{
  when(db.request(eq(HttpMethod.PATCH),any(),eq("user-token"),any())).thenReturn(mapper.readTree("[]"));
  mvc.perform(put("/api/profiles/me/avatar").header("Authorization","Bearer user-token").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("image",jpeg(2,2))))).andExpect(status().isConflict());
  verify(db,never()).request(HttpMethod.DELETE,"/storage/v1/object/depor-avatars","user-token",Map.of("prefixes",List.of(old)));
 }
 @Test void avatarReadUsesOwnPathAndRemoveClearsPointerBeforeStorage()throws Exception{
  when(db.signedAvatar(old,"user-token")).thenReturn("https://storage.example.invalid/signed");
  mvc.perform(get("/api/profiles/me/avatar").header("Authorization","Bearer user-token")).andExpect(status().isOk()).andExpect(jsonPath("$.data.url").value("https://storage.example.invalid/signed"));
  when(db.request(eq(HttpMethod.PATCH),any(),eq("user-token"),any())).thenReturn(mapper.readTree("[{\"id\":\""+uid+"\"}]"));
  mvc.perform(delete("/api/profiles/me/avatar").header("Authorization","Bearer user-token")).andExpect(status().isOk()).andExpect(jsonPath("$.data.avatar_path").doesNotExist());
  verify(db).request(HttpMethod.DELETE,"/storage/v1/object/depor-avatars","user-token",Map.of("prefixes",List.of(old)));
 }
 @Test void ownThumbnailIsSmallPrivateAndUsesOnlyCurrentPointer()throws Exception{
  when(db.downloadAvatar(old,"user-token")).thenReturn(Base64.getDecoder().decode(jpeg(512,512)));
  var result=mvc.perform(get("/api/profiles/me/avatar").param("size","96").param("version","untrusted-cache-key").header("Authorization","Bearer user-token"))
   .andExpect(status().isOk()).andExpect(header().string("Cache-Control",org.hamcrest.Matchers.containsString("no-store"))).andReturn();
  String url=mapper.readTree(result.getResponse().getContentAsString()).path("data").path("url").asText();
  assertTrue(url.startsWith("data:image/jpeg;base64,"));
  var image=ImageIO.read(new java.io.ByteArrayInputStream(Base64.getDecoder().decode(url.substring(url.indexOf(',')+1))));
  assertEquals(96,image.getWidth());assertEquals(96,image.getHeight());assertTrue(url.length()<5000);
  verify(db).downloadAvatar(old,"user-token");verify(db,never()).signedAvatar(any(),any());
 }
 @Test void thumbnailRejectsSizeWithoutDownloadingAndNeverReadsForeignPointer()throws Exception{
  mvc.perform(get("/api/profiles/me/avatar").param("size","4096").header("Authorization","Bearer user-token")).andExpect(status().isBadRequest());
  when(db.authenticate("user-token")).thenReturn(new Principal(uid,"staff","BPH","user-token","00000000-0000-0000-0000-000000000099/00000000-0000-0000-0000-000000000002.jpg"));
  mvc.perform(get("/api/profiles/me/avatar").param("size","96").header("Authorization","Bearer user-token")).andExpect(status().isNotFound());
  verify(db,never()).downloadAvatar(any(),any());
 }
 @Test void thumbnailFailureDoesNotMutatePhotoOrProfile()throws Exception{
  when(db.downloadAvatar(old,"user-token")).thenThrow(new ApiException(503,"STORAGE_UNAVAILABLE","Foto belum dapat dimuat."));
  mvc.perform(get("/api/profiles/me/avatar").param("size","96").header("Authorization","Bearer user-token")).andExpect(status().isServiceUnavailable());
  verify(db,never()).request(any(),any(),any(),any());verify(db,never()).uploadAvatar(any(),any(),any());
 }
 @Test void thumbnailsBoundDecodeDimensionsAndDoNotUpscale()throws Exception{
  assertThrows(ApiException.class,()->AvatarController.thumbnail(Base64.getDecoder().decode(jpeg(1025,1)),96));
  assertThrows(ApiException.class,()->AvatarController.thumbnail(new byte[]{1,2,3},96));
  String url=AvatarController.thumbnail(Base64.getDecoder().decode(jpeg(20,10)),96);
  var image=ImageIO.read(new java.io.ByteArrayInputStream(Base64.getDecoder().decode(url.substring(url.indexOf(',')+1))));assertEquals(10,image.getWidth());assertEquals(10,image.getHeight());
 }
}
