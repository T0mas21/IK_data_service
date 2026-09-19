package org.config.service;

import org.config.data.model.Config;
import org.config.data.model.File;
import org.config.dto.FileDto;
import org.config.dto.UploadUrlDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface ConfigService {

    /**
     * {@code requestedFiles} může kombinovat soubory už dříve nahrané přes signed upload URL
     * (identifikované vyplněným {@code storagePath}) a nové soubory poslané jako base64
     * v {@code content} — ty se nahrají do Supabase Storage se storagePath vygenerovanou
     * serverem po uložení configu.
     */
    Config createConfig(Config newConfig, List<FileDto> requestedFiles);

    Config findByName(String name);

    List<String> findAllNames();

    List<Config> findAll();

    /**
     * {@code requestedFiles} reprezentuje kompletní požadovaný seznam souborů configu:
     * záznam bez {@code content} musí odpovídat již existujícímu souboru (jinak beze změny),
     * záznam s vyplněným base64 {@code content} se nahraje (u shodného fileName nahradí starý
     * obsah) a existující soubory, které v seznamu chybí, se smažou. {@code null} znamená
     * "soubory needitovat".
     */
    Config updateConfig(String name, Config updatedConfig, List<FileDto> requestedFiles);

    void deleteByName(String name);

    File addFileToConfig(Long configId, MultipartFile multipartFile);

    void removeFileFromConfig(Long configId, Long fileId);

    /**
     * Vytvoří signed upload URL pro nahrání souboru přímo do Supabase Storage, bez vytvoření
     * záznamu souboru v DB — ten vznikne až zavoláním {@link #registerFile}.
     */
    UploadUrlDto createUploadUrl(Long configId, String fileName, String fileType);

    /**
     * Vytvoří signed upload URL ještě předtím, než config v DB existuje — cesta v úložišti se
     * odvozuje z {@code configName} místo z {@code configId}, protože ten v tuto chvíli ještě
     * neexistuje. Po nahrání bajtů se {@code storagePath} z odpovědi pošle v {@code files} rovnou
     * v požadavku na {@link #createConfig(Config, List)}.
     */
    UploadUrlDto createUploadUrlForNewConfig(String configName, String fileName, String fileType);

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
