package id.csui.depor;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/profiles/avatars")
public class DirectoryAvatarController {
 private final Supabase db;
 DirectoryAvatarController(Supabase db){this.db=db;}
 @GetMapping Map<String,Object> read(@RequestParam(defaultValue="") String ids,@AuthenticationPrincipal Principal p) {
  String[] values=ids.split(",",-1);
  if(values.length>100||Arrays.stream(values).anyMatch(id->!id.matches("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")))throw new ApiException(400,"VALIDATION_ERROR","Pilih 1–100 akun yang valid.");
  var requested=new LinkedHashSet<String>();for(String id:values)requested.add(UUID.fromString(id).toString());
  JsonNode profiles=db.request(HttpMethod.GET,"/rest/v1/profiles?select=id,avatar_path&active=eq.true&id=in.("+String.join(",",requested)+")",p.token,null);
  if(profiles==null||!profiles.isArray())throw new ApiException(502,"DATABASE_UNAVAILABLE","Foto tim belum dapat dimuat.");
  var paths=new LinkedHashMap<String,String>();
  for(JsonNode profile:profiles) {
   String id=profile.path("id").asText(),path=profile.path("avatar_path").asText("");
   if(requested.contains(id)&&path.matches(java.util.regex.Pattern.quote(id)+"/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg"))paths.put(id,path);
  }
  var signed=paths.isEmpty()?Map.<String,String>of():db.signedAvatars(new ArrayList<>(paths.values()),p.token);
  var data=new LinkedHashMap<String,Object>();
  paths.forEach((id,path)->{if(signed.containsKey(path))data.put(id,Map.of("path",path,"url",signed.get(path)));});
  return Map.of("data",data,"meta",Map.of("expiresIn",900));
 }
}
