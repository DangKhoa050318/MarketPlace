import {
  AfterViewInit,
  Directive,
  ElementRef,
  EventEmitter,
  OnDestroy,
  Output
} from '@angular/core';

/**
 * Fires {@code impressed} exactly once when the host element first reaches 50% visibility
 * (F-405). Mirrors the recommendation carousel's impression rule so merchandising and
 * recommendation analytics stay consistent. When IntersectionObserver is unavailable
 * (e.g. jsdom in unit tests) it emits immediately so tracking still runs.
 */
@Directive({
  selector: '[appMerchandisingImpression]',
  standalone: true
})
export class MerchandisingImpressionDirective implements AfterViewInit, OnDestroy {
  @Output() impressed = new EventEmitter<void>();

  private observer?: IntersectionObserver;
  private fired = false;

  constructor(private host: ElementRef<HTMLElement>) {}

  ngAfterViewInit(): void {
    if (typeof IntersectionObserver === 'undefined') {
      this.emitOnce();
      return;
    }
    this.observer = new IntersectionObserver(
      entries => {
        for (const entry of entries) {
          if (entry.isIntersecting && entry.intersectionRatio >= 0.5) {
            this.emitOnce();
            this.observer?.disconnect();
            break;
          }
        }
      },
      { threshold: 0.5 }
    );
    this.observer.observe(this.host.nativeElement);
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }

  private emitOnce(): void {
    if (this.fired) return;
    this.fired = true;
    this.impressed.emit();
  }
}
