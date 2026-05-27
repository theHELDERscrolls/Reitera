import { inject } from '@angular/core';
import { CanDeactivateFn } from '@angular/router';

import { StudyStateService } from '@features/study/services/study-state.service';

export const studySessionGuard: CanDeactivateFn<unknown> = () => {
  const studyState = inject(StudyStateService);

  if (!studyState.hasPendingRatings()) return true;

  return studyState.requestDeactivation();
};
