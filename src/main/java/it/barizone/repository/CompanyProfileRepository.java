package it.barizone.repository;

import java.util.List;
import java.util.Optional;

import it.barizone.model.entity.CompanyProfile;
import it.barizone.model.enums.CompanyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, String> {

    List<CompanyProfile> findAllByOrderByUpdatedAtDesc();

    List<CompanyProfile> findByStatusOrderByUpdatedAtDesc(CompanyStatus status);

    List<CompanyProfile> findByStatusOrderByIdAsc(CompanyStatus status);

    Optional<CompanyProfile> findByIdAndStatus(String id, CompanyStatus status);

    /** Vero se la foto è collegata a una scheda pubblicata (copertina o galleria). */
    @Query(value = """
            SELECT EXISTS (SELECT 1 FROM company_profiles
                           WHERE status = 'published' AND (cover_image = :path OR :path = ANY(photos)))
            """, nativeQuery = true)
    boolean isMediaPublished(String path);
}
