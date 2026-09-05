package org.config.service.storage;

public interface SupabaseStorageService {

    String uploadFile(String storagePath, byte[] content, String contentType);

    void deleteFile(String storagePath);
}