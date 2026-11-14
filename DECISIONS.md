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

## Gdzie AI się pomyliło

- **Regex telefonu w intencie** odrzucał typowe zapisy (`+48 600 100 200`), a po mojej poprawce przepuszczał `+---------`. Wyłapane przeglądem, naprawione dwoma commitami (`a40ec30`, `aead8a5`).
- **Scenariusz 5 w `INTENT_B.md`** zakładał jeden błąd dla pustego `phone`, a są dwa (`@NotBlank` i `@Pattern`). Skill zauważył to przy testach i zapisał jako założenie, kod był poprawny. Poprawiłem intent, nie skill (`781f511`). Tak naprawdę błąd na etapie akceptacji.
- **Skrypt budujący intent** nadpisał jeden wiersz tabeli; agent zauważył to sam przy przeglądzie.

Nie było przypadku, w którym wygenerowany kod trzeba było cofać.

## Problemy środowiskowe

- **Zła wersja Node w `./test.sh`:** domyślny `node` w PATH to 24.0.1, a Angular CLI wymaga 24.15+ (`.nvmrc`). Skrypty chodzą w niinteraktywnym shellu bez nvm, więc `check-node.sh` odrzucał Node i `./test.sh` kończył się exit 1, co agent mylnie czytał jako porażkę testów i tracił czas na diagnozę. Naprawa: `check-node.sh` sam ładuje nvm i robi `nvm use` wg `.nvmrc`, gdy bieżący Node jest za stary. Komunikat błędu zostaje, gdy nvm nie ma.

## Decyzje projektowe (zbiorczo)

- **Intent ponad zgadywanie:** niejasności zapisywane jako założenia z uzasadnieniem i informacją, co zmienić, gdy klient zdecyduje inaczej. Przy konflikcie w kliencie wygrywa Anna, a spór o odrzucone dokumenty (`A-Q1`) zostaje otwarty do decyzji dyrektora.
- **Reguły biznesowe tylko w backendzie**, frontend je wyświetla. Czas przez wstrzyknięty `Clock`.
- **Zakres wąski i zgodny z kontraktem:** bez skanów, audytu, edycji i usuwania dokumentów, bez powiązania klient–auto i bez SMS-ów (odłożone przez klienta). Rozszerzenia kontraktu ograniczone do tego, co konieczne (`GET` dokumentu dla `Location`, osobny `PUT` flagi rejestracji, bo zamrożone testy pojazdów nie pozwalają zmienić formularza).
- **Proste reguły walidacji** zamiast rozbudowanych (format telefonu i NIP bez normalizacji i sumy kontrolnej, e-mail porównywany dokładnie, NIP nieunikalny).
- **Unikalność aktywnego dokumentu w serwisie**, bez ograniczenia w bazie, bo odrzucone dokumenty muszą zostać w historii. Wyścig dwóch zapisów świadomie bez siatki.
- **Proces:** worktree per feature, subagenci po jednym na etap, testy zamrożone przed kodem, intent poprawiam przy błędzie intentu, skill tylko przy luce skilla.

Szczegóły i pełne listy założeń: `INTENT_A.md`, `INTENT_B.md`, `docs/slices/*.md`.
