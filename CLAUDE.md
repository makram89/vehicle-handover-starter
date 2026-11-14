# CLAUDE.md

Salon samochodowy: Spring Boot 3.5 (Java 21, Maven wrapper) + Angular 22 (signal forms, vitest), H2 w pamięci, Flyway.

## Źródła prawdy
- Kontrakt API (ścieżki, nazwy enumów, kody 409, kształt `handover-readiness`) i transkrypcja spotkania: `BRIEF.md`. Nazwy i ścieżki są stałe, zewnętrzne testy na nich polegają.
- `INTENT.md` to indeks wymagań, szczegóły każdego feature są w `INTENT_<litera>.md`. Przed pracą przeczytaj plik tego feature; które istnieją: `ls INTENT*.md`.
- Wzorzec kodu: moduł pojazdów (`backend/.../vehicle/`, `error/`, `frontend/src/app/vehicles/`, `shared/`). Czego tu nie ma, kopiuj z pojazdów.

## Komendy
- Całość: `./test.sh` (zawsze oba człony; exit ≠ 0 przy porażce). Start: `./run.sh` (:8080 i :4200, `/api` przez proxy; baza odtwarzana z migracji przy każdym starcie).
- Jedna klasa: `cd backend && ./mvnw test -Dtest=VehicleServiceTest`
- Frontend: `cd frontend && npm test`; jeden spec: `npx ng test --watch=false --include src/app/vehicles/vehicle-form.spec.ts`
- Node: 22.22.3+ lub 24.15+ (`.nvmrc`, `check-node.sh`). Angular CLI odmawia starszego; `check-node.sh` (z `test.sh`/`run.sh`) sam przełącza przez nvm wg `.nvmrc`. Przy ręcznym `npm`/`ng` poza skryptami sprawdź `node -v` albo najpierw `source check-node.sh`. Exit 1 z `test.sh` z komunikatem „Node.js … is not supported" to środowisko, nie test.

## Backend (`com.example.dealership.<feature>`)
- Pakiet per feature: encja, `*Controller`, `*Service`, `*Repository`, `*Mapper`, `*Request`/`*Response` jako rekordy. Bez Lombok i MapStruct.
- Encja: `@Table`, `Long id` z `IDENTITY`, `protected` konstruktor bezargumentowy plus publiczny z polami, gettery/settery, enumy `EnumType.STRING`, `@Column(length)` zgodne z migracją.
- Mapper: `final` klasa ze statycznymi `toEntity`, `updateEntity`, `toResponse`.
- Kontroler cienki: `/api/<mnoga>`, `@Valid @RequestBody`, zero logiki. POST → 201 + `Location`, PUT/GET → 200, DELETE → 204.
- Walidacja formatu w `*Request` (Bean Validation, komunikaty po angielsku, locale wymuszone w `application.properties`). Własna reguła = adnotacja + validator jak `ProductionYear`; validator zwraca `true` dla `null`, `null` łapie `@NotNull`. Reguły zależne od stanu (unikalność, statusy) żyją w serwisie.
- Błędy: `NotFoundException` → 404 (`"Vehicle with id 99 not found"`), `ConflictException` → 409, oba obsługuje `GlobalExceptionHandler` jako `ProblemDetail`. 400 ma `detail: "Validation failed"` i `errors: [{field, message}]` posortowane po `field` (`null` pierwszy). Nie łap wyjątków w kontrolerach; nowy kod HTTP = nowy handler w `GlobalExceptionHandler`.
- Unikalność: serwis sprawdza `existsByX` (przy update `existsByXAndIdNot`) i rzuca `ConflictException("Vehicle with VIN X already exists")`. W migracji dodatkowo `CONSTRAINT uq_<tabela>_<kolumna> UNIQUE`; `DataIntegrityViolationException` → 409 to tylko siatka na wyścigi.
- Serwis: klasa `@Transactional(readOnly = true)`, metody zapisu `@Transactional`; `findAll` z `Sort.by("id")`; prywatny `getX(id)` rzucający `NotFoundException`.
- Migracje: `backend/src/main/resources/db/migration/V<n>__<opis>.sql`. Kolejny numer: `ls` tego katalogu. `ddl-auto=validate`, więc encja musi dokładnie odpowiadać schematowi. NOT NULL na istniejącej tabeli: kolumna nullable, uzupełnienie, `SET NOT NULL` (jak `V3`). Seed w osobnej migracji `*_seed`.
- Nazwy SQL: tabela w liczbie pojedynczej (`vehicle`), kolumny snake_case. Hibernate sam mapuje camelCase na snake_case (`plannedHandoverDate` → `planned_handover_date`), więc `@Column(name)` tylko przy kolizji. Słowa zarezerwowane H2 2.x nie mogą być nazwą tabeli ani kolumny (m.in. `user`, `order`, `group`, `value`, `values`, `year`, `month`, `day`, `hour`, `minute`, `second`, `key`, `end`, `default`, `row`, `rows`, `limit`, `offset`, `top`, `minus`, `interval`, `range`, `window`); wtedy inna nazwa SQL w `@Table`/`@Column(name)`, nazwa w JSON bez zmian.
- Komunikaty 400 spoza Bean Validation (z `GlobalExceptionHandler`): nieznana wartość enuma `must be one of: A, B` (kolejność deklaracji), zły typ `has an invalid value`, body niebędące JSON-em `field: null` i `request body must be valid JSON`. Siatka na wyścig unikalności daje 409 `Request conflicts with existing data`.
- Czas: logika zależna od daty dostaje wstrzyknięty `Clock`, nigdy `LocalDate.now()`. `ProductionYearValidator` używa `Year.now()`, to wyjątek, nie wzorzec. Czy bean `Clock` już jest: `grep -rn Clock backend/src`.

## Frontend (`frontend/src/app/<feature>/`)
- Pliki: katalog i trasy w liczbie mnogiej, reszta w pojedynczej: `vehicles/`, `vehicles.routes.ts` (eksport `vehiclesRoutes`), `vehicle-api.ts` (`VehicleApi`), `vehicle-list|detail|form.ts` z osobnym `.html` (`VehicleList`, `app-vehicle-list`), `vehicle-form.spec.ts`. Bez plików `.css` komponentów; style tylko globalne `styles.css`. Komponenty standalone, `inject()`, `protected readonly`, selektor `app-<feature>-<rola>`.
- Api (`providedIn: 'root'`): odczyty przez `httpResource` (`list()` z `defaultValue: []`, `get(id: Signal<number | undefined>)` zwraca `undefined` zamiast URL, gdy brak id), zapisy przez `HttpClient` zwracający `Observable`. Request = `Omit<Entity, 'id'>`. Enum = typ unii + `X_LABELS: Record` + lista z `Object.keys`. Daty jako string `YYYY-MM-DD`, brak = `null`.
- Id z trasy: `input.required({ transform: numberAttribute })` albo `input(undefined, { transform: optionalNumberAttribute })` z `shared/route-params.ts` (włączone `withComponentInputBinding`).
- Formularz: signal forms (`form`, `required`, `FormField`, `FormRoot` z `@angular/forms/signals`), model w `linkedSignal`, `toFormModel` i `toRequest` (pusty string → `null`). Po stronie klienta tylko `required` z komunikatem `"X is required"`; format, zakres i unikalność zostają w backendzie. Pola tekstowe (także e-mail i telefon) mają `type="text"`, bo natywna walidacja przeglądarki nie może wyprzedzać backendu, a spec wybiera pola po `input[type=…]` i indeksie.
- Błędy backendu: w `submission.action` `catch` → `problemDetailToFormErrors(error, form)` (400 z polem → pole; `field: null` i 409 → błąd całego formularza). Ładowanie: `loadErrorMessage(error, 'X not found')`; akcje poza formularzem (delete): `problemDetailMessage`. Szablon: `<p class="error" role="alert">`, błędy pola dopiero po `touched()`.
- Nowy feature rejestrujesz w dwóch miejscach: lazy `loadChildren` w `app.routes.ts` i `routerLink` z `routerLinkActive="active"` w `app.html`. W `*.routes.ts` `new` i `:id/edit` stoją przed `:id`.

## Testy
- Backend, trzy pliki na feature: `*ControllerTest` (`@WebMvcTest` + `@MockitoBean` serwis; status, `application/problem+json`, `errors[i].field/message`, `verifyNoInteractions` przy 400), `*ServiceTest` (`MockitoExtension`, `@InjectMocks`), `*RepositoryTest` (`@DataJpaTest`, `TestEntityManager` flush/clear). `@DataJpaTest` uruchamia wszystkie migracje Flyway łącznie z seedami: tabela nie jest pusta, a dane testowe nie mogą kolidować z seedem. W `@WebMvcTest` nagłówek `Location` to `http://localhost/api/<mnoga>/<id>`. AssertJ, opisowe nazwy: `createReturnsConflictProblemWhenVinAlreadyExists`. Testy dat ze stałym `Clock`.
- Frontend: vitest (`vi`, nie jasmine), `HttpTestingController` z `afterEach(() => http.verify())`, `vi.waitFor`, `fixture.componentRef.setInput('id', '3')`.

## Zasady
- Minimalny diff: tylko to, o co poproszono, bez refaktoryzacji pojazdów przy okazji. Bez nowych zależności w `pom.xml` i `package.json`.
- Nie edytuj istniejących migracji (zmiana schematu = nowa migracja). Nie zmieniaj zamrożonych testów (istniejące testy pojazdów, `error/`, `vehicle-form.spec.ts`); gdy test wydaje się błędny, napisz o tym zamiast go poprawiać.
- Reguły biznesowe tylko w backendzie, frontend je wyświetla. Logika zależna od daty: `Clock`.
- Nowa encja zgodna ze wzorcem: skill `feature-slice` (`.claude/skills/feature-slice/`, sprawdź `ls .claude/skills`) uruchomiony na `INTENT_<litera>.md`. Reguły spoza CRUD (statusy, gotowość do wydania) ręcznie i test-first: najpierw test, potem kod.
- Niejasność nie jest powodem do zgadywania: zapisz założenie w `INTENT_<litera>.md` i działaj dalej. Wyjątek: skill `feature-slice` nie edytuje intentu, swoje założenia zapisuje w `docs/slices/<encja>.md`.

## Git
- Jeden worktree i jedna gałąź na feature (`feat/<feature>`), tworzone z bieżącego HEAD lokalnego `main`, nie z `origin`. Po utworzeniu sprawdź, że `git log -1` w worktree = `git log -1 main`.
- Małe commity w worktree, komunikaty po angielsku w trybie rozkazującym (`Add basic class-free styles`). Przed każdym commitem `./test.sh`. Tury testowe i poprawki zostają w tej samej gałęzi.
- Po ukończeniu feature (testy zielone): w głównym checkoucie `git merge --ff-only feat/<feature>`, potem `git worktree remove <ścieżka>` i `git branch -d feat/<feature>`. Ten merge na `main` jest wprost dozwolony, nie pytaj o zgodę. Gdy `--ff-only` zawodzi, zatrzymaj się i zapytaj; bez merge commitów i bez rebase po cichu.
- Nigdy force-push. Koniec pracy = `git worktree list` pokazuje tylko główny checkout, `git branch` tylko `main`.
- Każdy krok kończy się commitem i czystym `git status`.

## Styl odpowiedzi
Zwięźle: odpowiedź najpierw, potem uzasadnienie. Bez wstępów, powtórzeń i streszczania diffa. Kod, komendy, ścieżki i komunikaty błędów podawaj dokładnie. Pytania i ostrzeżenia pełnymi zdaniami.
