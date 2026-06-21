import { Component, signal } from '@angular/core';
import { LucideBookOpen, LucideChevronDown } from '@lucide/angular';
import { TranslocoPipe } from '@jsverse/transloco';

import AppBadgeComponent from '@shared/components/ui/badge/badge.component';

interface GuideItem {
  id: string;
  variant: 'primary' | 'info' | 'warning' | 'success';
  labelKey: string;
  cardsKey: string;
  descriptionKey: string;
  example: string;
}

const GUIDE_ITEMS: GuideItem[] = [
  {
    id: 'basic',
    variant: 'primary',
    labelKey: 'profile.guide.basic.label',
    cardsKey: 'profile.guide.basic.cards',
    descriptionKey: 'profile.guide.basic.description',
    example:
      'What is the capital of France?\n\n---\n\nParis\n\n===\n\nParis has been the capital of France since the late 10th century.',
  },
  {
    id: 'basicReverse',
    variant: 'info',
    labelKey: 'profile.guide.basicReverse.label',
    cardsKey: 'profile.guide.basicReverse.cards',
    descriptionKey: 'profile.guide.basicReverse.description',
    example: 'H₂O\n\n---\n\nWater\n\n<->\n\n===\n\nWater is essential for all known forms of life.',
  },
  {
    id: 'cloze',
    variant: 'warning',
    labelKey: 'profile.guide.cloze.label',
    cardsKey: 'profile.guide.cloze.cards',
    descriptionKey: 'profile.guide.cloze.description',
    example:
      'The war ended in {{c1::1945}}.\nThe treaty was signed in {{c2::Paris}}.\n\n===\n\nWWII ended in Europe on May 8, 1945 (V-E Day). The Paris Peace Treaties were signed in 1947.',
  },
  {
    id: 'multipleChoice',
    variant: 'success',
    labelKey: 'profile.guide.multipleChoice.label',
    cardsKey: 'profile.guide.multipleChoice.cards',
    descriptionKey: 'profile.guide.multipleChoice.description',
    example:
      'Which is the largest ocean?\n\n- [x] Pacific\n- [ ] Atlantic\n- [ ] Indian\n\n===\n\nThe Pacific Ocean covers about 165 million km², more than all landmasses combined.',
  },
];

@Component({
  selector: 'app-profile-guide',
  imports: [AppBadgeComponent, LucideBookOpen, LucideChevronDown, TranslocoPipe],
  templateUrl: './profile-guide.component.html',
})
export class ProfileGuideComponent {
  readonly guideItems = GUIDE_ITEMS;
  readonly openGuideItems = signal(new Set<string>());

  toggleGuideItem(id: string): void {
    this.openGuideItems.update((set) => {
      const next = new Set(set);
      if (next.has(id)) next.delete(id);
      else next.add(id);
      return next;
    });
  }
}
