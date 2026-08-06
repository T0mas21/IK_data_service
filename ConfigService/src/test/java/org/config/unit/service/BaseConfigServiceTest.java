package org.config.unit.service;

import org.config.data.repository.ConfigRepository;
import org.config.service.impl.ConfigServiceImpl;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public abstract class BaseConfigServiceTest {

    @Mock
    protected ConfigRepository configRepository;

    @InjectMocks
    protected ConfigServiceImpl configService;
}