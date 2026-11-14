# DECISIONS

**Cały kod i wszystkie pliki w repo napisał agent (Claude), na moje polecenia. Nie poprawiałem niczego ręcznie.** Gdzie niżej stoi „ręcznie", chodzi o część poza skillem (reguły biznesowe pisane test-first), nie o moje edycje. Wszystkie poprawki zlecałem agentowi. Wyjątkiem jest plik DECISIONS.MD

**Feature A nie jest skończony.** Część 1 (skill) jest na gałęzi `feat/vehicle-documents`, niescalonej z `main`. Część 2 (reguły, gotowość, `Clock`, flaga pojazdu) jest w toku i nie ma jej jeszcze w gicie.

## Proces

1. `CLAUDE.md` z lektury modułu pojazdów (`2cb3dd4`).
2. Analiza transkrypcji: `docs/transcript.md`, `INTENT.md`, `INTENT_A.md`, `INTENT_B.md`, wymagania ze statusami i cytatami po numerach linii (`a40ec30`, `aead8a5`).
3. Skill `feature-slice` (`482a969`) i praca z gitem na worktree per feature z `--ff-only` (`f56fc5d`).
4. Feature B tylko skillem (`0c9834e`…`b1242fd`), potem feature A: część 1 skillem (`4c41b7a`…`18cd001`), część 2 ręcznie test-first.

Skill działa etapami (spec, testy na czerwono, backend, frontend, raport), każdy etap robi osobny subagent, a orkiestrator po każdym uruchamia testy. Testy po commicie na czerwono są zamrożone. Założenia skill zapisuje w `docs/slices/<encja>.md`. Kontekst między sesjami niosły pliki, nie pamięć agenta.

## Feature B (klienci)

Skill wygenerował całość: migracje z seedem, pakiet `customer/`, 33 testy backendu, spec formularza i moduł Angulara. `./test.sh` zielony za pierwszym razem (62 testy backendu, 18 frontendu). **Iteracji skilla: zero, poprawek kodu: zero.** Jedyna zmiana po generowaniu dotyczyła intentu (`781f511`, niżej).

## Feature A (dokumenty i gotowość)

- **Skill (część 1, zero iteracji):** tabela `vehicle_document` (`V6`), pakiet `document/`, lista, `GET /{id}`, POST, 400 i 404, moduł `documents/`. Backend 92 testy, frontend 26, testy bez zmian od `TEST_BASE`.
- **Poza wzorcem, skill sam to wypisał w raporcie:** przejścia statusów, 409 na aktywny dokument tego samego typu, walidacje dat, flaga `registrationByDealer` w pojeździe, endpoint `handover-readiness` z `Clock`, widok gotowości.
- **Część 2 (plan, w toku):** te reguły pisane test-first: testy i commit, potem kod. Wynik uzupełnię po zakończeniu.

## Gdzie AI się pomyliło

- **Regex telefonu w intencie** odrzucał typowe zapisy (`+48 600 100 200`), a po mojej poprawce przepuszczał `+---------`. Wyłapane przeglądem, naprawione dwoma commitami (`a40ec30`, `aead8a5`).
- **Scenariusz 5 w `INTENT_B.md`** zakładał jeden błąd dla pustego `phone`, a są dwa (`@NotBlank` i `@Pattern`). Skill zauważył to przy testach i zapisał jako założenie, kod był poprawny. Poprawiłem intent, nie skill (`781f511`).
- **Skrypt budujący intent** nadpisał jeden wiersz tabeli; agent zauważył to sam przy przeglądzie.
- **Środowisko:** domyślny Node w PATH jest za stary (`check-node.sh` zatrzymuje `./test.sh`), więc w każdej sesji trzeba `nvm use`. Wyłapane uruchomieniem testów.
- **Blokada harnessu** (zapis tylko w worktree): jedna sesja ominęła ją przez powłokę i sama o tym napisała, druga się zatrzymała i zapytała. Wyłączyłem izolację (`a71f325`).

Nie było przypadku, w którym wygenerowany kod trzeba było cofać.

## Decyzje projektowe (zbiorczo)

- **Intent ponad zgadywanie:** niejasności zapisywane jako założenia z uzasadnieniem i informacją, co zmienić, gdy klient zdecyduje inaczej. Przy konflikcie w kliencie wygrywa Anna, a spór o odrzucone dokumenty (`A-Q1`) zostaje otwarty do decyzji dyrektora.
- **Reguły biznesowe tylko w backendzie**, frontend je wyświetla. Czas przez wstrzyknięty `Clock`.
- **Zakres wąski i zgodny z kontraktem:** bez skanów, audytu, edycji i usuwania dokumentów, bez powiązania klient–auto i bez SMS-ów (odłożone przez klienta). Rozszerzenia kontraktu ograniczone do tego, co konieczne (`GET` dokumentu dla `Location`, osobny `PUT` flagi rejestracji, bo zamrożone testy pojazdów nie pozwalają zmienić formularza).
- **Proste reguły walidacji** zamiast rozbudowanych (format telefonu i NIP bez normalizacji i sumy kontrolnej, e-mail porównywany dokładnie, NIP nieunikalny).
- **Unikalność aktywnego dokumentu w serwisie**, bez ograniczenia w bazie, bo odrzucone dokumenty muszą zostać w historii. Wyścig dwóch zapisów świadomie bez siatki.
- **Proces:** worktree per feature, subagenci po jednym na etap, testy zamrożone przed kodem, intent poprawiam przy błędzie intentu, skill tylko przy luce skilla.

Szczegóły i pełne listy założeń: `INTENT_A.md`, `INTENT_B.md`, `docs/slices/*.md`.

## Czego nie zrobiłem i co dalej

- Dokończyć część 2 feature A, scalić `feat/vehicle-documents` (`--ff-only`, a `main` już poszedł do przodu, więc trzeba zdecydować jak) i uzupełnić ten plik o jej wynik.
- Decyzja dyrektora w sprawie odrzuconych dokumentów (`A-Q1`).
- Odłożone: SMS o gotowości, powiązanie klient–auto, logowanie i role.
- Luki skilla bez poprawek, bo tak zdecydowałem: sprzeczność w intencie wygląda w raporcie jak zwykłe założenie, brak instrukcji dla `node_modules` w nowym worktree.
- Nie porównałem skilla z agentem bez skilla na obcej encji; mam tylko dwa przebiegi, oba bez iteracji.
