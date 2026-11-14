import { Routes } from '@angular/router';
import { VehicleDocumentDetail } from './vehicle-document-detail';
import { VehicleDocumentForm } from './vehicle-document-form';
import { VehicleDocumentList } from './vehicle-document-list';

export const documentsRoutes: Routes = [
  { path: '', component: VehicleDocumentList },
  { path: 'new', component: VehicleDocumentForm },
  { path: ':id', component: VehicleDocumentDetail },
];
