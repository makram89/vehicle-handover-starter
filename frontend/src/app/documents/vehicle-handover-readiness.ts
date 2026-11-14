import { Component, input } from '@angular/core';
import {
  DOCUMENT_TYPE_LABELS,
  HandoverReadiness,
  READINESS_REASON_LABELS,
} from './vehicle-document-api';

@Component({
  selector: 'app-vehicle-handover-readiness',
  templateUrl: './vehicle-handover-readiness.html',
})
export class VehicleHandoverReadiness {
  readonly readiness = input.required<HandoverReadiness>();

  protected readonly documentTypeLabels = DOCUMENT_TYPE_LABELS;
  protected readonly readinessReasonLabels = READINESS_REASON_LABELS;
}
