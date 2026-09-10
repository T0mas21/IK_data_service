package org.vector.service.storage;

public interface SupabaseFileClient {
    byte[] downloadFile(String storagePath);
}
