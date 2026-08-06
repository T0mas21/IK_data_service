package org.config.data.repository;

import org.config.data.model.Config;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConfigRepository extends JpaRepository<Config, Long> {

    Optional<Config> findByName(String name);

    @Query("SELECT c.name FROM Config c")
    List<String> findAllNames();

    @Transactional
    void deleteByName(String name);

    @Transactional
    @Modifying
    @Query("UPDATE Config c SET c.data = :newData WHERE c.name = :name")
    int updateDataByName(@Param("name") String name, @Param("newData") String newData);
}
