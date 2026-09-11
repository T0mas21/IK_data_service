package org.config.unit.service;

import org.config.client.ScrapperServiceClient;
import org.config.client.VectorServiceClient;
import org.config.data.repository.ConfigRepository;
import org.config.data.repository.FileRepository;
import org.config.service.impl.ConfigServiceImpl;
import org.config.service.storage.SupabaseStorageService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public abstract class BaseConfigServiceTest {

    @Mock
    protected ConfigRepository configRepository;

    @Mock
    protected FileRepository fileRepository;

    @Mock
    protected SupabaseStorageService supabaseStorageService;

    @Mock
    protected ScrapperServiceClient scrapperServiceClient;

    @Mock
    protected VectorServiceClient vectorServiceClient;

    @InjectMocks
    protected ConfigServiceImpl configService;
}