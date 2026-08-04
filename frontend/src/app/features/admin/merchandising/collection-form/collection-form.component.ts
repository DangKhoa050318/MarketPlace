import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute } from '@angular/router';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { CdkDragDrop, DragDropModule, moveItemInArray } from '@angular/cdk/drag-drop';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { CollectionService } from '../../../../core/services/collection.service';
import { ProductService } from '../../../../core/services/product.service';
import { NotificationService } from '../../../../core/services/notification.service';
import { CollectionItemResponse, CreateCollectionRequest } from '../../../../core/models/collection.model';
import { ProductResponse } from '../../../../core/models/product.model';

@Component({
  selector: 'app-collection-form',
  standalone: true,
  imports: [
    CommonModule, FormsModule, ReactiveFormsModule, DragDropModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatProgressSpinnerModule
  ],
  template: `
    <div class="page">
      <div class="page-header">
        <button mat-icon-button (click)="back()"><mat-icon>arrow_back</mat-icon></button>
        <h1 class="page-title">{{ isEdit ? 'Edit Collection' : 'New Collection' }}</h1>
      </div>

      <form [formGroup]="form" (ngSubmit)="submit()" class="form glass-panel" *ngIf="!loading">
        <div class="row">
          <mat-form-field appearance="outline" class="grow">
            <mat-label>Name</mat-label>
            <input matInput formControlName="name" maxlength="150" />
            <mat-error *ngIf="form.get('name')?.hasError('required')">Name is required</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline" class="grow">
            <mat-label>Slug</mat-label>
            <input matInput formControlName="slug" placeholder="featured-picks" maxlength="160" />
            <mat-error *ngIf="form.get('slug')?.hasError('required')">Slug is required</mat-error>
            <mat-error *ngIf="form.get('slug')?.hasError('pattern')">Lowercase words separated by hyphens</mat-error>
          </mat-form-field>
        </div>
        <mat-form-field appearance="outline">
          <mat-label>Description</mat-label>
          <textarea matInput formControlName="description" rows="2"></textarea>
        </mat-form-field>
        <div class="actions">
          <button mat-button type="button" (click)="back()">Cancel</button>
          <button mat-raised-button class="btn-glowing" type="submit" [disabled]="form.invalid || saving">
            <mat-icon>save</mat-icon> {{ isEdit ? 'Save' : 'Create & manage items' }}
          </button>
        </div>
      </form>

      <!-- Item builder — only once the collection exists -->
      <div class="builder glass-panel" *ngIf="isEdit && !loading">
        <h2 class="builder-title"><mat-icon>inventory_2</mat-icon> Products ({{ items.length }})</h2>

        <div class="adder">
          <mat-form-field appearance="outline" class="grow">
            <mat-label>Add product</mat-label>
            <input matInput [(ngModel)]="query" (input)="onSearch()" (keyup.enter)="onSearch()" placeholder="Search by name" />
            <mat-icon matSuffix>search</mat-icon>
          </mat-form-field>
          <div class="results" *ngIf="results.length">
            <button type="button" class="result" *ngFor="let p of results" (click)="addProduct(p)">
              <img *ngIf="p.imageUrl" [src]="p.imageUrl" [alt]="p.name" />
              <span>{{ p.name }}</span>
              <mat-icon>add</mat-icon>
            </button>
          </div>
        </div>

        <p class="hint muted small">Drag rows to reorder — the new order is saved automatically.</p>

        <div cdkDropList (cdkDropListDropped)="drop($event)" class="items">
          <div class="item" *ngFor="let it of items" cdkDrag>
            <mat-icon class="handle" cdkDragHandle>drag_indicator</mat-icon>
            <img *ngIf="it.imageUrl" [src]="it.imageUrl" [alt]="it.productName || ''" class="thumb" />
            <div class="ph-thumb" *ngIf="!it.imageUrl"><mat-icon>image</mat-icon></div>
            <div class="info">
              <strong>{{ it.productName || ('#' + it.productId) }}</strong>
              <span class="muted small" *ngIf="it.productSlug">{{ it.productSlug }}</span>
            </div>
            <button mat-icon-button color="warn" type="button" (click)="removeProduct(it)"><mat-icon>close</mat-icon></button>
          </div>
          <div *ngIf="items.length === 0" class="empty">No products yet — search above to add.</div>
        </div>
      </div>

      <div *ngIf="loading" class="loading"><mat-spinner diameter="40"></mat-spinner></div>
    </div>
  `,
  styles: [`
    .page { display: flex; flex-direction: column; gap: 16px; max-width: 820px; }
    .page-header { display: flex; align-items: center; gap: 8px; }
    .page-title { margin: 0; font-size: 1.6rem; font-weight: 800; }
    .form { display: flex; flex-direction: column; gap: 6px; padding: 24px; }
    .row { display: flex; gap: 16px; } .row .grow { flex: 1; }
    .actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 8px; }
    .builder { padding: 20px 24px; }
    .builder-title { display: flex; align-items: center; gap: 8px; font-size: 1.1rem; margin: 0 0 12px; }
    .adder { position: relative; }
    .adder .grow { width: 100%; }
    .results { position: absolute; z-index: 5; top: 60px; left: 0; right: 0; background: var(--surface, #0f172a); border: 1px solid var(--glass-border-subtle); border-radius: 10px; max-height: 260px; overflow-y: auto; box-shadow: 0 12px 30px rgba(0,0,0,.35); }
    .result { display: flex; align-items: center; gap: 10px; width: 100%; padding: 8px 12px; background: none; border: none; cursor: pointer; text-align: left; color: inherit; }
    .result:hover { background: rgba(56,189,248,.12); }
    .result img { width: 34px; height: 34px; object-fit: cover; border-radius: 6px; }
    .result span { flex: 1; }
    .hint { margin: 4px 2px 10px; }
    .items { display: flex; flex-direction: column; gap: 8px; }
    .item { display: flex; align-items: center; gap: 12px; padding: 8px 12px; border: 1px solid var(--glass-border-subtle); border-radius: 10px; background: rgba(148,163,184,.05); }
    .handle { cursor: grab; color: var(--text-muted); }
    .thumb, .ph-thumb { width: 44px; height: 44px; border-radius: 8px; object-fit: cover; }
    .ph-thumb { display: flex; align-items: center; justify-content: center; background: rgba(148,163,184,.12); color: var(--text-muted); }
    .info { display: flex; flex-direction: column; flex: 1; }
    .muted { color: var(--text-muted); } .small { font-size: .78rem; }
    .empty { text-align: center; color: var(--text-muted); padding: 24px; }
    .cdk-drag-preview { border-radius: 10px; box-shadow: 0 8px 24px rgba(0,0,0,.4); }
    .cdk-drag-placeholder { opacity: .4; }
    .cdk-drop-list-dragging .item:not(.cdk-drag-placeholder) { transition: transform .2s ease; }
    .loading { display: flex; justify-content: center; padding: 60px; }
  `]
})
export class CollectionFormComponent implements OnInit {
  form: FormGroup;
  isEdit = false;
  editId!: number;
  loading = false;
  saving = false;
  items: CollectionItemResponse[] = [];
  query = '';
  results: ProductResponse[] = [];

  constructor(
    private fb: FormBuilder,
    private collectionService: CollectionService,
    private productService: ProductService,
    private notification: NotificationService,
    private router: Router,
    private route: ActivatedRoute
  ) {
    this.form = this.fb.group({
      name: ['', Validators.required],
      slug: ['', [Validators.required, Validators.pattern('^[a-z0-9]+(?:-[a-z0-9]+)*$')]],
      description: [null]
    });
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.isEdit = true;
      this.editId = +id;
      this.loadCollection();
    }
  }

  private loadCollection(): void {
    this.loading = true;
    this.collectionService.getById(this.editId).subscribe({
      next: (res) => {
        this.loading = false;
        const c = res.data;
        this.form.patchValue({ name: c.name, slug: c.slug, description: c.description ?? null });
        this.items = c.items ?? [];
      },
      error: () => { this.loading = false; this.notification.error('Failed to load collection'); }
    });
  }

  submit(): void {
    if (this.form.invalid) return;
    this.saving = true;
    const req: CreateCollectionRequest = this.form.value;
    if (this.isEdit) {
      this.collectionService.update(this.editId, req).subscribe({
        next: () => { this.saving = false; this.notification.success('Collection updated'); },
        error: () => { this.saving = false; this.notification.error('Failed to update collection'); }
      });
    } else {
      this.collectionService.create(req).subscribe({
        next: (res) => {
          this.saving = false;
          this.notification.success('Collection created — add products below');
          this.router.navigate(['/admin/collections', res.data.id, 'edit']);
        },
        error: () => { this.saving = false; this.notification.error('Failed to create collection'); }
      });
    }
  }

  onSearch(): void {
    const q = this.query.trim();
    if (q.length < 2) { this.results = []; return; }
    this.productService.searchProducts(q, 0, 8).subscribe({
      next: (res) => {
        const existing = new Set(this.items.map(i => i.productId));
        this.results = (res.data?.content ?? []).filter(p => !existing.has(p.id));
      },
      error: () => { this.results = []; }
    });
  }

  addProduct(p: ProductResponse): void {
    this.collectionService.addItem(this.editId, p.id).subscribe({
      next: (res) => {
        this.items = res.data.items ?? [];
        this.query = '';
        this.results = [];
        this.notification.success('Product added');
      },
      error: () => this.notification.error('Failed to add product')
    });
  }

  removeProduct(it: CollectionItemResponse): void {
    this.collectionService.removeItem(this.editId, it.productId).subscribe({
      next: () => { this.items = this.items.filter(x => x.productId !== it.productId); },
      error: () => this.notification.error('Failed to remove product')
    });
  }

  drop(event: CdkDragDrop<CollectionItemResponse[]>): void {
    if (event.previousIndex === event.currentIndex) return;
    moveItemInArray(this.items, event.previousIndex, event.currentIndex);
    this.collectionService.reorder(this.editId, { productIds: this.items.map(i => i.productId) }).subscribe({
      next: (res) => { this.items = res.data.items ?? this.items; },
      error: () => { this.notification.error('Failed to save order'); this.loadCollection(); }
    });
  }

  back(): void { this.router.navigate(['/admin/collections']); }
}
