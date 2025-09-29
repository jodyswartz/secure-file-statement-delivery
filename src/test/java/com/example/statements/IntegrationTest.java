package com.example.statements;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(SpringExtension.class)
@Testcontainers
public class IntegrationTest {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
      .withDatabaseName("statements")
      .withUsername("statements")
      .withPassword("statements");

  private static final DockerImageName LOCALSTACK_IMG =
      DockerImageName.parse("localstack/localstack:3.7");

  @Container
  static LocalStackContainer localstack = new LocalStackContainer(LOCALSTACK_IMG)
      .withServices(LocalStackContainer.Service.S3);

  @DynamicPropertySource
  static void props(DynamicPropertyRegistry r) {
    r.add("spring.datasource.url", postgres::getJdbcUrl);
    r.add("spring.datasource.username", postgres::getUsername);
    r.add("spring.datasource.password", postgres::getPassword);

    r.add("s3.bucket", () -> "statements");
    r.add("s3.region", () -> "us-east-1");
    r.add("s3.endpoint", () -> localstack.getEndpointOverride(LocalStackContainer.Service.S3).toString());
    r.add("s3.accessKey", localstack::getAccessKey);
    r.add("s3.secretKey", localstack::getSecretKey);

    r.add("admin.api.key", () -> "test-key");

    // Rely on Flyway and your migrations; make Hibernate validate the schema.
    r.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
  }

  @Autowired
  MockMvc mvc;

  ObjectMapper om = new ObjectMapper();

  @Test
  void happyPath() throws Exception {
    // 1) Upload a PDF
    byte[] pdf = "%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF".getBytes(StandardCharsets.UTF_8);
    MockMultipartFile file = new MockMultipartFile("file", "s.pdf", "application/pdf", pdf);

    String uploadJson = mvc.perform(multipart("/admin/statements")
            .file(file)
            .param("accountId", "ACC-1")
            .param("period", "2025-08")
            .param("uploadedBy", "tester")
            .header("X-API-Key", "test-key"))
        .andExpect(status().isCreated()) // 201 Created is correct for a create
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.objectKey").exists())
        .andExpect(jsonPath("$.checksumSha256").exists())
        .andExpect(jsonPath("$.sizeBytes").exists())
        .andReturn()
        .getResponse()
        .getContentAsString();

    JsonNode uploaded = om.readTree(uploadJson);
    String id = uploaded.get("id").asText();

    // 2) Create a time-limited download link
    String body = "{\"expiresMinutes\":30,\"createdBy\":\"qa\"}";
    String linkRes = mvc.perform(post("/admin/statements/" + id + "/links")
            .contentType(MediaType.APPLICATION_JSON)
            .content(body)
            .header("X-API-Key", "test-key"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").exists())
        .andReturn()
        .getResponse()
        .getContentAsString();

    String url = om.readTree(linkRes).get("url").asText();

    // 3) Hitting the download redirect
    mvc.perform(get(url))
        .andExpect(status().is3xxRedirection())
        .andExpect(header().exists("Location"));
  }
}
