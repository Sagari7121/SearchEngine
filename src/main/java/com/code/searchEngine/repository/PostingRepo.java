package com.code.searchEngine.repository;

import com.code.searchEngine.model.Posting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface PostingRepo extends JpaRepository<Posting, Long> {

    List<Posting> findByTermIn(Collection<String> terms);

    @Query("SELECT p.term, COUNT(DISTINCT p.pageId) FROM Posting p WHERE p.term IN :terms GROUP BY p.term")
    List<Object[]> docFrequencies(@Param("terms") Collection<String> terms);

    void deleteByPageId(UUID pageId);

    // PostingRepo
    @Query(value = """
    SELECT * FROM posting
    WHERE term = :term
    ORDER BY term_frequency DESC
    LIMIT :cap
    """, nativeQuery = true)
    List<Posting> findTopByTerm(@Param("term") String term, @Param("cap") int cap);
}
