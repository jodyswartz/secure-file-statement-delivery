package com.example.statements.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.model.ServerSideEncryption;

import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class S3StorageService {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;

    @Value("${s3.sse.enabled:false}") private boolean sseEnabled;
    @Value("${s3.kms.key-id:}") private String kmsKeyId;

    public S3StorageService(S3Client s3, S3Presigner presigner, @Value("${s3.bucket}") String bucket) {
        this.s3 = s3; this.presigner = presigner; this.bucket = bucket;
        ensureBucket();
    }

    private void ensureBucket() {
        try { s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build()); }
        catch (BucketAlreadyOwnedByYouException | BucketAlreadyExistsException e) { /* ok */ }
    }

    public static String sha256Hex(byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(bytes));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    public String upload(String accountId, String period, MultipartFile file) throws Exception {
        if (!file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed");
        }
        byte[] bytes = file.getBytes();
        String checksum = sha256Hex(bytes);
        String key = accountId + "/" + period + "/" + UUID.randomUUID() + ".pdf";
        PutObjectRequest.Builder putBuilder = PutObjectRequest.builder()
                .bucket(bucket).key(key)
                .contentType("application/pdf")
                .contentLength((long) bytes.length);
        if (sseEnabled) {
            putBuilder = putBuilder.serverSideEncryption(ServerSideEncryption.AWS_KMS);
            if (kmsKeyId != null && !kmsKeyId.isBlank()) {
                putBuilder = putBuilder.ssekmsKeyId(kmsKeyId);
            }
        }
        s3.putObject(putBuilder.build(), RequestBody.fromBytes(bytes));
        return key + "|" + checksum + "|" + bytes.length;
    }

    public String presignedGetUrl(String objectKey, int minutes) {
        GetObjectRequest get = GetObjectRequest.builder().bucket(bucket).key(objectKey).build();
        GetObjectPresignRequest preq = GetObjectPresignRequest.builder()
                .getObjectRequest(get)
                .signatureDuration(Duration.ofMinutes(minutes))
                .build();
        PresignedGetObjectRequest presigned = presigner.presignGetObject(preq);
        return presigned.url().toExternalForm();
    }
}
