import { Injectable } from '@angular/core';

export interface ChatCartCommand {
  position?: number;
}

@Injectable({ providedIn: 'root' })
export class ChatCartCommandService {
  parse(message: string): ChatCartCommand | null {
    const normalized = this.normalize(message);
    const mentionsCart = /\bgio(?: hang)?\b/.test(normalized);
    const addsItem = /\b(them|bo|dat)\b/.test(normalized);
    const removesItem = /\b(xoa|bo)\b.*\b(khoi|ra)\b/.test(normalized);

    if (!mentionsCart || !addsItem || removesItem) {
      return null;
    }

    const ordinalPatterns: Array<[number, RegExp]> = [
      [1, /\b(dau tien|thu nhat|so 1|san pham 1|laptop 1)\b/],
      [2, /\b(thu hai|so 2|san pham 2|laptop 2)\b/],
      [3, /\b(thu ba|so 3|san pham 3|laptop 3)\b/],
      [4, /\b(thu tu|so 4|san pham 4|laptop 4)\b/],
      [5, /\b(thu nam|so 5|san pham 5|laptop 5)\b/],
      [6, /\b(thu sau|so 6|san pham 6|laptop 6)\b/]
    ];
    const matched = ordinalPatterns.find(([, pattern]) => pattern.test(normalized));
    return matched ? { position: matched[0] } : {};
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
