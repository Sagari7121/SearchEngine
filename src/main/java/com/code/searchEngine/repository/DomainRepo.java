package com.code.searchEngine.repository;

import com.code.searchEngine.model.Domain;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DomainRepo extends JpaRepository<Domain, Integer> {

    @Query("SELECT d FROM Domain d WHERE d.domainName IN :domainNames")
    public List<Domain> getDataInDomainName(@Param("domainNames") List<String> domains);
}
