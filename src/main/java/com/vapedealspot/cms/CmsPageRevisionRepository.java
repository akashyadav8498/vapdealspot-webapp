package com.vapedealspot.cms;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CmsPageRevisionRepository extends JpaRepository<CmsPageRevision, Long> {

    List<CmsPageRevision> findByPageIdOrderByIdDesc(String pageId, Pageable pageable);

    Optional<CmsPageRevision> findByIdAndPageId(Long id, String pageId);

    long countByPageId(String pageId);
}

