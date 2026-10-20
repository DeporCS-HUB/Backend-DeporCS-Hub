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
class PerformanceReadApiTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @MockitoBean Supabase db;
 @BeforeEach void auth(){when(db.authenticate("pj-token")).thenReturn(new Principal(UUID.fromString("00000000-0000-0000-0000-000000000001"),"member","PJ","pj-token"));}
 @Test void boardUsesOneUserJwtRpcForAllColumnsAndSummary()throws Exception {
  when(db.request(eq(HttpMethod.POST),eq("/rest/v1/rpc/task_board"),eq("pj-token"),any())).thenReturn(mapper.readTree("{\"summary\":{\"total\":25},\"columns\":{\"To Do\":{\"items\":[],\"hasMore\":true}}}"));
  mvc.perform(get("/api/tasks/board").header("Authorization","Bearer pj-token").param("pages","0,1,0,0,0").param("q","Needle")).andExpect(status().isOk()).andExpect(jsonPath("$.data.summary.total").value(25));
  verify(db,times(1)).request(HttpMethod.POST,"/rest/v1/rpc/task_board","pj-token",Map.of("p_pages",List.of(0,1,0,0,0),"p_filters",Map.of("q","Needle")));
  verify(db,never()).request(eq(HttpMethod.GET),any(),any(),any());
 }
 @Test void badPaginationAndFiltersCannotReachRpc()throws Exception {
  for(String pages:List.of("-1,0,0,0,0","0,0","100001,0,0,0,0","0,0,0,0,x","0,0,0,0,"))mvc.perform(get("/api/tasks/board").header("Authorization","Bearer pj-token").param("pages",pages)).andExpect(status().isBadRequest());
  for(String key:List.of("size","page","evil"))mvc.perform(get("/api/tasks/board").header("Authorization","Bearer pj-token").param(key,"1")).andExpect(status().isBadRequest());
  mvc.perform(get("/api/tasks/board").header("Authorization","Bearer pj-token").param("program_id","untrusted")).andExpect(status().isBadRequest());verify(db,never()).request(any(),any(),any(),any());
 }
 @Test void endpointsRequireVerifiedSession()throws Exception {
  for(String path:List.of("/api/directory","/api/tasks/board","/api/dashboard/overview"))mvc.perform(get(path)).andExpect(status().isUnauthorized());verify(db,never()).request(any(),any(),any(),any());
 }
 @Test void directoryAndOverviewEachUseOneCallerJwtRead()throws Exception {
  when(db.request(HttpMethod.POST,"/rest/v1/rpc/workspace_directory","pj-token",Map.of())).thenReturn(mapper.readTree("{\"profiles\":[],\"programs\":[]}"));
  when(db.request(HttpMethod.POST,"/rest/v1/rpc/dashboard_overview","pj-token",Map.of())).thenReturn(mapper.readTree("{\"summary\":{\"totalPrograms\":0}}"));
  mvc.perform(get("/api/directory").header("Authorization","Bearer pj-token")).andExpect(status().isOk()).andExpect(jsonPath("$.data.profiles").isArray());
  mvc.perform(get("/api/dashboard/overview").header("Authorization","Bearer pj-token")).andExpect(status().isOk()).andExpect(jsonPath("$.data.summary.totalPrograms").value(0));
  verify(db).request(HttpMethod.POST,"/rest/v1/rpc/workspace_directory","pj-token",Map.of());verify(db).request(HttpMethod.POST,"/rest/v1/rpc/dashboard_overview","pj-token",Map.of());
 }
 @Test void upstreamFailuresAndInvalidShapesAreNeverEmptySuccesses()throws Exception {
  when(db.request(eq(HttpMethod.POST),any(),eq("pj-token"),any())).thenReturn(mapper.readTree("[]"));
  for(String path:List.of("/api/directory","/api/tasks/board"))mvc.perform(get(path).header("Authorization","Bearer pj-token")).andExpect(status().isBadGateway());
 }
 @Test void taskDetailsUseCurrentCallerPermissionsAndCannotReturnHiddenRows()throws Exception {
  String id="00000000-0000-0000-0000-000000000004";
  String path="/rest/v1/tasks?id=eq."+id+"&select=*";
  when(db.request(HttpMethod.GET,path,"pj-token",null)).thenReturn(mapper.readTree("[{\"id\":\""+id+"\",\"description\":\"Full detail retained\"}]"));
  mvc.perform(get("/api/tasks/"+id).header("Authorization","Bearer pj-token")).andExpect(status().isOk()).andExpect(jsonPath("$.data.description").value("Full detail retained"));
  when(db.request(HttpMethod.GET,path,"pj-token",null)).thenReturn(mapper.readTree("[]"));
  mvc.perform(get("/api/tasks/"+id).header("Authorization","Bearer pj-token")).andExpect(status().isNotFound());
 }

}
