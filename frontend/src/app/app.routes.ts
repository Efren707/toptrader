import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { guestGuard } from './core/guards/guest.guard';

export const routes: Routes = [
  {
    path: 'register',
    loadComponent: () => import('./features/auth/register/register').then((m) => m.Register),
    canActivate: [guestGuard]
  },
  {
    path: 'login',
    loadComponent: () => import('./features/auth/login/login').then((m) => m.Login),
    canActivate: [guestGuard]
  },
  {
    path: 'forgot-password',
    loadComponent: () => import('./features/auth/forgot-password/forgot-password').then((m) => m.ForgotPassword),
    canActivate: [guestGuard]
  },
  {
    path: 'reset-password',
    loadComponent: () => import('./features/auth/reset-password/reset-password').then((m) => m.ResetPassword),
    canActivate: [guestGuard]
  },
  { 
    path: '',
    loadComponent: () => import('./shared/layout/layout').then((m) => m.Layout),
    children: [
      {
        path: 'dashboard',
        loadComponent: () => import('./features/dashboard/dashboard').then((m) => m.Dashboard),
      },
      {
        path: 'stocks/:ticker',
        loadComponent: () => import('./features/stock-details/stock-details').then((m) => m.StockDetails),
      },
      {
        path: 'transactions',
        loadComponent: () => import('./features/transactions/transactions').then((m) => m.Transactions),
      },
      {
        path: 'performance',
        loadComponent: () => import('./features/performance/performance').then((m) => m.Performance),
      },
    ],
    canActivate: [authGuard]
  },
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: '**', redirectTo: 'login' },
];
