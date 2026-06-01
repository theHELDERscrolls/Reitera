import { Component, computed, input } from '@angular/core';

export type AvatarSize = 'xs' | 'sm' | 'md' | 'lg' | 'xl';

@Component({
  selector: 'app-avatar',
  standalone: true,
  imports: [],
  templateUrl: './avatar.component.html',
})
export default class AppAvatarComponent {
  readonly src = input<string | null>(null);
  readonly name = input<string>('');
  readonly size = input<AvatarSize>('md');

  readonly initials = computed(() => {
    return this.name()
      .trim()
      .split(/\s+/)
      .filter(Boolean)
      .slice(0, 2)
      .map((word) => word[0].toUpperCase())
      .join('');
  });

  private readonly SIZE_CLASSES: Record<AvatarSize, string> = {
    xs: 'size-6 text-xs',
    sm: 'size-8 text-xs',
    md: 'size-10 text-sm',
    lg: 'size-14 text-base',
    xl: 'size-20 text-xl',
  };

  readonly containerClasses = computed(() => {
    return [
      'rounded-full border-2 border-border shrink-0 overflow-hidden',
      this.SIZE_CLASSES[this.size()],
    ].join(' ');
  });

  readonly initialsClasses = computed(() => {
    return 'flex items-center justify-center w-full h-full font-bold uppercase bg-overlay text-foreground';
  });
}
