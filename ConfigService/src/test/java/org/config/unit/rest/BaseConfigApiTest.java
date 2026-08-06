package org.config.unit.rest;

import org.config.facade.ConfigFacade;
import org.config.rest.ConfigApi;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public abstract class BaseConfigApiTest {

    @Mock
    protected ConfigFacade configFacade;

    @InjectMocks
    protected ConfigApi configApi;
}
