package it.barizone.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "admin_accounts")
@Getter
@Setter
@NoArgsConstructor
public class AdminAccount {

    @Id
    private String username;
    private String name;
    private String salt;
    private String passwordHash;
    private int iterations;
}
