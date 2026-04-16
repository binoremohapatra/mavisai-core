package com.studentlifeos.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "users") // ✅ Crucial: Rename table to avoid Postgres reserved keyword "user"
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private Long id; // We set this manually in data.sql, so no @GeneratedValue needed for now

    private String name;
    
    private String email;
    
    private String password;
}
