import { Directive, ElementRef, Input, OnDestroy, OnInit } from '@angular/core';

/**
 * ScrollReveal — phát hiện khi phần tử xuất hiện trong viewport
 * và thêm class `revealed` để kích hoạt CSS animation.
 *
 * Usage:
 *   <section appScrollReveal>...</section>
 *
 * Tuỳ chỉnh:
 *   [delay]="200"     — delay (ms) trước khi thêm class
 *   [offset]="60"     — pixel offset sớm hơn so với mép viewport
 *   [once]="true"     — chỉ animate 1 lần (mặc định: true)
 */
@Directive({
  selector: '[appScrollReveal]',
  standalone: true
})
export class ScrollRevealDirective implements OnInit, OnDestroy {
  @Input() delay = 0;
  @Input() offset = 40;
  @Input() once = true;

  private observer: IntersectionObserver | null = null;

  constructor(private el: ElementRef<HTMLElement>) {}

  ngOnInit(): void {
    // Start hidden
    this.el.nativeElement.style.opacity = '0';
    this.el.nativeElement.style.transform = 'translateY(30px)';
    this.el.nativeElement.style.transition =
      `opacity 520ms cubic-bezier(0.25, 0.46, 0.45, 0.94), transform 520ms cubic-bezier(0.25, 0.46, 0.45, 0.94)`;
    this.el.nativeElement.style.willChange = 'opacity, transform';

    this.observer = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            const target = entry.target as HTMLElement;

            if (this.delay > 0) {
              target.style.transitionDelay = `${this.delay}ms`;
            }

            // Small RAF to ensure the browser has the initial styles painted
            requestAnimationFrame(() => {
              target.style.opacity = '1';
              target.style.transform = 'translateY(0)';
            });

            if (this.once && this.observer) {
              this.observer.unobserve(target);
            }
          } else if (!this.once) {
            // Re-hide when scrolled back out (only if once=false)
            entry.target as HTMLElement;
          }
        }
      },
      {
        threshold: 0,
        rootMargin: `0px 0px -${this.offset}px 0px`
      }
    );

    this.observer.observe(this.el.nativeElement);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }
}
