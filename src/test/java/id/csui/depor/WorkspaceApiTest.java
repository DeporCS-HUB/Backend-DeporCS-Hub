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
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={"app.supabase-url=http://localhost:54321","app.supabase-anon-key=test-key","app.allowed-origins=http://localhost:3000","app.cookie-secure=false"})
@AutoConfigureMockMvc
class WorkspaceApiTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @MockitoBean Supabase db;
 final String uid="00000000-0000-0000-0000-000000000001", pid="00000000-0000-0000-0000-000000000002", mid="00000000-0000-0000-0000-000000000003";
 @BeforeEach void auth(){when(db.authenticate("pj-token")).thenReturn(new Principal(UUID.fromString(uid),"member","PJ","pj-token"));}
 void program(boolean visible)throws Exception{when(db.request(eq(HttpMethod.GET),contains("/rest/v1/programs?id=eq."),eq("pj-token"),isNull())).thenReturn(mapper.readTree(visible?"[{\"id\":\""+pid+"\",\"program_assignees\":[{\"profile_id\":\""+uid+"\"}]}]":"[]"));}
 @Test void directProgramLinkNormalizesAccounts()throws Exception{program(true);mvc.perform(get("/api/programs/"+pid).header("Authorization","Bearer pj-token")).andExpect(status().isOk()).andExpect(jsonPath("$.data.assignee_ids[0]").value(uid));}
 @Test void inaccessibleProgramStopsChildReadsAndWrites()throws Exception{
  program(false);
  mvc.perform(get("/api/programs/"+pid+"/timeline").header("Authorization","Bearer pj-token")).andExpect(status().isNotFound());
  mvc.perform(post("/api/programs/"+pid+"/notes").header("Authorization","Bearer pj-token").contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Update\"}")).andExpect(status().isNotFound());
  verify(db,never()).request(eq(HttpMethod.POST),any(),any(),any());
 }
 @Test void pjCanAddNoteWithServerOwnedAuthorAndProgram()throws Exception{
  program(true);when(db.request(eq(HttpMethod.POST),eq("/rest/v1/program_notes"),eq("pj-token"),any())).thenReturn(mapper.readTree("[{\"id\":\""+mid+"\"}]"));
  mvc.perform(post("/api/programs/"+pid+"/notes").header("Authorization","Bearer pj-token").contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Update\"}")).andExpect(status().isCreated());
  var c=org.mockito.ArgumentCaptor.forClass(Object.class);verify(db).request(eq(HttpMethod.POST),eq("/rest/v1/program_notes"),eq("pj-token"),c.capture());JsonNode payload=(JsonNode)c.getValue();assertEquals(pid,payload.path("program_id").asText());assertEquals(uid,payload.path("created_by").asText());
 }
 @Test void pjCanUpdateMilestoneButCannotSpoofProgramOrCreator()throws Exception{
  program(true);when(db.request(eq(HttpMethod.PATCH),contains("program_milestones?program_id=eq."+pid+"&id=eq."+mid),eq("pj-token"),any())).thenReturn(mapper.readTree("[{\"id\":\""+mid+"\"}]"));
  mvc.perform(put("/api/programs/"+pid+"/milestones/"+mid).header("Authorization","Bearer pj-token").contentType(MediaType.APPLICATION_JSON).content("{\"title\":\"Selection\",\"status\":\"Done\"}")).andExpect(status().isOk());
  mvc.perform(post("/api/programs/"+pid+"/notes").header("Authorization","Bearer pj-token").contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"Fake\",\"created_by\":\""+uid+"\"}")).andExpect(status().isBadRequest());
 }
 @Test void emptyAgendaWriteDoesNotPretendSuccess()throws Exception{
  program(true);when(db.request(eq(HttpMethod.DELETE),contains("program_milestones?program_id=eq."),eq("pj-token"),isNull())).thenReturn(mapper.readTree("[]"));
  mvc.perform(delete("/api/programs/"+pid+"/milestones/"+mid).header("Authorization","Bearer pj-token")).andExpect(status().isNotFound());
 }
 @Test void taskStatusIsPartialAndRejectsStaleWrite()throws Exception{
  when(db.request(eq(HttpMethod.GET),contains("/rest/v1/tasks?id=eq."),eq("pj-token"),isNull())).thenReturn(mapper.readTree("[{\"id\":\""+mid+"\",\"assignee_id\":\""+uid+"\"}]"));
  when(db.request(eq(HttpMethod.PATCH),contains("&updated_at=eq.2026-10-19T01%3A00%3A00Z"),eq("pj-token"),any())).thenReturn(mapper.readTree("[]"));
  mvc.perform(put("/api/tasks/"+mid+"/status").header("Authorization","Bearer pj-token").contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"Done\",\"expected_updated_at\":\"2026-10-19T01:00:00Z\"}")).andExpect(status().isConflict());
  var c=org.mockito.ArgumentCaptor.forClass(Object.class);verify(db).request(eq(HttpMethod.PATCH),any(),eq("pj-token"),c.capture());assertEquals(Map.of("status","Done"),c.getValue());
 }
 @Test void taskFiltersValidateUuidAndScopeBeforeDatabaseCall()throws Exception{
  mvc.perform(get("/api/tasks/summary").param("program_id","bad,or=(id.eq.x)").header("Authorization","Bearer pj-token")).andExpect(status().isBadRequest());
  mvc.perform(get("/api/tasks").param("scope","admin").header("Authorization","Bearer pj-token")).andExpect(status().isBadRequest());
  verify(db,never()).request(any(),any(),any(),any());
 }
 @Test void mineFilterUsesVerifiedIdentity()throws Exception{
  when(db.request(eq(HttpMethod.GET),contains("/rest/v1/tasks?"),eq("pj-token"),isNull())).thenReturn(mapper.readTree("[]"));
  mvc.perform(get("/api/tasks").param("scope","mine").header("Authorization","Bearer pj-token")).andExpect(status().isOk());
  verify(db).request(eq(HttpMethod.GET),contains("&assignee_id=eq."+uid),eq("pj-token"),isNull());
 }

 @Test void goalUsesCallerScopeAndRejectsUnverifiableActuals()throws Exception{
  program(true);when(db.request(eq(HttpMethod.POST),eq("/rest/v1/program_goals"),eq("pj-token"),any())).thenReturn(mapper.readTree("[{\"id\":\""+mid+"\"}]"));
  String base="{\"title\":\"Weekly sessions\",\"metric_type\":\"count\",\"target_value\":1,\"unit\":\"sesi\",\"target_period\":\"Per minggu\"";
  mvc.perform(post("/api/programs/"+pid+"/goals").header("Authorization","Bearer pj-token").contentType(MediaType.APPLICATION_JSON).content(base+"}")).andExpect(status().isCreated());
  mvc.perform(post("/api/programs/"+pid+"/goals").header("Authorization","Bearer pj-token").contentType(MediaType.APPLICATION_JSON).content(base+",\"actual_value\":0}")).andExpect(status().isBadRequest());
  mvc.perform(post("/api/programs/"+pid+"/goals").header("Authorization","Bearer pj-token").contentType(MediaType.APPLICATION_JSON).content(base+",\"actual_value\":0,\"measurement_period\":\"Week 1\",\"measured_on\":\"2026-10-21\",\"evidence\":\"Verified no session this week\"}")).andExpect(status().isCreated());
  var c=org.mockito.ArgumentCaptor.forClass(Object.class);verify(db,times(2)).request(eq(HttpMethod.POST),eq("/rest/v1/program_goals"),eq("pj-token"),c.capture());
  assertEquals(pid,((JsonNode)c.getValue()).path("program_id").asText());assertEquals(uid,((JsonNode)c.getValue()).path("created_by").asText());
 }
 @Test void inaccessibleGoalNeverReachesChildTable()throws Exception{
  program(false);mvc.perform(get("/api/programs/"+pid+"/goals").header("Authorization","Bearer pj-token")).andExpect(status().isNotFound());
  verify(db,never()).request(eq(HttpMethod.GET),contains("program_goals?"),any(),any());
 }
 @Test void goalsRejectInvalidUnitsAndUnknownImportFields()throws Exception{
  String invalid="{\"title\":\"Satisfaction\",\"metric_type\":\"percent\",\"target_value\":101,\"unit\":\"%\",\"target_period\":\"Annual\"}";
  mvc.perform(post("/api/programs/"+pid+"/goals").header("Authorization","Bearer pj-token").contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
  mvc.perform(post("/api/programs/"+pid+"/goals").header("Authorization","Bearer pj-token").contentType(MediaType.APPLICATION_JSON).content(invalid.replace("101","85").replace("Annual","Annual").replace("}",",\"source_key\":\"spoof\"}"))).andExpect(status().isBadRequest());
  verify(db,never()).request(any(),any(),any(),any());
 }
 @Test void timedAgendaPreservesLocalTimesAndRejectsMissingDateOrReverseRange()throws Exception{
  program(true);when(db.request(eq(HttpMethod.POST),eq("/rest/v1/program_milestones"),eq("pj-token"),any())).thenReturn(mapper.readTree("[{\"id\":\""+mid+"\"}]"));
  String body="{\"title\":\"Fixture match\",\"due_date\":\"2026-10-28\",\"start_time\":\"18:30\",\"end_time\":\"19:30\",\"status\":\"Planned\",\"schedule_status\":\"Tentative\",\"description\":\"Await revised slot\"}";
  mvc.perform(post("/api/programs/"+pid+"/milestones").header("Authorization","Bearer pj-token").contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
  mvc.perform(post("/api/programs/"+pid+"/milestones").header("Authorization","Bearer pj-token").contentType(MediaType.APPLICATION_JSON).content(body.replace("19:30","17:30"))).andExpect(status().isBadRequest());
  mvc.perform(post("/api/programs/"+pid+"/milestones").header("Authorization","Bearer pj-token").contentType(MediaType.APPLICATION_JSON).content(body.replace("\"due_date\":\"2026-10-28\",", ""))).andExpect(status().isBadRequest());
 }
}
