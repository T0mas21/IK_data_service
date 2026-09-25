# ScrapperService

Umožňuje stahovat obsah webových stránek (tabulky, čistý text a odkazy na soubory ke stažení). Bezstavová služba - žádná DB, žádná perzistence, jen on-demand scraping přes Jsoup.

Port: **8082**, base cesta REST API: `/scrapper_api/scrape` (pozor: samotný scraping endpoint je `/scrapper_api/scrape/scrape` - `@RequestMapping` na kontroleru i `@PostMapping` metody obsahují `/scrape`).

## Endpointy (`ScrapperApi`)

- `POST /scrapper_api/scrape/scrape` - vstup `ScrapeRequestDto` (`url` - povinné, validní URL; `timeoutSeconds` - min. 1; `userAgent` - povinné; `strategy` - nepovinné, `null` = spustí všechny strategie najednou). Volá `ScrapperService.scrape(...)`, návratový typ je `Object` (tvar odpovědi se liší podle zvolené strategie).
- `GET /scrapper_api/scrape/strategies` - vrátí `{"strategies": [...]}` se všemi hodnotami `ScrapperStrategy`.

## Strategie (`ScrapperStrategy`)

- `EXTRACT_TABLES` - vrátí `{"tables": [...]}"`. Každá `<table>` na stránce se převede na `columns` (názvy hlaviček, `column_N` jako fallback) a `row` (mapa sloupec → hodnota buňky; pokud buňka obsahuje odkaz, hodnota je `[{"name":..., "url":...}]` místo prostého textu).
- `EXTRACT_TEXT` - vrátí `{"text": "..."}`. Implementace (`extractText`) nejdřív odstraní navigační/skryté prvky (`script,style,nav,header,footer,form,...`, `[aria-hidden=true]`, `display:none` apod.), pak vytáhne text z textových bloků (`p,h1-h6,li,blockquote,...`) uvnitř nejpravděpodobnějšího hlavního kontejneru (`main,article,[role=main],.content,#content`, jinak `body`). Fallback: pokud tímhle nezíská nic, vezme prostý text stránky rozdělený po řádcích.
- `DOWNLOAD_FILE` - vrátí `{"files": [{"name":..., "url":...}]}`. Odkaz se považuje za soubor ke stažení **výhradně** podle přípony `href` (i s případným query stringem, např. `...soubor.pdf?v=1` platí) - jen `.pdf` a `.txt`. Žádné jiné signály (atribut `download`, umístění v `<table>`, text odkazu) se už neberou v potaz - záměrně, aby se do `files` nedostávaly obrázky, videa, archivy, office formáty ani běžné webové odkazy jen proto, že zmiňují "pdf" v textu nebo mají `download` atribut. Kdo potřebuje širší množinu typů, musí rozšířit `fileExtensionsRegex` v `ScrapperServiceImpl.extractDownloadLinks`.
- `strategy == null` - spustí všechny tři a vrátí kombinovaný výsledek `{"text":..., "tables":..., "files":...}`.

Chyba při stažení URL (`Jsoup.connect(...).get()` selže) se jen zaloguje na stderr a vrátí se prázdný výsledek (`Map.of()`), request neshodí.

## Kdo volá tuhle službu

Interně `ConfigService` (`ScrapperServiceClientImpl.scrapeText`) při reindexaci configu - vždy se strategií `EXTRACT_TEXT`, s fallbackem na výchozí timeout/user-agent, pokud nejsou v configu vyplněné. Chyba na této službě se v `ConfigService` jen loguje (config se uloží i bez naskrapovaného textu).

## Testy

JUnit 5 (+ AssertJ ze `spring-boot-starter-test`). `ScrapperServiceImplExtractDownloadLinksTest` testuje `extractDownloadLinks` přímo (metoda je kvůli tomu package-private, ne `private`) nad `Jsoup.parse(...)` HTML fixture, bez síťového volání.
