package id.csui.depor;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PrincipalTest {
 @Test void existingManagementAccountsExposeBphWithoutChangingDatabaseRoles() {
  for(String role:java.util.List.of("staff","admin")) {
   Principal p=new Principal(UUID.randomUUID(),role,"Pengelola","test-token");
   assertEquals(role,p.profile().get("role"));
   assertEquals("bph",p.profile().get("departmentRole"));
   assertTrue(p.manages());
   assertFalse(p.profile().containsKey("token"));
  }
 }
 @Test void existingMemberIsStaffAndCannotManageDepartmentData() {
  Principal p=new Principal(UUID.randomUUID(),"member","Pelaksana","test-token");
  assertEquals("staff",p.profile().get("departmentRole"));
  assertFalse(p.manages());
 }
 @Test void unexpectedDatabaseRoleDoesNotGainManagementAccess() {
  Principal p=new Principal(UUID.randomUUID(),"invented","Unknown","test-token");
  assertEquals("unknown",p.profile().get("departmentRole"));
  assertFalse(p.manages());
 }
}
