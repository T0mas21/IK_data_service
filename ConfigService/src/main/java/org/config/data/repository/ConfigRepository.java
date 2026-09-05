package org.config.data.repository;

import org.config.data.model.Config;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfigRepository extends JpaRepository<Config, Long> {

    Optional<Config> findByName(String name);

    @Query("SELECT c.name FROM Config c")
    List<String> findAllNames();

    void deleteByName(String name);

    @Query("SELECT DISTINCT c FROM Config c LEFT JOIN FETCH c.files WHERE c.id = :id")
    Optional<Config> findByIdWithFiles(@Param("id") Long id);
}