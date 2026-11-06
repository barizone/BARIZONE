package it.barizone.repository;

import it.barizone.model.entity.AppConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

public interface AppConfigRepository extends JpaRepository<AppConfig, String> {

    /** Inserisce solo se la chiave non esiste (prima scrittura vince: es. generazione chiavi VAPID). */
    @Transactional
    @Modifying
    @Query(value = "INSERT INTO app_config (id, value) VALUES (:id, :value) ON CONFLICT (id) DO NOTHING", nativeQuery = true)
    int insertIfAbsent(String id, String value);
}
