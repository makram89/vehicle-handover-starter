# <Encja>: specyfikacja wycinka

Źródło: `INTENT_<L>.md` (encja `<Encja>`) | luźny opis (cytat poniżej, jako dane)
START: `<sha>` · TEST_BASE: `<sha, po etapie 2>` · Wariant: główna | podrzędna pod `<Rodzic>`

## Nazwy
| Element | Wartość | Wzorzec |
|---|---|---|
| Klasa / pakiet | `<Encja>` / `com.example.dealership.<encja>` | `Vehicle` / `...vehicle` |
| Tabela (l. poj., snake_case) | `<encja>` | `vehicle` |
| API | `/api/<encje>` lub `/api/<rodzice>/{<rodzic>Id}/<encje>` | `/api/vehicles` |
| Katalog / plik tras / eksport | `<encje>/` / `<encje>.routes.ts` / `<encje>Routes` | `vehicles/`, `vehiclesRoutes` |
| Pliki i klasy frontu | `<encja>-api.ts` `<Encja>Api`, `<encja>-list` `<Encja>List`, `-detail`, `-form` | `vehicle-api.ts` `VehicleApi` |
| Typy TS | `<Encja>`, `<Encja>Request = Omit<<Encja>, 'id'>` | `Vehicle`, `VehicleRequest` |
| Nawigacja | `<Encje>` (brak w wariancie podrzędnym) | `Vehicles` |
| 404 backend / frontend | `<Encja> with id 99 not found` / `<Encja> not found` | `Vehicle with id 99 not found` |
| 409 unikalności | `<Encja> with <pole> <wartość> already exists` | `Vehicle with VIN X already exists` |
| Migracje | `V<n>__<encja>.sql`, `V<n+1>__<encja>_seed.sql` | `V1`, `V2` |

## Pola
Jedno źródło dla migracji, `@Column`, `*Request`, formularza i testów.

| Pole (Java/JSON) | Kolumna | Java | SQL | Wymag. | Unik. | Walidacja `*Request` (komunikat) | TS: encja / model formularza | Kontrolka | Etykieta | ID / S<n> |
|---|---|---|---|---|---|---|---|---|---|---|
| `<pole>` | `<pole_snake>` | | | tak/nie | tak/nie | | | | `<Etykieta>` | |

## Typy (ściąga)
| Rodzaj | Wzorzec | SQL | Java / `*Request` | TS encja / model formularza / `toRequest` | Kontrolka |
|---|---|---|---|---|---|
| tekst | `make` | `VARCHAR(n)` | `String`, `@NotBlank @Size(max = n)` | `string` / `string` / bez zmian; opcjonalny: `\|\| null` | `input type="text"` |
| tekst w formacie | `vin` | `VARCHAR(n)` | `+ @Pattern(regexp, message = "must be ...")` | jak tekst | `input type="text"` |
| e-mail, telefon, kod | — | `VARCHAR(n)` | `@Email` / `@Pattern` | jak tekst | `input type="text"` (nie `email`/`tel`) |
| liczba całkowita | `productionYear` | `INTEGER` / `BIGINT` | `Integer`/`Long`, `@NotNull @Min/@Max` | `number` / `number \| null` / `!` | `input type="number"` |
| dziesiętna, kwota | — | `NUMERIC(p, s)` | `BigDecimal`, `@Digits(integer = p-s, fraction = s)`, `@Column(precision = p, scale = s)` | `number` / `number \| null` | `input type="number" step="0.01"` |
| enum | `fuelType` | `VARCHAR(20)` | enum `EnumType.STRING`, `@NotNull` | unia + `<X>_LABELS` + lista `<X>S` / `<X> \| ''` / `as <X>` | `select` z `<option value="">` |
| data | `plannedHandoverDate` | `DATE` | `LocalDate` | `string \| null` / `string` / `\|\| null` | `input type="date"` |
| boolean | — | `BOOLEAN NOT NULL DEFAULT FALSE` | `Boolean`, `@NotNull` | `boolean` / `boolean` / bez zmian; bez `required` na froncie | `input type="checkbox"` |
| długi tekst | — | `VARCHAR(n ≤ 2000)` | `@Size(max = n)` | jak tekst | `textarea` |
| id rodzica | — | `BIGINT NOT NULL` + FK | `Long`, tylko w `*Response` | `number` / brak / brak | z trasy |

## Wymagania
| ID | Odnośnik | Klasa | Pokrycie (test) |
|---|---|---|---|
| `<L>-R<n>` | `INTENT_<L>.md#<sekcja>` | SKILL / RĘCZNIE | `<Klasa>#<test>` / `spec: '<opis>'` / — |

## Pominięte z intentu
| ID | Status | Odnośnik |
|---|---|---|

## Kolizje nazw i słów zarezerwowanych
| Sprawdzenie | Komenda / lista | Wynik | Decyzja |
|---|---|---|---|
| Klasy w repo | `grep -rnwE "(class\|interface\|enum\|record\|type) <Nazwa>" backend/src frontend/src` dla `<Encja>`, `<Encja>Request`…, enumów | | |
| JDK, Spring, Jakarta | `Record`, `Module`, `Process`, `Package`, `Class`, `Object`, `System`, `Order`, `Sort`, `Page`, `Entity`, `Table`, `Column` | | |
| Globalne typy TS/DOM | `Document`, `Event`, `Location`, `Request`, `Response`, `File`, `Node`, `Image`, `Option`, `Text`, `Comment`, `Notification`, `Range`, `Selection` | | |
| Tabela i kolumny SQL | słowa zarezerwowane H2 z `CLAUDE.md` | | |
| Trasa, katalog, nawigacja | `app.routes.ts`, `app.html`, `ls frontend/src/app` | | |

## Założenia
| ID | Dotyczy | Założenie | Uzasadnienie | Jeśli inaczej |
|---|---|---|---|---|
| S1 | | | | |

## Scenariusze intentu → testy
| # / ID | Klasa | Plik | Test |
|---|---|---|---|

## Raport
(wypełniany w etapie 4, format w `SKILL.md`)
