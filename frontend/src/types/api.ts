// ====== COMMON TYPES ======

export type Role = 'USER' | 'ADMIN';

export interface ApiResponse<T> {
  success: boolean;
  message?: string;
  data?: T;
  error?: string;
}

export interface AuthResponse {
  token: string;
  user: UserProfile;
}

export interface User {
  id: string;
  username: string;
  email: string;
  fullName: string;
  role: Role;
  createdAt: string;
}

export interface UserProfile extends User {
  walletAddress?: string;
  balance?: {
    usdt: string;
    points: number;
  };
}

// ====== WALLET / DEPOSIT ======

export interface WalletInfo {
  address: string;
  usdtBalance: string;
  trxBalance: string;
  pointsBalance: number;
  lastUpdated: string;
}

export interface DepositInfo {
  address: string;
  qrCode: string;
  network: string;
  tokenContract: string;
}

// ====== TRANSACTIONS ======

export interface TransactionHistory {
  transactions: Transaction[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
}

export type TransactionType = 'DEPOSIT' | 'WITHDRAWAL' | 'SWEEP' | 'TRANSFER';
export type TransactionStatus = 'PENDING' | 'CONFIRMED' | 'FAILED';

export interface Transaction {
  id: string;
  txHash: string;
  fromAddress: string;
  toAddress: string;
  amount: string;
  transactionType: TransactionType;
  status: TransactionStatus;
  createdAt: string;
  confirmedAt?: string;
  blockNumber?: number;
}

export interface TransactionSummary {
  totalCount: number;
  totalVolume: string;
  startDate: string;
  endDate: string;
}

// ====== WITHDRAWALS ======

export interface WithdrawalRequest {
  toAddress: string;
  amount: string;
  note?: string;
}

export interface Withdrawal {
  id: string;
  amount: string;
  toAddress: string;
  status: TransactionStatus;
  createdAt: string;
  confirmedAt?: string;
  txHash?: string;
}

// ====== DASHBOARD (ADMIN) ======

export interface DashboardData {
  masterWallet: {
    address: string;
    trxBalance: string;
    usdtBalance: string;
    isLowTrxBalance: boolean;
    isLowUsdtBalance: boolean;
  };
  walletPool: {
    total: number;
    free: number;
    assigned: number;
    active: number;
    utilizationRate: number;
  };
  withdrawals: WithdrawalStats;
  withdrawalQueue: QueueStats;
  depositScanner: ScannerStats;
  systemHealth: SystemHealth;
  timestamp: string;
}

export interface WithdrawalStats {
  totalProcessed: number;
  pendingCount: number;
  failedCount: number;
  avgProcessingTime: number;
  totalVolume: string;
}

export interface QueueStats {
  queueSize: number;
  processingRate: number;
  averageWaitTime: number;
}

export interface ScannerStats {
  lastScannedBlock: number;
  totalDepositsDetected: number;
  scanningRate: number;
  isScanning: boolean;
}

export interface SystemHealth {
  status: 'HEALTHY' | 'WARNING' | 'ERROR';
  services: {
    database: 'UP' | 'DOWN';
    blockchain: 'UP' | 'DOWN';
    redis: 'UP' | 'DOWN';
    sweepService: 'UP' | 'DOWN';
  };
  alerts: Alert[];
}

export interface Alert {
  level: 'INFO' | 'WARNING' | 'ERROR';
  message: string;
  timestamp: string;
}

// ====== SYSTEM ======

export interface MasterWalletInfo {
  address: string;
  trxBalance: string;
  createdAt: string;
  isLowBalance: boolean;
}

export interface PoolStats {
  total: number;
  free: number;
  assigned: number;
  active: number;
}

// ====== TOKEN SWEEP ======

export interface TokenSweepData {
  totalCount: number;
  showingCount: number;
  statusFilter: string;
  sweeps: TokenSweep[];
  timestamp: string;
}

export interface TokenSweep {
  id: number;
  childIndex: number;
  fromAddress: string;
  toAddress: string;
  amount: string;
  txHash: string;
  status: TransactionStatus;
  createdAt: string;
  confirmedAt?: string;
}

// ====== GAS TOP-UP ======

export interface GasTopupData {
  totalCount: number;
  showingCount: number;
  statusFilter: string;
  topups: GasTopup[];
  timestamp: string;
}

export type GasTopupStatus = 'PENDING' | 'SENT' | 'CONFIRMED' | 'FAILED';

export interface GasTopup {
  id: number;
  childIndex: number;
  toAddress: string;
  amount: string;
  txHash: string;
  status: GasTopupStatus;
  createdAt: string;
  confirmedAt?: string;
}

// ====== REQUEST TYPES ======

export interface RegisterRequest {
  username: string;
  password: string;
  email: string;
  fullName: string;
}

export type CreateAdminRequest = RegisterRequest;

export interface TransferPointsRequest {
  toUsername: string;
  amount: number;
  note?: string;
}

// ====== POINTS ======

export interface PointsBalance {
  balance: number;
  lastUpdated: string;
}

export interface PointsHistory {
  transactions: PointsTransaction[];
  totalElements: number;
  totalPages: number;
  currentPage: number;
}

export type PointsTransactionType = 'TRANSFER' | 'BONUS' | 'DEDUCTION';

export interface PointsTransaction {
  id: string;
  fromUser: string;
  toUser: string;
  amount: number;
  type: PointsTransactionType;
  note?: string;
  createdAt: string;
}
