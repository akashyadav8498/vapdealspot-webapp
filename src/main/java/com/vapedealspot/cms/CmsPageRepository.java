package com.vapedealspot.cms;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CmsPageRepository extends JpaRepository<CmsPage, String> {

    Optional<CmsPage> findBySlug(String slug);
}

