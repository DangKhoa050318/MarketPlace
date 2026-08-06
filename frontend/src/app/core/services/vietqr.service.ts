import { Injectable } from '@angular/core';
import * as QRCode from 'qrcode';
import { VIETNAMESE_BANKS, BankData } from './banks-data';

export interface VietQrOptions {
  bankId: string;      // Bank BIN, e.g., '970415' for VietinBank
  accountId: string;   // Merchant account number, e.g., '108870123456'
  accountName?: string;
  amount: number;      // Amount in VND
  description: string; // Transfer note, e.g., 'ORD-123'
}

/**
 * CRC16 calculation for VietQR (EMVCo standard)
 */
function crc16(str: string): string {
  let crc = 0xffff;
  for (let i = 0; i < str.length; i++) {
    crc ^= str.charCodeAt(i) << 8;
    for (let j = 0; j < 8; j++) {
      if (crc & 0x8000) {
        crc = (crc << 1) ^ 0x1021;
      } else {
        crc = crc << 1;
      }
    }
  }
  crc = crc & 0xffff;
  return crc.toString(16).padStart(4, '0').toUpperCase();
}

function formatField(id: string, value: string): string {
  const length = value.length.toString().padStart(2, '0');
  return `${id}${length}${value}`;
}

@Injectable({
  providedIn: 'root'
})
export class VietQrService {
  constructor() {}

  /**
   * Get complete list of Vietnamese banks
   */
  getBanks(): BankData[] {
    return VIETNAMESE_BANKS;
  }

  /**
   * Find bank by BIN (bankId)
   */
  findBankByBin(bin: string): BankData | undefined {
    return this.getBanks().find(b => b.bin === bin);
  }

  /**
   * Generate raw VietQR string standard content (EMVCo specification)
   */
  generateQrContent(options: VietQrOptions): string {
    const guid = formatField('00', 'A000000727');
    const paymentNetwork = formatField('01', formatField('00', options.bankId) + formatField('01', options.accountId));
    const serviceCode = formatField('02', 'QRIBFTTA');
    const merchantAccountInfo = formatField('38', guid + paymentNetwork + serviceCode);

    let payload = formatField('00', '01') + formatField('01', '12') + merchantAccountInfo + formatField('53', '704');
    if (options.amount) {
      payload += formatField('54', options.amount.toString());
    }
    payload += formatField('58', 'VN');
    if (options.description) {
      payload += formatField('62', formatField('08', options.description));
    }
    payload += '6304';
    const checksum = crc16(payload);
    return payload + checksum;
  }

  /**
   * Render raw QR string directly to Data URL (image/png)
   */
  async renderRawQrDataUrl(rawPayload: string): Promise<string> {
    try {
      return await QRCode.toDataURL(rawPayload, {
        errorCorrectionLevel: 'M',
        margin: 2,
        width: 320,
        color: {
          dark: '#0f172a',
          light: '#ffffff'
        }
      });
    } catch (err) {
      console.error('Failed to render raw VietQR QRCode image:', err);
      throw err;
    }
  }

  /**
   * Generate Data URL (image/png) for VietQR code string
   */
  async generateQrDataUrl(options: VietQrOptions): Promise<string> {
    const rawContent = this.generateQrContent(options);
    return this.renderRawQrDataUrl(rawContent);
  }
}
