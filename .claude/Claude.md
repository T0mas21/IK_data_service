# Systém pro správu znalostní báze jazykového modelu

Hlavním úkolem je nastavování datových zdrojů - stahování obsahu webových stránek, vlastní informace nebo vkládání souborů.
Data se pomocí vektorizace předpřipravím pro vyhledávání pro jazykový model.

## Služby

Podrobný popis každé služby (architektura, datový model, API kontrakt, testy) je v `.md` souboru přímo ve složce dané služby - ne tady, ať se to nerozjede a nezastará.

1. ConfigService - [ConfigService/ConfigService.md](../ConfigService/ConfigService.md)
2. ScrapperService - [ScrapperService/ScrapperService.md](../ScrapperService/ScrapperService.md)
3. VectorService - [VectorService/VectorService.md](../VectorService/VectorService.md)

## Struktura

Každá služba je samostatný Maven/Spring Boot modul se svým `pom.xml` a `Dockerfile`. Lokální spuštění všech tří přes `docker-compose.yml` v rootu (ConfigService 8081, ScrapperService 8082, VectorService 8083).

## Konvence kódu

Chybové hlášky (`ResponseStatusException` reason) i komentáře v kódu jsou v **češtině**.

## Build/test příkazy

Testy se spouští per modul, ne z rootu:
```
cd ConfigService && mvn -o clean test
```
(analogicky `ScrapperService`, `VectorService`)

## Deployment

Nasazeno na Render (např. `config-service-5zts.onrender.com`), free tier → **cold start** po delší neaktivitě může trvat 1–2 minuty. Projeví se to jako neobvykle dlouhé `Request duration` v logu volajícího klienta a nesouvisí to s chybou v naší logice.

## Externí klient

API konzumuje externí integrační platforma "Metada" (flow operace typu `IK_CreateConfig`, `IK_EditConfig`). Mapping polí v tomto flow je mimo tento repozitář, takže při debugování 400/500 chyb z produkce vždy nejdřív zkontroluj **přesný tvar JSON těla** v logu klienta, ne jen naši serverovou logiku – většina dosavadních chyb byla způsobená nesouladem názvů polí nebo neplatnou hodnotou (např. placeholder text místo skutečné hodnoty), ne bugem na backendu. Přesný kontrakt polí (`files` u configu apod.) je v `ConfigService/ConfigService.md`.

## Postup při vývoji nových funkcí

- Test-first: nejdřív se vytvoří test pro danou funkcionalitu (musí bez implementace shodně selhat), teprve poté samotná implementace.
- Žádná nová funkcionalita se nepovažuje za hotovou, dokud pro ni neexistuje test a celá test suite modulu neprochází (`mvn -o clean test` v daném modulu).
- Po odsouhlasené implementaci, která mění API kontrakt/veřejné chování služby, doplň stručný záznam do příslušného `<Service>.md` (ne do tohoto souboru) - jen to, co by jinak muselo být znovu objevováno (endpoint, kontrakt, netriviální gotcha), ne celý changelog.

## Práce s více agenty (subagents)

Tenhle projekt je malý (3 služby), takže vícero agentů/subagentů se vyplatí jen výjimečně:

- Použij subagenta (fork nebo fresh) na nezávislé, samostatně popsatelné úkoly - průzkum napříč všemi třemi službami, čtení/analýza logů z Renderu, hledání výskytů něčeho v celém repu.
- Paralelně spouštěj jen když jsou úkoly na sobě skutečně nezávislé.
- Pro běžný bugfix/feature v rámci jedné služby subagenty nepoužívej - lineární postup (test → implementace → test suite) je rychlejší a přehlednější bez nich.
- Workflow (víceagentová orchestrace) spouštěj jen na výslovnou žádost - kvůli spotřebě tokenů to není defaultní volba.

## Práce s gitem

- Branch model: pracuje se přímo na `main`, žádné feature branche/PR flow.
- Commit zprávy začínají prefixem `fix:` nebo `feat:` (podle toho, jestli jde o opravu chyby, nebo novou funkčnost) + krátký popis v dalším textu zprávy, který si vytvořím sám podle toho, co jsem v dané části skutečně implementoval.
- Commit dělám až po odsouhlasení implementace uživatelem – ne automaticky hned po dokončení úprav.
- Push dělám až po odsouhlasení – nikdy sám od sebe hned po commitu.
- Důvod opatrnosti: Render je napojený na `main` a při pushi rovnou nasazuje na produkci (`config-service-5zts.onrender.com` apod.) – push sem tedy není neutrální akce.
