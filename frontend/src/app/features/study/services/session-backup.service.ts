import { Injectable } from '@angular/core';
import { SessionBackup } from '@core/models/study.model';

const BACKUP_KEY = 'reitera_session_backup';
const MAX_AGE_MS = 24 * 60 * 60 * 1000;

@Injectable({
  providedIn: 'root',
})
export class SessionBackupService {
  save(backup: SessionBackup): void {
    try {
      localStorage.setItem(BACKUP_KEY, JSON.stringify(backup));
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
        this.clear();
        
        return null;
      }

      if (Date.now() - parsed.updatedAt > MAX_AGE_MS) {
        this.clear();

        return null;
      }

      return parsed;
    } catch {
      this.clear();

      return null;
    }
  }

  clear(): void {
    localStorage.removeItem(BACKUP_KEY);
  }
}
