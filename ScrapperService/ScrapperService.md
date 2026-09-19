# ScrapperService

Umožňuje stahovat obsah webových stránek (tabulky, čistý text a odkazy na soubory ke stažení). Bezstavová služba - žádná DB, žádná perzistence, jen on-demand scraping přes Jsoup.

Port: **8082**, base cesta REST API: `/scrapper_api/scrape` (pozor: samotný scraping endpoint je `/scrapper_api/scrape/scrape` - `@RequestMapping` na kontroleru i `@PostMapping` metody obsahují `/scrape`).

## Endpointy (`ScrapperApi`)

- `POST /scrapper_api/scrape/scrape` - vstup `ScrapeRequestDto` (`url` - povinné, validní URL; `timeoutSeconds` - min. 1; `userAgent` - povinné; `strategy` - nepovinné, `null` = spustí všechny strategie najednou). Volá `ScrapperService.scrape(...)`, návratový typ je `Object` (tvar odpovědi se liší podle zvolené strategie).
- `GET /scrapper_api/scrape/strategies` - vrátí `{"strategies": [...]}` se všemi hodnotami `ScrapperStrategy`.

## Strategie (`ScrapperStrategy`)

- `EXTRACT_TABLES` - vrátí `{"tables": [...]}"`. Každá `<table>` na stránce se převede na `columns` (názvy hlaviček, `column_N` jako fallback) a `row` (mapa sloupec → hodnota buňky; pokud buňka obsahuje odkaz, hodnota je `[{"name":..., "url":...}]` místo prostého textu).
- `EXTRACT_TEXT` - vrátí `{"text": "..."}`. Implementace (`extractText`) nejdřív odstraní navigační/skryté prvky (`script,style,nav,header,footer,form,...`, `[aria-hidden=true]`, `display:none` apod.), pak vytáhne text z textových bloků (`p,h1-h6,li,blockquote,...`) uvnitř nejpravděpodobnějšího hlavního kontejneru (`main,article,[role=main],.content,#content`, jinak `body`). Fallback: pokud tímhle nezíská nic, vezme prostý text stránky rozdělený po řádcích.
- `DOWNLOAD_FILE` - vrátí `{"files": [{"name":..., "url":...}]}`. Odkaz se považuje za soubor ke stažení, pokud má atribut `download`, končí na typickou příponu (`fileExtensionsRegex`: pdf/zip/rar/7z/csv/xls(x)/doc(x)/txt/ppt(x)/xml/json/mp3/mp4/exe/apk), je uvnitř `<table>`, nebo text odkazu zmiňuje typ souboru.
- `strategy == null` - spustí všechny tři a vrátí kombinovaný výsledek `{"text":..., "tables":..., "files":...}`.

Chyba při stažení URL (`Jsoup.connect(...).get()` selže) se jen zaloguje na stderr a vrátí se prázdný výsledek (`Map.of()`), request neshodí.

## Kdo volá tuhle službu

Interně `ConfigService` (`ScrapperServiceClientImpl.scrapeText`) při reindexaci configu - vždy se strategií `EXTRACT_TEXT`, s fallbackem na výchozí timeout/user-agent, pokud nejsou v configu vyplněné. Chyba na této službě se v `ConfigService` jen loguje (config se uloží i bez naskrapovaného textu).
