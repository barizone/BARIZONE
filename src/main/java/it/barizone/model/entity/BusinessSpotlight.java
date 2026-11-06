package it.barizone.model.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Azienda in vetrina per un giorno di calendario (Europe/Rome). */
@Entity
@Table(name = "business_spotlight")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BusinessSpotlight {

    @Id
    private LocalDate spotlightDate;
    private String merchantId;
}
