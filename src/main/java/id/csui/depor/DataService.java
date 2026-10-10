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
 public JsonNode list(String table,Principal p,int page,int size) {
  if(page<0||page>100000||size<1||size>100) throw new ApiException(400,"VALIDATION_ERROR","Pagination tidak valid.");
  String fields=table.equals("profiles")?"id,name,role":table.equals("programs")?"*,program_assignees(profile_id)":"*";
  String order=table.equals("events")?"start_date.asc,id.asc":"created_at.desc,id.asc";
  JsonNode rows=db.request(HttpMethod.GET,"/rest/v1/"+table+"?select="+fields+"&order="+order+"&limit="+size+"&offset="+((long)page*size),p.token,null);
  if(table.equals("profiles") && rows.isArray()) {
   rows=rows.deepCopy();for(JsonNode row:rows) addDepartmentRole(row);
  }
  if(table.equals("programs") && rows.isArray()) { rows=rows.deepCopy();for(JsonNode row:rows) addAssignees(row); }
  return rows;
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
 public JsonNode dashboard(Principal p){return db.request(HttpMethod.POST,"/rest/v1/rpc/dashboard_stats",p.token,java.util.Map.of());}
}
