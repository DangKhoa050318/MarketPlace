import { ChatVoucherCommandService } from './chat-voucher-command.service';

describe('ChatVoucherCommandService', () => {
  const service = new ChatVoucherCommandService();

  it('extracts an explicit voucher code', () => {
    expect(service.parse('Áp dụng voucher school10 vào giỏ hàng')).toEqual({ code: 'SCHOOL10' });
  });

  it('resolves an ordinal voucher reference', () => {
    expect(service.parse('Dùng voucher của sản phẩm thứ hai cho giỏ hàng')).toEqual({ position: 2 });
  });

  it('keeps a contextual voucher command without treating "này" as a code', () => {
    expect(service.parse('Áp dụng voucher này vào giỏ hàng')).toEqual({});
  });

  it('ignores voucher discovery questions', () => {
    expect(service.parse('Có voucher nào cho laptop không?')).toBeNull();
  });
});
