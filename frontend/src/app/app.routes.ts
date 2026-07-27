import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { adminGuard } from './core/guards/admin.guard';

export const routes: Routes = [
  // User Storefront Routes (Immersive 3D Glassmorphism Theme)
  {
    path: '',
    loadComponent: () => import('./layout/main-layout/main-layout.component').then(m => m.MainLayoutComponent),
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'products', pathMatch: 'full' },
      {
        path: 'products',
        loadComponent: () => import('./features/products/product-list/product-list.component').then(m => m.ProductListComponent)
      },
      {
        path: 'cart',
        loadComponent: () => import('./features/cart/cart.component').then(m => m.CartComponent)
      },
      {
        path: 'orders',
        loadComponent: () => import('./features/orders/order-list/order-list.component').then(m => m.OrderListComponent)
      },
      {
        path: 'orders/:id',
        loadComponent: () => import('./features/orders/order-detail/order-detail.component').then(m => m.OrderDetailComponent)
      }
    ]
  },

  // Admin Command Center Routes (Light 3D Glassmorphic Portal)
  {
    path: '',
    loadComponent: () => import('./layout/admin-layout/admin-layout.component').then(m => m.AdminLayoutComponent),
    canActivate: [authGuard, adminGuard],
    children: [
      { path: 'admin', redirectTo: 'admin/dashboard', pathMatch: 'full' },
      {
        path: 'admin/dashboard',
        loadComponent: () => import('./features/dashboard/admin-dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'admin/products',
        loadComponent: () => import('./features/admin/products/admin-product-list/admin-product-list.component').then(m => m.AdminProductListComponent)
      },
      {
        path: 'admin/products/new',
        loadComponent: () => import('./features/products/product-form/product-form.component').then(m => m.ProductFormComponent)
      },
      {
        path: 'admin/products/:id/edit',
        loadComponent: () => import('./features/products/product-form/product-form.component').then(m => m.ProductFormComponent)
      },
      {
        path: 'admin/orders',
        loadComponent: () => import('./features/admin/orders/admin-order-list/admin-order-list.component').then(m => m.AdminOrderListComponent)
      },
      {
        path: 'admin/users',
        loadComponent: () => import('./features/users/user-list/user-list.component').then(m => m.UserListComponent)
      },
      {
        path: 'admin/users/new',
        loadComponent: () => import('./features/users/user-form/user-form.component').then(m => m.UserFormComponent)
      },
      {
        path: 'admin/users/:id/edit',
        loadComponent: () => import('./features/users/user-form/user-form.component').then(m => m.UserFormComponent)
      }
    ]
  },

  // Auth Routes
  {
    path: '',
    loadComponent: () => import('./layout/auth-layout/auth-layout.component').then(m => m.AuthLayoutComponent),
    children: [
      {
        path: 'login',
        loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent)
      },
      {
        path: 'register',
        loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent)
      }
    ]
  },
  { path: '**', redirectTo: '' }
];
