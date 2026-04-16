package com.studentlifeos.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subjects")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Subject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId; // Kis user ka subject hai
    private String name;
    private int attended;
    private int total;
}