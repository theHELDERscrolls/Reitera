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
    path: '',
    loadComponent: () => import('./features/shell/app-shell.component'),
    canActivate: [authGuard],
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard.component'),
      },
      {
        path: 'decks',
        loadComponent: () => import('./features/decks/decks.component'),
      },
      {
        path: 'decks/:id',
        loadComponent: () =>
          import('./features/decks/components/deck-detail/deck-detail.component'),
      },
      {
        path: 'study',
        loadComponent: () => import('./features/study/study.component'),
      },
      {
        path: 'profile',
        loadComponent: () => import('./features/profile/profile.component'),
      },
    ],
  },
];
