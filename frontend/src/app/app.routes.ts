import { Routes } from '@angular/router';

import { authGuard } from '@core/guards/auth.guard';
import { noAuthGuard } from '@core/guards/no-auth-guard';

export const routes: Routes = [
  {
    path: '',
    redirectTo: 'auth/login',
    pathMatch: 'full',
  },
  {
    path: 'auth',
    loadChildren: () => import('./features/auth/auth.routes'),
    canActivate: [noAuthGuard],
  },
  {
    path: 'dashboard',
    loadComponent: () => import('./features/dashboard/dashboard/dashboard.component'),
    canActivate: [authGuard],
  },
];
