import { Component, Input, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { ProductAnswer, ProductQuestion } from '../../../core/models/question.model';
import { QuestionService } from '../../../core/services/question.service';
import { VoteService } from '../../../core/services/vote.service';
import { AuthService } from '../../../core/services/auth.service';
import { NotificationService } from '../../../core/services/notification.service';
import { ContentType } from '../../../core/models/vote.model';

@Component({
  selector: 'app-product-question-list',
  standalone: true,
  imports: [
    CommonModule, FormsModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatPaginatorModule
  ],
  template: `
    <div class="questions-container surface-card">
      <div class="header">
        <h3>Hỏi & Đáp về sản phẩm</h3>
        <span class="count-badge">{{ totalElements }} câu hỏi</span>
      </div>

      <!-- Form Đặt Câu Hỏi -->
      <div class="ask-box">
        <h4>Đặt câu hỏi cho người bán & cộng đồng:</h4>
        <textarea
          [(ngModel)]="newQuestionContent"
          #qText="ngModel"
          maxlength="1000"
          rows="3"
          placeholder="Nhập câu hỏi của bạn (ví dụ: Sản phẩm có hỗ trợ bảo hành tận nơi không?)..."
          [disabled]="submittingQuestion"
          class="form-control"></textarea>
        
        <div class="form-footer">
          <span class="char-count" [class.warn]="newQuestionContent.length > 900">
            {{ newQuestionContent.length }}/1000 ký tự
          </span>
          <button
            mat-raised-button
            color="primary"
            [disabled]="!newQuestionContent.trim() || submittingQuestion"
            (click)="submitQuestion()">
            <mat-icon>send</mat-icon> Gửi câu hỏi
          </button>
        </div>
      </div>

      <!-- State Loading / Empty / List -->
      @if (loading) {
        <div class="center-state"><mat-spinner diameter="36"></mat-spinner></div>
      } @else if (questions.length === 0) {
        <div class="center-state empty">
          <mat-icon>question_answer</mat-icon>
          <p>Chưa có câu hỏi nào cho sản phẩm này. Hãy là người đầu tiên đặt câu hỏi!</p>
        </div>
      } @else {
        <div class="question-thread-list">
          @for (q of questions; track q.id) {
            <div class="question-card">
              <div class="q-header">
                <div class="q-user">
                  <mat-icon class="avatar-icon">account_circle</mat-icon>
                  <div>
                    <span class="user-name">{{ q.userName }}</span>
                    <span class="date">{{ q.createdAt | date:'dd/MM/yyyy HH:mm' }}</span>
                  </div>
                </div>
                <!-- Vote Hữu ích câu hỏi -->
                <button
                  mat-stroked-button
                  class="vote-btn"
                  [class.voted]="q.isVotedByCurrentUser"
                  (click)="vote(q.id, 'QUESTION', q)">
                  <mat-icon>{{ q.isVotedByCurrentUser ? 'thumb_up' : 'thumb_up_off_alt' }}</mat-icon>
                  <span>Hữu ích ({{ q.helpfulCount }})</span>
                </button>
              </div>

              <p class="q-content">{{ q.content }}</p>

              <!-- Danh sách câu trả lời -->
              @if (q.answers && q.answers.length > 0) {
                <div class="answers-list">
                  @for (a of q.answers; track a.id) {
                    <div class="answer-item" [class.official-item]="a.isOfficial">
                      <div class="a-header">
                        <div class="a-user">
                          <span class="user-name">{{ a.userName }}</span>
                          @if (a.isOfficial) {
                            <span class="official-badge"><mat-icon>verified</mat-icon> Đã xác minh</span>
                          }
                          <span class="date">{{ a.createdAt | date:'dd/MM/yyyy HH:mm' }}</span>
                        </div>
                        <div class="a-actions">
                          @if (isStaffOrAdmin && !a.isOfficial) {
                            <button mat-button color="accent" class="mark-btn" (click)="markOfficial(a)">
                              <mat-icon>verified</mat-icon> Xác minh câu trả lời
                            </button>
                          }
                          <button
                            mat-button
                            class="vote-btn sm"
                            [class.voted]="a.isVotedByCurrentUser"
                            (click)="vote(a.id, 'ANSWER', a)">
                            <mat-icon>{{ a.isVotedByCurrentUser ? 'thumb_up' : 'thumb_up_off_alt' }}</mat-icon>
                            <span>{{ a.helpfulCount }}</span>
                          </button>
                        </div>
                      </div>
                      <p class="a-content">{{ a.content }}</p>
                    </div>
                  }
                </div>
              }

              <!-- Nút Reply & Form trả lời -->
              <div class="reply-section">
                @if (activeReplyQuestionId === q.id) {
                  <div class="reply-box">
                    <textarea
                      [(ngModel)]="replyContent"
                      maxlength="1000"
                      rows="2"
                      placeholder="Nhập câu trả lời của bạn..."
                      class="form-control sm"></textarea>
                    <div class="form-footer sm">
                      <span class="char-count">{{ replyContent.length }}/1000</span>
                      <div>
                        <button mat-button (click)="activeReplyQuestionId = null">Hủy</button>
                        <button
                          mat-flat-button
                          color="primary"
                          [disabled]="!replyContent.trim() || submittingReply"
                          (click)="submitReply(q)">
                          Gửi trả lời
                        </button>
                      </div>
                    </div>
                  </div>
                } @else {
                  <button mat-button class="reply-toggle-btn" (click)="openReplyForm(q.id)">
                    <mat-icon>reply</mat-icon> Trả lời câu hỏi này
                  </button>
                }
              </div>
            </div>
          }
        </div>

        <mat-paginator
          [length]="totalElements"
          [pageSize]="pageSize"
          [pageIndex]="page"
          (page)="onPageChange($event)"
          showFirstLastButtons>
        </mat-paginator>
      }
    </div>
  `,
  styles: [`
    .questions-container { padding: 24px; border-radius: 16px; margin-top: 24px; background: #fff; border: 1px solid #e2e8f0; }
    .header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
    .header h3 { margin: 0; font-size: 1.3rem; font-weight: 800; color: #0f172a; }
    .count-badge { font-size: 0.85rem; font-weight: 700; background: #e0f2fe; color: #0369a1; padding: 4px 12px; border-radius: 20px; }
    .ask-box { background: #f8fafc; border: 1px solid #cbd5e1; border-radius: 12px; padding: 16px; margin-bottom: 24px; }
    .ask-box h4 { margin: 0 0 10px; font-size: 0.95rem; font-weight: 700; color: #334155; }
    .form-control { width: 100%; border: 1px solid #cbd5e1; border-radius: 8px; padding: 10px; font-family: inherit; font-size: 0.95rem; resize: vertical; box-sizing: border-box; }
    .form-control:focus { outline: none; border-color: #0284c7; box-shadow: 0 0 0 2px rgba(2,132,199,0.15); }
    .form-control.sm { font-size: 0.88rem; }
    .form-footer { display: flex; justify-content: space-between; align-items: center; margin-top: 10px; }
    .form-footer.sm { margin-top: 6px; }
    .char-count { font-size: 0.8rem; color: #64748b; }
    .char-count.warn { color: #e11d48; font-weight: 700; }
    .center-state { display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 40px 0; color: #64748b; text-align: center; }
    .center-state mat-icon { font-size: 48px; width: 48px; height: 48px; margin-bottom: 12px; opacity: 0.5; }
    .question-card { border: 1px solid #f1f5f9; border-radius: 12px; padding: 18px; margin-bottom: 16px; background: #fafafa; }
    .q-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
    .q-user { display: flex; align-items: center; gap: 10px; }
    .avatar-icon { font-size: 32px; width: 32px; height: 32px; color: #94a3b8; }
    .user-name { font-weight: 700; color: #0f172a; font-size: 0.92rem; }
    .date { font-size: 0.78rem; color: #94a3b8; margin-left: 8px; }
    .q-content { font-size: 1rem; font-weight: 600; color: #1e293b; margin: 8px 0 16px; line-height: 1.5; }
    .vote-btn { border-radius: 20px; font-size: 0.82rem; }
    .vote-btn.voted { color: #0284c7; border-color: #0284c7; background: #f0f9ff; }
    .vote-btn.sm { height: 28px; line-height: 28px; padding: 0 8px; }
    .answers-list { margin-left: 20px; border-left: 2px solid #e2e8f0; padding-left: 16px; margin-bottom: 14px; }
    .answer-item { background: #fff; border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px; margin-bottom: 10px; }
    .answer-item.official-item { border-color: #38bdf8; background: #f0f9ff; }
    .a-header { display: flex; justify-content: space-between; align-items: center; }
    .a-user { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
    .official-badge { display: inline-flex; align-items: center; gap: 4px; font-size: 0.75rem; font-weight: 800; background: #0284c7; color: #fff; padding: 2px 8px; border-radius: 12px; }
    .official-badge mat-icon { font-size: 14px; width: 14px; height: 14px; }
    .a-content { font-size: 0.92rem; color: #334155; margin: 6px 0 0; line-height: 1.5; }
    .reply-section { margin-top: 10px; }
    .reply-toggle-btn { color: #0284c7; font-size: 0.85rem; }
    .reply-box { background: #fff; border: 1px solid #cbd5e1; border-radius: 8px; padding: 10px; margin-top: 8px; }
  `]
})
export class ProductQuestionListComponent implements OnInit {
  @Input() productId!: number;

  questions: ProductQuestion[] = [];
  loading = false;
  totalElements = 0;
  page = 0;
  pageSize = 10;

  newQuestionContent = '';
  submittingQuestion = false;

  activeReplyQuestionId: number | null = null;
  replyContent = '';
  submittingReply = false;

  get isStaffOrAdmin(): boolean {
    const role = this.authService.getRole();
    return role === 'ADMIN' || role === 'MANAGER' || role === 'STAFF';
  }

  constructor(
    private questionService: QuestionService,
    private voteService: VoteService,
    private authService: AuthService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    if (this.productId) {
      this.loadQuestions();
    }
  }

  loadQuestions(): void {
    this.loading = true;
    this.questionService.getQuestions(this.productId, this.page, this.pageSize).subscribe({
      next: (res) => {
        if (res.data) {
          this.questions = res.data.content || [];
          this.totalElements = res.data.totalElements || 0;
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  submitQuestion(): void {
    if (!this.newQuestionContent.trim()) return;

    if (!this.authService.isAuthenticated()) {
      this.notificationService.info('Vui lòng đăng nhập để đặt câu hỏi');
      return;
    }

    this.submittingQuestion = true;
    this.questionService.askQuestion(this.productId, { content: this.newQuestionContent.trim() }).subscribe({
      next: () => {
        this.notificationService.success('Câu hỏi của bạn đã được gửi thành công!');
        this.newQuestionContent = '';
        this.submittingQuestion = false;
        this.loadQuestions();
      },
      error: (err) => {
        this.notificationService.error(err?.error?.message || 'Không thể gửi câu hỏi');
        this.submittingQuestion = false;
      }
    });
  }

  openReplyForm(questionId: number): void {
    this.activeReplyQuestionId = questionId;
    this.replyContent = '';
  }

  submitReply(q: ProductQuestion): void {
    if (!this.replyContent.trim()) return;

    if (!this.authService.isAuthenticated()) {
      this.notificationService.info('Vui lòng đăng nhập để trả lời');
      return;
    }

    this.submittingReply = true;
    this.questionService.answerQuestion(q.id, { content: this.replyContent.trim() }).subscribe({
      next: (res) => {
        this.notificationService.success('Trả lời đã được ghi nhận');
        if (!q.answers) q.answers = [];
        if (res.data) {
          q.answers.push(res.data);
        }
        this.activeReplyQuestionId = null;
        this.replyContent = '';
        this.submittingReply = false;
      },
      error: (err) => {
        this.notificationService.error(err?.error?.message || 'Không thể gửi câu trả lời');
        this.submittingReply = false;
      }
    });
  }

  markOfficial(answer: ProductAnswer): void {
    this.questionService.markOfficial(answer.id).subscribe({
      next: () => {
        answer.isOfficial = true;
        this.notificationService.success('Đã xác minh câu trả lời');
      },
      error: (err) => {
        this.notificationService.error(err?.error?.message || 'Lỗi khi đánh dấu');
      }
    });
  }

  vote(targetId: number, targetType: ContentType, item: { upvoteCount?: number; userUpvoted?: boolean }): void {
    if (!this.authService.isAuthenticated()) {
      this.notificationService.info('Vui lòng đăng nhập để bình chọn');
      return;
    }

    this.voteService.toggleVote({ targetType, targetId }).subscribe({
      next: (res) => {
        if (res.data) {
          item.isVotedByCurrentUser = res.data.isVoted;
          item.helpfulCount = res.data.helpfulCount;
        }
      },
      error: () => {
        this.notificationService.error('Không thể thực hiện bình chọn');
      }
    });
  }

  onPageChange(event: PageEvent): void {
    this.page = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadQuestions();
  }
}
