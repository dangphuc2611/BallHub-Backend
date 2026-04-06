package com.ballhub.ballhub_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Materials")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Material {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaterialID")
    private Integer materialId;

    @Column(name = "MaterialName", length = 50, unique = true, nullable = false)
    private String materialName;
}
