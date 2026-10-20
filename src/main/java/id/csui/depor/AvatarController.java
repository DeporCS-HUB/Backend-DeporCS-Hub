package id.csui.depor;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.awt.image.BufferedImage;
import java.awt.RenderingHints;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/profiles/me/avatar")
public class AvatarController {
 record Image(@NotBlank @Size(max=699052) String image) { @Override public String toString(){return "Image[REDACTED]";} }
 private final Supabase db;
 AvatarController(Supabase db){this.db=db;}
 static byte[] sanitize(String encoded) {
  byte[] bytes;try{bytes=Base64.getDecoder().decode(encoded);}catch(IllegalArgumentException e){throw invalid();}
  if(bytes.length>524288||bytes.length<3||(bytes[0]&255)!=255||(bytes[1]&255)!=216)throw invalid();
  try(var stream=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
   var readers=ImageIO.getImageReaders(stream);if(!readers.hasNext())throw invalid();
   var reader=readers.next();
   try {
    reader.setInput(stream,true,true);int w=reader.getWidth(0),h=reader.getHeight(0);
    if(w<1||h<1||w>1024||h>1024)throw invalid();
    BufferedImage source=reader.read(0),image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);
    var graphics=image.createGraphics();try{graphics.drawImage(source,0,0,null);}finally{graphics.dispose();}
    var output=new ByteArrayOutputStream();if(!ImageIO.write(image,"jpg",output)||output.size()>524288)throw invalid();return output.toByteArray();
   }finally{reader.dispose();}
  }catch(IOException|IllegalArgumentException e){throw invalid();}
 }
 static String thumbnail(byte[] bytes,int size) {
  byte[] safe=sanitize(Base64.getEncoder().encodeToString(bytes));
  try {
   BufferedImage source=ImageIO.read(new ByteArrayInputStream(safe));
   int edge=Math.min(source.getWidth(),source.getHeight()),target=Math.min(edge,size);
   BufferedImage image=new BufferedImage(target,target,BufferedImage.TYPE_INT_RGB);
   var graphics=image.createGraphics();
   try {
    graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    int x=(source.getWidth()-edge)/2,y=(source.getHeight()-edge)/2;
    graphics.drawImage(source,0,0,target,target,x,y,x+edge,y+edge,null);
   }finally{graphics.dispose();}
   var out=new ByteArrayOutputStream();if(!ImageIO.write(image,"jpg",out))throw invalid();
   return "data:image/jpeg;base64,"+Base64.getEncoder().encodeToString(out.toByteArray());
  }catch(IOException e){throw invalid();}
 }
 private static ApiException invalid(){return new ApiException(400,"INVALID_IMAGE","Foto harus berupa JPEG valid, maksimal 512 KB dan 1024 × 1024 piksel.");}
 private boolean owned(String path,Principal p){return path!=null&&path.matches(java.util.regex.Pattern.quote(p.id.toString())+"/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg");}
 private Map<String,Object> profile(Principal p,String path){var result=new LinkedHashMap<>(p.profile());result.put("avatar_path",path);return result;}
 private void cleanup(String path,Principal p){if(owned(path,p))try{db.request(HttpMethod.DELETE,"/storage/v1/object/depor-avatars",p.token,Map.of("prefixes",List.of(path)));}catch(ApiException ignored){/* Profile write is committed; an inaccessible orphan is safer than deleting the current image. */}}
 private void pointer(String path,Principal p) {
  var payload=new LinkedHashMap<String,Object>();payload.put("avatar_path",path);
  String previous=p.avatarPath==null?"is.null":"eq."+ListQuery.encode(p.avatarPath);
  JsonNode rows=db.request(HttpMethod.PATCH,"/rest/v1/profiles?id=eq."+p.id+"&avatar_path="+previous,p.token,payload);
  if(!rows.isArray()||rows.size()!=1)throw new ApiException(409,"CONFLICT","Foto profil sudah berubah. Muat ulang sebelum mencoba lagi.");
 }
 @GetMapping Map<String,Object> read(@AuthenticationPrincipal Principal p,@RequestParam(required=false) Integer size) {
  if(size!=null&&!Set.of(96,192).contains(size))throw new ApiException(400,"VALIDATION_ERROR","Ukuran thumbnail tidak valid.");
  if(p.avatarPath==null)return Map.of("data",Map.of("available",false));
  if(!owned(p.avatarPath,p))throw new ApiException(404,"NOT_FOUND","Foto profil tidak tersedia.");
  if(size!=null)return Map.of("data",Map.of("available",true,"url",thumbnail(db.downloadAvatar(p.avatarPath,p.token),size)));
  return Map.of("data",Map.of("available",true,"url",db.signedAvatar(p.avatarPath,p.token),"expiresIn",900));
 }
 @PutMapping Map<String,Object> upload(@Valid @RequestBody Image input,@AuthenticationPrincipal Principal p) {
  byte[] bytes=sanitize(input.image());String next=p.id+"/"+UUID.randomUUID()+".jpg";
  db.uploadAvatar(next,p.token,bytes);
  try{pointer(next,p);}catch(ApiException e){if(e.status<500)cleanup(next,p);throw e;}
  cleanup(p.avatarPath,p);return Map.of("data",profile(p,next));
 }
 @DeleteMapping Map<String,Object> remove(@AuthenticationPrincipal Principal p) {
  if(p.avatarPath!=null){pointer(null,p);cleanup(p.avatarPath,p);}
  return Map.of("data",profile(p,null));
 }
}
