import { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { adminApi } from '../../services/api';
import { 
  Shield, 
  RefreshCw, 
  TrendingUp, 
  TrendingDown, 
  Clock, 
  Wallet, 
  Users, 
  DollarSign,
  AlertTriangle,
  CheckCircle,
  ArrowUpCircle,
  ArrowDownCircle,
  BarChart3,
  CreditCard,
  Settings,
  Activity
} from 'lucide-react';

interface AdminDashboardData {
  overview: any;
  withdrawals: any;
}

export function AdminDashboardPage() {
  const [activeTab, setActiveTab] = useState<'overview' | 'withdrawals'>('overview');
  const [data, setData] = useState<AdminDashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [softWarn, setSoftWarn] = useState<string | null>(null);

  const fetchAdminData = async () => {
    try {
      setLoading(true);
      setError('');

      const results = await Promise.allSettled([
        adminApi.getDashboardOverview(),
        adminApi.getWithdrawalsManagement(0, 10),
      ]);

      const [ov, wd] = results;
      const get = (s?: PromiseSettledResult<any>) => s && s.status === 'fulfilled' ? s.value : (s ? s.reason : { success: false, message: 'Skipped' });
      const overviewResponse = get(ov);
      const withdrawalsResponse = get(wd);

      const softs: string[] = [];
      if (!overviewResponse?.success) softs.push(overviewResponse?.message || 'Overview failed');
      if (!withdrawalsResponse?.success) softs.push(withdrawalsResponse?.message || 'Withdrawals failed');
      setSoftWarn(softs.length ? softs.join(' | ') : null);

      setData({
        overview: overviewResponse?.data ?? null,
        withdrawals: withdrawalsResponse?.data ?? {},
      });

    } catch (err: any) {
      console.error('Admin dashboard error:', err);
      setError(err.message || 'Failed to load admin dashboard');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAdminData();

    // Auto-refresh every 30 seconds
    const interval = setInterval(() => {
      fetchAdminData();
    }, 30000);

    return () => clearInterval(interval);
  }, []);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-96">
        <div className="flex flex-col items-center gap-4">
          <div className="animate-spin rounded-full h-16 w-16 border-2 border-transparent border-t-yellow-400 border-r-yellow-400"></div>
          <p className="text-gray-400">Loading admin dashboard...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <motion.div
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
        className="ui-card border border-red-500/20 bg-red-500/5"
      >
        <div className="ui-card-body text-center">
          <AlertTriangle className="w-12 h-12 text-red-400 mx-auto mb-4" />
          <p className="text-red-400 text-lg font-semibold mb-4">Error loading admin dashboard: {error}</p>
          <button 
            onClick={fetchAdminData}
            className="ui-btn ui-btn-primary"
          >
            <RefreshCw className="w-4 h-4" />
            Retry
          </button>
        </div>
      </motion.div>
    );
  }

  if (!data) return null;

  const tabs = [
    { id: 'overview', label: 'Overview', icon: BarChart3 },
    { id: 'withdrawals', label: 'Withdrawals', icon: ArrowUpCircle }
  ];

  return (
    <div className="ui-section">
      {/* Admin Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="ui-card"
      >
        <div className="absolute inset-0 bg-gradient-to-r from-red-600/90 via-red-500/90 to-red-400/90"></div>
        <div className="absolute inset-0 bg-gradient-to-br from-transparent via-red-400/20 to-red-600/30"></div>
        <div className="relative z-10 ui-card-body text-center">
          <div className="flex items-center justify-center gap-3 mb-4">
            <div className="p-3 bg-white/20 rounded-2xl">
              <Shield className="w-8 h-8 text-white" />
            </div>
            <h1 className="text-3xl font-bold text-white">Admin Dashboard</h1>
          </div>
          <p className="text-white/80">Monitor and manage the USDT payment system</p>
          <button
            onClick={fetchAdminData}
            className="mt-4 ui-btn bg-white/20 hover:bg-white/30 border-white/30 text-white"
          >
            <RefreshCw className="w-4 h-4" />
            Refresh
          </button>
        </div>
      </motion.div>

      {/* System Status */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="ui-card"
      >
        <div className="ui-card-body">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-green-500/20 rounded-xl">
                <Activity className="w-6 h-6 text-green-400" />
              </div>
              <div>
                <div className="text-white font-semibold">System Status</div>
                <div className="text-gray-400 text-sm">All services operational</div>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-3 h-3 bg-green-400 rounded-full animate-pulse"></div>
              <span className="text-green-400 font-medium">Online</span>
            </div>
          </div>
        </div>
      </motion.div>

      {/* Tabs */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2 }}
        className="ui-tabs"
      >
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id as any)}
            className={`ui-tab ${activeTab === tab.id ? 'ui-tab-active' : 'ui-tab-inactive'}`}
          >
            <tab.icon className="w-5 h-5" />
            <span className="hidden sm:inline">{tab.label}</span>
          </button>
        ))}
      </motion.div>

      {/* Soft Warning */}
      {softWarn && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="ui-card border border-yellow-500/20 bg-yellow-500/5"
        >
          <div className="ui-card-body">
            <div className="flex items-center gap-3">
              <AlertTriangle className="w-5 h-5 text-yellow-400" />
              <p className="text-yellow-300">{softWarn}</p>
            </div>
          </div>
        </motion.div>
      )}

      {/* Tab Content */}
      <motion.div
        key={activeTab}
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
      >
        {activeTab === 'overview' && <OverviewTab data={data.overview} />}
        {activeTab === 'withdrawals' && <WithdrawalsTab data={data.withdrawals} />}
      </motion.div>
    </div>
  );
}

function OverviewTab({ data }: { data: any }) {
  return (
    <div className="ui-section">
      {/* Master Wallet Card */}
      <div className="ui-card">
        <div className="absolute inset-0 bg-gradient-to-br from-yellow-500/20 via-yellow-400/10 to-transparent"></div>
        <div className="ui-card-header">
          <h2 className="text-xl font-semibold text-white flex items-center gap-3">
            <div className="p-2 bg-yellow-500/20 rounded-xl">
              <Wallet className="w-6 h-6 text-yellow-300" />
            </div>
            Master Wallet
          </h2>
          <span className={`ui-badge ${
            data?.masterWallet?.isLowBalance
              ? 'status-error'
              : 'status-success'
          }`}>
            {data?.masterWallet?.isLowBalance ? (
              <>
                <AlertTriangle className="w-3 h-3" />
                Low Balance
              </>
            ) : (
              <>
                <CheckCircle className="w-3 h-3" />
                Healthy
              </>
            )}
          </span>
        </div>
        <div className="ui-card-body">
          <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="text-center">
              <div className="text-gray-400 text-sm mb-1">Wallet Address</div>
              <div className="text-white text-sm font-mono break-all bg-white/5 p-3 rounded-lg border border-white/10">
                {data?.masterWallet?.address || '—'}
              </div>
            </div>
            <div className="text-center">
              <div className="text-gray-400 text-sm mb-1">TRX Balance</div>
              <div className={`text-2xl font-bold ${
                data?.masterWallet?.isLowTrxBalance ? 'text-red-400' : 'text-white'
              }`}>
                {typeof data?.masterWallet?.trxBalance === 'number' 
                  ? data.masterWallet.trxBalance.toFixed(2) 
                  : '0.00'}
              </div>
            </div>
            <div className="text-center">
              <div className="text-gray-400 text-sm mb-1">USDT Balance</div>
              <div className={`text-2xl font-bold ${
                data?.masterWallet?.isLowUsdtBalance ? 'text-red-400' : 'text-yellow-300'
              }`}>
                {typeof data?.masterWallet?.usdtBalance === 'number' 
                  ? data.masterWallet.usdtBalance.toFixed(2) 
                  : '0.00'}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* KPI Cards */}
      <div className="ui-grid-kpi">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="ui-card"
        >
          <div className="absolute inset-0 bg-gradient-to-br from-green-500/20 via-green-400/10 to-transparent"></div>
          <div className="ui-card-body">
            <div className="flex items-center gap-3 mb-3">
              <div className="p-2 bg-green-500/20 rounded-xl">
                <TrendingUp className="w-6 h-6 text-green-400" />
              </div>
              <div>
                <div className="text-green-400 text-sm font-medium">Total Deposits</div>
                <div className="text-white text-xl font-bold">
                  ${(() => {
                    const total = data?.stats?.totalDeposits ?? data?.stats?.total_deposits ?? data?.stats?.depositsTotal ?? data?.stats?.totalDepositsToday;
                    return typeof total === 'number' ? total.toFixed(2) : '0.00';
                  })()}
                </div>
              </div>
            </div>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="ui-card"
        >
          <div className="absolute inset-0 bg-gradient-to-br from-red-500/20 via-red-400/10 to-transparent"></div>
          <div className="ui-card-body">
            <div className="flex items-center gap-3 mb-3">
              <div className="p-2 bg-red-500/20 rounded-xl">
                <TrendingDown className="w-6 h-6 text-red-400" />
              </div>
              <div>
                <div className="text-red-400 text-sm font-medium">Total Withdrawals</div>
                <div className="text-white text-xl font-bold">
                  ${(() => {
                    const total = data?.stats?.totalWithdrawals ?? data?.stats?.total_withdrawals ?? data?.stats?.withdrawalsTotal ?? data?.stats?.totalWithdrawalsToday;
                    return typeof total === 'number' ? total.toFixed(2) : '0.00';
                  })()}
                </div>
              </div>
            </div>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="ui-card"
        >
          <div className="absolute inset-0 bg-gradient-to-br from-yellow-500/20 via-yellow-400/10 to-transparent"></div>
          <div className="ui-card-body">
            <div className="flex items-center gap-3 mb-3">
              <div className="p-2 bg-yellow-500/20 rounded-xl">
                <Clock className="w-6 h-6 text-yellow-400" />
              </div>
              <div>
                <div className="text-yellow-400 text-sm font-medium">Pending Withdrawals</div>
                <div className="text-white text-xl font-bold">
                  {data?.stats?.pendingWithdrawals || 0}
                </div>
              </div>
            </div>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          className="ui-card"
        >
          <div className="absolute inset-0 bg-gradient-to-br from-blue-500/20 via-blue-400/10 to-transparent"></div>
          <div className="ui-card-body">
            <div className="flex items-center gap-3 mb-3">
              <div className="p-2 bg-blue-500/20 rounded-xl">
                <Users className="w-6 h-6 text-blue-400" />
              </div>
              <div>
                <div className="text-blue-400 text-sm font-medium">Active Wallets</div>
                <div className="text-white text-xl font-bold">
                  {data?.walletPool?.assigned ?? data?.activeWallets ?? 0}
                </div>
              </div>
            </div>
          </div>
        </motion.div>
      </div>

      {/* Wallet Pool */}
      {data?.walletPool && (
        <div className="ui-card">
          <div className="ui-card-header">
            <h2 className="text-xl font-semibold text-white flex items-center gap-3">
              <div className="p-2 bg-blue-500/20 rounded-xl">
                <CreditCard className="w-6 h-6 text-blue-300" />
              </div>
              Wallet Pool Statistics
            </h2>
          </div>
          <div className="ui-card-body">
            <div className="ui-grid-kpi">
              <div className="text-center">
                <div className="text-gray-400 text-sm mb-2">Total Wallets</div>
                <div className="text-white text-2xl font-bold">{data.walletPool.total ?? 0}</div>
              </div>
              <div className="text-center">
                <div className="text-gray-400 text-sm mb-2">Free</div>
                <div className="text-green-400 text-2xl font-bold">{data.walletPool.free ?? 0}</div>
              </div>
              <div className="text-center">
                <div className="text-gray-400 text-sm mb-2">Assigned</div>
                <div className="text-yellow-400 text-2xl font-bold">{data.walletPool.assigned ?? 0}</div>
              </div>
              <div className="text-center">
                <div className="text-gray-400 text-sm mb-2">Active</div>
                <div className="text-blue-400 text-2xl font-bold">{data.walletPool.active ?? 0}</div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

function WithdrawalsTab({ data }: { data: any }) {
  const [actionMsg, setActionMsg] = useState<string>('');
  
  const runAdminAction = async (type: 'retry' | 'stop' | 'resume') => {
    let res: any;
    if (type === 'retry') {
      res = await adminApi.retryFailedWithdrawals();
    } else if (type === 'stop') {
      res = await adminApi.emergencyStopWithdrawals();
    } else {
      res = await adminApi.resumeWithdrawals();
    }
    setActionMsg(res?.message || res?.data?.message || 'Done');
    setTimeout(() => setActionMsg(''), 3000);
  };

  const processing = data?.processingStats || {};
  const queue = data?.queueStats || {};

  return (
    <div className="ui-section">
      {/* Control Panel */}
      <div className="ui-card">
        <div className="ui-card-header">
          <h2 className="text-xl font-semibold text-white flex items-center gap-3">
            <div className="p-2 bg-yellow-500/20 rounded-xl">
              <Settings className="w-6 h-6 text-yellow-300" />
            </div>
            Withdrawals Control Panel
          </h2>
        </div>
        <div className="ui-card-body">
          <div className="flex flex-wrap gap-3">
            <button 
              onClick={() => runAdminAction('retry')} 
              className="ui-btn bg-blue-600/20 hover:bg-blue-600/30 border-blue-500/30 text-blue-400"
            >
              <RefreshCw className="w-4 h-4" />
              Retry Failed
            </button>
            <button 
              onClick={() => runAdminAction('stop')} 
              className="ui-btn bg-red-600/20 hover:bg-red-600/30 border-red-500/30 text-red-400"
            >
              <AlertTriangle className="w-4 h-4" />
              Emergency Stop
            </button>
            <button 
              onClick={() => runAdminAction('resume')} 
              className="ui-btn bg-green-600/20 hover:bg-green-600/30 border-green-500/30 text-green-400"
            >
              <CheckCircle className="w-4 h-4" />
              Resume
            </button>
          </div>
          
          {actionMsg && (
            <div className="mt-4 ui-card bg-yellow-500/10 border border-yellow-500/20">
              <div className="ui-card-body">
                <div className="text-yellow-300">{actionMsg}</div>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Processing Statistics */}
      <div className="ui-card">
        <div className="ui-card-header">
          <h2 className="text-xl font-semibold text-white flex items-center gap-3">
            <div className="p-2 bg-purple-500/20 rounded-xl">
              <BarChart3 className="w-6 h-6 text-purple-300" />
            </div>
            Processing Statistics
          </h2>
        </div>
        <div className="ui-card-body">
          <div className="ui-grid-kpi">
            <div className="ui-card bg-white/5">
              <div className="ui-card-body text-center">
                <div className="text-gray-400 text-sm mb-1">Confirmed</div>
                <div className="text-green-400 font-bold text-2xl">{processing.confirmed ?? 0}</div>
              </div>
            </div>
            <div className="ui-card bg-white/5">
              <div className="ui-card-body text-center">
                <div className="text-gray-400 text-sm mb-1">Processing</div>
                <div className="text-yellow-400 font-bold text-2xl">{processing.processing ?? 0}</div>
              </div>
            </div>
            <div className="ui-card bg-white/5">
              <div className="ui-card-body text-center">
                <div className="text-gray-400 text-sm mb-1">Failed</div>
                <div className="text-red-400 font-bold text-2xl">{processing.failed ?? 0}</div>
              </div>
            </div>
            <div className="ui-card bg-white/5">
              <div className="ui-card-body text-center">
                <div className="text-gray-400 text-sm mb-1">Queue Size</div>
                <div className="text-blue-400 font-bold text-2xl">{queue.queueSize ?? 0}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Queue Status */}
      <div className="ui-card">
        <div className="ui-card-header">
          <h2 className="text-xl font-semibold text-white flex items-center gap-3">
            <div className="p-2 bg-orange-500/20 rounded-xl">
              <Clock className="w-6 h-6 text-orange-300" />
            </div>
            Queue Status
          </h2>
        </div>
        <div className="ui-card-body">
          <div className="ui-grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="text-center">
              <div className="text-gray-400 text-sm mb-1">Processing Count</div>
              <div className="text-blue-400 font-bold text-2xl">{queue.processingCount ?? 0}</div>
            </div>
            <div className="text-center">
              <div className="text-gray-400 text-sm mb-1">Delayed</div>
              <div className="text-orange-400 font-bold text-2xl">{queue.delayedSize ?? 0}</div>
            </div>
            <div className="text-center">
              <div className="text-gray-400 text-sm mb-1">Total Queue</div>
              <div className="text-white font-bold text-2xl">{queue.queueSize ?? 0}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}