# DECISIONS

**Cały kod i wszystkie pliki w repo napisał agent (Claude), na moje polecenia. Nie poprawiałem niczego ręcznie.** Gdzie niżej stoi „ręcznie", chodzi o część poza skillem (reguły biznesowe pisane test-first), nie o moje edycje. Wszystkie poprawki zlecałem agentowi. Wyjątkiem jest plik DECISIONS.MD

## Proces
0. Przygotowanie do pracy z opisu mailowego zadania
1. Rozpoznanie zadania w repo 
2. Przygotowanie prompta i założenie `CLAUDE.md` 
3. Analiza transkrypcji i założenie intentów: `docs/transcript.md`, `INTENT.md`, `INTENT_A.md`, `INTENT_B.md`. Intenty zostały robite na A i B ponieważ to są osobne featury/taski. PRzy zleceaniu zadania oszczędzam kontekst a podział
4. Przgotowanie prompta i skila `feature-slice` 
5. Początkowo założyłem prace bez worktree (uznałem że bedzie czyściej), ale musiałem to zmienić z uwagi na problem w działaniu subagentów. 
5. Feature B tylko skillem, orkiestrator znalazł błąd inetntu. potem feature A: część 1 skillem, część 2 ręcznie test-first.

Skill działa etapami (spec, testy na czerwono, backend, frontend, raport), każdy etap robi osobny subagent, a orkiestrator po każdym uruchamia testy. Testy po commicie na czerwono są zamrożone. Założenia skill zapisuje w `docs/slices/<encja>.md`. Kontekst między sesjami niosły pliki, nie pamięć agenta.

## Feature B (klienci)

Skill wygenerował całość: migracje z seedem, pakiet `customer/`, 33 testy backendu, spec formularza i moduł Angulara. `./test.sh` zielony za pierwszym razem (62 testy backendu, 18 frontendu). **Iteracji skilla: zero, poprawek kodu: zero.** Jedyna zmiana po generowaniu dotyczyła intentu (`781f511`, niżej).

## Feature A (dokumenty i gotowość do wydania)

- **Skill (część 1, `f1cc927`…`d1aaa8f`), wariant encji podrzędnej:** encja `VehicleDocument`, migracja `V6` (FK `ON DELETE CASCADE`, bez `UNIQUE`), lista, `GET` po id, `POST` (zawsze `PENDING`), 400 formatu, 404 auta i dokumentu innego auta, moduł `documents/` na trasie `vehicles/:vehicleId/documents`. Raport skilla poprawnie wypisał resztę jako niepokrytą (RĘCZNIE). Świadome odstępstwa: brak seeda (żeby nie kolidował z testami „auto bez dokumentów” i 409), brak edycji i usuwania (`A-Q12`), typy TS `VehicleDocumentType`, bo `DocumentType` koliduje z globalnym typem DOM.
- **Część 2 ręcznie, test-first:** przed kodem przedstawiłem listę testów i tabelę przejść do akceptacji. Testy w osobnym commicie `5dadebd`, potem kod backendu `61c4344` i frontendu `8e24615`, każdy etap robił osobny subagent. Zakres: 409 aktywnego typu, `PUT …/status` z tabelą przejść (`DocumentStatus.canChangeTo`), `@NotInFuture` (z `Clock`) i `expiryDate ≥ issueDate`, `V7 registration_by_dealer` z `PUT …/registration-by-dealer`, `GET …/handover-readiness`, sekcja w szczegółach auta (gotowość z backendu, przyciski statusu, checkbox flagi). Wynik: 161 testów backendu, 36 frontendu.

## Gdzie AI się pomyliło

- **Regex telefonu w intencie** odrzucał typowe zapisy (`+48 600 100 200`), a po mojej poprawce przepuszczał `+---------`. Wyłapane przeglądem, naprawione dwoma commitami (`a40ec30`, `aead8a5`).
- **Scenariusz 5 w `INTENT_B.md`** zakładał jeden błąd dla pustego `phone`, a są dwa (`@NotBlank` i `@Pattern`). Skill zauważył to przy testach i zapisał jako założenie, kod był poprawny. Poprawiłem intent, nie skill (`781f511`). Tak naprawdę błąd na etapie akceptacji.
- **Skrypt budujący intent** nadpisał jeden wiersz tabeli; agent zauważył to sam przy przeglądzie.
- **Bean `Clock` w złym miejscu (plan feature A):** plan zakładał `config.ClockConfig` (`@Configuration`). Subagent piszący testy sprawdził to na kodzie tymczasowym, a `@WebMvcTest` nie ładuje `@Configuration`. Zamrożony `VehicleDocumentControllerTest` ze skilla padłby wtedy z `No qualifying bean of type 'java.time.Clock'` (walidator `@NotInFuture` wstrzykuje `Clock`). Decyzja: bean w `DealershipApplication`, który ładuje się w każdym teście wycinka. Pozostałość: komentarz w zamrożonym `FixedClockTestConfig` wspomina `ClockConfig`.
- **Subagent testów wyszedł poza zaakceptowaną listę:** dodał przypadki (`rejectedDocumentStaysRejected`, granica „dziś” bez daty wydania) i wzmocnił `usesHandoverDateEvenWhenInPast`. Zostawione, bo zaostrzają reguły, a nie je osłabiają. Nazwy testów frontu w camelCase zamiast zdań, inaczej niż w istniejących specach.

Nie było przypadku, w którym wygenerowany kod trzeba było cofać.

## Problemy środowiskowe

- **Zła wersja Node w `./test.sh`:** domyślny `node` w PATH to 24.0.1, a Angular CLI wymaga 24.15+ (`.nvmrc`). Skrypty chodzą w niinteraktywnym shellu bez nvm, więc `check-node.sh` odrzucał Node i `./test.sh` kończył się exit 1, co agent mylnie czytał jako porażkę testów i tracił czas na diagnozę. Naprawa: `check-node.sh` sam ładuje nvm i robi `nvm use` wg `.nvmrc`, gdy bieżący Node jest za stary. Komunikat błędu zostaje, gdy nvm nie ma.
- **Świeży worktree bez `node_modules`:** przed pierwszym `./test.sh` trzeba było `npm ci`.
- **`main` przesunął się w trakcie feature A** (moje commity `DECISIONS.md` i `check-node.sh`), więc `merge --ff-only` się zatrzymał, tak jak wymaga `CLAUDE.md`. Po mojej zgodzie: rebase gałęzi na `main`, a osobny `DECISIONS.md` agenta wpleciony w ten plik.
- **Commit backendu `61c4344` przy czerwonym froncie:** świadomy wyjątek od zasady „`./test.sh` przed commitem”, bo testy i kod były osobnymi etapami. Od `8e24615` całość jest zielona.

## Decyzje projektowe (zbiorczo)

- **Intent ponad zgadywanie:** niejasności zapisywane jako założenia z uzasadnieniem i informacją, co zmienić, gdy klient zdecyduje inaczej. Przy konflikcie w kliencie wygrywa Anna, a spór o odrzucone dokumenty (`A-Q1`) zostaje otwarty do decyzji dyrektora.
- **Reguły biznesowe tylko w backendzie**, frontend je wyświetla. Czas przez wstrzyknięty `Clock`.
- **Zakres wąski i zgodny z kontraktem:** bez skanów, audytu, edycji i usuwania dokumentów, bez powiązania klient–auto i bez SMS-ów (odłożone przez klienta). Rozszerzenia kontraktu ograniczone do tego, co konieczne (`GET` dokumentu dla `Location`, osobny `PUT` flagi rejestracji, bo zamrożone testy pojazdów nie pozwalają zmienić formularza).
- **Doprecyzowania kontraktu w feature A** (kontrakt mówi tylko „niedozwolona zmiana = 409”): `REJECTED` końcowy (`A-Q1`, ryzyko przy innej decyzji dyrektora), `VERIFIED → REJECTED` dozwolone (`A-Q3`), zmiana na ten sam status = 409, pełnomocnictwo ignorowane bez flagi (`A-Q8`), `EXPIRED` liczone na dziś, gdy brak daty wydania (`A-Q4`), `PENDING` może dać jednocześnie `NOT_VERIFIED` i `EXPIRED` (`A-Q2`). Do tego 400 dla `issueDate` z przyszłości i `expiryDate < issueDate` (`A-R10`, `A-Q10`).
- **Flaga rejestracji poza `VehicleRequest`** (backend i front): `PUT` pojazdu jej nie zeruje, a front czyta ją osobnym `httpResource` z `GET /api/vehicles/{id}`. `VehicleResponse` ma dodatkowy 7-argumentowy konstruktor dla zamrożonych testów. Front pokazuje tylko przyciski dozwolonych przejść; decyzję i 409 podejmuje backend.
- **Proste reguły walidacji** zamiast rozbudowanych (format telefonu i NIP bez normalizacji i sumy kontrolnej, e-mail porównywany dokładnie, NIP nieunikalny).
- **Unikalność aktywnego dokumentu w serwisie**, bez ograniczenia w bazie, bo odrzucone dokumenty muszą zostać w historii. Wyścig dwóch zapisów świadomie bez siatki.
- **Proces:** worktree per feature, subagenci po jednym na etap, testy zamrożone przed kodem, intent poprawiam przy błędzie intentu, skill tylko przy luce skilla.

Szczegóły i pełne listy założeń: `INTENT_A.md`, `INTENT_B.md`, `docs/slices/*.md`.
