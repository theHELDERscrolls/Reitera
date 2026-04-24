import * as echarts from 'echarts/core';
import type { EChartsCoreOption } from 'echarts/core';
import {
  AfterViewInit,
  Component,
  computed,
  DestroyRef,
  ElementRef,
  inject,
  input,
  signal,
} from '@angular/core';
import {
  CalendarComponent,
  TooltipComponent,
  VisualMapContinuousComponent,
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import { HeatmapChart } from 'echarts/charts';
import { NgxEchartsDirective, provideEchartsCore } from 'ngx-echarts';

import { DailyStudyCount } from '@core/models/dashboard.model';

echarts.use([
  HeatmapChart,
  CalendarComponent,
  TooltipComponent,
  VisualMapContinuousComponent,
  CanvasRenderer,
]);

@Component({
  selector: 'app-study-heatmap',
  imports: [NgxEchartsDirective],
  providers: [provideEchartsCore({ echarts })],
  templateUrl: './study-heatmap.component.html',
  host: { class: 'block w-full' },
})
export class StudyHeatmapComponent implements AfterViewInit {
  private readonly el = inject(ElementRef);
  private readonly destroyRef = inject(DestroyRef);

  readonly data = input.required<DailyStudyCount[]>();

  private readonly containerWidth = signal(0);

  /**
   * Number of weeks to display, derived from the measured container width.
   * Each cell column is assumed to occupy 15px (13px cell + 2px border gap).
   * Clamped to the range [12, 52].
   */
  readonly weeksToShow = computed(() => {
    const width = this.containerWidth();
    if (width === 0) return 52;
    const available = width - 35;
    return Math.min(52, Math.max(12, Math.floor(available / 15)));
  });

  ngAfterViewInit(): void {
    this.containerWidth.set(this.el.nativeElement.clientWidth);

    const observer = new ResizeObserver((entries) => {
      this.containerWidth.set(entries[0].contentRect.width);
    });

    observer.observe(this.el.nativeElement);
    this.destroyRef.onDestroy(() => observer.disconnect());
  }

  readonly chartOptions = computed<EChartsCoreOption>(() => {
    const rows = this.data();
    const weeks = this.weeksToShow();
    const seriesData = rows.map((r) => [r.date, r.count]);
    const maxCount = rows.reduce((m, r) => Math.max(m, r.count), 1);

    const end = new Date();
    const start = new Date(end);
    start.setDate(start.getDate() - weeks * 7);

    return {
      tooltip: {
        position: 'top',
        formatter: (p: { data: [string, number] }) =>
          `${p.data[0]}: ${p.data[1]} card${p.data[1] !== 1 ? 's' : ''}`,
      },
      visualMap: {
        min: 0,
        max: maxCount,
        type: 'continuous',
        show: false,
        inRange: { color: ['#313244', '#89b4fa'] },
      },
      calendar: {
        top: 25,
        left: 30,
        right: 5,
        bottom: 5,
        cellSize: ['auto', 13],
        range: [start.toISOString().slice(0, 10), end.toISOString().slice(0, 10)],
        itemStyle: { borderWidth: 2, borderColor: '#1e1e2e' },
        dayLabel: { show: true, firstDay: 1, color: '#6c7086', nameMap: 'en' },
        monthLabel: { color: '#6c7086' },
        yearLabel: { show: false },
        splitLine: { show: false },
      },
      series: [
        {
          type: 'heatmap',
          coordinateSystem: 'calendar',
          data: seriesData,
        },
      ],
    };
  });
}
