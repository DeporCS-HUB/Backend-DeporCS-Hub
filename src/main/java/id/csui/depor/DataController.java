package id.csui.depor;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api")
public class DataController {
 private final DataService data;public DataController(DataService data){this.data=data;}
 @GetMapping("/health") Map<String,Object> health(){return Map.of("data",Map.of("status","ok","runtime","Java 21"));}
 @GetMapping("/dashboard") Map<String,Object> dashboard(@AuthenticationPrincipal Principal p){return Map.of("data",data.dashboard(p));}
 @GetMapping({"/programs","/tasks","/finances","/inventory","/profiles","/events"}) Map<String,Object> list(jakarta.servlet.http.HttpServletRequest req,@AuthenticationPrincipal Principal p,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size,@RequestParam Map<String,String> filters){var result=data.list(req.getRequestURI().substring(5),p,page,size,filters);return Map.of("data",result.rows(),"meta",Map.of("page",page,"size",size,"hasMore",result.hasMore()));}

 @GetMapping("/programs/{id}") Map<String,Object> programDetail(@PathVariable UUID id,@AuthenticationPrincipal Principal p){return Map.of("data",data.program(id,p));}
 @GetMapping("/programs/{id}/timeline") Map<String,Object> timeline(@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@AuthenticationPrincipal Principal p){return Map.of("data",data.timeline(id,page,size,p));}
 @GetMapping("/programs/{id}/goals") Map<String,Object> goals(@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@AuthenticationPrincipal Principal p){var result=data.goals(id,page,size,p);return Map.of("data",result.rows(),"meta",Map.of("hasMore",result.hasMore()));}
 @PostMapping("/programs/{id}/goals") ResponseEntity<?> goal(@PathVariable UUID id,@Valid @RequestBody Inputs.Goal x,@AuthenticationPrincipal Principal p){return ResponseEntity.status(201).body(Map.of("data",data.workspaceWrite("program_goals",id,null,x,p,false)));}
 @PutMapping("/programs/{id}/goals/{goalId}") Map<String,Object> goalUpdate(@PathVariable UUID id,@PathVariable UUID goalId,@Valid @RequestBody Inputs.Goal x,@AuthenticationPrincipal Principal p){return Map.of("data",data.workspaceWrite("program_goals",id,goalId,x,p,false));}
 @DeleteMapping("/programs/{id}/goals/{goalId}") Map<String,Object> goalDelete(@PathVariable UUID id,@PathVariable UUID goalId,@AuthenticationPrincipal Principal p){data.workspaceWrite("program_goals",id,goalId,java.util.Map.of(),p,true);return Map.of("data",Map.of("deleted",true));}
 @GetMapping("/programs/{id}/milestones") Map<String,Object> milestones(@PathVariable UUID id,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="") String status,@AuthenticationPrincipal Principal p){var result=data.milestones(id,page,size,status,p);return Map.of("data",result.rows(),"meta",Map.of("hasMore",result.hasMore()));}
 @PostMapping("/programs/{id}/notes") ResponseEntity<?> note(@PathVariable UUID id,@Valid @RequestBody Inputs.Note x,@AuthenticationPrincipal Principal p){return ResponseEntity.status(201).body(Map.of("data",data.workspaceWrite("program_notes",id,null,x,p,false)));}
 @PostMapping("/programs/{id}/milestones") ResponseEntity<?> milestone(@PathVariable UUID id,@Valid @RequestBody Inputs.Milestone x,@AuthenticationPrincipal Principal p){return ResponseEntity.status(201).body(Map.of("data",data.workspaceWrite("program_milestones",id,null,x,p,false)));}
 @PutMapping("/programs/{id}/milestones/{milestoneId}") Map<String,Object> milestoneUpdate(@PathVariable UUID id,@PathVariable UUID milestoneId,@Valid @RequestBody Inputs.Milestone x,@AuthenticationPrincipal Principal p){return Map.of("data",data.workspaceWrite("program_milestones",id,milestoneId,x,p,false));}
 @DeleteMapping("/programs/{id}/milestones/{milestoneId}") Map<String,Object> milestoneDelete(@PathVariable UUID id,@PathVariable UUID milestoneId,@AuthenticationPrincipal Principal p){data.workspaceWrite("program_milestones",id,milestoneId,java.util.Map.of(),p,true);return Map.of("data",Map.of("deleted",true));}
 @GetMapping("/dashboard/overview") Map<String,Object> dashboardOverview(@AuthenticationPrincipal Principal p){return Map.of("data",data.dashboardOverview(p));}
 @GetMapping("/directory") Map<String,Object> directory(@AuthenticationPrincipal Principal p){return Map.of("data",data.directory(p));}
 @GetMapping("/tasks/{id}") Map<String,Object> task(@PathVariable UUID id,@AuthenticationPrincipal Principal p){return Map.of("data",data.task(id,p));}
 @GetMapping("/tasks/board") Map<String,Object> taskBoard(@RequestParam Map<String,String> filters,@AuthenticationPrincipal Principal p){return Map.of("data",data.taskBoard(filters,p));}
 @GetMapping("/tasks/summary") Map<String,Object> taskSummary(@RequestParam Map<String,String> filters,@AuthenticationPrincipal Principal p){return Map.of("data",data.taskSummary(filters,p));}
 @PutMapping("/tasks/{id}/status") Map<String,Object> taskStatus(@PathVariable UUID id,@Valid @RequestBody Inputs.TaskStatus x,@AuthenticationPrincipal Principal p){return Map.of("data",data.taskStatus(id,x,p));}
 private ResponseEntity<?> write(String table,UUID id,Object body,Principal p){return ResponseEntity.status(id==null?201:200).body(Map.of("data",data.write(table,id,body,p)));}
 @PutMapping("/profiles/me") Map<String,Object> profileUpdate(@Valid @RequestBody Inputs.Profile x,@AuthenticationPrincipal Principal p){return Map.of("data",data.updateProfile(x,p));}
 @PostMapping("/events") ResponseEntity<?> event(@Valid @RequestBody Inputs.Event x,@AuthenticationPrincipal Principal p){return write("events",null,x,p);}
 @PutMapping("/events/{id}") ResponseEntity<?> eventUpdate(@PathVariable UUID id,@Valid @RequestBody Inputs.Event x,@AuthenticationPrincipal Principal p){return write("events",id,x,p);}
 @PostMapping("/programs") ResponseEntity<?> program(@Valid @RequestBody Inputs.Program x,@AuthenticationPrincipal Principal p){return write("programs",null,x,p);}
 @PutMapping("/programs/{id}") ResponseEntity<?> programUpdate(@PathVariable UUID id,@Valid @RequestBody Inputs.Program x,@AuthenticationPrincipal Principal p){return write("programs",id,x,p);}
 @PutMapping("/programs/{id}/progress") Map<String,Object> progressUpdate(@PathVariable UUID id,@Valid @RequestBody Inputs.ProgramProgress x,@AuthenticationPrincipal Principal p){return Map.of("data",data.updateProgress(id,x,p));}
 @PostMapping("/tasks") ResponseEntity<?> task(@Valid @RequestBody Inputs.Task x,@AuthenticationPrincipal Principal p){return write("tasks",null,x,p);}
 @PutMapping("/tasks/{id}") ResponseEntity<?> taskUpdate(@PathVariable UUID id,@Valid @RequestBody Inputs.Task x,@AuthenticationPrincipal Principal p){return write("tasks",id,x,p);}
 @PostMapping("/finances") ResponseEntity<?> finance(@Valid @RequestBody Inputs.Finance x,@AuthenticationPrincipal Principal p){return write("finances",null,x,p);}
 @PutMapping("/finances/{id}") ResponseEntity<?> financeUpdate(@PathVariable UUID id,@Valid @RequestBody Inputs.Finance x,@AuthenticationPrincipal Principal p){return write("finances",id,x,p);}
 @PostMapping("/inventory") ResponseEntity<?> asset(@Valid @RequestBody Inputs.Inventory x,@AuthenticationPrincipal Principal p){return write("inventory",null,x,p);}
 @PutMapping("/inventory/{id}") ResponseEntity<?> assetUpdate(@PathVariable UUID id,@Valid @RequestBody Inputs.Inventory x,@AuthenticationPrincipal Principal p){return write("inventory",id,x,p);}
 @DeleteMapping({"/programs/{id}","/tasks/{id}","/finances/{id}","/inventory/{id}","/events/{id}"}) Map<String,Object> delete(jakarta.servlet.http.HttpServletRequest req,@PathVariable UUID id,@AuthenticationPrincipal Principal p){data.delete(req.getRequestURI().split("/")[2],id,p);return Map.of("data",Map.of("deleted",true));}
}
