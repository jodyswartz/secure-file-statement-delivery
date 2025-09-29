package com.example.statements.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public class GenerateLinkRequest {
    @Min(1) @Max(1440)
    private int expiresMinutes = 60;
    private String createdBy;

    public int getExpiresMinutes() { return expiresMinutes; }
    public void setExpiresMinutes(int expiresMinutes) { this.expiresMinutes = expiresMinutes; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
}
