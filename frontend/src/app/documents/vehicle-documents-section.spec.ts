import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  TestRequest,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { HandoverReadiness, VehicleDocument } from './vehicle-document-api';
import { VehicleDocumentsSection } from './vehicle-documents-section';

const DOCUMENTS_URL = '/api/vehicles/1/documents';
const READINESS_URL = '/api/vehicles/1/handover-readiness';
const VEHICLE_URL = '/api/vehicles/1';

const NOT_READY: HandoverReadiness = {
  ready: false,
  missingDocumentTypes: ['COC'],
  problems: [],
};

function doc(
  id: number,
  type: VehicleDocument['type'],
  number: string,
  status: VehicleDocument['status'],
) {
  return {
    id,
    vehicleId: 1,
    type,
    number,
    issueDate: '2026-11-14',
    expiryDate: null,
    status,
  } satisfies VehicleDocument;
}

function vehicle(registrationByDealer: boolean) {
  return {
    id: 1,
    vin: 'WVWZZZ1KZAW000001',
    make: 'Volkswagen',
    model: 'Golf',
    productionYear: 2025,
    fuelType: 'PETROL',
    plannedHandoverDate: '2026-12-15',
    registrationByDealer,
  };
}

describe('VehicleDocumentsSection', () => {
  let fixture: ComponentFixture<VehicleDocumentsSection>;
  let element: HTMLElement;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    fixture = TestBed.createComponent(VehicleDocumentsSection);
    fixture.componentRef.setInput('vehicleId', 1);
    element = fixture.nativeElement;
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  // Obsługuje trzy początkowe żądania: lista dokumentów, gotowość i flaga rejestracji (GET pojazdu).
  async function load(
    documents: VehicleDocument[],
    readiness: HandoverReadiness = NOT_READY,
    registrationByDealer = false,
  ): Promise<void> {
    // Zasoby startują przy pierwszym wykryciu zmian; whenStable czekałoby na niewysłane odpowiedzi.
    fixture.detectChanges();
    http.expectOne(DOCUMENTS_URL).flush(documents);
    http.expectOne(READINESS_URL).flush(readiness);
    http.expectOne(VEHICLE_URL).flush(vehicle(registrationByDealer));
    await fixture.whenStable();
  }

  // Zbiera żądania GET, które komponent wysłał po zapisie (reload zasobów).
  async function reloaded(...urls: string[]): Promise<Record<string, TestRequest>> {
    const found: Record<string, TestRequest[]> = Object.fromEntries(urls.map((url) => [url, []]));
    await vi.waitFor(() => {
      for (const url of urls) {
        found[url].push(...http.match(url));
        expect(found[url]).toHaveLength(1);
      }
    });
    return Object.fromEntries(urls.map((url) => [url, found[url][0]]));
  }

  function buttons(root: ParentNode = element): string[] {
    return Array.from(root.querySelectorAll('button'), (button) => button.textContent!.trim());
  }

  function button(label: string): HTMLButtonElement {
    return Array.from(element.querySelectorAll('button')).find(
      (candidate) => candidate.textContent!.trim() === label,
    )!;
  }

  function row(number: string): HTMLTableRowElement {
    return Array.from(element.querySelectorAll('tbody tr')).find((candidate) =>
      candidate.textContent!.includes(number),
    ) as HTMLTableRowElement;
  }

  function text(): string {
    return element.textContent!.replace(/\s+/g, ' ').trim();
  }

  // A-R13, A-R5, A-R7: statusy zmienia backend, front tylko wysyła dokładne body i odświeża dane
  it('sendsStatusChangeWithExactBody', async () => {
    const pending = doc(5, 'INVOICE', 'FV/1', 'PENDING');
    await load([pending]);
    expect(text()).toContain('Not ready for handover');

    button('Verify').click();

    const put = http.expectOne({ method: 'PUT', url: `${DOCUMENTS_URL}/5/status` });
    expect(put.request.body).toEqual({ status: 'VERIFIED' });
    put.flush({ ...pending, status: 'VERIFIED' });

    const requests = await reloaded(DOCUMENTS_URL, READINESS_URL);
    requests[DOCUMENTS_URL].flush([{ ...pending, status: 'VERIFIED' }]);
    requests[READINESS_URL].flush({ ready: true, missingDocumentTypes: [], problems: [] });
    await fixture.whenStable();

    expect(text()).toContain('Ready for handover');
    expect(buttons(row('FV/1'))).toEqual(['Reject']);
  });

  // A-R7, A-Q3, A-R11: błąd 409 z backendu trafia do użytkownika
  it('showsConflictMessageWhenStatusChangeRejected', async () => {
    await load([doc(5, 'INVOICE', 'FV/1', 'PENDING')]);

    button('Verify').click();

    const put = http.expectOne({ method: 'PUT', url: `${DOCUMENTS_URL}/5/status` });
    expect(put.request.body).toEqual({ status: 'VERIFIED' });
    put.flush(
      {
        title: 'Conflict',
        status: 409,
        detail: 'Cannot change document status from REJECTED to VERIFIED',
      },
      { status: 409, statusText: 'Conflict' },
    );

    await vi.waitFor(() => {
      const alert = element.querySelector('p.error[role="alert"]');
      expect(alert?.textContent?.trim()).toBe(
        'Cannot change document status from REJECTED to VERIFIED',
      );
    });
  });

  // A-R7 (REJECTED jest stanem końcowym), A-Q3 (VERIFIED można tylko odrzucić)
  it('hidesStatusButtonsForRejectedDocument', async () => {
    await load([
      doc(5, 'INVOICE', 'FV/1', 'PENDING'),
      doc(6, 'COC', 'COC/1', 'VERIFIED'),
      doc(7, 'SALES_AGREEMENT', 'UM/1', 'REJECTED'),
    ]);

    expect(buttons(row('FV/1'))).toEqual(['Verify', 'Reject']);
    expect(buttons(row('COC/1'))).toEqual(['Reject']);
    expect(buttons(row('UM/1'))).toEqual([]);
  });

  // A-R3, A-Q7, scenariusz 3
  it('togglesRegistrationByDealerWithExactBody', async () => {
    await load([]);
    const checkbox = element.querySelector<HTMLInputElement>('input[type="checkbox"]')!;
    expect(element.textContent).toContain('Registration by dealer');
    expect(checkbox.checked).toBe(false);

    checkbox.click();

    const put = http.expectOne({ method: 'PUT', url: `${VEHICLE_URL}/registration-by-dealer` });
    expect(put.request.body).toEqual({ registrationByDealer: true });
    put.flush(vehicle(true));

    const requests = await reloaded(VEHICLE_URL, READINESS_URL);
    requests[VEHICLE_URL].flush(vehicle(true));
    requests[READINESS_URL].flush({
      ready: false,
      missingDocumentTypes: ['COC', 'REGISTRATION_POA'],
      problems: [],
    });
    await fixture.whenStable();

    expect(element.querySelector<HTMLInputElement>('input[type="checkbox"]')!.checked).toBe(true);
    expect(text()).toContain('Registration power of attorney');
  });

  // A-R3: stan checkboxa pochodzi z backendu
  it('showsRegistrationByDealerFromVehicle', async () => {
    await load([], NOT_READY, true);

    expect(element.querySelector<HTMLInputElement>('input[type="checkbox"]')!.checked).toBe(true);
  });

  it('linksToNewDocumentForm', async () => {
    await load([]);

    const link = Array.from(element.querySelectorAll('a')).find(
      (candidate) => candidate.textContent!.trim() === 'Add document',
    );
    expect(link?.getAttribute('href')).toBe('/vehicles/1/documents/new');
  });
});
