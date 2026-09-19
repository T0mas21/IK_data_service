# ConfigService

Umožňuje vytvářet a konfigurovat datové zdroje (config) - odkaz na webovou stránku, vlastní text a/nebo nahrané soubory. Po vytvoření/editaci configu spouští (best-effort) přeindexování obsahu ve VectorService a případné scrapnutí URL přes ScrapperService.

Port: **8081**, base cesta REST API: `/scrapper_api/config`.

## Vrstvení

`rest` (`ConfigApi`) → `facade` (`ConfigFacade`/`ConfigFacadeImpl`) → `service` (`ConfigService`/`ConfigServiceImpl`) → `repository` (`ConfigRepository`, `FileRepository`). DTO (`record`, balíček `dto`) se na JPA entity (balíček `data.model`) mapují přes `mappers` (`ConfigMapperImpl`, `FileMapperImpl`) - mapují se ale jen skalární pole, soubory (`files`) se mezi vrstvami posílají jako `List<FileDto>` napřímo (viz níže), ne přes entitní mapping.

## Datový model

- `Config` (tabulka `configs`): `id`, `name` (unikátní, povinné), `description`, `timeout` (>= 0), `userAgent`, `url`, `customText`, `files` (`@OneToMany`, `cascade = ALL`, `orphanRemoval = true`), `createdAt`/`updatedAt`.
- `File` (tabulka `config_files`, unique `(config_id, file_name)`): `id`, `storagePath`, `fileName`, `fileType`, `createdAt`. **Neobsahuje binární obsah** - ten je v Supabase Storage, DB drží jen metadata.
- `ConfigDto`: `name`, `description`, `timeout`, `userAgent`, `url`, `customText`, `files: List<FileDto>`.
- `FileDto`: `id`, `fileName`, `storagePath`, `fileType`, `content` (base64, nullable).

## Práce se soubory - klíčový kontrakt

- `fileName` - vždy povinné, identifikuje soubor v rámci configu.
- `content` - base64 obsah; vyplněný = nový/nahrazovaný soubor, který se nahraje do Supabase Storage. `storagePath` se v tomto případě **vždy generuje na serveru** (`buildStoragePath`, tvar `configs/<configId>/<uuid>_<fileName>`) - klient ho neposílá.
- `storagePath` - použije se jen u staršího flow, kdy klient soubor nahrál sám přímo do Supabase Storage přes signed upload URL (`POST /files/upload-url` → nahrání bajtů → referuje se v `files` přes `storagePath`+`fileName`, bez `content`).
- `fileType` - nepovinné; pokud je vyplněné, musí být validní MIME typ `typ/podtyp` (jinak `MediaType.parseMediaType` v `SupabaseStorageServiceImpl.uploadFile` spadne s neošetřenou `InvalidMimeTypeException` → 500, ne 400 - **známá mezera**, neplatný `fileType` by měl vracet 400).

### Create (`POST /scrapper_api/config`, JSON) vs. Edit (`PUT /scrapper_api/config/{name}`)

- **Create**: `files` může kombinovat oba flow (staré `storagePath` i nové `content`) v jednom požadavku. Soubor musí mít vždy buď `content`, nebo `storagePath` (jinak 400). Config se nejdřív uloží (kvůli DB id), teprve pak se base64 soubory nahrají a připojí.
- **Edit**: `files` reprezentuje **kompletní požadovaný seznam** souborů configu - záznam bez `content` musí odpovídat existujícímu souboru (jinak 400, "beze změny"); záznam s `content` u shodného `fileName` **nahradí** starý obsah (smaže starý soubor ze storage i DB, nahraje nový); existující soubory, které v požadavku vůbec nejsou, se **smažou**. `requestedFiles == null` znamená "soubory vůbec needitovat".
- Existuje i multipart create (`POST /scrapper_api/config`, `multipart/form-data`) a přímé file-endpointy (`POST/DELETE /{configId}/files`) pro editaci souborů mimo tento JSON kontrakt.

## Reindexace

Po každém create/update configu (i po přidání/smazání souboru) se volá `reindexConfig`: naskrapuje aktuální `url` přes `ScrapperServiceClient.scrapeText` (POST `${scrapper-service.url}/scrapper_api/scrape/scrape`) a pošle `customText` + naskrapovaný text + seznam souborů do `VectorServiceClient.indexConfig` (POST `${vector-service.url}/scrapper_api/vector/index`). Obě volání jsou **best-effort** - chyby se jen logují (`log.warn`), nikdy neshodí operaci nad configem.

## Testy

JUnit 5 + Mockito, `MockitoSettings(strictness = LENIENT)`. Abstraktní `Base*Test` třídy per vrstva (`BaseConfigServiceTest`, `BaseConfigFacadeTest`, `BaseConfigApiTest`) s mocky/DI, konkrétní testovací třída per akce (`ConfigServiceCreateTest`, `ConfigServiceUpdateTest`, `ConfigServiceFileUploadTest`, ...). Testy nad repozitáři (`org.config.unit.repository`) běží nad H2 přes `@SpringBootTest` (ne čisté unit testy).
