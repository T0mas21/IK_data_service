package org.config.service.storage.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SupabaseStorageServiceImplUriEncodingTest {

    private final SupabaseStorageServiceImpl service = new SupabaseStorageServiceImpl(new RestTemplateBuilder());

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "supabaseUrl", "https://example.supabase.co");
        ReflectionTestUtils.setField(service, "bucket", "config-files");
    }

    @Test
    void objectUri_EncodesSpaceExactlyOnce() {
        URI uri = service.objectUri("configs/36/uuid_Navrh staze.pdf");

        assertEquals(
                "https://example.supabase.co/storage/v1/object/config-files/configs/36/uuid_Navrh%20staze.pdf",
                uri.toString()
        );
    }

    @Test
    void objectUri_DoesNotDoubleEncode() {
        URI uri = service.objectUri("configs/1/a b.txt");

        assertFalse(uri.toString().contains("%2520"), "URI by nemělo obsahovat dvojitě enkódovanou mezeru");
        assertFalse(uri.toString().contains("%25"), "URI by nemělo obsahovat enkódovaný znak '%' - znamenalo by to dvojité enkódování");
    }

    @Test
    void objectUri_PreservesPathSeparators() {
        URI uri = service.objectUri("configs/36/soubor.pdf");

        assertEquals(
                "https://example.supabase.co/storage/v1/object/config-files/configs/36/soubor.pdf",
                uri.toString()
        );
    }

    @Test
    void objectUri_EncodesDiacritics() {
        URI uri = service.objectUri("configs/1/žádost.pdf");

        assertEquals(
                "https://example.supabase.co/storage/v1/object/config-files/configs/1/%C5%BE%C3%A1dost.pdf",
                uri.toString()
        );
    }

    @Test
    void signUploadUri_EncodesSpaceExactlyOnce() {
        URI uri = service.signUploadUri("configs/1/a b.txt");

        assertEquals(
                "https://example.supabase.co/storage/v1/object/upload/sign/config-files/configs/1/a%20b.txt",
                uri.toString()
        );
    }

    @Test
    void signDownloadUri_EncodesSpaceExactlyOnce() {
        URI uri = service.signDownloadUri("configs/1/a b.txt");

        assertEquals(
                "https://example.supabase.co/storage/v1/object/sign/config-files/configs/1/a%20b.txt",
                uri.toString()
        );
    }
}
