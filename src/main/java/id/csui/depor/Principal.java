package id.csui.depor;
import java.util.UUID;
/** Deliberately no generated toString: the access token must never be logged. */
public final class Principal {
 public final UUID id; public final String role; public final String name; final String token;
 Principal(UUID id,String role,String name,String token) { this.id=id;this.role=role;this.name=name;this.token=token; }
 public boolean manages() { return role.equals("staff") || role.equals("admin"); }
 public java.util.Map<String,Object> profile() { return java.util.Map.of("id",id,"role",role,"name",name); }
}
