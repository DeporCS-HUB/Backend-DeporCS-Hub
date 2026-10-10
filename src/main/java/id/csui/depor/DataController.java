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
 @GetMapping({"/programs","/tasks","/finances","/inventory","/profiles","/events"}) Map<String,Object> list(jakarta.servlet.http.HttpServletRequest req,@AuthenticationPrincipal Principal p,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="50") int size){return Map.of("data",data.list(req.getRequestURI().substring(5),p,page,size),"meta",Map.of("page",page,"size",size));}
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
