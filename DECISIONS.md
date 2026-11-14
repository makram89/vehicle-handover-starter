# DECISIONS

Stan na 2026-11-14, ok. 22:45. Źródła: `git log`, raporty skilla (`docs/slices/customer.md`, `docs/slices/vehicle-document.md`) i zapisy sesji Claude Code z tego repo. Hashe to skróty commitów.

**Cały kod i wszystkie pliki w repo napisał agent (Claude), na moje polecenia. Nie poprawiałem niczego ręcznie.** Gdzie niżej stoi „ręcznie", chodzi o część poza skillem (reguły biznesowe pisane test-first), nie o moje edycje. Wszystkie poprawki zlecałem agentowi.

**Feature A nie jest skończony.** Zrobiona jest część 1 (skill, CRUD dokumentów, `feat/vehicle-documents`, niescalona z `main`). Część 2 (reguły, gotowość, `Clock`, flaga pojazdu) jest w toku: w worktree leżą jeszcze niezacommitowane testy. Sekcje o A opisują więc to, co jest w gicie, i plan. Po części 2 trzeba je uzupełnić (patrz „Czego nie zrobiłem").

## 1. Proces

Kolejność i pochodzenie plików:

| # | Krok | Wynik | Commit |
|---|---|---|---|
| 1 | Przeczytanie `README.md`, `BRIEF.md`, `./test.sh` na starterze | bez zmian w repo; `./test.sh` przeszedł dopiero na Node 24.22.3 (domyślny 24.0.1 odrzuca `check-node.sh`) | |
| 2 | `CLAUDE.md` z lektury modułu pojazdów | konwencje backendu, frontendu, testów, git | `2cb3dd4` |
| 3 | Analiza transkrypcji: `docs/transcript.md` (L1–L27), `INTENT.md`, `INTENT_A.md`, `INTENT_B.md` z tabelą wymagań (CONFIRMED / SUPERSEDED / DEFERRED / OPEN / ASSUMED) i cytatami po `L<n>`. Cytaty sprawdzone skryptem (30/30 dosłownie) | pliki intentu | `a40ec30`, `aead8a5` |
| 4 | Skill `feature-slice` (`SKILL.md`, `entity-spec-template.md`, `checklist.md`) i uzupełnienia `CLAUDE.md` | skill | `482a969` |
| 5 | Zmiana pracy z gitem na worktree per feature, merge `--ff-only` | sekcja Git w `CLAUDE.md` | `f56fc5d` |
| 6 | Feature B skillem (5 commitów, patrz niżej) | klienci | `0c9834e`…`b1242fd` |
| 7 | Feature A część 1 skillem (5 commitów) | dokumenty (CRUD) | `4c41b7a`…`18cd001` |
| 8 | Feature A część 2 ręcznie test-first | w toku | brak |

Jak działał skill w obu featurach: etapy spec → testy (commit na czerwono, `TEST_BASE`) → backend → frontend → raport, każdy etap osobnym subagentem (model sonnet), po jednym naraz. Orkiestrator po każdym etapie sam uruchamiał testy. Testy po commicie `TEST_BASE` nie mogły się zmieniać; checklist C.2 sprawdza to diffem (`git diff --stat $TEST_BASE -- '*Test.java' '*.spec.ts'`). Założenia skill zapisuje w `docs/slices/<encja>.md` jako `S<n>`, bo nie edytuje intentów.

Kolejność B przed A wynika z briefu (B jest testem skilla) i z numeracji migracji: B ma `V4`/`V5`, A `V6`+.

Sesje były osobne i kontekst przenosiły pliki: `CLAUDE.md`, `INTENT*.md`, `docs/slices/*.md`, nie pamięć agenta.

## 2. Feature B (klienci): tylko skill

- **Co wygenerował skill** (`0c9834e` spec, `0aee4f6` testy na czerwono, `6426c5b` backend, `134cdf6` frontend, `b1242fd` raport; ff-only na `main`): migracje `V4__customer.sql` (z `uq_customer_email`) i `V5__customer_seed.sql` (3 rekordy, jeden z NIP), pakiet `customer/` (encja, repozytorium, request/response, mapper, serwis, kontroler), 33 testy backendu (17+9+7), `customer-form.spec.ts` (9), moduł `customers/` i po jednym wpisie w `app.routes.ts` i `app.html`. Po całości `./test.sh` zielony: 62 testy backendu, 18 frontendu. Testy bez zmian od `TEST_BASE`. Wszystkie 12 punktów checklisty C spełnione.
- **Iteracje skilla: zero.** Nie było resetu ani regeneracji. Skill ma jeden commit (`482a969`) i nie zmieniał się po żadnym featurze.
- **Ręczne poprawki kodu: brak.** Jedyna poprawka po wygenerowaniu to intent (`781f511`, patrz „Pomyłki", pkt 3). Kod i testy zostały takie, jakie wygenerował skill.
- **Założenia skilla** (`docs/slices/customer.md`, S1–S11): m.in. S1 body 400 ma `phone` pominięty, bo `""` daje dwa błędy; S2 `@Email` Hibernate bez własnego regexa (przepuszcza `a@b`); S3 `"nip": ""` daje 400, front wysyła `null`; S5 seed z innymi danymi niż testy; S10 dodatkowe testy `existsByEmailIsCaseSensitive` i `saveAllowsTwoCustomersWithTheSameNip` dla B-Q3/B-Q4, żeby każde założenie miało test.

## 3. Feature A (dokumenty i gotowość)

### Część 1: skill (`feat/vehicle-documents`, wariant encji podrzędnej)
Commity: `4c41b7a` spec, `3f0f715` testy na czerwono, `9ce4244` backend, `393e80b` frontend, `18cd001` raport. Skill dostał zakres: lista, `GET /{id}`, POST, 400 formatu, 404.

- **Wygenerował:** `V6__vehicle_document.sql` (FK `ON DELETE CASCADE`, bez `UNIQUE`), pakiet `document/` (`VehicleDocument`, kontroler, serwis, repozytorium, mapper, `DocumentRequest`, `DocumentResponse`, `DocumentType`, `DocumentStatus`), 30 testów backendu, moduł `frontend/src/app/documents/` z listą, szczegółami i formularzem (8 testów), wpis w `app.routes.ts`. `./test.sh` zielony: backend 92, frontend 26. Testy bez zmian od `TEST_BASE`.
- **Iteracje skilla: zero.** Skill nie zmieniał się po B.
- **Skill sam wypisał jako niepokryte** („RĘCZNIE" w raporcie): A-R1, A-R2, A-R9, A-R12, A-R13, A-R14, A-Q2, A-Q4, A-Q8, A-Q13 (endpoint gotowości i `Clock`); A-R3, A-Q7 (flaga w `Vehicle`); A-R5, A-R7, A-Q3 (przejścia statusów); A-R10 (data z przyszłości); A-R11 (409 aktywnego dokumentu); A-Q10 (`expiryDate` ≥ `issueDate`).
- **Wzorzec nie pokrył:** sekcji dokumentów w widoku pojazdu, zmiany encji rodzica, update/delete w wariancie podrzędnym, reguł zależnych od `Clock` i stanu innych encji, linku w nawigacji dla trasy podrzędnej.
- **Założenia skilla:** S1 tylko GET lista, GET `/{id}`, POST; S2 status `PENDING` ustawia mapper, wartość z body jest ignorowana; S3 brak seeda (odstępstwo od checklisty, żeby seed nie kolidował z testami reguł); S5 kaskada przy usunięciu auta; S7 `number` bez unikalności i wzorca; S8 formularz tylko tworzy.

### Część 2: ręcznie i test-first (plan, w toku)
Zlecenie: najpierw testy i commit, potem kod, testów po commicie nie zmieniamy. Plan testów i tabelę przejść statusów zaakceptowałem przed startem. Zakres: 409 na aktywny dokument tego samego typu, `PUT …/status` z tabelą przejść, walidatory dat (`@NotInFuture` z `Clock`, `expiryDate ≥ issueDate`), `registrationByDealer` w `Vehicle` (nowa migracja, osobny `PUT …/registration-by-dealer`), `GET …/handover-readiness`, widok gotowości w szczegółach auta. Plan obejmuje też test integracyjny `@SpringBootTest` z H2, który łapie okablowanie niewidoczne w testach z mockami (bean `Clock` w walidatorze, migracje, kaskada). Wynik tej części nie jest jeszcze w gicie.

Tabela przejść (wiersz = obecny, kolumna = docelowy):

| Z \ Na | `PENDING` | `VERIFIED` | `REJECTED` |
|---|---|---|---|
| `PENDING` | 409 | 200 | 200 |
| `VERIFIED` | 409 | 409 | 200 |
| `REJECTED` | 409 | 409 | 409 |

## 4. Gdzie AI się pomyliło

| # | Pomyłka | Jak wyłapane | Co zrobiłem |
|---|---|---|---|
| 1 | Przy budowie `INTENT_A.md` skrypt mapujący ID nadpisał wiersz SMS w §6 (`A-R15` stał się `A-Q2`) | agent sam, przy przeglądzie pliku | poprawione przed commitem |
| 2 | Pierwszy regex telefonu `\+?[0-9]{9,15}` odrzucałby `+48 600 100 200` i `600-100-200` | mój przegląd intentu | kazałem zmienić na wzorzec ze spacjami i myślnikami, `VARCHAR(25)` (`a40ec30`) |
| 3 | Mój poprawiony regex przepuszczał `+---------` | agent sam to zgłosił po zastosowaniu | kazałem wymagać cyfry na początku i końcu: `\+?[0-9][0-9 \-]{7,18}[0-9]` (`aead8a5`). Pytanie o wiodący `+` rozstrzygnięte testem na przykładach |
| 4 | Scenariusz 5 w `INTENT_B.md` zakładał 4 błędy dla pustych pól łącznie z `phone` = `""`. `""` łamie `@NotBlank` i `@Pattern`, więc wychodzi 5 | skill zauważył niezgodność przy pisaniu testu (S1), ja dopiero z komentarza orkiestratora; zapytałem „jaki jest błąd w phone" | błąd intentu, nie skilla: kod i testy bez zmian, poprawiłem scenariusz (body z `phone` = `null`, dopisek o 5 błędach dla `""`), `781f511` |
| 5 | Sesja analizy intentu ominęła blokadę harnessu (zapis tylko w worktree), zapisując pliki przez powłokę. Powiedziała to wprost w raporcie | z raportu agenta | zaakceptowałem tamten wynik. Sesja skilla w tej samej sytuacji zatrzymała się i zapytała; wyłączyłem izolację (`a71f325`, `.claude/settings.json`: `bgIsolation: none`) |
| 6 | Ucięty fragment mojej wiadomości („INTENT_Ba main", urwane zdanie o kolejności). Agent domyślił się, że ma dodać `docs/transcript.md` do commita | agent zgłosił to w raporcie | zostawiłem: pliki intentów cytują transkrypcję |
| 7 | `./test.sh` w nowej sesji kończył się na `check-node.sh`: PATH wskazuje Node 24.0.1 (wymagane 24.15+) | uruchomienie `./test.sh` | w każdej sesji `nvm use`, czyli 24.22.3; w świeżym worktree brak `node_modules`, B-session obeszła to kopią z twardymi linkami. Tej luki nie dopisałem do `CLAUDE.md` |
| 8 | Skill nie odróżnia w raporcie sprzeczności w intencie (S1) od zwykłego założenia (np. S9 „teksty UI jak w pojazdach") | pytanie „Skill jest ok?" i odpowiedź agenta | zdecydowałem: bez nowych reguł w skillu, kod B zostaje. Świadomie niezałatane (patrz niżej) |

Nie było przypadku, w którym wygenerowany kod trzeba było cofać lub poprawiać. Błędy, które wyłapałem, dotyczyły intentów i środowiska.

## 5. Decyzje projektowe

Statusy i numery z `INTENT_A.md` / `INTENT_B.md`. Każde założenie `ASSUMED` jest jawne w intencie z kolumną „Jeśli klient zdecyduje inaczej".

| Decyzja | Uzasadnienie | Odrzucone alternatywy |
|---|---|---|
| **A-Q1 / A-R7:** `REJECTED` jest stanem końcowym, poprawka to nowy dokument. Zostaje OPEN do decyzji dyrektora | L24 i L26 Anny („Na razie róbcie po mojemu"). Kontrakt (aktywny = nieodrzucony) jest z tym zgodny | Wersja Tomka (L12, L25: przestawianie na `VERIFIED`). Ryzyko: zewnętrzne testy oczekujące 200 dostaną 409 |
| **A-Q3:** `VERIFIED → REJECTED` dozwolone; powrót do `PENDING` i ta sama wartość: 409 | Bez tego błędnie zweryfikowanego dokumentu nie da się zastąpić, bo A-R11 blokuje nowy | Zablokować `VERIFIED → REJECTED` (zmiana jednej komórki tabeli) |
| **A-R11:** unikalność aktywnego dokumentu w serwisie, bez `UNIQUE` w bazie | Odrzucone dokumenty się powtarzają i zostają w historii (L18). Wyścig dwóch POST nie ma siatki: akceptuję | `UNIQUE (vehicle_id, type)` (blokuje historię); indeks częściowy (H2 go nie ma) |
| **A-Q2 / A-Q4 / A-Q13:** `PENDING` blokuje wydanie (`NOT_VERIFIED`) i jest też sprawdzany pod kątem ważności; bez `plannedHandoverDate` ważność liczona na dziś z `Clock` (zgodnie z przykładem kontraktu); data wydania w przeszłości nie blokuje | Kontrakt ma te powody i przykład z `EXPIRED` razem z `NO_HANDOVER_DATE` | Pomijać `PENDING` w problemach; pomijać `EXPIRED` bez daty wydania |
| **A-Q8:** `REGISTRATION_POA` ignorowane, gdy flaga `registrationByDealer` = `false` | L9: wymagane tylko tam, gdzie Tomek zaznaczy | Raportować jego problemy |
| **A-Q7:** flaga zmieniana osobnym `PUT /api/vehicles/{id}/registration-by-dealer`, jest w `VehicleResponse`, nie w `VehicleRequest`; 6-argumentowe konstruktory `Vehicle`/`VehicleRequest` zostają | Zamrożone `vehicle-form.spec.ts` i `VehicleControllerTest` sprawdzają dokładne body i liczbę błędów; PUT pojazdu bez flagi by ją zerował | Pole w formularzu pojazdu (wymaga zmiany zamrożonych testów) |
| **`Clock`:** bean + `@NotInFuture` z wstrzykniętym `Clock` (klasa walidatora, nie `Year.now()` jak `ProductionYear`) | Zasada z `CLAUDE.md`; testy dat ze stałym `Clock` (2026-11-14) | `LocalDate.now()` |
| **A-Q10:** `expiryDate` ≥ `issueDate`, walidator klasowy z `addPropertyNode("expiryDate")`, żeby błąd miał `field` | Oczywisty błąd wpisu, jak L16 | Brak walidacji |
| **A-Q9:** `expiryDate` opcjonalna także dla `OC_POLICY`; brak daty = ważny bezterminowo | Kontrakt: opcjonalnie. Wymuszenie dałoby 400 tam, gdzie testy mogą oczekiwać 201 | Warunkowe `@NotNull` dla polisy |
| **A-Q5, A-Q6, A-Q12:** bez skanów, bez audytu (kto/dlaczego), bez edycji i usuwania dokumentu; kaskadowe usunięcie z autem | Kontrakt nie ma tych pól ani endpointów; historia służy kontroli (L24) | Upload plików; `comment` i `statusChangedAt`; FK bez kaskady (409 przy usuwaniu auta) |
| **A-Q11:** `number` wymagany, do 50 znaków, bez unikalności | Kontrakt nie oznacza go jako opcjonalnego; transkrypcja milczy | `@Pattern` lub unikalność per typ |
| **Dodatkowe endpointy** `GET …/documents/{id}` (cel `Location`) i `PUT …/registration-by-dealer` | Rozszerzają kontrakt bez zmiany jego ścieżek; wskazane w planie jako odchylenie | Brak `GET {id}`: `Location` wskazywałby nieistniejący zasób |
| **Gotowość liczy tylko backend**, frontend wyświetla listę braków i problemów | Zasada z `CLAUDE.md` (reguły tylko w backendzie) | Liczenie po stronie Angulara |
| **B-Q1:** `firstName`, `lastName`, `email`, `phone` wymagane, NIP opcjonalny | L23 wymienia je dla każdego klienta; telefon będzie potrzebny do odłożonych SMS | Telefon opcjonalny (nowa migracja) |
| **B-Q2:** telefon `\+?[0-9][0-9 \-]{7,18}[0-9]`, `VARCHAR(25)`; NIP dokładnie 10 cyfr, bez sumy kontrolnej | Typowe zapisy ze spacjami i myślnikami nie mogą dawać 400; prosta reguła `@Pattern` | Normalizacja do samych cyfr; walidator sumy kontrolnej jak `ProductionYear` |
| **B-Q3:** e-mail porównywany dokładnie (wielkość liter ma znaczenie) | Zgodne z wzorcem VIN i `UNIQUE` | `existsByEmailIgnoreCase` i zapis małymi literami |
| **B-Q4 / B-Q5:** NIP nieunikalny; brak `companyName` i typu klienta (firma = NIP wypełniony) | L23 wymienia unikalność tylko dla e-maila; kilka osób z jednej firmy ma ten sam NIP | `uq_customer_nip`; `companyName` lub `CustomerType` |
| **Skill (S3 w A):** brak seeda dokumentów | Seed kolidowałby z testami reguł A | Seed 2–3 rekordów jak w checklist A.2 |
| **Skill:** wariant podrzędny bez linku w `app.html`, trasa `vehicles/:vehicleId/documents` przed `vehicles` | Potrzebuje id pojazdu, nie ma sensu jako pozycja menu | Link w menu |
| **Proces:** worktree per feature + `--ff-only` zamiast liniowo na `main` | Feature trwa długo i ma tury testowe | Praca na `main` bez gałęzi (pierwotna wersja `CLAUDE.md`) |
| **Proces:** orkiestrator + subagenci sonnet, jeden na etap, test-first ze zamrożonym `TEST_BASE` | Etapy mają jasne wejście i wyjście; zamrożone testy nie pozwalają „dopasować testu do kodu" | Jeden agent na cały wycinek |
| **Intent poprawiam, kod nie** (pkt 4.4) | Sprzeczność leżała w scenariuszu, a kod i test były zgodne z modelem danych | Regeneracja B ze zmienionym skillem |

Pytania o intent, na które odpowiedziałem po numerach (A-Q1…A-Q13, B-Q1…B-Q5): akceptacja wszystkich oprócz B-Q2 (zmiana telefonu) i dopisku do A-Q2 (ważność także dla `PENDING`). SMS (A-R15) zostaje DEFERRED: zgłosił Tomek (klient), odłożyła Kasia (dostawca), więc to nie jest pomysł dostawcy bez zgody.

## 6. Czego nie zrobiłem i co dalej

- **Feature A część 2** jest w toku i nie ma jej w gicie: przejścia statusów, 409 na aktywny dokument, walidatory dat, `Clock`, flaga `registrationByDealer`, `handover-readiness`, widok w szczegółach auta. Po jej zakończeniu: `./test.sh`, `git merge --ff-only feat/vehicle-documents`, `git worktree remove ../vhs-feat-documents`, `git branch -d feat/vehicle-documents`, a w tym pliku sekcje 3 i 4 uzupełnić o faktyczny wynik (w tym pomyłki z części 2).
- **Gałąź i worktree A zostały otwarte celowo.** Piszą w nich jeszcze subagenci, więc ich nie scalałem ani nie usuwałem. Ten plik został dodany na `main` w trakcie pracy gałęzi, więc `--ff-only` zawiedzie i trzeba będzie zdecydować jak scalić (zasada: bez merge commitów i bez cichego rebase).
- **A-Q1 zostaje OPEN:** wersja Anny, czeka na dyrektora.
- **Odłożone w intencie:** SMS o gotowości (A-R15) i powiązanie klient–auto (B-R5); SMS wymagają obu.
- **Nie dodałem** logowania i ról, skanów dokumentów, audytu zmian statusu, edycji ani usuwania dokumentu, normalizacji e-maila i telefonu, sumy kontrolnej NIP.
- **Nie ma nowego widoku w nawigacji** dla dokumentów; wchodzi się przez szczegóły pojazdu (sekcja Documents w `vehicle-detail` jest pusta i część 2 ma ją wypełnić).
- **Luki skilla, których nie zamykałem** (zdecydowałem: bez dopisywania reguł): sprzeczność scenariusza z modelem danych wygląda w raporcie jak zwykłe założenie; brak instrukcji dla `node_modules` w nowym worktree (miejsce: `CLAUDE.md`); `@Email` Hibernate przepuszcza adres bez TLD (S2).
- **Nie porównałem skilla z agentem bez skilla** na nieznanej encji; brief mówi, że zrobi to oceniający. Mam tylko dwa przebiegi (B, A część 1), oba bez iteracji, co nie dowodzi, że skill jest dobry na obcej encji.
- **Test wyścigu** dwóch POST tego samego typu dokumentu: nie ma, brak siatki w bazie (świadomie).
