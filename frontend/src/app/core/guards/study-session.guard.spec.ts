import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot, RouterStateSnapshot } from '@angular/router';
import { Observable } from 'rxjs';

import { studySessionGuard } from './study-session.guard';
import { StudyStateService } from '@features/study/services/study-state.service';

describe('studySessionGuard', () => {
  let studyState: StudyStateService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    studyState = TestBed.inject(StudyStateService);
  });

  it('returns true immediately when bypass flag is set', () => {
    studyState.bypassNextGuardCheck();

    const result = TestBed.runInInjectionContext(() =>
      studySessionGuard(
        undefined,
        {} as ActivatedRouteSnapshot,
        {} as RouterStateSnapshot,
        {} as RouterStateSnapshot,
      ),
    );

    expect(result).toBe(true);
  });

  it('returns true immediately when there are no pending ratings', () => {
    const result = TestBed.runInInjectionContext(() =>
      studySessionGuard(
        undefined,
        {} as ActivatedRouteSnapshot,
        {} as RouterStateSnapshot,
        {} as RouterStateSnapshot,
      ),
    );

    expect(result).toBe(true);
  });

  it('returns an Observable when there are pending ratings', () => {
    studyState.hasPendingRatings.set(true);

    const result = TestBed.runInInjectionContext(() =>
      studySessionGuard(
        undefined,
        {} as ActivatedRouteSnapshot,
        {} as RouterStateSnapshot,
        {} as RouterStateSnapshot,
      ),
    );

    expect(result).toBeInstanceOf(Observable);
  });

  it('emits true when the user confirms leaving', () => {
    studyState.hasPendingRatings.set(true);

    let emitted: boolean | undefined;
    const result = TestBed.runInInjectionContext(() =>
      studySessionGuard(
        undefined,
        {} as ActivatedRouteSnapshot,
        {} as RouterStateSnapshot,
        {} as RouterStateSnapshot,
      ),
    ) as Observable<boolean>;
    result.subscribe((v) => (emitted = v));

    studyState.confirmDeactivation();

    expect(emitted).toBe(true);
  });

  it('emits false when the user cancels and stays on the page', () => {
    studyState.hasPendingRatings.set(true);

    let emitted: boolean | undefined;
    const result = TestBed.runInInjectionContext(() =>
      studySessionGuard(
        undefined,
        {} as ActivatedRouteSnapshot,
        {} as RouterStateSnapshot,
        {} as RouterStateSnapshot,
      ),
    ) as Observable<boolean>;
    result.subscribe((v) => (emitted = v));

    studyState.cancelDeactivation();

    expect(emitted).toBe(false);
  });
});
