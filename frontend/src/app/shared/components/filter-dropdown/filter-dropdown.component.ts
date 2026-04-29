import {
  Component,
  ElementRef,
  HostListener,
  computed,
  inject,
  input,
  output,
  signal,
} from '@angular/core';
import { TranslocoPipe } from '@jsverse/transloco';
import { LucideChevronDown } from '@lucide/angular';

export type FilterOptionValue = string | number | null;

export interface FilterOption {
  value: FilterOptionValue;
  labelKey?: string;
  label?: string;
  colorHex?: string;
}

@Component({
  selector: 'app-filter-dropdown',
  imports: [LucideChevronDown, TranslocoPipe],
  templateUrl: './filter-dropdown.component.html',
})
export class FilterDropdownComponent {
  private readonly elRef = inject(ElementRef);

  readonly options = input.required<FilterOption[]>();
  readonly selected = input<FilterOptionValue>(null);

  readonly selectOption = output<FilterOptionValue>();

  readonly isOpen = signal(false);

  readonly isActive = computed(() => this.selected() !== null);

  readonly selectedOption = computed(
    () => this.options().find((o) => o.value === this.selected()) ?? this.options()[0],
  );

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.elRef.nativeElement.contains(event.target)) {
      this.isOpen.set(false);
    }
  }

  select(value: FilterOptionValue): void {
    this.selectOption.emit(value);
    this.isOpen.set(false);
  }
}
