# VectorService

Umožňuje vektorizovat obsah datového zdroje (config) tak, aby v něm jazykový model mohl vyhledávat na základě sémantického významu (RAG). Postavené na `langchain4j` + `pgvector` (Supabase Postgres).

Port: **8083**, base cesta REST API: `/scrapper_api/vector`.

## Endpointy (`VectorApi`)

- `POST /scrapper_api/vector/index` - vstup `IndexRequestDto` (`configId` - povinné; `customText`, `scrapedText` - nepovinné; `tables` - nepovinné, syrový JSON ve stejném formátu jako pole `tables` z `ScrapperService` strategie `EXTRACT_TABLES`, viz níže; `files: List<FileRefDto>` - nepovinné, každý `fileId`+`fileName`+`storagePath` povinné, `fileType` volitelné). Vrátí `IndexResponseDto` s počtem zaindexovaných chunků.
- `DELETE /scrapper_api/vector/configs/{configId}` - smaže všechny chunky daného configu z embedding store.
- `POST /scrapper_api/vector/search` - vstup `SearchRequestDto` (`configId` - nepovinné, omezí hledání na jeden config; `query` - povinné; `topK` - nepovinné, default 5). Vrátí `SearchResponseDto` se seznamem `SearchResultDto` (`text`, `score`, `sourceType`, `fileId`, `fileName`).

## Indexace (`IndexingServiceImpl`)

1. Nejdřív smaže všechny existující chunky daného `configId` z embedding store (`removeAll(metadataKey("configId")...)`) - reindexace je vždy úplná náhrada, ne přírůstková.
2. Text se štěpí na chunky přes `DocumentSplitters.recursive(500, 50)` (max 500 znaků na segment, 50 znaků překryv) zvlášť pro `customText`, `scrapedText`, každý řádek tabulky z `tables` a text extrahovaný z každého souboru v `files`.
3. Každý chunk nese metadata `configId`, `sourceType` (`CUSTOM_TEXT`/`SCRAPED`/`TABLE`/`FILE`), a u souborů navíc `fileId`+`fileName`.
4. Text ze souboru se získá stažením bajtů ze Supabase Storage (`SupabaseFileClient.downloadFile(storagePath)`) a extrakcí přes Apache Tika (`TextExtractionServiceImpl`, `ApacheTikaDocumentParser`). Selhání extrakce/stažení jednoho souboru se jen zaloguje (`log.warn`) a soubor se v indexu přeskočí - nezastaví indexaci zbytku configu.
5. `tables` (syrový JSON, stejný formát jako `ScrapperService` strategie `EXTRACT_TABLES` - pole `{"table": {"columns": [...], "row": [...]}}`) se parsuje a **každý řádek tabulky** se převede na vlastní čitelný text ve tvaru `"Sloupec1: hodnota1, Sloupec2: hodnota2, ..."` (buňka s odkazem - `{"name":..., "url":...}` - se zformátuje jako `"název (url)"`), teprve ten se chunkuje/embeduje. Neplatný/needitovatelný JSON v `tables` se jen zaloguje a přeskočí (best-effort) - nezastaví indexaci ostatních zdrojů.
6. Embeddingy počítá `AllMiniLmL6V2QuantizedEmbeddingModel` (lokální, žádné externí volání embedding API).

## Vyhledávání (`SearchServiceImpl`)

Zaembeduje dotaz stejným modelem, volitelně filtruje podle `configId` (`metadataKey("configId").isEqualTo(...)`), vrátí top-K nejbližších chunků s `score`.

## Uložiště embeddingů (`EmbeddingStoreConfig`)

`PgVectorEmbeddingStore` nad vlastním malým Hikari poolem (`vectorStoreDataSource`, max. 3 spojení, min. idle 0) - **záměrně** omezeno, protože Supabase pooler má strop na počet současných spojení sdílený se všemi ostatními službami projektu. `useIndex(false)` - při aktuálním (malém, diplomka-scale) objemu dat stačí sekvenční scan, IVFFlat index by navíc bylo potřeba trénovat až na neprázdné tabulce.

## Kdo volá tuhle službu

Interně `ConfigService` (`VectorServiceClientImpl`) po každém create/update configu (`indexConfig`) a při smazání configu (`deleteConfig`). Obě volání jsou z pohledu `ConfigService` best-effort - chyba se jen loguje.
