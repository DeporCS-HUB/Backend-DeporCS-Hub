package id.csui.depor;
import java.util.UUID;
/** Deliberately no generated toString: the access token must never be logged. */
public final class Principal {
 public final UUID id; public final String role; public final String name; public final String avatarPath; final String token;
 Principal(UUID id,String role,String name,String token) {this(id,role,name,token,null);}
 Principal(UUID id,String role,String name,String token,String avatarPath) { this.id=id;this.role=role;this.name=name;this.token=token;this.avatarPath=avatarPath; }
 public boolean manages() { return role.equals("staff") || role.equals("admin"); }
 public static String departmentRoleOf(String databaseRole) {
  return switch(databaseRole) {case "staff","admin" -> "bph";case "member" -> "staff";default -> "unknown";};
 }
 public java.util.Map<String,Object> profile() { var profile=new java.util.LinkedHashMap<String,Object>();profile.put("id",id);profile.put("role",role);profile.put("name",name);profile.put("departmentRole",departmentRoleOf(role));profile.put("avatar_path",avatarPath);return profile; }
}
