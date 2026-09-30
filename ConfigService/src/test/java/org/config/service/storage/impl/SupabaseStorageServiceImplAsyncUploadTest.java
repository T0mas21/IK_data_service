package org.config.service.storage.impl;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.scheduling.annotation.Async;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupabaseStorageServiceImplAsyncUploadTest {

    @Test
    void uploadFileAsync_IsAnnotatedAsync() throws NoSuchMethodException {
        Method method = SupabaseStorageServiceImpl.class.getMethod("uploadFileAsync", String.class, byte[].class, String.class);

        assertTrue(method.isAnnotationPresent(Async.class));
    }

    @Test
    void uploadFileAsync_CatchesAndLogsException_DoesNotPropagate() {
        SupabaseStorageServiceImpl service = new SupabaseStorageServiceImpl(new RestTemplateBuilder());

        assertDoesNotThrow(() -> service.uploadFileAsync("configs/1/a.pdf", "obsah".getBytes(), "application/pdf"));
    }
}
