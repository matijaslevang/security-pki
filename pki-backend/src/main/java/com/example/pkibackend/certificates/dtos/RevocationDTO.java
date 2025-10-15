package com.example.pkibackend.certificates.dtos;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevocationDTO {
    private int reason;
    private String comment;
}