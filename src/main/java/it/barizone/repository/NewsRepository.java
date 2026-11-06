package it.barizone.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import it.barizone.model.entity.NewsArticle;
import it.barizone.model.enums.NewsStatus;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsRepository extends JpaRepository<NewsArticle, String> {

    List<NewsArticle> findAllByOrderByUpdatedAtDesc();

    List<NewsArticle> findByStatusInOrderByPublishedAtDesc(Collection<NewsStatus> statuses, Limit limit);

    Optional<NewsArticle> findByIdAndStatusIn(String id, Collection<NewsStatus> statuses);
}
