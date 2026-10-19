package id.csui.depor;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ListQueryTest {
 @Test void literalSearchEscapesPostgrestGrammarAndNeverExecutesUserRegex() {
  for(String q:List.of("100%_* (a+)+$", "quote, \"hello\" \\", "olahraga é你好")) {
   String result=URLDecoder.decode(ListQuery.filters("programs",Map.of("q",q)),StandardCharsets.UTF_8);
   String literal="\"***="+q.replace("\\","\\\\").replace("\"","\\\"")+"\"";
   assertTrue(result.contains("name.imatch."+literal));assertTrue(result.contains("progress_notes.imatch."+literal));
  }
 }
 @Test void ongoingFilterIncludesLegacyActive(){assertEquals("&status=in.(Ongoing,Active)",ListQuery.filters("programs",Map.of("status","Ongoing")));}
 @Test void filtersAreResourceScopedAndWhitelisted() {
  for(var input:List.of(Map.of("order","id"),Map.of("kind","eq.Proker"),Map.of("q","x".repeat(121)),Map.of("q","line\nbreak"),Map.of("status","Done"))) assertEquals(400,assertThrows(ApiException.class,()->ListQuery.filters("programs",input)).status);
  assertThrows(ApiException.class,()->ListQuery.filters("profiles",Map.of("status","Active")));
  assertEquals("&role=in.(staff,admin)",ListQuery.filters("profiles",Map.of("departmentRole","bph")));
  assertEquals("&role=eq.member",ListQuery.filters("profiles",Map.of("departmentRole","staff")));
  assertEquals("&status=eq.In%20Progress&priority=eq.High",ListQuery.filters("tasks",Map.of("status","In Progress","priority","High")));
 }
}
