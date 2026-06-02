import { Component } from '@angular/core';
import { LucideMinus } from '@lucide/angular';
import { TranslocoPipe } from '@jsverse/transloco';

interface TipItem {
  id: string;
  variant: 'primary' | 'info' | 'warning' | 'success';
  labelKey: string;
  tipKeys: string[];
}

const TIP_ITEMS: TipItem[] = [
  {
    id: 'basic',
    variant: 'primary',
    labelKey: 'profile.guide.basic.label',
    tipKeys: ['profile.tips.basic.tip1', 'profile.tips.basic.tip2', 'profile.tips.basic.tip3'],
  },
  {
    id: 'basicReverse',
    variant: 'info',
    labelKey: 'profile.guide.basicReverse.label',
    tipKeys: [
      'profile.tips.basicReverse.tip1',
      'profile.tips.basicReverse.tip2',
      'profile.tips.basicReverse.tip3',
    ],
  },
  {
    id: 'cloze',
    variant: 'warning',
    labelKey: 'profile.guide.cloze.label',
    tipKeys: ['profile.tips.cloze.tip1', 'profile.tips.cloze.tip2', 'profile.tips.cloze.tip3'],
  },
  {
    id: 'multipleChoice',
    variant: 'success',
    labelKey: 'profile.guide.multipleChoice.label',
    tipKeys: [
      'profile.tips.multipleChoice.tip1',
      'profile.tips.multipleChoice.tip2',
      'profile.tips.multipleChoice.tip3',
    ],
  },
];

const TIP_CARD_CLASSES: Record<string, string> = {
  primary: 'bg-primary border-primary-foreground/50',
  info: 'bg-info border-primary-foreground/50',
  warning: 'bg-warning border-primary-foreground/50',
  success: 'bg-success border-primary-foreground/50',
};

@Component({
  selector: 'app-profile-tips',
  imports: [LucideMinus, TranslocoPipe],
  templateUrl: './profile-tips.component.html',
})
export class ProfileTipsComponent {
  readonly tipItems = TIP_ITEMS;

  tipCardClasses(variant: string): string {
    return TIP_CARD_CLASSES[variant] ?? 'bg-overlay border-border';
  }
}
