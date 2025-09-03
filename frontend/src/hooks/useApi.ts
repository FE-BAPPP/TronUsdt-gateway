import { useEffect, useState } from 'react';
import { userApi } from '../services/api';
import { useAuth } from './useAuth';

// Fetch wallet summary (usdtBalance, pointsBalance)
export function useWallet() {
  const { isUser } = useAuth();
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = async () => {
    if (!isUser) { setLoading(false); return; }
    try {
      setLoading(true);
      setError(null);
      const [walletRes, limitsRes, pointsRes] = await Promise.all([
        userApi.getWallet(),
        userApi.getWithdrawalLimits(),
        userApi.getPointsBalance(),
      ]);
      if (!walletRes.success) throw new Error(walletRes.message || 'Wallet load failed');
      const result: any = walletRes.data || {};
      // Merge points balance as source of truth
      if (pointsRes.success && pointsRes.data) {
        const pb = pointsRes.data.balance ?? pointsRes.data.points ?? pointsRes.data;
        result.pointsBalance = typeof pb === 'number' ? pb : Number(pb || 0);
      }
      if (limitsRes.success) result.limits = limitsRes.data;
      setData(result);
    } catch (e: any) {
      setError(e.message || 'Network error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, [isUser]);

  return { data, loading, error, refetch: fetchData };
}

// Deposits: history + pending + address
export function useDeposits(page = 0, size = 10) {
  const { isUser } = useAuth();
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = async () => {
    if (!isUser) { setLoading(false); return; }
    try {
      setLoading(true);
      setError(null);
      const [historyRes, pendingRes, addressRes] = await Promise.all([
        userApi.getDepositHistory({ page, size, sortBy: 'createdAt', sortDir: 'desc' }),
        userApi.getPendingDeposits(),
        userApi.getDepositAddress(),
      ]);
      if (!historyRes.success) throw new Error(historyRes.message || 'Deposit history failed');
      setData({
        history: historyRes.data,
        pending: pendingRes.success ? pendingRes.data : [],
        address: addressRes.success ? addressRes.data : null,
      });
    } catch (e: any) {
      setError(e.message || 'Network error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, [isUser, page, size]);

  return { data, loading, error, refetch: fetchData };
}

// Withdrawals: history + limits
export function useWithdrawals(page = 0, size = 10) {
  const { isUser } = useAuth();
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = async () => {
    if (!isUser) { setLoading(false); return; }
    try {
      setLoading(true);
      setError(null);
      const [historyRes, limitsRes] = await Promise.all([
        userApi.getWithdrawalHistory(page, size),
        userApi.getWithdrawalLimits(),
      ]);
      if (!historyRes.success) throw new Error(historyRes.message || 'Withdrawal history failed');
      const h = historyRes.data;
      const content = Array.isArray(h) ? h
        : h?.content ?? h?.items ?? h?.records ?? h?.withdrawals ?? h?.data ?? [];
      const normalized = {
        content,
        page: h?.page ?? h?.pageNumber ?? h?.currentPage ?? 0,
        size: h?.size ?? h?.pageSize ?? content.length ?? 0,
        total: h?.total ?? h?.totalElements ?? content.length ?? 0,
      };
      setData({
        history: normalized,
        limits: limitsRes.success ? limitsRes.data : null,
      });
    } catch (e: any) {
      setError(e.message || 'Network error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, [isUser, page, size]);

  return { data, loading, error, refetch: fetchData };
}

// Transactions: all
export function useTransactions(page = 0, size = 10) {
  const { isUser } = useAuth();
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = async () => {
    if (!isUser) { setLoading(false); return; }
    try {
      setLoading(true);
      setError(null);
      const res = await userApi.getAllTransactions({ page, size, sortBy: 'createdAt', sortDir: 'desc' });
      if (!res.success) throw new Error(res.message || 'Transactions load failed');
      setData(res.data);
    } catch (e: any) {
      setError(e.message || 'Network error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, [isUser, page, size]);

  return { data, loading, error, refetch: fetchData };
}

// Withdrawal history only
export function useWithdrawalHistory(page = 0, size = 20) {
  const { isUser } = useAuth();
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = async () => {
    if (!isUser) { setLoading(false); return; }
    try {
      setLoading(true);
      setError(null);
      const res = await userApi.getWithdrawalHistory(page, size);
      if (!res.success) throw new Error(res.message || 'Withdrawal history failed');
      const h: any = res.data;
      const content = Array.isArray(h) ? h : (h?.content ?? h?.items ?? h?.records ?? h?.withdrawals ?? h?.data ?? []);
      const total = h?.total ?? h?.totalElements ?? (h?.totalElements ?? content.length ?? 0);
      const normalized = {
        content,
        page: h?.page ?? h?.pageNumber ?? h?.currentPage ?? page,
        size: h?.size ?? h?.pageSize ?? h?.pageSize ?? size,
        total,
        totalPages: h?.totalPages ?? (h?.size || h?.pageSize || size ? Math.max(1, Math.ceil(total / (h?.size || h?.pageSize || size))) : 1),
      };
      setData(normalized);
    } catch (e: any) {
      setError(e.message || 'Network error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, [isUser, page, size]);

  return { data, loading, error, refetch: fetchData };
}

// Points: balance + history
export function usePoints() {
  const { isUser } = useAuth();
  const [data, setData] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = async () => {
    if (!isUser) { setLoading(false); return; }
    try {
      setLoading(true);
      setError(null);
  const [balanceRes, historyRes] = await Promise.all([
        userApi.getPointsBalance(),
        userApi.getP2PHistory(),
      ]);
      if (!balanceRes.success) throw new Error(balanceRes.message || 'Points balance failed');
      const raw = historyRes.success ? historyRes.data : [];
      const content = Array.isArray(raw) ? raw : (raw?.content ?? raw?.items ?? raw?.records ?? raw?.data ?? []);
      const normalized = {
        content,
        page: raw?.page ?? raw?.pageNumber ?? 0,
        size: raw?.size ?? raw?.pageSize ?? content.length ?? 0,
        total: raw?.total ?? raw?.totalElements ?? content.length ?? 0,
      };
      setData({
        balance: balanceRes.data,
        history: normalized,
      });
    } catch (e: any) {
      setError(e.message || 'Network error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchData(); }, [isUser]);

  return { data, loading, error, refetch: fetchData };
}
