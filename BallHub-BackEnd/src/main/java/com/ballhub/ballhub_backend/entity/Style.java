package com.ballhub.ballhub_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Styles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Style {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "StyleID")
    private Integer styleId;

    @Column(name = "StyleName", length = 100, unique = true, nullable = false)
    private String styleName;
}
