package org.config.service;

import org.config.data.model.Config;
import org.config.data.model.File;
import org.config.dto.UploadUrlDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface ConfigService {

    Config createConfig(Config newConfig);

    Config findByName(String name);

    List<String> findAllNames();

    List<Config> findAll();

    Config updateConfig(String name, Config updatedConfig);

    void deleteByName(String name);

    File addFileToConfig(Long configId, MultipartFile multipartFile);

    void removeFileFromConfig(Long configId, Long fileId);

    /**
     * Vytvoří signed upload URL pro nahrání souboru přímo do Supabase Storage, bez vytvoření
     * záznamu souboru v DB — ten vznikne až zavoláním {@link #registerFile}.
     */
    UploadUrlDto createUploadUrl(Long configId, String fileName, String fileType);

    /**
     * Zaregistruje soubor, který byl už nahrán přímo do Supabase Storage (viz {@link #createUploadUrl}).
     */
    File registerFile(Long configId, String storagePath, String fileName, String fileType);

    /**
     * Vrátí jednorázovou signed URL pro stažení souboru identifikovaného kombinací configId + jméno
     * souboru (jméno je unikátní jen v rámci daného configu, viz {@code ux_config_files_config_id_file_name}).
     */
    String getFileDownloadUrl(Long configId, String fileName);
}
