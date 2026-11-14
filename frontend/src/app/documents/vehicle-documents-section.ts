import { Component, inject, input, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Observable } from 'rxjs';
import { loadErrorMessage, problemDetailMessage } from '../shared/problem-detail';
import {
  DOCUMENT_STATUS_LABELS,
  DOCUMENT_TYPE_LABELS,
  VehicleDocumentApi,
  VehicleDocumentStatus,
} from './vehicle-document-api';
import { VehicleHandoverReadiness } from './vehicle-handover-readiness';

@Component({
  selector: 'app-vehicle-documents-section',
  imports: [RouterLink, VehicleHandoverReadiness],
  templateUrl: './vehicle-documents-section.html',
})
export class VehicleDocumentsSection {
  private readonly vehicleDocumentApi = inject(VehicleDocumentApi);

  readonly vehicleId = input.required<number>();

  protected readonly documents = this.vehicleDocumentApi.list(this.vehicleId);
  protected readonly readiness = this.vehicleDocumentApi.readiness(this.vehicleId);
  protected readonly registration = this.vehicleDocumentApi.registration(this.vehicleId);
  protected readonly documentTypeLabels = DOCUMENT_TYPE_LABELS;
  protected readonly documentStatusLabels = DOCUMENT_STATUS_LABELS;
  protected readonly actionError = signal<string | null>(null);

  protected loadError(error: unknown): string {
    return loadErrorMessage(error, 'Vehicle not found');
  }

  protected changeStatus(id: number, status: VehicleDocumentStatus): void {
    this.run(this.vehicleDocumentApi.changeStatus(this.vehicleId(), id, status), () => {
      this.documents.reload();
      this.readiness.reload();
    });
  }

  protected setRegistrationByDealer(event: Event): void {
    const checkbox = event.target as HTMLInputElement;
    this.run(
      this.vehicleDocumentApi.setRegistrationByDealer(this.vehicleId(), checkbox.checked),
      () => {
        this.registration.reload();
        this.readiness.reload();
      },
      () => (checkbox.checked = !checkbox.checked),
    );
  }

  private run(request: Observable<unknown>, onSuccess: () => void, onError?: () => void): void {
    this.actionError.set(null);
    request.subscribe({
      next: onSuccess,
      error: (error: unknown) => {
        this.actionError.set(problemDetailMessage(error));
        onError?.();
      },
    });
  }
}
