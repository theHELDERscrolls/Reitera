import { Injectable, signal } from '@angular/core';
import { Subject } from 'rxjs';
import { take } from 'rxjs/operators';

@Injectable({
  providedIn: 'root',
})
export class StudyStateService {
  readonly hasPendingRatings = signal(false);
  readonly deactivationRequested = signal(false);
  private readonly _deactivation$ = new Subject<boolean>();
  private _bypassGuard = false;

  requestDeactivation() {
    this.deactivationRequested.set(true);

    return this._deactivation$.pipe(take(1));
  }

  confirmDeactivation(): void {
    this.deactivationRequested.set(false);
    this._deactivation$.next(true);
  }

  cancelDeactivation(): void {
    this.deactivationRequested.set(false);
    this._deactivation$.next(false);
  }

  bypassNextGuardCheck(): void {
    this._bypassGuard = true;
  }

  consumeBypass(): boolean {
    const bypass = this._bypassGuard;
    this._bypassGuard = false;
    return bypass;
  }
}
