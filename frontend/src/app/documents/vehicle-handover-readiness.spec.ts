import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HandoverReadiness } from './vehicle-document-api';
import { VehicleHandoverReadiness } from './vehicle-handover-readiness';

describe('VehicleHandoverReadiness', () => {
  let fixture: ComponentFixture<VehicleHandoverReadiness>;
  let element: HTMLElement;

  beforeEach(() => {
    fixture = TestBed.createComponent(VehicleHandoverReadiness);
    element = fixture.nativeElement;
  });

  async function show(readiness: HandoverReadiness): Promise<string> {
    fixture.componentRef.setInput('readiness', readiness);
    await fixture.whenStable();
    return element.textContent!.replace(/\s+/g, ' ').trim();
  }

  // A-R13
  it('showsReadyWhenBackendSaysReady', async () => {
    const text = await show({ ready: true, missingDocumentTypes: [], problems: [] });

    expect(text).toContain('Ready for handover');
    expect(text).not.toContain('Not ready');
    expect(text).not.toContain('Missing:');
  });

  // A-R13, A-R1, A-R9: przykład z kontraktu BRIEF.md
  it('listsMissingDocumentTypesAndProblemsFromBackend', async () => {
    const text = await show({
      ready: false,
      missingDocumentTypes: ['COC'],
      problems: [
        { documentType: 'OC_POLICY', reason: 'EXPIRED' },
        { documentType: null, reason: 'NO_HANDOVER_DATE' },
      ],
    });

    expect(text).toContain('Not ready for handover');
    expect(text).toContain('Missing: CoC');
    expect(text).toContain('OC policy');
    expect(text).toContain('Expired');
    expect(text).toContain('No planned handover date');
    // komponent niczego nie liczy: pokazuje tylko to, co przyszło z backendu
    expect(text).not.toContain('Sales agreement');
    expect(text).not.toContain('Invoice');
    expect(text).not.toContain('Not verified');
  });

  // A-R13: stan z backendu jest ważniejszy niż zawartość list (brak liczenia gotowości na froncie)
  it('trustsReadyFlagFromBackendInsteadOfComputingIt', async () => {
    const text = await show({ ready: false, missingDocumentTypes: [], problems: [] });

    expect(text).toContain('Not ready for handover');
    expect(text).not.toContain('Missing:');
  });

  // A-R14, A-Q2
  it('showsNoHandoverDateProblem', async () => {
    const text = await show({
      ready: false,
      missingDocumentTypes: [],
      problems: [{ documentType: null, reason: 'NO_HANDOVER_DATE' }],
    });

    expect(text).toContain('Not ready for handover');
    expect(text).toContain('No planned handover date');
    expect(text).not.toContain('Missing:');
    // dla problemu bez typu dokumentu nie ma etykiety typu
    for (const label of [
      'Sales agreement',
      'Invoice',
      'CoC',
      'OC policy',
      'Registration power of attorney',
    ]) {
      expect(text).not.toContain(label);
    }
  });
});
