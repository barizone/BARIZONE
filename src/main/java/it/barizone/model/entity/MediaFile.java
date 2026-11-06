package it.barizone.model.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Basic;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "media_files")
@Getter
@Setter
@NoArgsConstructor
public class MediaFile {

    @Id
    private UUID id;
    private String mime;
    private int sizeBytes;

    /** Contenuto binario quando lo storage è il database. */
    @Basic(fetch = FetchType.LAZY)
    private byte[] content;

    /** Chiave dell'oggetto quando lo storage è esterno (S3/R2). */
    private String storageKey;
    private Instant createdAt;
}
