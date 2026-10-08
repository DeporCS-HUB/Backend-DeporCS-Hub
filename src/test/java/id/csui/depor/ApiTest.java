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
import jakarta.servlet.http.Cookie;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"app.supabase-url=http://localhost:54321","app.supabase-anon-key=test-key","app.allowed-origins=http://localhost:3000","app.cookie-secure=false","app.cookie-same-site=Lax"})
@AutoConfigureMockMvc
class ApiTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @MockitoBean Supabase db;
 final UUID uid=UUID.fromString("00000000-0000-0000-0000-000000000001");
 final UUID other=UUID.fromString("00000000-0000-0000-0000-000000000002");
 final UUID row=UUID.fromString("00000000-0000-0000-0000-000000000003");
 Principal member(){return new Principal(uid,"member","Test Member","member-token");}
 Principal staff(){return new Principal(uid,"staff","Test Staff","staff-token");}
 @BeforeEach void auth(){when(db.authenticate("member-token")).thenReturn(member());when(db.authenticate("staff-token")).thenReturn(staff());}
 String task(String assignee){return "{\"title\":\"Prepare proposal\",\"description\":null,\"program_id\":null,\"assignee_id\":"+assignee+",\"status\":\"To Do\",\"priority\":\"Medium\",\"due_date\":null}";}
 @Test void healthPublic() throws Exception {mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.data.runtime").value("Java 21"));}
 @Test void protectedListRequiresToken() throws Exception {mvc.perform(get("/api/programs")).andExpect(status().isUnauthorized());verifyNoInteractions(db);}
 @Test void invalidTokenFailsClosed() throws Exception {when(db.authenticate("forged")).thenThrow(new ApiException(401,"UNAUTHENTICATED","Invalid"));mvc.perform(get("/api/dashboard").header("Authorization","Bearer forged")).andExpect(status().isUnauthorized());}
 @Test void memberCannotWriteProgram() throws Exception {mvc.perform(post("/api/programs").header("Authorization","Bearer member-token").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());verify(db,never()).request(any(),any(),any(),any());}
 @Test void memberCannotDeleteFinance() throws Exception {mvc.perform(delete("/api/finances/"+row).header("Authorization","Bearer member-token")).andExpect(status().isForbidden());}
 @Test void validSessionUsesTrustedProfile() throws Exception {mvc.perform(get("/api/auth/session").header("Authorization","Bearer member-token")).andExpect(status().isOk()).andExpect(jsonPath("$.data.role").value("member"));}
 @Test void staffInvalidProgramRejectedBeforeDatabase() throws Exception {mvc.perform(post("/api/programs").header("Authorization","Bearer staff-token").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\",\"pic\":\"Staff\",\"start_date\":\"2026-10-09\",\"end_date\":\"2026-10-08\",\"status\":\"Planning\",\"progress\":101,\"budget\":-1}")).andExpect(status().isBadRequest());verify(db,never()).request(any(),any(),any(),any());}
 @Test void unknownRoleInputRejected() throws Exception {mvc.perform(post("/api/tasks").header("Authorization","Bearer member-token").contentType(MediaType.APPLICATION_JSON).content(task("null").replace("\"title\"","\"role\":\"admin\",\"title\""))).andExpect(status().isBadRequest());}
 @Test void memberCannotAssignTaskToOthers() throws Exception {mvc.perform(post("/api/tasks").header("Authorization","Bearer member-token").contentType(MediaType.APPLICATION_JSON).content(task("\""+other+"\""))).andExpect(status().isForbidden());verify(db,never()).request(any(),any(),any(),any());}
 @Test void memberTaskCreatedWithServerOwner() throws Exception {
  when(db.request(eq(HttpMethod.POST),eq("/rest/v1/tasks"),eq("member-token"),any())).thenReturn(mapper.readTree("[{\"id\":\""+row+"\"}]"));
  mvc.perform(post("/api/tasks").header("Authorization","Bearer member-token").contentType(MediaType.APPLICATION_JSON).content(task("null"))).andExpect(status().isCreated()).andExpect(jsonPath("$.data.id").value(row.toString()));
  var captor=org.mockito.ArgumentCaptor.forClass(Object.class);verify(db).request(eq(HttpMethod.POST),eq("/rest/v1/tasks"),eq("member-token"),captor.capture());JsonNode body=(JsonNode)captor.getValue();assertEquals(uid.toString(),body.path("created_by").asText());assertEquals(uid.toString(),body.path("assignee_id").asText());
 }
 @Test void memberCannotChangeOthersTask() throws Exception {when(db.request(eq(HttpMethod.GET),contains("/rest/v1/tasks?id=eq."),eq("member-token"),isNull())).thenReturn(mapper.readTree("[{\"created_by\":\""+other+"\",\"assignee_id\":\""+other+"\"}]"));mvc.perform(put("/api/tasks/"+row).header("Authorization","Bearer member-token").contentType(MediaType.APPLICATION_JSON).content(task("null"))).andExpect(status().isForbidden());verify(db,never()).request(eq(HttpMethod.PATCH),any(),any(),any());}
 @Test void ownedTaskStatusCanPersist() throws Exception {when(db.request(eq(HttpMethod.GET),contains("/rest/v1/tasks?id=eq."),eq("member-token"),isNull())).thenReturn(mapper.readTree("[{\"created_by\":\""+uid+"\"}]"));when(db.request(eq(HttpMethod.PATCH),any(),eq("member-token"),any())).thenReturn(mapper.readTree("[{\"status\":\"Done\"}]"));mvc.perform(put("/api/tasks/"+row).header("Authorization","Bearer member-token").contentType(MediaType.APPLICATION_JSON).content(task("null").replace("To Do","Done"))).andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("Done"));}
 @Test void zeroFinanceRejected() throws Exception {mvc.perform(post("/api/finances").header("Authorization","Bearer staff-token").contentType(MediaType.APPLICATION_JSON).content("{\"description\":\"Equipment\",\"type\":\"expense\",\"amount\":0,\"category\":\"Sports\",\"transaction_date\":\"2026-10-08\",\"status\":\"approved\"}")).andExpect(status().isBadRequest());}
 @Test void negativeInventoryRejected() throws Exception {mvc.perform(post("/api/inventory").header("Authorization","Bearer staff-token").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Ball\",\"category\":\"Equipment\",\"quantity\":-1,\"status\":\"Available\",\"condition\":\"Baik\"}")).andExpect(status().isBadRequest());}
 @Test void invalidPaginationRejected() throws Exception {mvc.perform(get("/api/programs?size=101").header("Authorization","Bearer staff-token")).andExpect(status().isBadRequest());}
 @Test void nonexistentDeleteDoesNotReportSuccess() throws Exception {when(db.request(eq(HttpMethod.GET),contains("/rest/v1/inventory?id=eq."),eq("staff-token"),isNull())).thenReturn(mapper.readTree("[]"));mvc.perform(delete("/api/inventory/"+row).header("Authorization","Bearer staff-token")).andExpect(status().isNotFound());verify(db,never()).request(eq(HttpMethod.DELETE),any(),any(),any());}
 @Test void dashboardCallsDatabaseRpc() throws Exception {when(db.request(eq(HttpMethod.POST),eq("/rest/v1/rpc/dashboard_stats"),eq("member-token"),any())).thenReturn(mapper.readTree("{\"summary\":{\"totalPrograms\":7}}"));mvc.perform(get("/api/dashboard").header("Authorization","Bearer member-token")).andExpect(status().isOk()).andExpect(jsonPath("$.data.summary.totalPrograms").value(7));}
 @Test void loginRequiresTrustedOrigin() throws Exception {mvc.perform(post("/api/auth/login").header("Origin","https://evil.invalid").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"staff@example.invalid\",\"password\":\"test-password\"}")).andExpect(status().isForbidden());verify(db,never()).request(any(),any(),any(),any());}
 @Test void loginSetsHttpOnlyRefreshCookie() throws Exception {when(db.request(eq(HttpMethod.POST),eq("/auth/v1/token?grant_type=password"),isNull(),any())).thenReturn(mapper.readTree("{\"access_token\":\"member-token\",\"refresh_token\":\"refresh-test\",\"expires_in\":3600}"));mvc.perform(post("/api/auth/login").header("Origin","http://localhost:3000").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"member@example.invalid\",\"password\":\"test-password\"}")).andExpect(status().isOk()).andExpect(header().string("Set-Cookie",org.hamcrest.Matchers.containsString("HttpOnly"))).andExpect(jsonPath("$.data.user.role").value("member")).andExpect(jsonPath("$.data.refresh_token").doesNotExist());}
 @Test void refreshRotatesCookie() throws Exception {when(db.request(eq(HttpMethod.POST),eq("/auth/v1/token?grant_type=refresh_token"),isNull(),eq(Map.of("refresh_token","old-refresh")))).thenReturn(mapper.readTree("{\"access_token\":\"member-token\",\"refresh_token\":\"new-refresh\",\"expires_in\":3600}"));mvc.perform(post("/api/auth/refresh").header("Origin","http://localhost:3000").cookie(new Cookie("depor_refresh","old-refresh"))).andExpect(status().isOk()).andExpect(header().string("Set-Cookie",org.hamcrest.Matchers.containsString("new-refresh")));}
 @Test void missingRefreshRejectedAndCookieCleared() throws Exception {mvc.perform(post("/api/auth/refresh").header("Origin","http://localhost:3000")).andExpect(status().isUnauthorized()).andExpect(header().string("Set-Cookie",org.hamcrest.Matchers.containsString("Max-Age=0")));}
 @Test void logoutRevokesThroughSupabaseAndClearsCookie() throws Exception {mvc.perform(post("/api/auth/logout").header("Origin","http://localhost:3000").header("Authorization","Bearer member-token")).andExpect(status().isOk()).andExpect(header().string("Set-Cookie",org.hamcrest.Matchers.containsString("Max-Age=0")));verify(db).request(HttpMethod.POST,"/auth/v1/logout?scope=local","member-token",null);}
 @Test void logoutUpstreamFailureNotReportedSuccessful() throws Exception {when(db.request(eq(HttpMethod.POST),eq("/auth/v1/logout?scope=local"),any(),isNull())).thenThrow(new ApiException(503,"SUPABASE_UNAVAILABLE","Unavailable"));mvc.perform(post("/api/auth/logout").header("Origin","http://localhost:3000").header("Authorization","Bearer member-token")).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.data.loggedOut").doesNotExist());}
 @Test void programDtoHasOnlyDatabaseFields() throws Exception {var input=new Inputs.Program("Program",null,"Staff",null,java.time.LocalDate.now(),java.time.LocalDate.now(),"Planning",0,java.math.BigDecimal.ZERO);JsonNode json=mapper.valueToTree(input);assertFalse(json.has("dateRangeValid"));assertEquals(9,json.size());}
}
