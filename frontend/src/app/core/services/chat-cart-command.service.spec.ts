import { TestBed } from '@angular/core/testing';
import { ChatCartCommandService } from './chat-cart-command.service';

describe('ChatCartCommandService', () => {
  let service: ChatCartCommandService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(ChatCartCommandService);
  });

  it('parses an accented first-product add command', () => {
    expect(service.parse('Hãy thêm laptop đầu tiên vào giỏ hàng')).toEqual({ position: 1 });
  });

  it('parses numeric and unaccented positions', () => {
    expect(service.parse('them san pham so 2 vao gio')).toEqual({ position: 2 });
  });

  it('returns an add command without a position for later disambiguation', () => {
    expect(service.parse('Thêm sản phẩm này vào giỏ')).toEqual({});
  });

  it('does not treat a remove command as an add command', () => {
    expect(service.parse('Bỏ laptop đầu tiên ra khỏi giỏ hàng')).toBeNull();
  });

  it('ignores ordinary shopping questions', () => {
    expect(service.parse('Gợi ý laptop dưới 20 triệu')).toBeNull();
  });
});
