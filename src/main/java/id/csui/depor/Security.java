package id.csui.depor;
import java.io.IOException;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.*;
import org.springframework.web.filter.OncePerRequestFilter;
@Configuration
public class Security {
 @Bean SecurityFilterChain apiSecurity(HttpSecurity http,Supabase supabase,ObjectMapper mapper,@Value("${app.allowed-origins}") String origins) throws Exception {
  var allowed=Arrays.stream(origins.split(",")).map(String::trim).toList();
  var cors=new CorsConfiguration();cors.setAllowedOrigins(allowed);cors.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS"));cors.setAllowedHeaders(List.of("Authorization","Content-Type"));cors.setAllowCredentials(true);cors.setMaxAge(3600L);
  var source=new UrlBasedCorsConfigurationSource();source.registerCorsConfiguration("/**",cors);
  var filter=new OncePerRequestFilter() {
   @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
    try {
     // Only refresh cookies authenticate a request without a bearer token. Require a trusted browser Origin for all auth mutations.
     if(req.getRequestURI().startsWith("/api/auth/") && !req.getMethod().equals("GET") && !req.getMethod().equals("OPTIONS") && !allowed.contains(req.getHeader("Origin"))) throw new ApiException(403,"UNTRUSTED_ORIGIN","Origin tidak diizinkan.");
     String auth=req.getHeader("Authorization");
     if(auth!=null) {
      if(!auth.startsWith("Bearer ") || auth.length()<8) throw new ApiException(401,"UNAUTHENTICATED","Token tidak valid.");
      var p=supabase.authenticate(auth.substring(7));
      SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(p,null,List.of(new SimpleGrantedAuthority("ROLE_"+p.role.toUpperCase(Locale.ROOT)))));
     }
     chain.doFilter(req,res);
    } catch(ApiException e) {res.setStatus(e.status);res.setContentType("application/json");mapper.writeValue(res.getOutputStream(),ApiErrors.body(e.code,e.getMessage()));}
    finally {SecurityContextHolder.clearContext();}
   }
  };
  return http.cors(c->c.configurationSource(source)).csrf(c->c.disable()).sessionManagement(s->s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
   .authorizeHttpRequests(a->a.requestMatchers("/api/health","/api/auth/login","/api/auth/refresh").permitAll()
    .requestMatchers(HttpMethod.OPTIONS,"/**").permitAll()
    .requestMatchers(HttpMethod.POST,"/api/programs/**","/api/finances/**","/api/inventory/**","/api/events/**").hasAnyRole("STAFF","ADMIN")
    .requestMatchers(HttpMethod.PUT,"/api/programs/**","/api/finances/**","/api/inventory/**","/api/events/**").hasAnyRole("STAFF","ADMIN")
    .requestMatchers(HttpMethod.DELETE,"/api/programs/**","/api/finances/**","/api/inventory/**","/api/events/**").hasAnyRole("STAFF","ADMIN")
    .anyRequest().authenticated())
   .exceptionHandling(e->e.authenticationEntryPoint((q,s,x)->{s.setStatus(401);s.setContentType("application/json");mapper.writeValue(s.getOutputStream(),ApiErrors.body("UNAUTHENTICATED","Silakan login kembali."));})
    .accessDeniedHandler((q,s,x)->{s.setStatus(403);s.setContentType("application/json");mapper.writeValue(s.getOutputStream(),ApiErrors.body("FORBIDDEN","Anda tidak memiliki hak untuk operasi ini."));}))
   .addFilterBefore(filter,UsernamePasswordAuthenticationFilter.class).build();
 }
}
