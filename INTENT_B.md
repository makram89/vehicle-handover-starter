# B: Klienci

Cytaty: `docs/transcript.md` (`L<n>`). Kontrakt: `CRUD /api/customers` według konwencji modułu pojazdów (`BRIEF.md`). Generowane wyłącznie skillem `feature-slice`.

## 1. Cel
Prosta kartoteka klientów salonu (dodawanie, lista, podgląd, edycja, usuwanie), na razie niepowiązana z autami.

## 2. Wymagania
| ID | Treść | Status | Źródło |
|---|---|---|---|
| B-R1 | Lista klientów z pełnym CRUD pod `/api/customers` | CONFIRMED | L23 „Chcemy prostą listę klientów” |
| B-R2 | Pola: imię, nazwisko, e-mail, telefon | CONFIRMED | L23 „imię, nazwisko, e-mail, telefon” |
| B-R3 | NIP dla klientów firmowych, opcjonalny | CONFIRMED | L23 „a dla firm NIP” |
| B-R4 | E-mail unikalny, duplikat → 409 | CONFIRMED | L23 „E-mail nie może się powtarzać.” |

## 3. Model danych
**`Customer`**, tabela `customer`, API `/api/customers`, frontend `frontend/src/app/customers/`, nawigacja `Customers`.

| Pole | Java / SQL | Wymagane | Walidacja (`CustomerRequest`) | Źródło |
|---|---|---|---|---|
| `id` | `Long` / `BIGINT IDENTITY` | auto | | konwencja |
| `firstName` | `String` / `VARCHAR(50) NOT NULL` | tak | `@NotBlank @Size(max = 50)` | B-R2, B-Q1 |
| `lastName` | `String` / `VARCHAR(50) NOT NULL` | tak | `@NotBlank @Size(max = 50)` | B-R2, B-Q1 |
| `email` | `String` / `VARCHAR(100) NOT NULL`, `CONSTRAINT uq_customer_email UNIQUE (email)` | tak, unikalny | `@NotBlank @Email @Size(max = 100)` | B-R4, B-Q3 |
| `phone` | `String` / `VARCHAR(25) NOT NULL` | tak | `@NotBlank @Pattern(regexp = "\\+?[0-9][0-9 \\-]{7,18}[0-9]", message = "must be 9 to 20 characters: digits, spaces, hyphens, optional leading +, starting and ending with a digit")` | B-R2, B-Q1, B-Q2 |
| `nip` | `String` / `VARCHAR(10)` | nie | `@Pattern(regexp = "[0-9]{10}", message = "must be 10 digits")` | B-R3, B-Q2, B-Q4 |

Frontend: etykiety `First name`, `Last name`, `Email`, `Phone`, `NIP`; `required` dla czterech pierwszych (`"First name is required"` itd.); pusty NIP → `null`.

| Metoda i ścieżka | Sukces | Błędy |
|---|---|---|
| `GET /api/customers` | 200, sortowanie po `id` | |
| `GET /api/customers/{id}` | 200 | 404 `Customer with id 99 not found` |
| `POST /api/customers` | 201 + `Location` | 400, 409 `Customer with email jan@example.com already exists` |
| `PUT /api/customers/{id}` | 200 | 400, 404, 409 (`existsByEmailAndIdNot`) |
| `DELETE /api/customers/{id}` | 204 | 404 |

Migracje: `V<n>__customer.sql` i `V<n+1>__customer_seed.sql` (2–3 rekordy, w tym jeden z NIP). B powstaje przed A, więc dostaje niższe numery.

## 4. Reguły biznesowe
| Reguła | Gdzie | Wynik |
|---|---|---|
| E-mail zajęty przez innego klienta (POST: `existsByEmail`, PUT: `existsByEmailAndIdNot`) | serwis | 409 `ConflictException` |
| Wyścig przy zapisie tego samego e-maila | `uq_customer_email` | 409 przez `DataIntegrityViolationException` |
| Pozostałe reguły (format, wymagalność) | `CustomerRequest` | 400 `Validation failed`, `errors` posortowane po `field` |

## 5. Kryteria akceptacji
| # | Dado | Kiedy | Wtedy |
|---|---|---|---|
| 1 | brak klientów | POST kompletny, bez `nip` | 201, `Location: /api/customers/{id}`, `nip=null` (B-R1, B-R3) |
| 2 | klient z `jan@example.com` | POST z tym samym e-mailem | 409 `application/problem+json`, `detail: Customer with email jan@example.com already exists` (B-R4) |
| 3 | klient 1 z `jan@example.com` | PUT klienta 1 z tym samym e-mailem | 200 (własny e-mail nie jest konfliktem) |
| 4 | klienci 1 i 2 | PUT klienta 2 z e-mailem klienta 1 | 409 (B-R4) |
| 5 | — | POST z `firstName`, `lastName`, `email` = `""` i bez `phone` (`null`) | 400, 4 błędy `must not be blank` po `field`: `email`, `firstName`, `lastName`, `phone`; serwis nie wywołany. Uwaga: `phone` = `""` daje dla `phone` dwa błędy (`@NotBlank` i `@Pattern`), razem 5 |
| 6 | — | POST z `nip` = `123`, `phone` = `abc`, `email` = `x` | 400 z błędami pól `email`, `nip`, `phone` |
| 7 | brak klienta 99 | GET / PUT / DELETE 99 | 404 `Customer with id 99 not found` |
| 8 | klient 1 | DELETE 1 | 204, potem GET 1 → 404 |
| 9 | formularz klienta | submit z pustym imieniem | po `touched()` komunikat `First name is required`; 409 z backendu → błąd całego formularza |

## 6. Poza zakresem
| ID | Co | Status | Źródło |
|---|---|---|---|
| B-R5 | Powiązanie klienta z autami | DEFERRED | L23 „Na razie bez powiązań z autami, to później.” |

## 7. Pytania i założenia
| # | Status | Pytanie do klienta | Założenie | Uzasadnienie | Jeśli klient zdecyduje inaczej |
|---|---|---|---|---|---|
| B-Q1 | ASSUMED | Które pola są obowiązkowe (L23 „imię, nazwisko, e-mail, telefon”)? | Wszystkie cztery wymagane, NIP opcjonalny | Anna wymienia je dla każdego klienta, a NIP tylko dla firm. Telefon będzie potrzebny do SMS (A-R15) | Zdjąć `@NotBlank`/`NOT NULL` z `phone` (nowa migracja) |
| B-Q2 | ASSUMED | Jaki format telefonu i NIP? | Telefon: opcjonalny `+` na początku, potem 9–20 znaków: cyfry, spacje, myślniki, pierwszy i ostatni znak to cyfra (`+48 600 100 200` i `600-100-200` przechodzą, `+---------` daje 400). NIP: dokładnie 10 cyfr, bez sumy kontrolnej | Typowe zapisy numeru ze spacjami i myślnikami nie mogą dawać 400. Prosta reguła `@Pattern`, którą skill generuje | Normalizacja telefonu do samych cyfr w mapperze; walidator sumy kontrolnej NIP jak `ProductionYear` |
| B-Q3 | ASSUMED | Czy `Jan@x.pl` i `jan@x.pl` to ten sam e-mail? | Porównanie dokładne, wielkość liter ma znaczenie | Zgodne z wzorcem VIN i constraintem `UNIQUE` | Zapisywać e-mail małymi literami w mapperze, `existsByEmailIgnoreCase` |
| B-Q4 | ASSUMED | Czy NIP musi być unikalny? | Nie | L23 wymienia unikalność tylko dla e-maila. Kilka osób kontaktowych tej samej firmy ma ten sam NIP | `existsByNip` + `uq_customer_nip` |
| B-Q5 | ASSUMED | Czy klient firmowy potrzebuje nazwy firmy albo pola typu klienta (osoba/firma)? | Nie: firma = klient z wypełnionym NIP | L23 wymienia tylko NIP | Pole `companyName` (opcjonalne) lub enum `CustomerType` |
