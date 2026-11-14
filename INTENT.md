# INTENT

## Kontekst
Salon Auto Mazur sprawdza dokumenty aut przed wydaniem na papierowej checkliście, przez co zdarza się wydać auto bez kompletu (L2).
Chce, żeby system prowadził dokumenty każdego auta z ich weryfikacją, sam oceniał gotowość do wydania i trzymał prostą listę klientów.
Klient: Anna (kierowniczka, decyduje; spór z Tomkiem eskaluje do dyrektora) i Tomek (doradca handlowy). Dostawca: Kasia (PM). Transkrypcja z numeracją: [docs/transcript.md](docs/transcript.md).

## Featury
- A: dokumenty pojazdu ze statusami weryfikacji i endpoint gotowości auta do wydania → [INTENT_A.md](INTENT_A.md)
- B: kartoteka klientów (CRUD) z unikalnym e-mailem, bez powiązań z autami → [INTENT_B.md](INTENT_B.md)

## Sprawy wspólne
- Odłożone SMS-y o gotowości (A-R15 w [INTENT_A.md](INTENT_A.md#6-poza-zakresem)) będą wymagały odłożonego powiązania klient–auto (B-R5 w [INTENT_B.md](INTENT_B.md#6-poza-zakresem)) i telefonu klienta (B-Q1).
- Brak użytkowników i ról w obu featurach: A-Q6 w [INTENT_A.md](INTENT_A.md#7-pytania-i-założenia).
