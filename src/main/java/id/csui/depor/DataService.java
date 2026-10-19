package id.csui.depor;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
@Service
public class DataService {
 private final Supabase db;private final ObjectMapper mapper;
 public DataService(Supabase db,ObjectMapper mapper){this.db=db;this.mapper=mapper;}
 public record Page(JsonNode rows,boolean hasMore) {}
 public Page list(String table,Principal p,int page,int size,java.util.Map<String,String> filters) {
  if(page<0||page>100000||size<1||size>100) throw new ApiException(400,"VALIDATION_ERROR","Pagination tidak valid.");
  String criteria=ListQuery.filters(table,filters);
  if(table.equals("tasks")&&"mine".equals(filters.get("scope")))criteria+="&assignee_id=eq."+p.id;
  if(table.equals("profiles"))criteria+="&active=eq.true";
  String fields=table.equals("profiles")?"id,name,role,active":table.equals("programs")?"*,program_assignees(profile_id)":"*";
  String order=table.equals("events")?"start_date.asc,id.asc":"created_at.desc,id.asc";
  JsonNode rows=db.request(HttpMethod.GET,"/rest/v1/"+table+"?select="+fields+"&order="+order+"&limit="+(size+1)+"&offset="+((long)page*size)+criteria,p.token,null);
  if(rows==null||!rows.isArray())throw new ApiException(502,"DATABASE_UNAVAILABLE","Daftar tidak dapat dimuat.");
  var visible=mapper.createArrayNode();
  for(int i=0;i<Math.min(size,rows.size());i++) {
   JsonNode row=rows.get(i).deepCopy();if(table.equals("profiles"))addDepartmentRole(row);if(table.equals("programs"))addAssignees(row);visible.add(row);
  }
  return new Page(visible,rows.size()>size);
 }
 public JsonNode write(String table,UUID id,Object input,Principal p) {
  boolean task=table.equals("tasks");
  if(!task&&!p.manages()) throw new ApiException(403,"FORBIDDEN","Hanya BPH yang dapat mengelola data ini.");
  if(id!=null) authorize(table,id,p);
  ObjectNode payload=mapper.valueToTree(input);
  if(table.equals("programs")) {
   ObjectNode rpc=mapper.createObjectNode();if(id==null) rpc.putNull("p_id");else rpc.put("p_id",id.toString());rpc.set("p_payload",payload);
   JsonNode result=db.request(HttpMethod.POST,"/rest/v1/rpc/save_program",p.token,rpc);
   if(!result.isObject()||!result.hasNonNull("id")) throw new ApiException(404,"NOT_FOUND","Program tidak ditemukan atau sudah berubah.");
   return result;
  }
  if(id==null) payload.put("created_by",p.id.toString());
  if(task&&!p.manages()&&payload.hasNonNull("assignee_id")&&!payload.get("assignee_id").asText().equals(p.id.toString())) throw new ApiException(403,"FORBIDDEN","Staff hanya dapat menetapkan tugas untuk dirinya sendiri.");
  if(id==null&&task&&!p.manages()&&!payload.hasNonNull("assignee_id")) payload.put("assignee_id",p.id.toString());
  JsonNode result=db.request(id==null?HttpMethod.POST:HttpMethod.PATCH,"/rest/v1/"+table+(id==null?"":"?id=eq."+id),p.token,payload);
  if(!result.isArray()||result.isEmpty()) throw new ApiException(404,"NOT_FOUND","Data tidak ditemukan atau sudah berubah.");
  return result.get(0);
 }
 private JsonNode addAssignees(JsonNode row) {
  if(row instanceof ObjectNode object) {
   var ids=mapper.createArrayNode();for(JsonNode assignment:row.path("program_assignees")) ids.add(assignment.path("profile_id").asText());
   object.set("assignee_ids",ids);object.remove("program_assignees");
  }
  return row;
 }
 public JsonNode updateProgress(UUID id,Inputs.ProgramProgress input,Principal p) {
  JsonNode rows=db.request(HttpMethod.GET,"/rest/v1/programs?id=eq."+id+"&select=*,program_assignees(profile_id)",p.token,null);
  if(!rows.isArray()||rows.size()!=1) throw new ApiException(404,"NOT_FOUND","Program tidak ditemukan.");
  boolean assigned=false;for(JsonNode assignment:rows.get(0).path("program_assignees")) if(p.id.toString().equals(assignment.path("profile_id").asText())) assigned=true;
  if(!p.manages()&&!assigned) throw new ApiException(403,"FORBIDDEN","Hanya PJ kegiatan atau BPH yang dapat memperbarui progres.");
  JsonNode result=db.request(HttpMethod.PATCH,"/rest/v1/programs?id=eq."+id,p.token,mapper.valueToTree(input));
  if(!result.isArray()||result.size()!=1) throw new ApiException(404,"NOT_FOUND","Progres tidak tersimpan; muat ulang program.");
  return result.get(0);
 }
 void authorize(String table,UUID id,Principal p) {
  JsonNode rows=db.request(HttpMethod.GET,"/rest/v1/"+table+"?id=eq."+id+"&select=*",p.token,null);
  if(!rows.isArray()||rows.isEmpty()) throw new ApiException(404,"NOT_FOUND","Data tidak ditemukan.");
  if(table.equals("tasks")&&!p.manages()) {
   JsonNode row=rows.get(0);String uid=p.id.toString();
   if(!uid.equals(row.path("created_by").asText())&&!uid.equals(row.path("assignee_id").asText())) throw new ApiException(403,"FORBIDDEN","Tugas ini bukan milik Anda.");
  }
 }
 public void delete(String table,UUID id,Principal p) {
  if(!table.equals("tasks")&&!p.manages()) throw new ApiException(403,"FORBIDDEN","Hanya BPH yang dapat menghapus data ini.");
  authorize(table,id,p);
  JsonNode result=db.request(HttpMethod.DELETE,"/rest/v1/"+table+"?id=eq."+id,p.token,null);
  if(!result.isArray()||result.isEmpty()) throw new ApiException(404,"NOT_FOUND","Data tidak ditemukan atau sudah berubah.");
 }
 public JsonNode updateProfile(Inputs.Profile input,Principal p) {
  JsonNode result=db.request(HttpMethod.PATCH,"/rest/v1/profiles?id=eq."+p.id+"&select=id,name,role",p.token,java.util.Map.of("name",input.name().strip()));
  if(!result.isArray()||result.size()!=1) throw new ApiException(404,"NOT_FOUND","Profil tidak ditemukan atau tidak dapat diperbarui.");
  return addDepartmentRole(result.get(0).deepCopy());
 }
 private JsonNode addDepartmentRole(JsonNode profile) {
  if(profile instanceof ObjectNode object) object.put("departmentRole",Principal.departmentRoleOf(profile.path("role").asText()));
  return profile;
 }

 public JsonNode program(UUID id,Principal p) {
  JsonNode rows=db.request(HttpMethod.GET,"/rest/v1/programs?id=eq."+id+"&select=*,program_assignees(profile_id)",p.token,null);
  if(!rows.isArray()||rows.size()!=1)throw new ApiException(404,"NOT_FOUND","Program tidak ditemukan atau akses tidak tersedia.");
  return addAssignees(rows.get(0).deepCopy());
 }
 public JsonNode timeline(UUID id,int page,int size,Principal p) {
  program(id,p);if(page<0||page>100000||size<1||size>100)throw new ApiException(400,"VALIDATION_ERROR","Pagination tidak valid.");
  JsonNode result=db.request(HttpMethod.POST,"/rest/v1/rpc/program_timeline",p.token,java.util.Map.of("p_id",id,"p_page",page,"p_size",size));
  if(!result.isObject())throw new ApiException(404,"NOT_FOUND","Timeline tidak tersedia.");return result;
 }
 public Page milestones(UUID id,int page,int size,String status,Principal p) {
  program(id,p);if(page<0||page>100000||size<1||size>100)throw new ApiException(400,"VALIDATION_ERROR","Pagination tidak valid.");
  if(!java.util.Set.of("","Planned","Done").contains(status))throw new ApiException(400,"VALIDATION_ERROR","Status agenda tidak valid.");
  JsonNode rows=db.request(HttpMethod.GET,"/rest/v1/program_milestones?program_id=eq."+id+"&order=due_date.asc.nullslast,id.asc&limit="+(size+1)+"&offset="+((long)page*size)+(status.isEmpty()?"":"&status=eq."+status),p.token,null);
  if(!rows.isArray())throw new ApiException(502,"DATABASE_UNAVAILABLE","Agenda tidak dapat dimuat.");
  var items=mapper.createArrayNode();for(int i=0;i<Math.min(size,rows.size());i++)items.add(rows.get(i));return new Page(items,rows.size()>size);
 }
 public JsonNode workspaceWrite(String table,UUID programId,UUID id,Object input,Principal p,boolean delete) {
  program(programId,p);
  String path="/rest/v1/"+table+(id==null?"":"?program_id=eq."+programId+"&id=eq."+id);
  ObjectNode payload=mapper.valueToTree(input);
  if(id==null){payload.put("program_id",programId.toString());payload.put("created_by",p.id.toString());}
  JsonNode rows=db.request(delete?HttpMethod.DELETE:id==null?HttpMethod.POST:HttpMethod.PATCH,path,p.token,delete?null:payload);
  if(!rows.isArray()||rows.size()!=1)throw new ApiException(404,"NOT_FOUND","Data tidak ditemukan atau sudah berubah.");return rows.get(0);
 }
 public JsonNode taskSummary(java.util.Map<String,String> filters,Principal p) {
  ListQuery.filters("tasks",filters);
  return db.request(HttpMethod.POST,"/rest/v1/rpc/task_summary",p.token,java.util.Map.of("p_filters",filters));
 }
 public JsonNode taskStatus(UUID id,Inputs.TaskStatus input,Principal p) {
  authorize("tasks",id,p);
  String condition=input.expected_updated_at()==null?"":"&updated_at=eq."+ListQuery.encode(input.expected_updated_at().toString());
  JsonNode rows=db.request(HttpMethod.PATCH,"/rest/v1/tasks?id=eq."+id+condition,p.token,java.util.Map.of("status",input.status()));
  if(!rows.isArray()||rows.size()!=1)throw new ApiException(409,"CONFLICT","Tugas berubah. Muat ulang sebelum memindahkannya.");return rows.get(0);
 }
 public JsonNode dashboard(Principal p){return db.request(HttpMethod.POST,"/rest/v1/rpc/dashboard_stats",p.token,java.util.Map.of());}
}
