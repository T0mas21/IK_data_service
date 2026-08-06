package org.config.unit.facade;

import org.config.facade.impl.ConfigFacadeImpl;
import org.config.mappers.ConfigMapper;
import org.config.service.ConfigService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public abstract class BaseConfigFacadeTest {

    @Mock
    protected ConfigService configService;

    @Mock
    protected ConfigMapper configMapper;

    @InjectMocks
    protected ConfigFacadeImpl configFacade;
}