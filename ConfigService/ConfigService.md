# ConfigService

Umožňuje vytvářet a konfigurovat datové zdroje (config) - odkaz na webovou stránku, vlastní text a/nebo nahrané soubory. Po vytvoření/editaci configu spouští (best-effort) přeindexování obsahu ve VectorService a případné scrapnutí URL přes ScrapperService.

Port: **8081**, base cesta REST API: `/scrapper_api/config`.

## Vrstvení

`rest` (`ConfigApi`) → `facade` (`ConfigFacade`/`ConfigFacadeImpl`) → `service` (`ConfigService`/`ConfigServiceImpl`) → `repository` (`ConfigRepository`, `FileRepository`). DTO (`record`, balíček `dto`) se na JPA entity (balíček `data.model`) mapují přes `mappers` (`ConfigMapperImpl`, `FileMapperImpl`) - mapují se ale jen skalární pole, soubory (`files`) se mezi vrstvami posílají jako `List<FileDto>` napřímo (viz níže), ne přes entitní mapping.

## Datový model

- `Config` (tabulka `configs`): `id`, `name` (unikátní, povinné), `description`, `timeout` (>= 0), `userAgent`, `url`, `customText`, `webText`, `tables` (viz níže), `files` (`@OneToMany`, `cascade = ALL`, `orphanRemoval = true`), `createdAt`/`updatedAt`.
- `File` (tabulka `config_files`, unique `(config_id, file_name)`): `id`, `storagePath`, `fileName`, `fileType`, `createdAt`. **Neobsahuje binární obsah** - ten je v Supabase Storage, DB drží jen metadata.
- `ConfigDto`: `name`, `description`, `timeout`, `userAgent`, `url`, `customText`, `webText`, `tables: List<Map<String, Object>>`, `files: List<FileDto>`.
- `FileDto`: `id`, `fileName`, `storagePath`, `fileType`, `content` (base64, nullable), `sourceUrl` (nullable, jen při create - viz níže).

## Práce se soubory - klíčový kontrakt

- `fileName` - vždy povinné, identifikuje soubor v rámci configu.
- `content` - base64 obsah; vyplněný = nový/nahrazovaný soubor, který se nahraje do Supabase Storage. `storagePath` se v tomto případě **vždy generuje na serveru** (`buildStoragePath`, tvar `configs/<configId>/<uuid>_<fileName>`) - klient ho neposílá.
- `storagePath` - použije se jen u staršího flow, kdy klient soubor nahrál sám přímo do Supabase Storage přes signed upload URL (`POST /files/upload-url` → nahrání bajtů → referuje se v `files` přes `storagePath`+`fileName`, bez `content`).
- `fileType` - nepovinné; pokud je vyplněné, musí být validní MIME typ `typ/podtyp` (jinak `MediaType.parseMediaType` v `SupabaseStorageServiceImpl.uploadFile` spadne s neošetřenou `InvalidMimeTypeException` → 500, ne 400 - **známá mezera**, neplatný `fileType` by měl vracet 400).
- `sourceUrl` - **jen při create**; vyplněný = server si sám stáhne obsah souboru z téhle URL a nahraje ho do Supabase Storage - klient posílá jen název a odkaz, ne obsah. Typicky odkaz, který `ScrapperService` strategie `DOWNLOAD_FILE` našel na webové stránce. Stažení i upload probíhá **asynchronně na pozadí** (`WebFileDownloadService.downloadAndStoreAsync`, balíček `org.config.service.webfile`) - stejný best-effort princip jako `uploadFileAsync`: chyba (stažení i uploadu) se jen loguje, request vrátí odpověď hned po uložení metadat.

### Create (`POST /scrapper_api/config`, JSON) vs. Edit (`PUT /scrapper_api/config/{name}`)

- **Create**: `files` může kombinovat všechny tři flow (staré `storagePath`, `content`, i nové `sourceUrl`) v jednom požadavku. Soubor musí mít vždy právě jedno z `content`/`storagePath`/`sourceUrl` (jinak 400). Config se nejdřív uloží (kvůli DB id), teprve pak se base64 soubory nahrají/soubory z `sourceUrl` stáhnou a připojí.
- **`sourceUrl` platí jen pro create** - `updateConfig`/`syncFiles` ho nezpracovává (soubor bez `content`, který neodpovídá existujícímu souboru, skončí standardní chybou "nebyl nalezen a nemá obsah k nahrání").
- **Edit**: `files` reprezentuje **kompletní požadovaný seznam** souborů configu - záznam bez `content` musí odpovídat existujícímu souboru (jinak 400, "beze změny"); záznam s `content` u shodného `fileName` **nahradí** starý obsah (smaže starý soubor ze storage i DB, nahraje nový); existující soubory, které v požadavku vůbec nejsou, se **smažou**. `requestedFiles == null` znamená "soubory vůbec needitovat".
- **Validace duplicit**: pokud `files` v jednom requestu (create i edit) obsahuje dva záznamy se **stejným `fileName`**, vrátí se 400 a nic se neuloží/nezmění (`ConfigServiceImpl.validateNoDuplicateFileNames`, volá se před jakoukoli mutací). Netýká se to legitimního nahrazení existujícího souboru (jeden záznam v requestu odpovídající souboru, co už u configu je) - to zůstává validní "replace".
- Existuje i multipart create (`POST /scrapper_api/config`, `multipart/form-data`) a přímé file-endpointy (`POST/DELETE /{configId}/files`) pro editaci souborů mimo tento JSON kontrakt.

## Reindexace

Po každém create/update configu (i po přidání/smazání souboru) se volá `reindexConfig`: pošle `customText` + text webové stránky + tabulky z webu + seznam souborů do `VectorServiceClient.indexConfig` (POST `${vector-service.url}/scrapper_api/vector/index`). Text webové stránky se získá takto:

- pokud klient v requestu vyplnil `webText` (sám si stránku naskrapoval, případně dal uživateli možnost výsledek upravit/vybrat), použije se **přímo** - `ScrapperService` se vůbec nevolá;
- jinak (zpětná kompatibilita se staršími configy) se dorovná automatickým scrapováním přes `ScrapperServiceClient.scrapeText` (POST `${scrapper-service.url}/scrapper_api/scrape/scrape`, strategie `EXTRACT_TEXT`) na `url` daného configu.

Obě volání (scraper i vector indexace) jsou **best-effort** - chyby se jen logují (`log.warn`), nikdy neshodí operaci nad configem.

### Soubory a tabulky z webu - kdo co dělá

ConfigService **sám o sobě nescrapuje soubory ani tabulky z webové stránky** - to dělá klient (typicky integrace Metada) ještě před voláním create/update, pomocí `ScrapperService` strategií `DOWNLOAD_FILE`/`EXTRACT_TABLES` (viz `ScrapperService.md`), s možností u uživatele vybrat/smazat nalezené položky. Teprve výsledek se pošle do ConfigService:

- **soubory z webu** se dají poslat dvěma způsoby: (a) klient si je sám stáhne a pošle jako ručně nahrané soubory - `files: List<FileDto>` s vyplněným `content` (base64); nebo (b) klient pošle jen `fileName` + `sourceUrl` a server si obsah stáhne sám (jen při create, viz výše).
- **tabulky z webu** se posílají v poli `tables` v **přesně stejném formátu**, jaký vrací `ScrapperService` pro strategii `EXTRACT_TABLES` (pole `tables` z jeho odpovědi - seznam `{"table": {"columns": [...], "row": [...]}}`, viz `ScrapperService.md`). V DB se ukládá jako serializovaný JSON text (`Config.tables`, sloupec `TEXT`) a stejný syrový JSON se posílá dál do `VectorServiceClient.indexConfig` jako pole `tables` - VectorService si ho sám parsuje a převádí na čitelný text pro indexaci (viz `VectorService.md`).
- **text webové stránky** se posílá v poli `webText` (viz výše) - typicky stejný text, který klient dostal z `ScrapperService` strategie `EXTRACT_TEXT`.

### Čtení configu vrací i obsah souborů

`GET /scrapper_api/config/{name}` vrací u každého souboru v `files` i jeho **base64 `content`** - obsah se při čtení stáhne ze Supabase Storage (`ConfigFacadeImpl.withFileContents` → `ConfigService.downloadFileContent`). Soubor, který se nepodaří stáhnout, zůstane s `content: null` (best-effort, jen `log.warn`) - jeden nedostupný soubor neshodí čtení celého configu.

**`GET /scrapper_api/config` (všechny configy) obsah souborů záměrně nevrací** (`content` zůstává `null`) - stahování všech souborů všech configů by odpověď neúnosně nafouklo a na Render free tieru riskovalo gateway timeout. Pro obsah konkrétního souboru bez čtení celého configu je levnější `GET /{configId}/files/{fileName}` (redirect na signed URL, bajty aplikací vůbec neprochází).

## Nahrávání obsahu souboru do Supabase Storage je asynchronní (best-effort)

`createConfig`/`updateConfig`/`addFileToConfig` uloží metadata souboru (`storagePath`, `fileName`, `fileType`) do DB synchronně a hned vrátí odpověď klientovi - skutečné nahrání bajtů do Supabase Storage (`SupabaseStorageService.uploadFileAsync`, `@Async`, viz `@EnableAsync` na `ConfigService` app třídě) běží až po odpovědi na pozadí. Chyba uploadu se jen loguje (`log.warn`), nikdy nezpůsobí chybu requestu - stejný princip jako u reindexace.

**Důsledek pro kontrakt:** úspěšná odpověď na create/update/upload **negarantuje**, že obsah souboru je už fyzicky v Supabase Storage - jen že metadata jsou uložená a upload byl odeslán na pozadí. Signed download URL vytvořené těsně po create/update proto teoreticky může chvíli ukazovat na ještě nenahraný soubor. Důvod je popsaný v [.claude/VyreseneProblemy.md](../.claude/VyreseneProblemy.md) - Render free tier má vlastní gateway timeout kratší než cokoliv nastavitelné na naší straně, takže synchronní upload velkého souboru uvnitř HTTP requestu riskoval 502 i s nastaveným connect/read timeoutem.

## Timeouty na odchozích HTTP volání

Všechny tři outbound `RestTemplate` klienty (`SupabaseStorageServiceImpl`, `ScrapperServiceClientImpl`, `VectorServiceClientImpl`) mají explicitní connect timeout 10 s a read timeout 30 s (`setConnectTimeout`/`setReadTimeout` na `RestTemplateBuilder` v konstruktoru). Bez toho `RestTemplate` čeká na odpověď neomezeně dlouho.

Tohle byla příčina opakovaných 502 z Cloudflare/Renderu při `createConfig`/`updateConfig`: appka visela na některém z odchozích volání (Supabase Storage při uploadu souboru, nebo ScrapperService/VectorService v `reindexConfig` - to druhé se volá **při každém** create/update, ne jen při uploadu souboru) bez jakéhokoli logu, dokud Cloudflare po pár sekundách spojení k Renderu nezabil. Instance přitom v Render logu ukazuje jen poslední proběhlý Hibernate dotaz (typicky `INSERT` configu) a nic dál - žádná výjimka, protože request nikdy neskončil.

## Testy

JUnit 5 + Mockito, `MockitoSettings(strictness = LENIENT)`. Abstraktní `Base*Test` třídy per vrstva (`BaseConfigServiceTest`, `BaseConfigFacadeTest`, `BaseConfigApiTest`) s mocky/DI, konkrétní testovací třída per akce (`ConfigServiceCreateTest`, `ConfigServiceUpdateTest`, `ConfigServiceFileUploadTest`, ...). Testy nad repozitáři (`org.config.unit.repository`) běží nad H2 přes `@SpringBootTest` (ne čisté unit testy).
