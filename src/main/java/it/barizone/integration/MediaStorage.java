package it.barizone.integration;

import it.barizone.model.entity.MediaFile;

/**
 * Dove finiscono i byte delle foto. L'implementazione predefinita usa PostgreSQL (come D1 nella
 * versione precedente); un'implementazione S3/R2 può sostituirla senza toccare il resto del codice.
 */
public interface MediaStorage {

    void store(MediaFile file, byte[] content);

    byte[] load(MediaFile file);
}
