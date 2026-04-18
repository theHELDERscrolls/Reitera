import { Component, computed, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';

import { StudyHubComponent } from './components/study-hub/study-hub.component';
import { StudySessionComponent } from './components/study-session/study-session.component';

@Component({
  selector: 'app-study',
  imports: [StudyHubComponent, StudySessionComponent],
  templateUrl: './study.component.html',
})
export default class StudyComponent {
  private readonly route = inject(ActivatedRoute);
  private readonly queryParams = toSignal(this.route.queryParamMap, { requireSync: true });

  readonly deckId = computed(() => {
    const val = this.queryParams().get('deckId');
    return val ? Number(val) : null;
  });

  readonly categoryId = computed(() => {
    const val = this.queryParams().get('categoryId');
    return val ? Number(val) : null;
  });

  readonly mode = computed(() =>
    this.deckId() !== null || this.categoryId() !== null ? 'session' : 'hub',
  );
}
