import { Component, computed, input, output } from '@angular/core';
import { LucideChevronLeft, LucideChevronRight } from '@lucide/angular';
import { TranslocoPipe } from '@jsverse/transloco';

@Component({
  selector: 'app-pagination',
  imports: [LucideChevronLeft, LucideChevronRight, TranslocoPipe],
  templateUrl: './pagination.component.html',
})
export default class PaginationComponent {
  readonly currentPage = input.required<number>();
  readonly totalPages = input.required<number>();

  readonly pageChange = output<number>();

  readonly pageRange = computed((): Array<number | 'gap'> => {
    const total = this.totalPages();
    const cur = this.currentPage();

    if (total <= 7) return Array.from({ length: total }, (_, i) => i);

    const result: Array<number | 'gap'> = [0];

    if (cur > 2) result.push('gap');

    for (let i = Math.max(1, cur - 1); i <= Math.min(total - 2, cur + 1); i++) result.push(i);

    if (cur < total - 3) result.push('gap');

    result.push(total - 1);

    return result;
  });
}
