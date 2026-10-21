package id.csui.depor;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/** Only server-owned identifiers/operators enter PostgREST's filter grammar. */
final class ListQuery {
 private ListQuery() {}
 private static final Map<String,List<String>> SEARCH = Map.of(
  "programs",List.of("name","pic","description","progress_notes"),
  "tasks",List.of("title","description"), "events",List.of("name","venue","description"),
  "inventory",List.of("name","category","location"), "finances",List.of("description","category"),
  "profiles",List.of("name"));
 private static final Map<String,Set<String>> STATUS = Map.of(
  "programs",Set.of("Planning","Ongoing","Active","Completed","Cancelled","Unspecified"),
  "tasks",Set.of("Backlog","To Do","In Progress","Review","Done"),
  "events",Set.of("Planning","Confirmed","Completed","Cancelled"),
  "inventory",Set.of("Available","Borrowed","Maintenance","Lost"),
  "finances",Set.of("pending","approved","rejected"));
 static String filters(String table,Map<String,String> input) {
  if(!SEARCH.containsKey(table)) throw invalid();
  var allowed=new HashSet<>(List.of("page","size","q"));
  if(STATUS.containsKey(table)) allowed.add("status");
  if(table.equals("programs"))allowed.add("kind");
  if(table.equals("tasks"))allowed.addAll(List.of("priority","program_id","assignee_id","scope","program_scope"));
  if(table.equals("events"))allowed.add("permit_status");
  if(table.equals("finances"))allowed.addAll(List.of("type","reimbursement_status"));
  if(table.equals("profiles"))allowed.add("departmentRole");
  if(!allowed.containsAll(input.keySet()))throw invalid();
  StringBuilder result=new StringBuilder();
  String q=input.getOrDefault("q","").strip();
  if(q.length()>120 || q.codePoints().anyMatch(Character::isISOControl))throw invalid();
  if(!q.isEmpty()) {
   // PostgreSQL ***= selects literal regex mode: no user regex or wildcard execution.
   String literal="\""+("***="+q).replace("\\","\\\\").replace("\"","\\\"")+"\"";
   String expression=SEARCH.get(table).stream().map(column->column+".imatch."+literal).collect(Collectors.joining(",","(",")"));
   result.append("&or=").append(encode(expression));
  }
  if(table.equals("programs")&&"Ongoing".equals(input.get("status")))result.append("&status=in.(Ongoing,Active)");
  else if(STATUS.containsKey(table))equal(result,input,"status",STATUS.get(table));
  if(table.equals("programs"))equal(result,input,"kind",Set.of("Proker","UKOR"));
  if(table.equals("tasks"))equal(result,input,"priority",Set.of("Low","Medium","High"));
  if(table.equals("events"))equal(result,input,"permit_status",Set.of("Not required","Pending","Approved","Rejected"));
  if(table.equals("finances"))equal(result,input,"type",Set.of("income","expense"));
  if(table.equals("finances"))equal(result,input,"reimbursement_status",Set.of("unknown","not_requested","pending","paid","not_required"));
  String role=input.getOrDefault("departmentRole","");
  if(table.equals("profiles")&&!role.isBlank()) {
   if(!Set.of("bph","staff").contains(role))throw invalid();
   result.append(role.equals("bph")?"&role=in.(staff,admin)":"&role=eq.member");
  }
  if(table.equals("tasks")) {
   for(String key:List.of("program_id","assignee_id")) {
    String value=input.getOrDefault(key,"");if(!value.isEmpty()) {
     try { if(!UUID.fromString(value).toString().equals(value))throw invalid(); }catch(IllegalArgumentException e){throw invalid();}
     result.append('&').append(key).append("=eq.").append(value);
    }
   }
   String programScope=input.getOrDefault("program_scope","");
   if(!Set.of("","linked","unlinked").contains(programScope))throw invalid();
   if(!programScope.isEmpty())result.append(programScope.equals("linked")?"&program_id=not.is.null":"&program_id=is.null");
   String scope=input.getOrDefault("scope","");
   if(!Set.of("","all","mine","overdue","unassigned").contains(scope))throw invalid();
   if(scope.equals("unassigned"))result.append("&assignee_id=is.null");
   if(scope.equals("overdue")) result.append("&status=neq.Done&due_date=lt.").append(java.time.LocalDate.now(java.time.ZoneId.of("Asia/Jakarta")));
  }
  return result.toString();
 }
 private static void equal(StringBuilder result,Map<String,String> input,String key,Set<String> values) {
  String value=input.getOrDefault(key,"");if(value.isEmpty())return;
  if(!values.contains(value))throw invalid();result.append('&').append(key).append("=eq.").append(encode(value));
 }
 static String encode(String value){return URLEncoder.encode(value,StandardCharsets.UTF_8).replace("+","%20");}
 private static ApiException invalid(){return new ApiException(400,"VALIDATION_ERROR","Filter atau pencarian tidak valid.");}
}
