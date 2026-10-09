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
  String fields=table.equals("profiles")?"id,name,role":"*";
  String order=table.equals("events")?"start_date.asc,id.asc":"created_at.desc,id.asc";
  JsonNode rows=db.request(HttpMethod.GET,"/rest/v1/"+table+"?select="+fields+"&order="+order+"&limit="+size+"&offset="+((long)page*size),p.token,null);
  if(table.equals("profiles") && rows.isArray()) {
   rows=rows.deepCopy();for(JsonNode row:rows) addDepartmentRole(row);
  }
  return rows;
 }
 public JsonNode write(String table,UUID id,Object input,Principal p) {
  boolean task=table.equals("tasks");
  if(!task&&!p.manages()) throw new ApiException(403,"FORBIDDEN","Hanya BPH yang dapat mengelola data ini.");
  if(id!=null) authorize(table,id,p);
  ObjectNode payload=mapper.valueToTree(input);
  if(id==null) payload.put("created_by",p.id.toString());
  if(task&&!p.manages()&&payload.hasNonNull("assignee_id")&&!payload.get("assignee_id").asText().equals(p.id.toString())) throw new ApiException(403,"FORBIDDEN","Staff hanya dapat menetapkan tugas untuk dirinya sendiri.");
  if(id==null&&task&&!p.manages()&&!payload.hasNonNull("assignee_id")) payload.put("assignee_id",p.id.toString());
  JsonNode result=db.request(id==null?HttpMethod.POST:HttpMethod.PATCH,"/rest/v1/"+table+(id==null?"":"?id=eq."+id),p.token,payload);
  if(!result.isArray()||result.isEmpty()) throw new ApiException(404,"NOT_FOUND","Data tidak ditemukan atau sudah berubah.");
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
