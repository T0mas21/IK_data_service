# Vyřešené problémy

Log vyřešených produkčních bugů a jejich diagnostiky - ne changelog změn v kódu (ten je v gitu), ale **postup, jak se na to přišlo**, aby se při podobném příznaku příště nezačínalo od nuly. Nejnovější nahoře.

---

## Upload souboru s mezerou/diakritikou v názvu tiše selhával - dvojité URL enkódování cesty

**Datum:** 2026-10-01
**Služba:** ConfigService

### Příznak

Config se vytvořil v pořádku (DB záznamy configu i souboru existují), ale skutečný obsah souboru se do Supabase Storage nenahrál. V logu (vidět až díky asynchronnímu uploadu, viz záznam níže - dřív by to byl synchronní 502) byla chyba:

```
Asynchronní nahrání souboru .../uuid_Navrh staze.pdf do Supabase Storage selhalo: 502 BAD_GATEWAY
"...{"statusCode":"400","error":"InvalidKey","message":"Invalid key: .../uuid_Navrh%20staze.pdf"...}"
```

### Diagnostický postup

1. **Nevěř první hypotéze jen podle toho, co "vypadá podezřele".** Mezera v názvu souboru vypadala jako viník, ale Supabase Storage mezery v klíči **povoluje** (ověřeno websearchem na skutečný regex validace v `supabase/storage` repu) - šlo by o unáhlený závěr bez ověření.
2. **Všimni si, že chybová hláška obsahuje `%20`, ne mezeru.** To napovídá, že cesta prošla URL enkódováním ještě předtím, než ji vidělo Supabase - otázka je, kolikrát.
3. **Projdi kód cesty od sestavení URL až po odeslání requestu.** `objectUrl()` ručně volal `UriUtils.encodePath(...)` (mezera → `%20`) a výsledek vrátil jako obyčejný `String`. Ten se pak posílal přes `RestTemplate.exchange(String url, ...)` - a tahle varianta si URL **enkóduje znovu sama** (neví, že už enkódovaná je), takže `%20` se stane `%2520`.
4. **Porovnej s dokumentovanými zakázanými znaky.** Znak `%` je u Supabase Storage na seznamu zakázaných znaků v klíči - `%2520` tedy obsahuje doslovné `%`, které validace odmítne jako `InvalidKey`. Sedí to přesně na pozorovanou chybu.

### Kořenová příčina

`SupabaseStorageServiceImpl` enkódoval cestu k souboru ručně (`UriUtils.encodePath`) a pak ji poslal jako `String` do `RestTemplate.exchange(String url, ...)`, která cestu enkóduje podruhé - výsledný klíč obsahoval dvojitě enkódované znaky (`%2520` místo `%20`), což Supabase odmítla kvůli zakázanému znaku `%`.

### Oprava

Cesta se teď sestavuje přes `UriComponentsBuilder` a enkóduje se **přesně jednou** (`.build().encode(UTF_8).toUri()`), výsledek je rovnou `java.net.URI` objekt. Volá se přes `RestTemplate.exchange(URI, ...)` přetížení, které už enkódovanou `URI` znovu neenkóduje. Test: ověřit na konkrétních vstupech (mezera, diakritika, víceúrovňová cesta), že výsledné URI obsahuje `%20`/`%C5%BE` přesně jednou a nikdy ne `%25` (= dvojité enkódování).

### Poučení do budoucna

Nikdy nekombinovat ruční URL enkódování (`UriUtils.encodePath` a podobné) s `RestTemplate`/`UriComponentsBuilder` metodami, které berou `String` URL - ty si enkódují samy. Buď enkóduj ručně a pošli jako `URI` objekt (přes `exchange(URI, ...)`), nebo nech enkódování úplně na `UriComponentsBuilder`/`RestTemplate` a posílej neenkódovaný vstup. Nikdy obojí najednou.

---

## 502 přetrvávalo i po nastavení timeoutu - Render free tier má vlastní (kratší) gateway timeout

**Datum:** 2026-09-30
**Služba:** ConfigService

### Příznak

I po nasazení opravy "tichých 502" (connect/read timeout na `RestTemplate`, viz záznam níže) dál chodily 502 při `createConfig`/`updateConfig` s nahrávaným souborem (base64 `content`). Render `app` log měl **stejný vzorec jako předtím** - poslední řádek `INSERT INTO configs`, pak ticho - ale doba trvání requestu u klienta (6,4 s) byla mnohem kratší než nově nastavený timeout (connect 10 s / read 30 s).

### Diagnostický postup

1. **Nejdřív ověř, že oprava vůbec běží v produkci.** `git log origin/main..HEAD` / `git log HEAD..origin/main` (žádný rozdíl = nasazeno) a `mcp__render__list_deploys` - najdi deploy s daným commitem a jeho `status: live` + čas nasazení. Teprve pak má smysl ptát se "proč oprava nefunguje".
2. **Poměř čas selhání s nastaveným timeoutem.** Pokud appka spadne na 502 **dřív**, než by mohl vypršet náš vlastní `RestTemplate` timeout, náš timeout není příčina ani řešení tohoto konkrétního selhání - hledej jinde.
3. **Zkontroluj metriky instance** (`mcp__render__get_metrics`, `memory_usage`/`memory_limit`/`cpu_usage`/`cpu_limit`) v přesném okně selhání - vyluč OOM/restart. V tomhle případě paměť byla v pohodě (258→272 MB z 512 MB), žádný restart - ale `cpu_limit` byl jen **0.15** (15 % jednoho jádra - Render free tier).
4. **Všimni si hlavičky odpovědi.** `x-render-origin-server=[Render]` v 502 odpovědi znamená, že 502 generuje **Renderova vlastní edge/proxy vrstva**, ne jen Cloudflare - tedy platformní timeout, který naše aplikace vůbec neovlivní žádným nastavením `RestTemplate`/`RestTemplateBuilder`.
5. **Závěr:** na free tieru s CPU throttlovaným na 0.15 vCPU může i relativně malá synchronní práce uvnitř HTTP requestu (upload pár set kB do Supabase) trvat déle, než je trpělivost Renderovy vlastní gateway - a tenhle limit je kratší a mimo naši kontrolu, na rozdíl od timeoutu na odchozím `RestTemplate`.

### Kořenová příčina

Upload obsahu souboru do Supabase Storage (`SupabaseStorageServiceImpl.uploadFile`) se dělal **synchronně uvnitř HTTP requestu** v `createConfig`/`updateConfig`/`addFileToConfig`. Na silně CPU-throttlovaném free tieru riskuje jakákoliv synchronní síťová práce v requestu, že ji Renderova gateway utne dřív, než dorazí odpověď - bez ohledu na to, jaký timeout má nastavený náš vlastní HTTP klient.

### Oprava

Upload obsahu souboru přesunut na pozadí: `SupabaseStorageService.uploadFileAsync` (`@Async`, vyžaduje `@EnableAsync` na Spring Boot aplikační třídě) volá stejnou logiku jako `uploadFile`, ale chybu jen loguje (`log.warn`), nikdy ji nevrací volajícímu. Metadata souboru (`storagePath`/`fileName`/`fileType`) se uloží do DB **před** spuštěním asynchronního uploadu, takže request může odpovědět klientovi hned - stejný "best-effort" princip, jaký už měla reindexace. Kontrakt API se tím mění: úspěšná odpověď už negarantuje, že obsah souboru je fyzicky v Supabase Storage (viz `ConfigService.md`).

### Poučení do budoucna

Nastavení timeoutu na odchozím HTTP klientovi **neřeší** timeout na infrastruktuře mezi klientem a naší appkou (Cloudflare/Render edge) - pokud appka běží na prostředí s omezeným CPU/síťovou kapacitou (typicky free/hobby tier), jakákoliv netriviální synchronní práce uvnitř HTTP requestu (upload/download většího objemu dat, CPU-náročné zpracování) patří na pozadí, ne do request-response cyklu. Při podezření na "oprava timeoutu nezabrala" vždy nejdřív poměř naměřenou dobu selhání s nastaveným timeoutem - pokud selhání přijde dřív, než timeout mohl vypršet, timeout není (a nemůže být) příčina.

---

## "Tiché" 502 z Cloudflare/Renderu - timeout na odchozím HTTP volání

**Datum:** 2026-09-30
**Služba:** ConfigService

### Příznak

Klient (Metada) dostává 502 od Cloudflare při `IK_CreateConfig`/`IK_EditConfig`. Doba trvání requestu je pokaždé jiná (2 s, 6 s, 6,7 s...). V Render `app` logu je vidět jen poslední proběhlý Hibernate dotaz (typicky `INSERT INTO configs`), pak úplné ticho - žádná výjimka, žádný další log řádek. Request tedy pořád běží (appka nespadla, jen visí), ale místo, kde visí, samo o sobě nic neloguje.

### Diagnostický postup

1. **Najdi přesné okno.** Z klientského (Metada) logu vezmi timestamp requestu a `Request duration`, přepočti na UTC a zeptej se Render `list_logs` na `app` typ logů v tomhle okně (± pár sekund). Tahle služba nemá `request`-type logy, takže surová HTTP komunikace není vidět - jen aplikační (Hibernate apod.) výstup.
2. **Poslední log řádek = místo, kde se ztrácí viditelnost.** Pokud vidíš třeba `SELECT ... where name=?` a `INSERT INTO configs ...` a nic dál, znamená to, že kód doběhl přesně tam a pak pokračuje v něčem, co se nikam neloguje - typicky odchozí HTTP volání na jinou službu/třetí stranu.
3. **Projdi kód od tohohle bodu dál** a najdi všechna odchozí síťová volání (HTTP klienty), která by na tomhle místě mohla běžet - i ta, co na první pohled nesouvisí s tím, co request právě dělá (např. `reindexConfig` se volá při KAŽDÉM create/update, ne jen při uploadu souboru).
4. **Zkontroluj, jestli má každé takové volání timeout.** U `RestTemplate` postaveného přes `RestTemplateBuilder.build()` bez `setConnectTimeout`/`setReadTimeout` je výchozí `SimpleClientHttpRequestFactory` timeout `-1` = **čeká neomezeně dlouho**. To je přesně scénář "appka visí bez logu, dokud to Cloudflare/Render edge samo neutne" - Cloudflare timeout je variabilní/kratší než cokoliv na naší straně, proto se doba trvání requestu u klienta pokaždé liší.
5. **Po opravě ověř číselně, ne jen "otestoval jsem to znovu".** Pokud přidáš timeout (např. connect 10 s / read 30 s) a příští 502 má dobu trvání kratší, než jsou tyhle limity, **tahle konkrétní oprava nemohla být příčinou** - hledej další neošetřené volání se stejným vzorcem, ne že oprava "nefunguje".
6. **Jedna instance chyby = podezření na systémovou chybu.** Jakmile najdeš `RestTemplateBuilder.build()` bez timeoutu na jednom místě, projdi všechny podobné konstruktory v `client`/`service` vrstvě (copy-paste vzorec se v malém projektu obvykle opakuje).
7. **Reprodukce mimo Metadu pro izolaci proměnných.** `curl` přímo na endpoint se syntetickým payloadem stejného tvaru (velikost, chybějící/přítomná pole) rychle vyloučí hypotézy typu "je to velikostí souboru" nebo "je to konkrétním obsahem" - pokud projde rychle a v pořádku, problém je specifický pro aktuální stav/zátěž produkčního prostředí (např. dočasně pomalá navazující služba), ne pro tvar requestu samotný.

### Kořenová příčina

Tři třídy v ConfigService stavěly `RestTemplate` bez timeoutu:
- `SupabaseStorageServiceImpl` (upload/delete souborů, signed URL)
- `ScrapperServiceClientImpl` (scrapování URL v `reindexConfig`)
- `VectorServiceClientImpl` (indexace ve VectorService v `reindexConfig`)

### Oprava

`RestTemplateBuilder.setConnectTimeout(Duration.ofSeconds(10)).setReadTimeout(Duration.ofSeconds(30)).build()` na všech třech místech. Test (test-first): reflexí ověřit, že `SimpleClientHttpRequestFactory` na výsledném `RestTemplate` má `connectTimeout`/`readTimeout` > 0 (bez opravy je `-1`).

### Poučení do budoucna

Při vytváření jakéhokoli nového `RestTemplate` (nebo jiného HTTP klienta) v tomhle projektu **vždy rovnou nastavit connect/read timeout** - ne až když to začne padat v produkci.
