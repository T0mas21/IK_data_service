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

## Nahrávání obsahu souboru do Supabase Storage je asynchronní (best-effort)

`createConfig`/`updateConfig`/`addFileToConfig` uloží metadata souboru (`storagePath`, `fileName`, `fileType`) do DB synchronně a hned vrátí odpověď klientovi - skutečné nahrání bajtů do Supabase Storage (`SupabaseStorageService.uploadFileAsync`, `@Async`, viz `@EnableAsync` na `ConfigService` app třídě) běží až po odpovědi na pozadí. Chyba uploadu se jen loguje (`log.warn`), nikdy nezpůsobí chybu requestu - stejný princip jako u reindexace.

**Důsledek pro kontrakt:** úspěšná odpověď na create/update/upload **negarantuje**, že obsah souboru je už fyzicky v Supabase Storage - jen že metadata jsou uložená a upload byl odeslán na pozadí. Signed download URL vytvořené těsně po create/update proto teoreticky může chvíli ukazovat na ještě nenahraný soubor. Důvod je popsaný v [.claude/VyreseneProblemy.md](../.claude/VyreseneProblemy.md) - Render free tier má vlastní gateway timeout kratší než cokoliv nastavitelné na naší straně, takže synchronní upload velkého souboru uvnitř HTTP requestu riskoval 502 i s nastaveným connect/read timeoutem.

## Timeouty na odchozích HTTP volání

Všechny tři outbound `RestTemplate` klienty (`SupabaseStorageServiceImpl`, `ScrapperServiceClientImpl`, `VectorServiceClientImpl`) mají explicitní connect timeout 10 s a read timeout 30 s (`setConnectTimeout`/`setReadTimeout` na `RestTemplateBuilder` v konstruktoru). Bez toho `RestTemplate` čeká na odpověď neomezeně dlouho.

Tohle byla příčina opakovaných 502 z Cloudflare/Renderu při `createConfig`/`updateConfig`: appka visela na některém z odchozích volání (Supabase Storage při uploadu souboru, nebo ScrapperService/VectorService v `reindexConfig` - to druhé se volá **při každém** create/update, ne jen při uploadu souboru) bez jakéhokoli logu, dokud Cloudflare po pár sekundách spojení k Renderu nezabil. Instance přitom v Render logu ukazuje jen poslední proběhlý Hibernate dotaz (typicky `INSERT` configu) a nic dál - žádná výjimka, protože request nikdy neskončil.

## Testy

JUnit 5 + Mockito, `MockitoSettings(strictness = LENIENT)`. Abstraktní `Base*Test` třídy per vrstva (`BaseConfigServiceTest`, `BaseConfigFacadeTest`, `BaseConfigApiTest`) s mocky/DI, konkrétní testovací třída per akce (`ConfigServiceCreateTest`, `ConfigServiceUpdateTest`, `ConfigServiceFileUploadTest`, ...). Testy nad repozitáři (`org.config.unit.repository`) běží nad H2 přes `@SpringBootTest` (ne čisté unit testy).
