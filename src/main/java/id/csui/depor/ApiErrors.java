package id.csui.depor;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
@RestControllerAdvice
public class ApiErrors {
 public static Map<String,Object> body(String code,String message) { return Map.of("error",Map.of("code",code,"message",message)); }
 @ExceptionHandler(ApiException.class) ResponseEntity<?> api(ApiException e) { return ResponseEntity.status(e.status).body(body(e.code,e.getMessage())); }
 @ExceptionHandler({MethodArgumentNotValidException.class,HttpMessageNotReadableException.class,MethodArgumentTypeMismatchException.class})
 ResponseEntity<?> bad(Exception e) { return ResponseEntity.badRequest().body(body("VALIDATION_ERROR","Periksa format dan kelengkapan input.")); }
 @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class) ResponseEntity<?> missing(Exception e) { return ResponseEntity.status(404).body(body("NOT_FOUND","Endpoint tidak ditemukan.")); }
 @ExceptionHandler(org.springframework.web.HttpRequestMethodNotSupportedException.class) ResponseEntity<?> method(Exception e) { return ResponseEntity.status(405).body(body("METHOD_NOT_ALLOWED","Metode tidak tersedia untuk endpoint ini.")); }
 @ExceptionHandler(Exception.class) ResponseEntity<?> unexpected(Exception e) { return ResponseEntity.internalServerError().body(body("INTERNAL_ERROR","Operasi gagal. Silakan coba kembali.")); }
}
