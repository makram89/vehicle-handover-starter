# A: Dokumenty pojazdu i gotowość do wydania

Cytaty: `docs/transcript.md` (`L<n>`). Kontrakt: sekcja *Kontrakt API* w `BRIEF.md`. Klient: Anna (decyduje), Tomek; dostawca: Kasia.

## 1. Cel
Zastąpić papierową checklistę (L2): salon wpisuje dokumenty auta i je weryfikuje, a system sam mówi, czy auto jest gotowe do wydania i czego brakuje.

## 2. Wymagania
| ID | Treść | Status | Źródło |
|---|---|---|---|
| A-R1 | Każde auto wymaga `SALES_AGREEMENT`, `INVOICE`, `COC`, `OC_POLICY` | CONFIRMED | L4 „Bez tych czterech auto nie wyjeżdża.” |
| A-R2 | `REGISTRATION_POA` jest wymagane tylko, gdy salon rejestruje auto za klienta | CONFIRMED | L4 „potrzebne tylko wtedy, kiedy to my rejestrujemy auto za klienta” |
| A-R3 | Flaga przy aucie *salon rejestruje* (`registrationByDealer`), ustawiana ręcznie, domyślnie `false` | CONFIRMED | L9 „Domyślnie klient rejestruje sam” |
| A-R4 | Nowy dokument ma status `PENDING` | CONFIRMED | L11 „dokument czeka na weryfikację” |
| A-R5 | Weryfikacja: `PENDING` → `VERIFIED` albo `REJECTED` | CONFIRMED | L11 „jest albo zweryfikowany, albo odrzucony” |
| A-R6 | Po poprawce przestawić `REJECTED` → `VERIFIED` | SUPERSEDED by A-R7 | L12 „przestawiamy status na zweryfikowany” |
| A-R7 | `REJECTED` jest stanem końcowym, poprawka = nowy dokument (decyzja tymczasowa, A-Q1) | CONFIRMED | L26 „odrzucony zostaje odrzucony, a poprawka to nowy dokument” |
| A-R8 | `expiryDate` opcjonalna, umowa i faktura jej nie mają | CONFIRMED | L14 „Umowa i faktura nie mają.” |
| A-R9 | Dokument musi być ważny w dniu wydania, włącznie z tym dniem | CONFIRMED | L14 „ten dzień się jeszcze liczy” |
| A-R10 | `issueDate` nie może być z przyszłości (dziś jest dozwolone) | CONFIRMED | L16 „Nie może być z przyszłości.” |
| A-R11 | Najwyżej jeden aktywny (nie `REJECTED`) dokument danego typu na auto; odrzucone zostają w historii | CONFIRMED | L18 „Odrzucone mogą wisieć w historii, ale aktywny tylko jeden.” |
| A-R12 | Odrzucony dokument liczy się jak brak dokumentu | CONFIRMED | L18 „Odrzucony to tak, jakby dokumentu nie było” |
| A-R13 | W aucie widać gotowość do wydania i listę braków | CONFIRMED | L19 „gotowe do wydania albo czego brakuje” |
| A-R14 | Bez ustalonej daty wydania (`plannedHandoverDate`) auto nie jest gotowe | CONFIRMED | L20 „nie może pokazywać, że gotowe” |

## 3. Model danych
**`VehicleDocument`**, tabela `vehicle_document` (A-R4…A-R11, kontrakt):

| Pole | Java / SQL | Wymagane | Walidacja / uwagi |
|---|---|---|---|
| `id` | `Long` / `BIGINT IDENTITY` | auto | |
| `vehicleId` | `Long` / `BIGINT NOT NULL`, FK `vehicle(id) ON DELETE CASCADE` | z URL, nie z body | A-Q12 |
| `type` | `DocumentType` / `VARCHAR(20) NOT NULL` | tak | `@NotNull` |
| `number` | `String` / `VARCHAR(50) NOT NULL` | tak | `@NotBlank @Size(max = 50)`, A-Q11 |
| `issueDate` | `LocalDate` / `DATE NOT NULL` | tak | `@NotNull`, ≤ dziś z `Clock`: `must not be in the future` (A-R10) |
| `expiryDate` | `LocalDate` / `DATE` | nie | ≥ `issueDate`: `must not be before issue date` (A-Q10) |
| `status` | `DocumentStatus` / `VARCHAR(20) NOT NULL` | serwer | POST zawsze `PENDING`, pola nie ma w body (A-R4) | Bez `UNIQUE` w bazie (odrzucone się powtarzają), unikalność aktywnego sprawdza serwis (A-R11).

**Enumy** (kolejność deklaracji = kolejność w odpowiedzi gotowości): `DocumentType`: `SALES_AGREEMENT`, `INVOICE`, `COC`, `OC_POLICY`, `REGISTRATION_POA`. `DocumentStatus`: `PENDING`, `VERIFIED`, `REJECTED`. `ReadinessReason`: `NOT_VERIFIED`, `EXPIRED`, `NO_HANDOVER_DATE`.

**Rekordy:** `DocumentRequest(type, number, issueDate, expiryDate)`, `DocumentStatusRequest(@NotNull status)`, `DocumentResponse(id, vehicleId, type, number, issueDate, expiryDate, status)`, `HandoverReadinessResponse(boolean ready, List<DocumentType> missingDocumentTypes, List<ReadinessProblem> problems)`, `ReadinessProblem(DocumentType documentType /* null przy NO_HANDOVER_DATE */, ReadinessReason reason)`. Kształt dokładnie jak w kontrakcie, bez dodatkowych pól.

**Zmiana `Vehicle`** (A-R3, A-Q7): `registrationByDealer`: `boolean` / `BOOLEAN NOT NULL DEFAULT FALSE` (nowa migracja). Pole jest w `VehicleResponse`, nie ma go w `VehicleRequest`. `plannedHandoverDate` (już istnieje) = data wydania.

**Endpointy** (404: `Vehicle with id 99 not found`, `Document with id 5 not found`, także gdy dokument należy do innego auta):

| Metoda i ścieżka | Sukces | Błędy |
|---|---|---|
| `GET /api/vehicles/{vehicleId}/documents` | 200, sortowanie po `id` | 404 |
| `GET /api/vehicles/{vehicleId}/documents/{documentId}` (konwencja, cel `Location`) | 200 | 404 |
| `POST /api/vehicles/{vehicleId}/documents` | 201 + `Location` | 400, 404, 409 `Vehicle with id 1 already has an active INVOICE document` |
| `PUT /api/vehicles/{vehicleId}/documents/{documentId}/status` | 200 `DocumentResponse` | 400, 404, 409 `Cannot change document status from REJECTED to VERIFIED` |
| `GET /api/vehicles/{vehicleId}/handover-readiness` | 200 | 404 |
| `PUT /api/vehicles/{vehicleId}/registration-by-dealer` body `{"registrationByDealer": true}` | 200 `VehicleResponse` | 400 (`@NotNull`), 404 |

## 4. Reguły biznesowe
**Przejścia statusu** (`PUT …/status`; wiersz = obecny status, kolumna = docelowy):

| Z \ Na | `PENDING` | `VERIFIED` | `REJECTED` |
|---|---|---|---|
| `PENDING` | 409 | 200 (A-R5) | 200 (A-R5) |
| `VERIFIED` | 409 | 409 | 200 (A-Q3) |
| `REJECTED` | 409 | 409 (A-R7) | 409 (A-R7) |

**Dodanie** (`POST`): 409, gdy auto ma już dokument tego `type` w statusie `PENDING` lub `VERIFIED` (A-R11). Odrzucony nie blokuje (A-R12).

**Gotowość** (`GET handover-readiness`):
1. `required` = A-R1 + `REGISTRATION_POA`, gdy `registrationByDealer` (A-R2). Pozostałe typy są pomijane (A-Q8).
2. `refDate` = `plannedHandoverDate`, a gdy jej brak: dziś z `Clock` (A-R9, A-Q4, A-Q13).
3. Dla każdego typu z `required` w kolejności enumu wybierz dokument aktywny (nie `REJECTED`):
   brak → `missingDocumentTypes` += typ; status `PENDING` → problem `(typ, NOT_VERIFIED)`; `expiryDate != null && expiryDate < refDate` → problem `(typ, EXPIRED)`, po `NOT_VERIFIED`.
4. `plannedHandoverDate == null` → problem `(null, NO_HANDOVER_DATE)` na końcu listy (A-R14).
5. `ready = missingDocumentTypes.isEmpty() && problems.isEmpty()`.

## 5. Kryteria akceptacji
Dziś (`Clock`) = 2026-11-14; *auto X* ma `plannedHandoverDate` 2026-12-15 i `registrationByDealer = false`, chyba że podano inaczej.

| # | Dado | Kiedy | Wtedy |
|---|---|---|---|
| 1 | auto X bez dokumentów | GET readiness | `ready=false`, `missing=[SALES_AGREEMENT, INVOICE, COC, OC_POLICY]`, `problems=[]` (A-R1) |
| 2 | j.w., `registrationByDealer=true` | GET readiness | `missing` kończy się `REGISTRATION_POA` (A-R2) |
| 3 | nowe auto | PUT `registration-by-dealer` `true`, potem GET auta | `registrationByDealer=true`; nowe auto ma `false` (A-R3) |
| 4 | auto X | POST `INVOICE`, `issueDate` 2026-11-14 | 201, `status=PENDING`, `Location` (A-R4, A-R10 granica) |
| 5 | auto X | POST z `issueDate` 2026-11-15 | 400, `errors[0].field=issueDate` (A-R10) |
| 6 | dokument `PENDING` | PUT `VERIFIED` / PUT `REJECTED` | 200 z nowym statusem (A-R5) |
| 7 | dokument `REJECTED` | PUT `VERIFIED` | 409, status bez zmian (A-R7) |
| 8 | dokument `VERIFIED` | PUT `PENDING`; PUT `VERIFIED`; PUT `REJECTED` | 409; 409; 200 (A-Q3) |
| 9 | aktywny `INVOICE` (`PENDING` lub `VERIFIED`) | POST `INVOICE` | 409 (A-R11) |
| 10 | tylko odrzucony `INVOICE` | POST `INVOICE`, potem GET listy | 201; lista zawiera oba dokumenty (A-R11) |
| 11 | 4 typy `VERIFIED`, ale `COC` `REJECTED` | GET readiness | `missing=[COC]` (A-R12) |
| 12 | komplet `VERIFIED`, `OC_POLICY.expiryDate` = 2026-12-15 | GET readiness | `ready=true`, `missing=[]`, `problems=[]` (A-R9 granica) |
| 13 | j.w., `expiryDate` 2026-12-14 | GET readiness | `problems=[{OC_POLICY, EXPIRED}]` (A-R9) |
| 14 | komplet `VERIFIED`, `INVOICE` bez `expiryDate` | GET readiness | brak `EXPIRED` dla `INVOICE` (A-R8) |
| 15 | komplet, `INVOICE` `PENDING` | GET readiness | `problems=[{INVOICE, NOT_VERIFIED}]` (A-Q2) |
| 16 | komplet `VERIFIED`, brak `plannedHandoverDate`, `OC_POLICY.expiryDate` 2026-11-13 | GET readiness | `problems=[{OC_POLICY, EXPIRED}, {null, NO_HANDOVER_DATE}]`, `ready=false` (A-R14, A-Q4) |
| 17 | flaga `false`, `REGISTRATION_POA` `PENDING` i przeterminowany | GET readiness | POA nie pojawia się w odpowiedzi (A-Q8) |
| 18 | auto X | POST z `expiryDate` < `issueDate` | 400, `field=expiryDate` (A-Q10) |
| 19 | auto 99 nie istnieje | dowolny endpoint z §3 | 404 `Vehicle with id 99 not found` |

## 6. Poza zakresem
| ID | Co | Status | Źródło |
|---|---|---|---|
| A-R15 | SMS do klienta, gdy auto jest gotowe (zgłosił Tomek w L21, przesunięte przez dostawcę) | DEFERRED | L22 „zostawiamy je na kolejny etap” |

## 7. Pytania i założenia
| # | Status | Pytanie do klienta | Założenie | Uzasadnienie | Jeśli klient zdecyduje inaczej |
|---|---|---|---|---|---|
| A-Q1 | OPEN | Konflikt: Tomek chce przestawiać odrzucony na zweryfikowany (L25 „zawsze robiliśmy tak, że przestawiamy status”), Anna: L24 „Odrzucony dokument zostaje odrzucony.” Co postanowi dyrektor? | Wersja Anny (A-R7) | Anna decyduje do czasu rozmowy z dyrektorem (L26 „Na razie róbcie po mojemu”); kontrakt (*aktywny (nieodrzucony)*) jest z tym zgodny | Dopuścić `REJECTED → VERIFIED`, gdy brak innego aktywnego dokumentu tego typu. **Ryzyko testów:** jeśli oceniający oczekują 200, dostaną 409 |
| A-Q2 | ASSUMED | Czy niezweryfikowany dokument blokuje wydanie? | Tak, `NOT_VERIFIED`. Ważność sprawdzamy także dla `PENDING`, więc ten sam dokument może dać jednocześnie `NOT_VERIFIED` i `EXPIRED` (§4 pkt 3) | Kontrakt ma ten powód, a L11 opisuje weryfikację jako obowiązkowy krok | Pominąć `PENDING` w problemach |
| A-Q3 | ASSUMED | Czy zweryfikowany dokument można później odrzucić? Czy da się wrócić do `PENDING`? | `VERIFIED → REJECTED` tak; powrót do `PENDING` i zmiana na ten sam status: 409 | Bez tego błędnie zweryfikowanego dokumentu nie da się zastąpić, bo A-R11 blokuje nowy | Zablokować `VERIFIED → REJECTED` (zmiana jednej komórki tabeli). **Ryzyko testów:** inna macierz 409 |
| A-Q4 | ASSUMED | Jak oceniać ważność, gdy nie ma daty wydania? | Na dziś (`Clock`) | Przykład z kontraktu pokazuje `EXPIRED` razem z `NO_HANDOVER_DATE` | Pomijać `EXPIRED` bez daty wydania. **Ryzyko testów:** przykład z kontraktu |
| A-Q5 | ASSUMED | Czy system ma przechowywać skany (L11 „Jak ktoś wgra skan”)? | Nie, tylko metadane | Kontrakt nie ma pola pliku | Osobny endpoint uploadu i storage |
| A-Q6 | ASSUMED | Czy zapisywać, kto zweryfikował lub odrzucił dokument i dlaczego (L11 „Ja albo Tomek sprawdzamy”, L24 „przy kontroli nie widać, co było źle”)? | Nie: bez logowania, ról i komentarza | Body statusu w kontrakcie ma tylko `status` | Opcjonalne `comment` w `DocumentStatusRequest` i `statusChangedAt` z `Clock` |
| A-Q7 | ASSUMED | Czy flagę rejestracji można zmieniać w dowolnym momencie (L9 „Tomek może to zaznaczać przy aucie”)? | Tak, osobnym `PUT …/registration-by-dealer` | Zamrożone `vehicle-form.spec.ts:107` i `VehicleControllerTest` sprawdzają dokładne body i liczbę błędów pojazdu. PUT pojazdu bez flagi by ją zerował | Pole w formularzu pojazdu wymaga zmiany zamrożonych testów (zgoda prowadzącego) |
| A-Q8 | ASSUMED | Czy pełnomocnictwo dodane przy aucie bez flagi ma wpływ na gotowość? | Nie, jest ignorowane | L9 „wymagane tylko przy autach, gdzie Tomek to zaznaczy” | Raportować jego problemy |
| A-Q9 | ASSUMED | Czy polisa i pełnomocnictwo muszą mieć datę ważności (L14 „Polisa ma, pełnomocnictwo zwykle też.”)? | Nie wymuszamy; brak daty = ważny bezterminowo | Kontrakt: *opcjonalnie `expiryDate`*. Wymuszenie dałoby 400 tam, gdzie testy mogą oczekiwać 201 | `@NotNull` warunkowe dla `OC_POLICY` (400 `expiryDate`) |
| A-Q10 | ASSUMED | Czy data ważności może być wcześniejsza niż wystawienia? | Nie, 400 | Oczywisty błąd wpisu, analogicznie do L16 | Usunąć walidator |
| A-Q11 | ASSUMED | Co to jest `number` i czy musi być unikalny? | Wymagany tekst do 50 znaków, bez unikalności | Kontrakt nie oznacza go jako opcjonalnego; transkrypcja milczy | Dodać `@Pattern` lub unikalność per typ |
| A-Q12 | ASSUMED | Co z dokumentami po usunięciu auta? Czy dokument można edytować lub usunąć? | Kaskadowe usunięcie z autem; brak edycji i usuwania dokumentu | Usuwanie pojazdu nadal daje 204. Historia służy kontroli (L24) | FK bez kaskady → 409 przy usuwaniu auta z dokumentami |
| A-Q13 | ASSUMED | Czy auto z datą wydania w przeszłości może być gotowe? | Tak, liczymy względem `plannedHandoverDate` | Kontrakt nie ma takiego powodu | Nowy `reason` (zmiana kontraktu) |

## 8. Zadania techniczne
- Bean `Clock` (nie istnieje). `@NotInFuture` z wstrzykniętym `Clock`. Walidator klasowy `expiryDate ≥ issueDate` z `addPropertyNode("expiryDate")`, żeby błąd miał `field`.
- Zamrożone testy: zachować 6-argumentowe konstruktory `Vehicle`/`VehicleRequest` i 7-argumentowy `VehicleResponse` (dodatkowy konstruktor rekordu). Flagi nie dodawać do frontendowych `Vehicle`/`VehicleRequest`; dokumenty mają własny typ.
- UI w szczegółach auta: blok gotowości (z backendu, bez liczenia na froncie), lista dokumentów, formularz dodania, przyciski statusu, checkbox flagi. Migracje po B. Wyścig dwóch POST tego samego typu nie ma siatki w bazie: akceptujemy.
