import { animate, query, stagger, style, transition, trigger } from '@angular/animations';

/**
 * Route transition: fade + slight vertical slide.
 * Use with <router-outlet @routeFade> in layout components.
 */
export const routeFade =
  trigger('routeFade', [
    transition('* <=> *', [
      query(':enter', [
        style({ opacity: 0, transform: 'translateY(12px)' }),
        animate('320ms ease-out', style({ opacity: 1, transform: 'translateY(0)' }))
      ], { optional: true })
    ])
  ]);

/**
 * List stagger: items fade in one after another.
 * Use with *ngFor container [@listStagger]="items.length"
 */
export const listStagger =
  trigger('listStagger', [
    transition('* => *', [
      query(':enter', [
        style({ opacity: 0, transform: 'translateX(-12px)' }),
        animate('250ms ease-out', style({ opacity: 1, transform: 'translateX(0)' }))
      ], { optional: true })
    ])
  ]);

/**
 * Fade-in from bottom — for standalone elements.
 * Each element with [@fadeInUp] animates independently on :enter.
 */
export const fadeInUp =
  trigger('fadeInUp', [
    transition(':enter', [
      style({ opacity: 0, transform: 'translateY(24px)' }),
      animate('380ms cubic-bezier(0.25, 0.46, 0.45, 0.94)',
        style({ opacity: 1, transform: 'translateY(0)' }))
    ])
  ]);

/**
 * Scale-in entrance — for cards / modals.
 */
export const scaleIn =
  trigger('scaleIn', [
    transition(':enter', [
      style({ opacity: 0, transform: 'scale(0.94)' }),
      animate('300ms cubic-bezier(0.34, 1.56, 0.64, 1)',
        style({ opacity: 1, transform: 'scale(1)' }))
    ])
  ]);

/**
 * Page load stagger: sections fade-up in sequence.
 * Use with <div [@pageStagger]="pageLoaded">.
 */
export const pageStagger =
  trigger('pageStagger', [
    transition('* => true', [
      query('.fade-section', [
        style({ opacity: 0, transform: 'translateY(28px)' }),
        stagger(140, [
          animate('480ms cubic-bezier(0.25, 0.46, 0.45, 0.94)',
            style({ opacity: 1, transform: 'translateY(0)' }))
        ])
      ], { optional: true })
    ])
  ]);
