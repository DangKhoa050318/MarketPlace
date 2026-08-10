import { Injectable } from '@angular/core';

export interface ChatVoucherCommand {
  code?: string;
  position?: number;
}

@Injectable({ providedIn: 'root' })
export class ChatVoucherCommandService {
  parse(message: string): ChatVoucherCommand | null {
    const normalized = this.normalize(message);
    const applyIntent = /\b(ap dung|dung|them)\b/.test(normalized);
    const voucherIntent = /\b(voucher|coupon|ma giam gia|ma)\b/.test(normalized);
    const cartIntent = /\bgio(?: hang)?\b/.test(normalized);
    if (!applyIntent || !voucherIntent || !cartIntent) return null;

    const explicitCode = this.extractCode(message);
    if (explicitCode) return { code: explicitCode };

    const ordinalPatterns: Array<[number, RegExp]> = [
      [1, /\b(dau tien|thu nhat|so 1|san pham 1|voucher 1)\b/],
      [2, /\b(thu hai|so 2|san pham 2|voucher 2)\b/],
      [3, /\b(thu ba|so 3|san pham 3|voucher 3)\b/],
      [4, /\b(thu tu|so 4|san pham 4|voucher 4)\b/],
      [5, /\b(thu nam|so 5|san pham 5|voucher 5)\b/],
      [6, /\b(thu sau|so 6|san pham 6|voucher 6)\b/]
    ];
    const matched = ordinalPatterns.find(([, pattern]) => pattern.test(normalized));
    return matched ? { position: matched[0] } : {};
  }

  private extractCode(message: string): string | undefined {
    const match = message.match(
      /(?:voucher|coupon|mã\s+giảm\s+giá|ma\s+giam\s+gia|mã|ma)\s+(?:mã\s+|ma\s+)?["']?([a-z0-9][a-z0-9_-]{2,31})/i
    );
    if (!match) return undefined;

    const candidate = match[1];
    const ignoredWords = new Set([
      'nay', 'này', 'do', 'đó', 'dau', 'đầu', 'thu', 'thứ', 'so', 'số', 'vao', 'vào', 'cho', 'cua', 'của'
    ]);
    return ignoredWords.has(candidate.toLowerCase()) ? undefined : candidate.toUpperCase();
  }

  private normalize(value: string): string {
    return value
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .replace(/đ/g, 'd')
      .toLowerCase()
      .replace(/[^a-z0-9\s]/g, ' ')
      .replace(/\s+/g, ' ')
      .trim();
  }
}
