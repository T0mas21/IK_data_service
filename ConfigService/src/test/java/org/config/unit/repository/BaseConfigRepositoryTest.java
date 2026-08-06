package org.config.unit.repository;

import org.config.data.repository.ConfigRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public abstract class BaseConfigRepositoryTest {

    @Autowired
    protected ConfigRepository configRepository;

    @Autowired
    protected TestEntityManager entityManager;
}