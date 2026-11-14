# feature-slice: checklista

Ścieżki: `B` = `backend/src/main/java/com/example/dealership`, `BT` = `backend/src/test/java/com/example/dealership`, `M` = `backend/src/main/resources/db/migration`, `F` = `frontend/src/app`.

## A. Mapa plików (kolejność implementacji)
| # | Wzorzec | Nowy plik | Uwagi |
|---|---|---|---|
| 1 | `M/V1__vehicle.sql` | `M/V<n>__<encja>.sql` | kolumny 1:1 z tabelą pól; `CONSTRAINT uq_<tabela>_<kolumna> UNIQUE` dla unikalnych |
| 2 | `M/V2__vehicle_seed.sql` | `M/V<n+1>__<encja>_seed.sql` | 2–3 rekordy; każde pole opcjonalne raz puste, raz wypełnione; wartości różne od danych testów |
| 3 | `B/vehicle/FuelType.java` | `B/<encja>/<Enum>.java` | tylko gdy jest enum |
| 4 | `B/vehicle/ProductionYear*.java` | `B/<encja>/<Reguła>*.java` | tylko reguła pojedynczego pola bez daty i stanu |
| 5 | `B/vehicle/Vehicle.java` | `B/<encja>/<Encja>.java` | `@Column(nullable, length)` = migracja |
| 6 | `B/vehicle/VehicleRepository.java` | `B/<encja>/<Encja>Repository.java` | `existsByX`, `existsByXAndIdNot` dla unikalnych |
| 7 | `B/vehicle/VehicleRequest.java` | `B/<encja>/<Encja>Request.java` | `@Size(max)` = długość kolumny |
| 8 | `B/vehicle/VehicleResponse.java` | `B/<encja>/<Encja>Response.java` | wszystkie pola + `id` |
| 9 | `B/vehicle/VehicleMapper.java` | `B/<encja>/<Encja>Mapper.java` | każde pole w `toEntity`, `updateEntity`, `toResponse` |
| 10 | `B/vehicle/VehicleService.java` | `B/<encja>/<Encja>Service.java` | |
| 11 | `B/vehicle/VehicleController.java` | `B/<encja>/<Encja>Controller.java` | |
| 12 | `F/vehicles/vehicle-api.ts` | `F/<encje>/<encja>-api.ts` | |
| 13 | `F/vehicles/vehicle-form.ts`, `.html` | `F/<encje>/<encja>-form.ts`, `.html` | `required` tylko dla wymaganych; kolejność pól jak w tabeli pól |
| 14 | `F/vehicles/vehicle-list.ts`, `.html` | `F/<encje>/<encja>-list.ts`, `.html` | `colspan` = liczba kolumn |
| 15 | `F/vehicles/vehicle-detail.ts`, `.html` | `F/<encje>/<encja>-detail.ts`, `.html` | bez sekcji spoza specyfikacji |
| 16 | `F/vehicles/vehicles.routes.ts` | `F/<encje>/<encje>.routes.ts` | `new`, `:id/edit` przed `:id` |
| 17 | `F/app.routes.ts` | ten sam (+1 wpis) | `loadChildren`; podrzędna: przed trasą rodzica |
| 18 | `F/app.html` | ten sam (+1 link) | `routerLinkActive="active"`; podrzędna: bez linku |

Testy (etap 2): `BT/vehicle/Vehicle{Controller,Service,Repository}Test.java` → `BT/<encja>/<Encja>…Test.java`; `F/vehicles/vehicle-form.spec.ts` → `F/<encje>/<encja>-form.spec.ts`.
Nie dotykasz: `M/V3__*` i żadnej istniejącej migracji, `B/error/`, `F/shared/`, plików pojazdów.

## B. Wymagane przypadki testowe
Warunek: **zawsze**, **U** = jest pole unikalne, **E** = jest enum, **T** = jest liczba lub data, **O** = jest pole opcjonalne, **P** = wariant podrzędny.

### `<Encja>ControllerTest`
| Przypadek | Warunek |
|---|---|
| `findAll` 200: długość, `id`, każde pole (enum jako nazwa, data jako `YYYY-MM-DD`) | zawsze |
| `findById` 200; 404 `application/problem+json`, `status`, `detail` | zawsze |
| `create` 201, `Location: http://localhost/api/<encje>/1` (P: z id rodzica) | zawsze |
| `create` 400: wszystkie błędne pola naraz, `errors.length()`, każdy `field` i `message` w kolejności po `field`, `verifyNoInteractions` | zawsze |
| `create` 201 dla body bez pól opcjonalnych (brak przypadkowego `@NotNull`) | O |
| `create` 400 z `Accept-Language: pl` → komunikat angielski | zawsze |
| `create` 400 nieznany enum `must be one of: …` (kolejność deklaracji) | E |
| `create` 400 zły typ `has an invalid value` | T |
| `create` 400 nie-JSON: `field` null, `request body must be valid JSON` | zawsze |
| `create` 409 `ConflictException` (`detail`); 409 `DataIntegrityViolationException` `Request conflicts with existing data` | U |
| `update` 200; 404; 400 + `verifyNoInteractions`; 409 | 409: U |
| `delete` 204 + `verify(service).delete`; 404 | zawsze |
| każdy endpoint: 404 brakującego rodzica | P |

### `<Encja>ServiceTest`
| Przypadek | Warunek |
|---|---|
| `findAll` z `Sort.by("id")` mapuje wszystkie | zawsze |
| `findById` 404 z dokładnym komunikatem | zawsze |
| `create` zapisuje i mapuje każde pole | zawsze |
| `create` konflikt → `ConflictException` z komunikatem, `verify(repo, never()).save(any())` | U |
| `update` zmienia encję i zapisuje; konflikt `existsByXAndIdNot` + `never()).save`; 404 | konflikt: U |
| `delete` usuwa; 404 | zawsze |
| brak rodzica → 404 rodzica, `never()).save`; dziecko innego rodzica → 404 dziecka | P |

### `<Encja>RepositoryTest`
| Przypadek | Warunek |
|---|---|
| zapis → `flush` → `clear` → odczyt: każde pole; tekst o długości max kolumny; opcjonalne raz `null` | zawsze |
| `existsByX`, `existsByXAndIdNot` (własny id → false, cudzy → true) | U |
| duplikat przez `saveAndFlush` → `DataIntegrityViolationException` (constraint w migracji) | U |
| `findBy<Rodzic>Id` zwraca tylko dzieci danego rodzica | P |

### `<encja>-form.spec.ts` (kopia struktury `vehicle-form.spec.ts`)
| Przypadek | Warunek |
|---|---|
| pusty submit: komunikaty `"<Etykieta> is required"` w kolejności pól, `expectNone` | zawsze |
| zły format idzie do backendu, błędy pól z 400 przy polach | zawsze |
| create: dokładne body (puste opcjonalne → `null`), nawigacja do `['/<encje>', id]` | zawsze |
| edit: GET `id`, zmiana pola, PUT z dokładnym body, nawigacja | zawsze |
| 404 przy ładowaniu → `<Encja> not found`; inny błąd → `detail` | zawsze |
| błąd z `field: null` nad formularzem; 409 → `detail` nad formularzem | zawsze |
| `<rodzic>Id` z `setInput` w URL każdego żądania | P |

## C. Sprawdzenie końcowe
| # | Komenda | Oczekiwane |
|---|---|---|
| 1 | `./test.sh` | exit 0, oba człony `PASSED` |
| 2 | `git diff --stat $TEST_BASE -- '*Test.java' '*.spec.ts'` | pusto |
| 3 | `git grep -nE '@Disabled\|\.(skip\|only)\(\|\bx(it\|describe)\(' -- backend/src/test frontend/src` | pusto |
| 4 | `git diff --name-status $START HEAD \| grep -v '^A'` | tylko `M frontend/src/app/app.routes.ts`, `M frontend/src/app/app.html` |
| 5 | `git diff $START -- backend/pom.xml frontend/package.json frontend/package-lock.json` | pusto |
| 6 | każdy wiersz tabeli pól vs migracja, `@Column`, `@Size`, `*Response`, typ TS, formularz, mapper | zgodne 1:1 |
| 7 | `git diff $START -- backend/src/main \| grep -nE 'now\(\)'` | pusto (data = `Clock`, poza skillem) |
| 8 | `git diff --name-only $START \| grep -E '\.css$'`; `grep -rn styleUrl F/<encje>` | pusto |
| 9 | `grep -nE 'pattern\|minLength\|maxLength\|email\(\|min\(\|max\(' F/<encje>/*-form.ts` | pusto (na froncie tylko `required`) |
| 10 | `cd frontend && npx prettier --check "src/app/<encje>/**" src/app/app.routes.ts src/app/app.html` | bez uwag |
| 11 | każdy przypadek z B ma test; każde wymaganie `SKILL` ma test w tabeli wymagań | tak |
| 12 | `git status --short` | pusto |
