import { HttpClient, httpResource } from '@angular/common/http';
import { Injectable, Signal, inject } from '@angular/core';
import { Observable } from 'rxjs';

export type VehicleDocumentType =
  'SALES_AGREEMENT' | 'INVOICE' | 'COC' | 'OC_POLICY' | 'REGISTRATION_POA';

export const DOCUMENT_TYPE_LABELS: Record<VehicleDocumentType, string> = {
  SALES_AGREEMENT: 'Sales agreement',
  INVOICE: 'Invoice',
  COC: 'CoC',
  OC_POLICY: 'OC policy',
  REGISTRATION_POA: 'Registration power of attorney',
};

export const DOCUMENT_TYPES = Object.keys(DOCUMENT_TYPE_LABELS) as VehicleDocumentType[];

export type VehicleDocumentStatus = 'PENDING' | 'VERIFIED' | 'REJECTED';

export const DOCUMENT_STATUS_LABELS: Record<VehicleDocumentStatus, string> = {
  PENDING: 'Pending',
  VERIFIED: 'Verified',
  REJECTED: 'Rejected',
};

export interface VehicleDocument {
  id: number;
  vehicleId: number;
  type: VehicleDocumentType;
  number: string;
  issueDate: string;
  expiryDate: string | null;
  status: VehicleDocumentStatus;
}

export type VehicleDocumentRequest = Omit<VehicleDocument, 'id' | 'vehicleId' | 'status'>;

export type ReadinessReason = 'NOT_VERIFIED' | 'EXPIRED' | 'NO_HANDOVER_DATE';

export const READINESS_REASON_LABELS: Record<ReadinessReason, string> = {
  NOT_VERIFIED: 'Not verified',
  EXPIRED: 'Expired',
  NO_HANDOVER_DATE: 'No planned handover date',
};

export interface ReadinessProblem {
  documentType: VehicleDocumentType | null;
  reason: ReadinessReason;
}

export interface HandoverReadiness {
  ready: boolean;
  missingDocumentTypes: VehicleDocumentType[];
  problems: ReadinessProblem[];
}

@Injectable({ providedIn: 'root' })
export class VehicleDocumentApi {
  private readonly http = inject(HttpClient);

  private url(vehicleId: number): string {
    return `/api/vehicles/${vehicleId}/documents`;
  }

  list(vehicleId: Signal<number>) {
    return httpResource<VehicleDocument[]>(() => this.url(vehicleId()), { defaultValue: [] });
  }

  get(vehicleId: Signal<number>, id: Signal<number | undefined>) {
    return httpResource<VehicleDocument>(() => {
      const documentId = id();
      return documentId === undefined ? undefined : `${this.url(vehicleId())}/${documentId}`;
    });
  }

  create(vehicleId: number, request: VehicleDocumentRequest): Observable<VehicleDocument> {
    return this.http.post<VehicleDocument>(this.url(vehicleId), request);
  }

  readiness(vehicleId: Signal<number>) {
    return httpResource<HandoverReadiness>(() => `/api/vehicles/${vehicleId()}/handover-readiness`);
  }

  registration(vehicleId: Signal<number>) {
    return httpResource<{ registrationByDealer: boolean }>(() => `/api/vehicles/${vehicleId()}`);
  }

  changeStatus(
    vehicleId: number,
    id: number,
    status: VehicleDocumentStatus,
  ): Observable<VehicleDocument> {
    return this.http.put<VehicleDocument>(`${this.url(vehicleId)}/${id}/status`, { status });
  }

  setRegistrationByDealer(vehicleId: number, registrationByDealer: boolean): Observable<unknown> {
    return this.http.put(`/api/vehicles/${vehicleId}/registration-by-dealer`, {
      registrationByDealer,
    });
  }
}
