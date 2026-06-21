import { TestBed } from '@angular/core/testing';
import { Observable } from 'rxjs';

import { StudyStateService } from './study-state.service';

describe('StudyStateService', () => {
  let service: StudyStateService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(StudyStateService);
  });

  describe('initial state', () => {
    it('hasPendingRatings should be false', () => {
      expect(service.hasPendingRatings()).toBe(false);
    });

    it('deactivationRequested should be false', () => {
      expect(service.deactivationRequested()).toBe(false);
    });
  });

  describe('bypassNextGuardCheck() / consumeBypass()', () => {
    it('consumeBypass() returns false when bypass was never set', () => {
      expect(service.consumeBypass()).toBe(false);
    });

    it('consumeBypass() returns true right after bypassNextGuardCheck() is called', () => {
      service.bypassNextGuardCheck();
      expect(service.consumeBypass()).toBe(true);
    });

    it('consumeBypass() resets to false on second call (one-shot flag)', () => {
      service.bypassNextGuardCheck();
      service.consumeBypass();
      expect(service.consumeBypass()).toBe(false);
    });
  });

  describe('requestDeactivation()', () => {
    it('sets deactivationRequested to true', () => {
      service.requestDeactivation();
      expect(service.deactivationRequested()).toBe(true);
    });

    it('returns an Observable', () => {
      const result = service.requestDeactivation();
      expect(result).toBeInstanceOf(Observable);
    });
  });

  describe('confirmDeactivation()', () => {
    it('emits true when the user confirms leaving', () => {
      let emitted: boolean | undefined;
      service.requestDeactivation().subscribe((v) => (emitted = v));

      service.confirmDeactivation();

      expect(emitted).toBe(true);
    });

    it('sets deactivationRequested to false after confirming', () => {
      service.requestDeactivation();
      service.confirmDeactivation();
      expect(service.deactivationRequested()).toBe(false);
    });
  });

  describe('cancelDeactivation()', () => {
    it('emits false when the user cancels and stays on the page', () => {
      let emitted: boolean | undefined;
      service.requestDeactivation().subscribe((v) => (emitted = v));

      service.cancelDeactivation();

      expect(emitted).toBe(false);
    });

    it('sets deactivationRequested to false after cancelling', () => {
      service.requestDeactivation();
      service.cancelDeactivation();
      expect(service.deactivationRequested()).toBe(false);
    });
  });
});
