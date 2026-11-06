package it.barizone.repository;

import java.util.UUID;

import it.barizone.model.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaFileRepository extends JpaRepository<MediaFile, UUID> {
}
