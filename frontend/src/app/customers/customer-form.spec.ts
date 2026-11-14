import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { CustomerForm } from './customer-form';

describe('CustomerForm', () => {
  let fixture: ComponentFixture<CustomerForm>;
  let element: HTMLElement;
  let http: HttpTestingController;

  beforeEach(async () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting(), provideRouter([])],
    });
    fixture = TestBed.createComponent(CustomerForm);
    element = fixture.nativeElement;
    http = TestBed.inject(HttpTestingController);
    await fixture.whenStable();
  });

  afterEach(() => http.verify());

  function fill(type: string, index: number, value: string): void {
    const input = element.querySelectorAll<HTMLInputElement>(`input[type="${type}"]`)[index];
    input.value = value;
    input.dispatchEvent(new Event('input'));
  }

  function fillValidCustomer(): void {
    fill('text', 0, 'Jan');
    fill('text', 1, 'Kowalski');
    fill('text', 2, 'jan@example.com');
    fill('text', 3, '+48 600 100 200');
  }

  async function submit(): Promise<void> {
    element.querySelector('form')!.dispatchEvent(new Event('submit'));
    await fixture.whenStable();
  }

  function errors(): string[] {
    return Array.from(element.querySelectorAll('.error'), (error) => error.textContent!.trim());
  }

  // B-R2, B-Q1, scenariusz 9
  it('shows required errors and sends nothing when the form is empty', async () => {
    await submit();

    http.expectNone('/api/customers');
    expect(errors()).toEqual([
      'First name is required',
      'Last name is required',
      'Email is required',
      'Phone is required',
    ]);
  });

  // B-Q2
  it('leaves format rules to the backend', async () => {
    fillValidCustomer();
    fill('text', 2, 'x');
    fill('text', 3, 'abc');
    fill('text', 4, '123');

    await submit();

    const request = http.expectOne({ method: 'POST', url: '/api/customers' });
    expect(request.request.body.email).toBe('x');
    expect(request.request.body.phone).toBe('abc');
    expect(request.request.body.nip).toBe('123');
    request.flush(
      {
        title: 'Bad Request',
        status: 400,
        detail: 'Validation failed',
        errors: [
          { field: 'email', message: 'must be a well-formed email address' },
          { field: 'nip', message: 'must be 10 digits' },
          {
            field: 'phone',
            message:
              'must be 9 to 20 characters: digits, spaces, hyphens, optional leading +, starting and ending with a digit',
          },
        ],
      },
      { status: 400, statusText: 'Bad Request' },
    );

    await vi.waitFor(() =>
      expect(errors()).toEqual([
        'must be a well-formed email address',
        'must be 9 to 20 characters: digits, spaces, hyphens, optional leading +, starting and ending with a digit',
        'must be 10 digits',
      ]),
    );
  });

  // B-R1, B-R3 (pusty NIP -> null)
  it('creates the customer and navigates to its details', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    fillValidCustomer();

    await submit();
    const request = http.expectOne({ method: 'POST', url: '/api/customers' });
    expect(request.request.body).toEqual({
      firstName: 'Jan',
      lastName: 'Kowalski',
      email: 'jan@example.com',
      phone: '+48 600 100 200',
      nip: null,
    });
    request.flush({ id: 7, ...request.request.body });

    await vi.waitFor(() => expect(navigate).toHaveBeenCalledWith(['/customers', 7]));
  });

  // B-R1
  it('loads the customer in edit mode and updates it', async () => {
    const navigate = vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
    const customer = {
      id: 3,
      firstName: 'Anna',
      lastName: 'Nowak',
      email: 'anna@example.com',
      phone: '600-100-201',
      nip: null,
    };
    fixture.componentRef.setInput('id', '3');
    fixture.detectChanges();
    http.expectOne({ method: 'GET', url: '/api/customers/3' }).flush(customer);
    await fixture.whenStable();
    fill('text', 1, 'Wisniewska');

    await submit();
    const request = http.expectOne({ method: 'PUT', url: '/api/customers/3' });
    expect(request.request.body).toEqual({
      firstName: 'Anna',
      lastName: 'Wisniewska',
      email: 'anna@example.com',
      phone: '600-100-201',
      nip: null,
    });
    request.flush({ ...customer, lastName: 'Wisniewska' });

    await vi.waitFor(() => expect(navigate).toHaveBeenCalledWith(['/customers', 3]));
  });

  it('shows a dedicated message when the customer to edit does not exist', async () => {
    fixture.componentRef.setInput('id', '99');
    fixture.detectChanges();
    http
      .expectOne({ method: 'GET', url: '/api/customers/99' })
      .flush(
        { title: 'Not Found', status: 404, detail: 'Customer with id 99 not found' },
        { status: 404, statusText: 'Not Found' },
      );

    await vi.waitFor(() => expect(errors()).toEqual(['Customer not found']));
  });

  it('shows the ProblemDetail detail when loading the customer fails', async () => {
    fixture.componentRef.setInput('id', '3');
    fixture.detectChanges();
    http
      .expectOne({ method: 'GET', url: '/api/customers/3' })
      .flush(
        { title: 'Service Unavailable', status: 503, detail: 'Database is unavailable' },
        { status: 503, statusText: 'Service Unavailable' },
      );

    await vi.waitFor(() => expect(errors()).toEqual(['Database is unavailable']));
  });

  it('shows object-level errors returned by the backend above the form', async () => {
    fillValidCustomer();

    await submit();
    http.expectOne({ method: 'POST', url: '/api/customers' }).flush(
      {
        title: 'Bad Request',
        status: 400,
        detail: 'Validation failed',
        errors: [{ field: null, message: 'request body must be valid JSON' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );

    await vi.waitFor(() => expect(errors()).toEqual(['request body must be valid JSON']));
  });

  it('shows field errors returned by the backend as ProblemDetail', async () => {
    fillValidCustomer();

    await submit();
    http.expectOne({ method: 'POST', url: '/api/customers' }).flush(
      {
        title: 'Bad Request',
        status: 400,
        detail: 'Validation failed',
        errors: [{ field: 'email', message: 'must be a well-formed email address' }],
      },
      { status: 400, statusText: 'Bad Request' },
    );

    await vi.waitFor(() => expect(errors()).toEqual(['must be a well-formed email address']));
  });

  // B-R4, scenariusz 9
  it('shows the ProblemDetail detail when the backend reports a conflict', async () => {
    fillValidCustomer();

    await submit();
    http.expectOne({ method: 'POST', url: '/api/customers' }).flush(
      {
        title: 'Conflict',
        status: 409,
        detail: 'Customer with email jan@example.com already exists',
      },
      { status: 409, statusText: 'Conflict' },
    );

    await vi.waitFor(() =>
      expect(errors()).toEqual(['Customer with email jan@example.com already exists']),
    );
  });
});
