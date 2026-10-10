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
class ProgramApiTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @MockitoBean Supabase db;
 final UUID uid=UUID.fromString("00000000-0000-0000-0000-000000000001"), row=UUID.fromString("00000000-0000-0000-0000-000000000003");
 String body(){return "{\"status\":\"Ongoing\",\"progress\":null,\"progress_notes\":\"Selection underway\"}";}
 @BeforeEach void auth(){when(db.authenticate("member-token")).thenReturn(new Principal(uid,"member","Executor","member-token"));when(db.authenticate("staff-token")).thenReturn(new Principal(uid,"staff","Manager","staff-token"));}
 void existing(boolean assigned) throws Exception {when(db.request(eq(HttpMethod.GET),contains("/rest/v1/programs?id=eq."),any(),isNull())).thenReturn(mapper.readTree("[{\"id\":\""+row+"\",\"program_assignees\":"+(assigned?"[{\"profile_id\":\""+uid+"\"}]":"[]")+"}]"));}
 @Test void assignedStaffUpdatesOnlyProgressFields() throws Exception {
  existing(true);when(db.request(eq(HttpMethod.PATCH),any(),eq("member-token"),any())).thenReturn(mapper.readTree("[{\"id\":\""+row+"\",\"progress\":null}]"));
  mvc.perform(put("/api/programs/"+row+"/progress").header("Authorization","Bearer member-token").contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isOk()).andExpect(jsonPath("$.data.id").value(row.toString()));
  var captor=org.mockito.ArgumentCaptor.forClass(Object.class);verify(db).request(eq(HttpMethod.PATCH),eq("/rest/v1/programs?id=eq."+row),eq("member-token"),captor.capture());
  JsonNode payload=(JsonNode)captor.getValue();assertEquals(3,payload.size());assertTrue(payload.path("progress").isNull());assertFalse(payload.has("budget"));
 }
 @Test void unassignedStaffCannotUpdateProgress() throws Exception {existing(false);mvc.perform(put("/api/programs/"+row+"/progress").header("Authorization","Bearer member-token").contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isForbidden());verify(db,never()).request(eq(HttpMethod.PATCH),any(),any(),any());}
 @Test void progressRequestRejectsBudgetAndOutOfRangeValue() throws Exception {
  for(String invalid:List.of(body().replace("null","101"),body().replace("\"status\"","\"budget\":999,\"status\""))) mvc.perform(put("/api/programs/"+row+"/progress").header("Authorization","Bearer member-token").contentType(MediaType.APPLICATION_JSON).content(invalid)).andExpect(status().isBadRequest());
  verify(db,never()).request(any(),any(),any(),any());
 }
 @Test void bphCanUpdateProgressWithoutBeingPjButEmptyWriteFails() throws Exception {existing(false);when(db.request(eq(HttpMethod.PATCH),any(),any(),any())).thenReturn(mapper.readTree("[]"));mvc.perform(put("/api/programs/"+row+"/progress").header("Authorization","Bearer staff-token").contentType(MediaType.APPLICATION_JSON).content(body())).andExpect(status().isNotFound());}
 @Test void pjStillCannotEditFullProgram() throws Exception {existing(true);mvc.perform(put("/api/programs/"+row).header("Authorization","Bearer member-token").contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());}
 @Test void programListNormalizesMultiplePjWithoutMutatingAdapterData() throws Exception {
  JsonNode input=mapper.readTree("[{\"id\":\""+row+"\",\"program_assignees\":[{\"profile_id\":\""+uid+"\"}]}]");
  when(db.request(eq(HttpMethod.GET),contains("select=*,program_assignees(profile_id)"),eq("member-token"),isNull())).thenReturn(input);
  mvc.perform(get("/api/programs").header("Authorization","Bearer member-token")).andExpect(status().isOk()).andExpect(jsonPath("$.data[0].assignee_ids[0]").value(uid.toString())).andExpect(jsonPath("$.data[0].program_assignees").doesNotExist());
  assertTrue(input.get(0).has("program_assignees"));
 }
 @Test void bphCreateWithUnknownDatesAndProgressUsesAtomicRpc() throws Exception {
  when(db.request(eq(HttpMethod.POST),eq("/rest/v1/rpc/save_program"),eq("staff-token"),any())).thenReturn(mapper.readTree("{\"id\":\""+row+"\"}"));
  mvc.perform(post("/api/programs").header("Authorization","Bearer staff-token").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Sports unit\",\"pic\":\"Executor\",\"kind\":\"UKOR\",\"status\":\"Planning\",\"assignee_ids\":[\""+uid+"\"]}")).andExpect(status().isCreated());
  var captor=org.mockito.ArgumentCaptor.forClass(Object.class);verify(db).request(eq(HttpMethod.POST),eq("/rest/v1/rpc/save_program"),eq("staff-token"),captor.capture());
  JsonNode rpc=(JsonNode)captor.getValue();assertTrue(rpc.path("p_id").isNull());assertEquals("UKOR",rpc.path("p_payload").path("kind").asText());assertFalse(rpc.path("p_payload").has("created_by"));
 }
}
