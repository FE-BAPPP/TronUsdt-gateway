import { useState, useEffect } from 'react';
import { userApi } from '../services/api';
import { useAuth } from './useAuth';

export function useWallet() {
  const { user } = useAuth();
  const [wallet, setWallet] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchWallet = async () => {
    if (!user) {
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);
  const response = await userApi.getWallet();
      
      if (response.success) {
        setWallet(response.data);
      } else {
        setError(response.message || 'Failed to fetch wallet data');
      }
    } catch (err: any) {
      setError(err.message || 'Network error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchWallet();
  }, [user]);

  const refetch = () => {
    fetchWallet();
  };

  return { wallet, loading, error, refetch };
}

// Hook for deposit address
export function useDepositAddress() {
  const { user } = useAuth();
  const [depositInfo, setDepositInfo] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchDepositAddress = async () => {
    if (!user) {
      setLoading(false);
      return;
    }

    try {
      setLoading(true);
      setError(null);
  const response = await userApi.getDepositAddress();
      
      if (response.success) {
        setDepositInfo(response.data);
      } else {
        setError(response.message || 'Failed to fetch deposit address');
      }
    } catch (err: any) {
      setError(err.message || 'Network error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchDepositAddress();
  }, [user]);

  return { depositInfo, loading, error, refetch: fetchDepositAddress };
}

// Hook for creating withdrawal
export function useWithdrawal() {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const createWithdrawal = async (data: { toAddress: string; amount: string; note?: string }) => {
    try {
      setLoading(true);
      setError(null);
      
  const response = await userApi.createWithdrawal({ amount: parseFloat(data.amount), toAddress: data.toAddress });
      
      if (response.success) {
        return response.data;
      } else {
        throw new Error(response.message || 'Withdrawal failed');
      }
    } catch (err: any) {
      setError(err.message);
      throw err;
    } finally {
      setLoading(false);
    }
  };

  return { createWithdrawal, loading, error };
}