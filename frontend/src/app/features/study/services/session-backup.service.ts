import { Injectable, signal } from '@angular/core';
import { SessionBackup } from '@core/models/study.model';

const BACKUP_KEY = 'reitera_session_backup';
const MAX_AGE_MS = 24 * 60 * 60 * 1000;

@Injectable({
  providedIn: 'root',
})
export class SessionBackupService {
  readonly backup = signal<SessionBackup | null>(this.load());

  private _autoResume = false;

  markAutoResume(): void {
    this._autoResume = true;
  }

  consumeAutoResume(): boolean {
    const value = this._autoResume;
    this._autoResume = false;
    return value;
  }

  save(backup: SessionBackup): void {
    try {
      localStorage.setItem(BACKUP_KEY, JSON.stringify(backup));
      this.backup.set(backup);
    } catch {
      return;
    }
  }

  load(): SessionBackup | null {
    try {
      const raw = localStorage.getItem(BACKUP_KEY);

      if (!raw) return null;

      const parsed = JSON.parse(raw) as SessionBackup;

      if (parsed.version !== '1') {
        localStorage.removeItem(BACKUP_KEY);

        return null;
      }

      if (Date.now() - parsed.updatedAt > MAX_AGE_MS) {
        localStorage.removeItem(BACKUP_KEY);

        return null;
      }

      return parsed;
    } catch {
      localStorage.removeItem(BACKUP_KEY);

      return null;
    }
  }

  clear(): void {
    localStorage.removeItem(BACKUP_KEY);
    this.backup.set(null);
  }
}
