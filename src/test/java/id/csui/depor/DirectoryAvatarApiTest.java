package id.csui.depor;
import com.fasterxml.jackson.databind.*;
import java.util.*;
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
@SpringBootTest(properties={"app.supabase-url=http://localhost:54321","app.supabase-anon-key=test-key","app.allowed-origins=http://localhost:3000","app.cookie-secure=false"})
@AutoConfigureMockMvc
class DirectoryAvatarApiTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @MockitoBean Supabase db;
 final UUID uid=UUID.fromString("00000000-0000-0000-0000-000000000001");
 final String target="00000000-0000-0000-0000-000000000002";
 final String path=target+"/00000000-0000-0000-0000-000000000003.jpg";
 @BeforeEach void auth(){when(db.authenticate("user-token")).thenReturn(new Principal(uid,"member","Reader","user-token",null));}
 String lookup(String ids){return "/rest/v1/profiles?select=id,avatar_path&active=eq.true&id=in.("+ids+")";}
 @Test void readingTeamPhotosRequiresAnAuthenticatedActiveAccount()throws Exception {
  mvc.perform(get("/api/profiles/avatars").param("ids",target)).andExpect(status().isUnauthorized());
  when(db.authenticate("user-token")).thenThrow(new ApiException(403,"FORBIDDEN","Akun tidak aktif."));
  mvc.perform(get("/api/profiles/avatars").header("Authorization","Bearer user-token").param("ids",target)).andExpect(status().isForbidden());
  verify(db,never()).signedAvatars(any(),any());
 }
 @Test void staffCanBatchReadCurrentTeamPhotosUsingTheirOwnJwt()throws Exception {
  when(db.request(HttpMethod.GET,lookup(target),"user-token",null)).thenReturn(mapper.readTree("[{\"id\":\""+target+"\",\"avatar_path\":\""+path+"\"}]"));
  when(db.signedAvatars(List.of(path),"user-token")).thenReturn(Map.of(path,"https://storage.example.invalid/signed"));
  mvc.perform(get("/api/profiles/avatars").header("Authorization","Bearer user-token").param("ids",target+","+target)).andExpect(status().isOk()).andExpect(jsonPath("$.data['"+target+"'].path").value(path)).andExpect(jsonPath("$.data['"+target+"'].url").value("https://storage.example.invalid/signed")).andExpect(jsonPath("$.meta.expiresIn").value(900));
  verify(db,times(1)).signedAvatars(List.of(path),"user-token");
 }
 @Test void emptyOrInactiveResultsNeverSignClientChosenPaths()throws Exception {
  when(db.request(HttpMethod.GET,lookup(target),"user-token",null)).thenReturn(mapper.readTree("[]"));
  mvc.perform(get("/api/profiles/avatars").header("Authorization","Bearer user-token").param("ids",target)).andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
  verify(db,never()).signedAvatars(any(),any());
 }
 @Test void malformedForeignAndUnrequestedPathsAreNotSigned()throws Exception {
  when(db.request(HttpMethod.GET,lookup(target),"user-token",null)).thenReturn(mapper.readTree("[{\"id\":\""+target+"\",\"avatar_path\":\""+uid+"/00000000-0000-0000-0000-000000000003.jpg\"},{\"id\":\""+uid+"\",\"avatar_path\":\"https://evil.invalid/x.jpg\"}]"));
  mvc.perform(get("/api/profiles/avatars").header("Authorization","Bearer user-token").param("ids",target)).andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
  verify(db,never()).signedAvatars(any(),any());
 }
 @Test void malformedMissingAndUnboundedIdsReturn400BeforeDatabaseReads()throws Exception {
  for(String ids:List.of("", "bad-id", "1-1-1-1-1", target+",", String.join(",",Collections.nCopies(101,target))))mvc.perform(get("/api/profiles/avatars").header("Authorization","Bearer user-token").param("ids",ids)).andExpect(status().isBadRequest());
  mvc.perform(get("/api/profiles/avatars").header("Authorization","Bearer user-token")).andExpect(status().isBadRequest());
  verify(db,never()).request(any(),any(),any(),any());
 }
 @Test void missingStorageObjectsAreOmittedAndStorageFailureIsNotFalseSuccess()throws Exception {
  when(db.request(HttpMethod.GET,lookup(target),"user-token",null)).thenReturn(mapper.readTree("[{\"id\":\""+target+"\",\"avatar_path\":\""+path+"\"}]"));
  when(db.signedAvatars(List.of(path),"user-token")).thenReturn(Map.of());
  mvc.perform(get("/api/profiles/avatars").header("Authorization","Bearer user-token").param("ids",target)).andExpect(status().isOk()).andExpect(jsonPath("$.data").isEmpty());
  when(db.signedAvatars(List.of(path),"user-token")).thenThrow(new ApiException(502,"STORAGE_UNAVAILABLE","Foto tim belum dapat dimuat."));
  mvc.perform(get("/api/profiles/avatars").header("Authorization","Bearer user-token").param("ids",target)).andExpect(status().isBadGateway());
 }
 @Test void directoryListIncludesPhotoVersionWithoutExposingEmail()throws Exception {
  when(db.request(eq(HttpMethod.GET),contains("/rest/v1/profiles?select=id,name,role,active,avatar_path&"),eq("user-token"),isNull())).thenReturn(mapper.readTree("[{\"id\":\""+target+"\",\"name\":\"Teammate\",\"role\":\"member\",\"active\":true,\"avatar_path\":\""+path+"\"}]"));
  mvc.perform(get("/api/profiles").header("Authorization","Bearer user-token")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].avatar_path").value(path)).andExpect(jsonPath("$.data[0].email").doesNotExist());
 }
}
