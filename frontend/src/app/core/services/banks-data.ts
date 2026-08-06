export interface BankData {
  id: number;
  name: string;
  code: string;
  bin: string;
  shortName: string;
  logoBase64?: string;
  transferSupported?: number;
  lookupSupported?: number;
  swiftCode?: string | null;
}

export const VIETNAMESE_BANKS: BankData[] = [
  {
    id: 1,
    name: 'Ngân hàng TMCP Quân Đội',
    code: 'MB',
    bin: '970422',
    shortName: 'MBBank',
    transferSupported: 1,
    lookupSupported: 1
  },
  {
    id: 17,
    name: 'Ngân hàng TMCP Công thương Việt Nam',
    code: 'ICB',
    bin: '970415',
    shortName: 'VietinBank',
    transferSupported: 1,
    lookupSupported: 1,
    swiftCode: 'ICBVVNVX'
  }
];
