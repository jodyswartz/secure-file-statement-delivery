package com.example.statements.api;

import com.example.statements.service.TokenService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DownloadController {

  private final TokenService tokens;

  public DownloadController(TokenService tokens) {
    this.tokens = tokens;
  }

  // Public endpoint: resolves a token and 302-redirects to a pre-signed URL
  @GetMapping("/download/{token}")
  public ResponseEntity<Object> download(@PathVariable("token") String token,
      HttpServletRequest req) {
    final String user = req.getHeader("X-User-Id");
    final String ip   = req.getRemoteAddr();
    final String ua   = req.getHeader("User-Agent");

    return tokens.resolveAndLog(token, user, ip, ua, 10)
        .map(url -> ResponseEntity.status(HttpStatus.FOUND)  // 302
            .header(HttpHeaders.LOCATION, url)
            .build())
        .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
  }

  @GetMapping("/health")
  public String health() {
    return "ok";
  }
}
