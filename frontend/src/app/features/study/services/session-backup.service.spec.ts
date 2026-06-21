import { TestBed } from '@angular/core/testing';

import { SessionBackupService } from './session-backup.service';
import { SessionBackup } from '@core/models/study.model';

const BACKUP_KEY = 'reitera_session_backup';

const makeValidBackup = (): SessionBackup => ({
  version: '1',
  deckId: 1,
  deckName: 'Test deck',
  categoryId: null,
  categoryName: null,
  ratings: [{ cardId: 10, rating: 3 }],
  startedAt: Date.now() - 5000,
  updatedAt: Date.now() - 1000,
});

describe('SessionBackupService', () => {
  afterEach(() => {
    localStorage.clear();
  });

  describe('backup signal initialization', () => {
    it('backup() is null when localStorage is empty', () => {
      TestBed.configureTestingModule({});
      const service = TestBed.inject(SessionBackupService);

      expect(service.backup()).toBeNull();
    });

    it('backup() loads the value when a valid backup exists in localStorage', () => {
      const validBackup = makeValidBackup();
      localStorage.setItem(BACKUP_KEY, JSON.stringify(validBackup));

      TestBed.configureTestingModule({});
      const service = TestBed.inject(SessionBackupService);

      expect(service.backup()).toEqual(validBackup);
    });

    it('backup() is null when stored version is not "1"', () => {
      const oldVersionBackup = { ...makeValidBackup(), version: '2' };
      localStorage.setItem(BACKUP_KEY, JSON.stringify(oldVersionBackup));

      TestBed.configureTestingModule({});
      const service = TestBed.inject(SessionBackupService);

      expect(service.backup()).toBeNull();
    });

    it('backup() is null and clears localStorage when backup is older than 24 hours', () => {
      const expiredBackup: SessionBackup = {
        ...makeValidBackup(),
        updatedAt: Date.now() - 25 * 60 * 60 * 1000,
      };
      localStorage.setItem(BACKUP_KEY, JSON.stringify(expiredBackup));

      TestBed.configureTestingModule({});
      const service = TestBed.inject(SessionBackupService);

      expect(service.backup()).toBeNull();
      expect(localStorage.getItem(BACKUP_KEY)).toBeNull();
    });

    it('backup() is null when localStorage data is corrupt JSON', () => {
      localStorage.setItem(BACKUP_KEY, 'not-valid-json{{');

      TestBed.configureTestingModule({});
      const service = TestBed.inject(SessionBackupService);

      expect(service.backup()).toBeNull();
    });
  });

  describe('save()', () => {
    let service: SessionBackupService;

    beforeEach(() => {
      TestBed.configureTestingModule({});
      service = TestBed.inject(SessionBackupService);
    });

    it('serializes the backup to localStorage under the correct key', () => {
      const backup = makeValidBackup();
      service.save(backup);

      const stored = JSON.parse(localStorage.getItem(BACKUP_KEY)!);
      expect(stored).toEqual(backup);
    });

    it('updates the backup signal with the new value', () => {
      const backup = makeValidBackup();
      service.save(backup);

      expect(service.backup()).toEqual(backup);
    });
  });

  describe('clear()', () => {
    let service: SessionBackupService;

    beforeEach(() => {
      TestBed.configureTestingModule({});
      service = TestBed.inject(SessionBackupService);
    });

    it('removes BACKUP_KEY from localStorage', () => {
      service.save(makeValidBackup());
      service.clear();

      expect(localStorage.getItem(BACKUP_KEY)).toBeNull();
    });

    it('sets the backup signal to null', () => {
      service.save(makeValidBackup());
      service.clear();

      expect(service.backup()).toBeNull();
    });
  });

  describe('markAutoResume() / consumeAutoResume()', () => {
    let service: SessionBackupService;

    beforeEach(() => {
      TestBed.configureTestingModule({});
      service = TestBed.inject(SessionBackupService);
    });

    it('consumeAutoResume() returns false before markAutoResume() is called', () => {
      expect(service.consumeAutoResume()).toBe(false);
    });

    it('consumeAutoResume() returns true right after markAutoResume()', () => {
      service.markAutoResume();
      expect(service.consumeAutoResume()).toBe(true);
    });

    it('consumeAutoResume() returns false on second call (one-shot flag)', () => {
      service.markAutoResume();
      service.consumeAutoResume();
      expect(service.consumeAutoResume()).toBe(false);
    });
  });
});
