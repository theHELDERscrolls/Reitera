import { Routes } from '@angular/router';

const authRoutes: Routes = [
  {
    path: 'login',
    loadComponent: () => import('./login/login.component'),
  },
  {
    path: 'register',
    loadComponent: () => import('./register/register.component'),
  },
  {
    path: 'verify',
    loadComponent: () => import('./verify/verify.component'),
  },
  {
    path: 'check-email',
    loadComponent: () => import('./check-email/check-email.component'),
  },
  {
    path: '',
    redirectTo: 'login',
    pathMatch: 'full',
  },
];

export default authRoutes;
