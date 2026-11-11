package com.vapedealspot.cms;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CmsPageDocumentRepository extends JpaRepository<CmsPageDocument, Long> {

    Optional<CmsPageDocument> findByPageIdAndStatus(String pageId, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM CmsPageDocument d WHERE d.pageId = :pageId AND d.status = :status")
    Optional<CmsPageDocument> findByPageIdAndStatusForUpdate(@Param("pageId") String pageId, @Param("status") String status);
}

