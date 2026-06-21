import { Component, computed, forwardRef, input, signal } from '@angular/core';
import { ControlValueAccessor, NG_VALUE_ACCESSOR, ReactiveFormsModule } from '@angular/forms';
import { LucideDynamicIcon, LucideIconInput } from '@lucide/angular';

let nextInputId = 0;

@Component({
  selector: 'app-input',
  standalone: true,
  imports: [LucideDynamicIcon, ReactiveFormsModule],
  templateUrl: './input.component.html',
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => AppInputComponent),
      multi: true,
    },
  ],
})
export default class AppInputComponent implements ControlValueAccessor {
  readonly label = input<string>('');
  readonly type = input<string>('text');
  readonly placeholder = input<string>('');
  readonly error = input<string>('');
  readonly hint = input<string>('');
  readonly iconLeft = input<LucideIconInput | null>(null);
  readonly isDisabled = input<boolean>(false);
  readonly multiline = input<boolean>(false);
  readonly rows = input<number>(3);

  readonly inputId = `app-input-${nextInputId++}`;

  readonly value = signal<string>('');
  private readonly _cvaDisabled = signal(false);

  readonly effectivelyDisabled = computed(() => this.isDisabled() || this._cvaDisabled());

  readonly inputClasses = computed(() => {
    const base =
      'w-full rounded-xs border-3 bg-surface-alt px-3.5 py-2.5 text-sm text-foreground ' +
      'placeholder:text-subtle-foreground outline-none transition ' +
      'focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-primary/50 ' +
      'disabled:cursor-not-allowed disabled:opacity-50';
    const borderState = this.error()
      ? 'border-error focus-visible:ring-error/30'
      : 'border-border focus-visible:border-primary';
    const iconPadding = this.iconLeft() ? 'pl-9' : '';
    const textareaResize = this.multiline() ? 'resize-none' : '';
    
    return [base, borderState, iconPadding, textareaResize].filter(Boolean).join(' ');
  });

  private onChange: (value: string) => void = () => undefined;
  
  private onTouched: () => void = () => undefined;

  handleInput(event: Event): void {
    const newValue = (event.target as HTMLInputElement).value;
    this.value.set(newValue);
    this.onChange(newValue);
  }

  handleBlur(): void {
    this.onTouched();
  }

  writeValue(value: string): void {
    this.value.set(value ?? '');
  }

  registerOnChange(fn: (value: string) => void): void {
    this.onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this._cvaDisabled.set(isDisabled);
  }
}
