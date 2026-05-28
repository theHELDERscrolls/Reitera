import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  computed,
  effect,
  inject,
  input,
  output,
  signal,
  viewChild,
} from '@angular/core';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { defaultKeymap, history, historyKeymap } from '@codemirror/commands';
import { markdown } from '@codemirror/lang-markdown';
import { languages } from '@codemirror/language-data';
import { Compartment, EditorState } from '@codemirror/state';
import { EditorView, keymap } from '@codemirror/view';

import { NoteRequest, NoteResponse, NoteType } from '@core/models/note.model';
import { ThemeService } from '@core/theme/theme.service';
import { NoteService } from '@features/decks/services/note.service';
import { ToastService } from '@core/toast/toast.service';

const EXPLANATION_SEPARATOR = '===';

@Component({
  selector: 'app-note-editor',
  imports: [TranslocoPipe],
  templateUrl: './note-editor.component.html',
})
export class NoteEditorComponent implements AfterViewInit, OnDestroy {
  private readonly noteService = inject(NoteService);
  private readonly themeService = inject(ThemeService);
  private readonly toastService = inject(ToastService);
  private readonly transloco = inject(TranslocoService);

  private readonly darkThemeCompartment = new Compartment();

  readonly deckId = input.required<number>();
  readonly noteToEdit = input<NoteResponse | null>(null);

  readonly saved = output<NoteResponse>();
  readonly closed = output<void>();

  readonly isSaving = signal(false);
  readonly rawContent = signal('');
  readonly detectedType = computed(() => this.detectType(this.rawContent()));

  readonly isEditMode = computed(() => this.noteToEdit() !== null);

  readonly hasConflict = computed(() => {
    const { content } = this.splitRaw(this.rawContent());
    return this.detectConflictingMarkers(content, this.detectedType());
  });

  readonly editorContainer = viewChild.required<ElementRef>('editorContainer');

  private view: EditorView | null = null;

  constructor() {
    effect(() => {
      const note = this.noteToEdit();
      if (this.view) {
        const incoming = this.buildRaw(note?.content ?? '', note?.explanation ?? null);
        if (incoming !== this.view.state.doc.toString()) {
          this.view.dispatch({
            changes: { from: 0, to: this.view.state.doc.length, insert: incoming },
          });
        }
      }
    });

    effect(() => {
      this.view?.dispatch({
        effects: this.darkThemeCompartment.reconfigure(
          EditorView.darkTheme.of(this.themeService.theme() === 'dark'),
        ),
      });
    });
  }

  ngAfterViewInit(): void {
    const note = this.noteToEdit();
    const initialRaw = this.buildRaw(note?.content ?? '', note?.explanation ?? null);

    const state = EditorState.create({
      doc: initialRaw,
      extensions: [
        history(),
        keymap.of([...defaultKeymap, ...historyKeymap]),
        markdown({ codeLanguages: languages }),
        EditorView.lineWrapping,
        this.darkThemeCompartment.of(EditorView.darkTheme.of(this.themeService.theme() === 'dark')),
        EditorView.updateListener.of((update) => {
          if (update.docChanged) {
            this.rawContent.set(update.state.doc.toString());
          }
        }),
      ],
    });

    this.view = new EditorView({
      state,
      parent: this.editorContainer().nativeElement,
    });

    this.rawContent.set(initialRaw);
  }

  ngOnDestroy(): void {
    this.view?.destroy();
  }

  submit(): void {
    if (this.isSaving() || this.detectedType() === 'UNKNOWN' || this.hasConflict()) return;

    const { content, explanation } = this.splitRaw(this.rawContent());
    const dto: NoteRequest = { content, explanation };

    this.isSaving.set(true);

    const note = this.noteToEdit();
    const operation = note
      ? this.noteService.updateNote(this.deckId(), note.id, dto)
      : this.noteService.createNote(this.deckId(), dto);

    operation.subscribe({
      next: (result) => {
        this.isSaving.set(false);
        if (!this.isEditMode()) {
          this.resetEditor();
        }
        this.saved.emit(result);
      },
      error: () => {
        this.isSaving.set(false);
        this.toastService.error(this.transloco.translate('common.error'));
      },
    });
  }

  private buildRaw(content: string, explanation: string | null): string {
    if (!explanation?.trim()) return content;
    return `${content}\n\n${EXPLANATION_SEPARATOR}\n\n${explanation}`;
  }

  private splitRaw(raw: string): { content: string; explanation: string | null } {
    const separatorPattern = /\n[ \t]*===[ \t]*\n/;
    const match = separatorPattern.exec(raw);

    if (!match) return { content: raw.trim(), explanation: null };

    const content = raw.slice(0, match.index).trim();
    const explanation = raw.slice(match.index + match[0].length).trim() || null;

    return { content, explanation };
  }

  private detectType(raw: string): NoteType {
    const { content } = this.splitRaw(raw);

    if (!content.trim()) return 'UNKNOWN';

    const lines = new Set(content.split('\n').map((line) => line.trim()));

    if (/\{\{c\d+::/.test(content)) return 'CLOZE';

    if (content.includes('- [x]') || content.includes('- [ ]')) return 'MULTIPLE_CHOICE';

    if (lines.has('---') && lines.has('<->')) return 'BASIC_REVERSE';

    if (lines.has('---')) return 'BASIC';

    return 'UNKNOWN';
  }

  private detectConflictingMarkers(content: string, detectedType: NoteType): boolean {
    if (
      detectedType === 'UNKNOWN' ||
      detectedType === 'BASIC' ||
      detectedType === 'BASIC_REVERSE'
    ) {
      return false;
    }

    const lines = new Set(content.split('\n').map((line) => line.trim()));
    const hasSeparator = lines.has('---');
    const hasReverse = lines.has('<->');

    if (detectedType === 'CLOZE') {
      const hasMCMarkers = content.includes('- [x]') || content.includes('- [ ]');

      return hasMCMarkers || hasSeparator || hasReverse;
    }

    if (detectedType === 'MULTIPLE_CHOICE') {
      return hasSeparator || hasReverse;
    }

    return false;
  }

  private resetEditor(): void {
    if (this.view) {
      this.view.dispatch({
        changes: { from: 0, to: this.view.state.doc.length, insert: '' },
      });
    }

    this.rawContent.set('');
  }
}
