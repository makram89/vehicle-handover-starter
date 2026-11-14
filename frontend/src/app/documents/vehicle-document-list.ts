import { Component, inject, input, numberAttribute } from '@angular/core';
import { RouterLink } from '@angular/router';
import { loadErrorMessage } from '../shared/problem-detail';
import {
  DOCUMENT_STATUS_LABELS,
  DOCUMENT_TYPE_LABELS,
  VehicleDocumentApi,
} from './vehicle-document-api';

@Component({
  selector: 'app-vehicle-document-list',
  imports: [RouterLink],
  templateUrl: './vehicle-document-list.html',
})
export class VehicleDocumentList {
  private readonly vehicleDocumentApi = inject(VehicleDocumentApi);

  readonly vehicleId = input.required({ transform: numberAttribute });

  protected readonly documents = this.vehicleDocumentApi.list(this.vehicleId);
  protected readonly documentTypeLabels = DOCUMENT_TYPE_LABELS;
  protected readonly documentStatusLabels = DOCUMENT_STATUS_LABELS;

  protected loadError(error: unknown): string {
    return loadErrorMessage(error, 'Vehicle not found');
  }
}
