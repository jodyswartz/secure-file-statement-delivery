package com.example.statements.api;

import com.example.statements.api.dto.GenerateLinkRequest;
import com.example.statements.api.dto.GenerateLinkResponse;
import com.example.statements.domain.Statement;
import com.example.statements.repo.StatementRepository;
import com.example.statements.service.S3StorageService;
import com.example.statements.service.TokenService;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping
public class AdminController {
  private final StatementRepository repo;
  private final S3StorageService storage;
  private final TokenService tokens;

  public AdminController(StatementRepository repo, S3StorageService storage, TokenService tokens) {
    this.repo = repo; this.storage = storage; this.tokens = tokens;
  }

  // ---- Admin: upload statement (protected by API key) ----
  @PostMapping(path = "/admin/statements", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<Map<String, Object>> upload(@RequestPart("file") MultipartFile file,
      @RequestParam("accountId") String accountId,
      @RequestParam("period") String period,
      @RequestParam("uploadedBy") String uploadedBy) throws Exception {
    String result = storage.upload(accountId, period, file);

    // objectKey | checksum | size
    String[] parts = result.split("\\|", 3);
    if (parts.length != 3) {
      throw new IllegalStateException("Unexpected storage result: " + result);
    }

    Statement s = new Statement();
    s.setAccountId(accountId);
    s.setPeriod(period);
    s.setObjectKey(parts[0]);
    s.setChecksumSha256(parts[1]);
    s.setSizeBytes(Long.parseLong(parts[2].trim()));
    s.setUploadedBy(uploadedBy);
    repo.save(s);

    Map<String, Object> body = Map.of(
        "id", s.getId(),
        "objectKey", s.getObjectKey(),
        "checksumSha256", s.getChecksumSha256(),
        "sizeBytes", s.getSizeBytes()
    );
    return ResponseEntity.status(HttpStatus.CREATED).body(body);
  }

  // ---- Admin: create a time-limited link for a statement ----
  @PostMapping("/admin/statements/{id}/links")
  public GenerateLinkResponse createLink(@PathVariable("id") UUID id,
      @RequestBody(required = false) GenerateLinkRequest req) {
    if (req == null) req = new GenerateLinkRequest(); // defaults inside DTO
    repo.findById(id).orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Statement not found"));
    String url = tokens.createDownloadUrl(id, req.getExpiresMinutes(), req.getCreatedBy());
    return new GenerateLinkResponse(url,
        OffsetDateTime.now().plusMinutes(req.getExpiresMinutes()));
  }
}
