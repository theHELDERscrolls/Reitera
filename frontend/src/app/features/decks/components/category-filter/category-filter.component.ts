import { Component, HostListener, input, output, signal } from '@angular/core';
import { LucideChevronDown } from '@lucide/angular';
import { TranslocoPipe } from '@jsverse/transloco';

import { Category } from '@core/models/category.model';

@Component({
  selector: 'app-category-filter',
  imports: [TranslocoPipe, LucideChevronDown],
  templateUrl: './category-filter.component.html',
})
export class CategoryFilterComponent {
  readonly activeCategory = input.required<string | null>();
  readonly categories = input.required<Category[]>();

  readonly categorySelected = output<string | null>();

  readonly isFilterOpen = signal(false);

  toggleFilter(): void {
    this.isFilterOpen.update((v) => !v);
  }

  selectCategory(value: string | null): void {
    this.categorySelected.emit(value);
    this.isFilterOpen.set(false);
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!(event.target as HTMLElement).closest('[data-category-filter]')) {
      this.isFilterOpen.set(false);
    }
  }
}
