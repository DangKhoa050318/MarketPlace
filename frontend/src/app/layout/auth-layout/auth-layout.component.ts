import { Component } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'app-auth-layout',
  standalone: true,
  imports: [RouterLink, RouterOutlet, MatIconModule],
  template: `
    <main class="auth-shell">
      <div class="auth-glow auth-glow-one"></div>
      <div class="auth-glow auth-glow-two"></div>

      <section class="auth-frame" aria-label="OrderFlow account">
        <aside class="brand-panel">
          <a class="brand" routerLink="/login" aria-label="OrderFlow home">
            <span class="brand-mark"><mat-icon>local_shipping</mat-icon></span>
            <span>Order<span>Flow</span></span>
          </a>

          <div class="brand-copy">
            <span class="eyebrow">Commerce, simplified</span>
            <h1>Every order.<br><span>Perfectly in flow.</span></h1>
            <p>
              Manage your shopping journey from discovery to delivery in one
              secure, seamless workspace.
            </p>
          </div>

          <div class="feature-list" aria-label="Platform benefits">
            <div class="feature">
              <span><mat-icon>verified_user</mat-icon></span>
              <div>
                <strong>Secure by design</strong>
                <small>Your account and orders stay protected.</small>
              </div>
            </div>
            <div class="feature">
              <span><mat-icon>bolt</mat-icon></span>
              <div>
                <strong>Fast checkout</strong>
                <small>Move from cart to confirmation in seconds.</small>
              </div>
            </div>
            <div class="feature">
              <span><mat-icon>route</mat-icon></span>
              <div>
                <strong>Live order journey</strong>
                <small>Follow each status from purchase to delivery.</small>
              </div>
            </div>
          </div>

          <p class="brand-footnote">
            <mat-icon>lock</mat-icon> Protected by enterprise-grade authentication
          </p>
        </aside>

        <section class="form-panel">
          <div class="mobile-brand">
            <span class="brand-mark"><mat-icon>local_shipping</mat-icon></span>
            <span>Order<span>Flow</span></span>
          </div>
          <router-outlet></router-outlet>
        </section>
      </section>
    </main>
  `,
  styles: [`
    :host { display: block; }

    .auth-shell {
      position: relative;
      min-height: 100dvh;
      display: grid;
      place-items: center;
      padding: 32px;
      overflow: hidden;
      background:
        radial-gradient(circle at 8% 12%, rgba(99, 102, 241, .14), transparent 32%),
        radial-gradient(circle at 92% 88%, rgba(6, 182, 212, .15), transparent 30%),
        #f7f9fc;
    }

    .auth-glow {
      position: absolute;
      border-radius: 999px;
      filter: blur(8px);
      pointer-events: none;
    }

    .auth-glow-one {
      width: 280px; height: 280px; top: -110px; right: 14%;
      background: rgba(139, 92, 246, .12);
    }

    .auth-glow-two {
      width: 220px; height: 220px; bottom: -100px; left: 12%;
      background: rgba(6, 182, 212, .12);
    }

    .auth-frame {
      position: relative;
      z-index: 1;
      display: grid;
      grid-template-columns: minmax(360px, .92fr) minmax(440px, 1.08fr);
      width: min(1080px, 100%);
      min-height: 680px;
      overflow: hidden;
      border: 1px solid rgba(255, 255, 255, .9);
      border-radius: 30px;
      background: rgba(255, 255, 255, .8);
      box-shadow: 0 35px 90px -35px rgba(30, 41, 59, .35);
      backdrop-filter: blur(20px);
      transform: translateZ(0);
    }

    .brand-panel {
      position: relative;
      display: flex;
      flex-direction: column;
      padding: 48px;
      overflow: hidden;
      color: #fff;
      background:
        radial-gradient(circle at 100% 0, rgba(6, 182, 212, .28), transparent 37%),
        radial-gradient(circle at 0 100%, rgba(139, 92, 246, .32), transparent 40%),
        linear-gradient(145deg, #111b3c 0%, #202a64 54%, #343183 100%);
    }

    .brand-panel::after {
      content: '';
      position: absolute;
      width: 260px; height: 260px;
      right: -130px; bottom: 70px;
      border: 1px solid rgba(255, 255, 255, .1);
      border-radius: 50%;
      box-shadow: 0 0 0 45px rgba(255, 255, 255, .025), 0 0 0 90px rgba(255, 255, 255, .018);
    }

    .brand, .mobile-brand {
      display: inline-flex;
      align-items: center;
      gap: 12px;
      color: inherit;
      font-size: 1.35rem;
      font-weight: 800;
      letter-spacing: -.04em;
      text-decoration: none;
    }

    .brand > span:last-child > span, .mobile-brand > span:last-child > span { color: #67e8f9; }

    .brand-mark {
      display: grid;
      width: 42px; height: 42px;
      place-items: center;
      border: 1px solid rgba(255, 255, 255, .25);
      border-radius: 13px;
      color: #fff;
      background: linear-gradient(135deg, #6366f1, #06b6d4);
      box-shadow: 0 10px 30px rgba(6, 182, 212, .24);
    }

    .brand-mark mat-icon { font-size: 23px; width: 23px; height: 23px; }

    .brand-copy { margin: auto 0 34px; }
    .eyebrow {
      display: inline-block;
      margin-bottom: 18px;
      color: #a5f3fc;
      font-size: .75rem;
      font-weight: 800;
      letter-spacing: .16em;
      text-transform: uppercase;
    }

    .brand-copy h1 {
      margin: 0 0 22px;
      font-size: clamp(2.35rem, 4vw, 3.45rem);
      line-height: 1.04;
      letter-spacing: -.055em;
    }

    .brand-copy h1 span {
      background: linear-gradient(90deg, #a5b4fc, #67e8f9);
      -webkit-background-clip: text;
      color: transparent;
    }

    .brand-copy p {
      max-width: 390px;
      margin: 0;
      color: rgba(226, 232, 240, .78);
      font-size: .98rem;
      line-height: 1.75;
    }

    .feature-list { display: grid; gap: 18px; margin-bottom: 40px; }
    .feature { display: flex; align-items: center; gap: 14px; }
    .feature > span {
      display: grid; flex: 0 0 39px; height: 39px; place-items: center;
      border: 1px solid rgba(255, 255, 255, .12);
      border-radius: 12px; color: #a5f3fc; background: rgba(255, 255, 255, .07);
    }
    .feature mat-icon { font-size: 20px; width: 20px; height: 20px; }
    .feature strong, .feature small { display: block; }
    .feature strong { margin-bottom: 3px; font-size: .9rem; }
    .feature small { color: rgba(226, 232, 240, .65); font-size: .76rem; }

    .brand-footnote {
      display: flex; align-items: center; gap: 7px;
      margin: 0; color: rgba(226, 232, 240, .55); font-size: .7rem;
    }
    .brand-footnote mat-icon { font-size: 14px; width: 14px; height: 14px; }

    .form-panel {
      display: grid;
      align-items: center;
      padding: 48px clamp(38px, 6vw, 78px);
      background: rgba(255, 255, 255, .88);
    }

    .mobile-brand { display: none; color: #172554; }

    @media (max-width: 840px) {
      .auth-shell { padding: 20px; align-items: start; overflow-y: auto; }
      .auth-frame { grid-template-columns: 1fr; width: min(560px, 100%); min-height: auto; }
      .brand-panel { display: none; }
      .form-panel { min-height: calc(100dvh - 40px); padding: 36px clamp(24px, 8vw, 56px); }
      .mobile-brand { display: inline-flex; align-self: start; margin-bottom: 42px; }
    }

    @media (max-width: 480px) {
      .auth-shell { padding: 0; background: #fff; }
      .auth-frame { min-height: 100dvh; border: 0; border-radius: 0; box-shadow: none; }
      .form-panel { min-height: 100dvh; padding: 28px 22px 36px; }
      .mobile-brand { margin-bottom: 34px; }
    }

    @media (prefers-reduced-motion: reduce) {
      .auth-frame, .auth-glow { transition: none; animation: none; }
    }
  `]
})
export class AuthLayoutComponent {}
