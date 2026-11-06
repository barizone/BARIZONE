package it.barizone.repository;

import java.util.List;
import java.util.UUID;

import it.barizone.model.entity.PushCampaign;
import it.barizone.model.enums.CampaignStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PushCampaignRepository extends JpaRepository<PushCampaign, UUID> {

    List<PushCampaign> findAllByOrderByCreatedAtDesc(Limit limit);

    List<PushCampaign> findByStatus(CampaignStatus status);
}
