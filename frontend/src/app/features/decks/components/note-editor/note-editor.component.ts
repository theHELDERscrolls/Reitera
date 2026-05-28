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
import { FormsModule } from '@angular/forms';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';

import { defaultKeymap, history, historyKeymap } from '@codemirror/commands';
import { markdown } from '@codemirror/lang-markdown';
import { languages } from '@codemirror/language-data';
import { EditorState } from '@codemirror/state';
import { EditorView, keymap } from '@codemirror/view';

import { NoteRequest, NoteResponse, NoteType } from '@core/models/note.model';
import { NoteService } from '@features/decks/services/note.service';
import { ToastService } from '@core/toast/toast.service';

@Component({
  selector: 'app-note-editor',
  imports: [FormsModule, TranslocoPipe],
  templateUrl: './note-editor.component.html',
})
export class NoteEditorComponent implements AfterViewInit, OnDestroy {
  private readonly noteService = inject(NoteService);
  private readonly toastService = inject(ToastService);
  private readonly transloco = inject(TranslocoService);

  readonly deckId = input.required<number>();
  readonly noteToEdit = input<NoteResponse | null>(null);

  readonly saved = output<NoteResponse>();
  readonly closed = output<void>();

  readonly isSaving = signal(false);
  readonly content = signal('');
  readonly explanation = signal('');
  readonly detectedType = signal<NoteType>('UNKNOWN');

  readonly isEditMode = computed(() => this.noteToEdit() !== null);

  readonly editorContainer = viewChild.required<ElementRef>('editorContainer');

  private view: EditorView | null = null;

  constructor() {
    effect(() => {
      const note = this.noteToEdit();
      if (this.view) {
        const incoming = note?.content ?? '';
        if (incoming !== this.view.state.doc.toString()) {
          this.view.dispatch({
            changes: { from: 0, to: this.view.state.doc.length, insert: incoming },
          });
        }
        this.explanation.set(note?.explanation ?? '');
      }
    });
  }

  ngAfterViewInit(): void {
    const initialContent = this.noteToEdit()?.content ?? '';
    this.explanation.set(this.noteToEdit()?.explanation ?? '');

    const state = EditorState.create({
      doc: initialContent,
      extensions: [
        history(),
        keymap.of([...defaultKeymap, ...historyKeymap]),
        markdown({ codeLanguages: languages }),
        EditorView.lineWrapping,
        EditorView.updateListener.of((update) => {
          if (update.docChanged) {
            const text = update.state.doc.toString();
            this.content.set(text);
            this.detectedType.set(this.detectType(text));
          }
        }),
      ],
    });

    this.view = new EditorView({
      state,
      parent: this.editorContainer().nativeElement,
    });

    this.content.set(initialContent);
    this.detectedType.set(this.detectType(initialContent));
  }

  ngOnDestroy(): void {
    this.view?.destroy();
  }

  submit(): void {
    if (this.isSaving() || this.detectedType() === 'UNKNOWN') return;

    const dto: NoteRequest = {
      content: this.content(),
      explanation: this.explanation().trim() || null,
    };

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

  private detectType(content: string): NoteType {
    if (!content.trim()) return 'UNKNOWN';
    const lines = content.split('\n').map((l) => l.trim());
    if (/\{\{c\d+::/.test(content)) return 'CLOZE';
    if (content.includes('- [x]') || content.includes('- [ ]')) return 'MULTIPLE_CHOICE';
    if (lines.includes('---') && lines.includes('<->')) return 'BASIC_REVERSE';
    if (lines.includes('---')) return 'BASIC';
    return 'UNKNOWN';
  }

  private resetEditor(): void {
    if (this.view) {
      this.view.dispatch({
        changes: { from: 0, to: this.view.state.doc.length, insert: '' },
      });
    }
    this.content.set('');
    this.explanation.set('');
    this.detectedType.set('UNKNOWN');
  }
}
