import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { adminApi } from '../../services/api';
import { 
  BarChart3, 
  Wallet, 
  TrendingUp, 
  RefreshCw, 
  Zap, 
  DollarSign, 
  Target,
  AlertTriangle,
  CheckCircle,
  Activity,
  Shield,
  Search
} from 'lucide-react';

export function AdminTrackingPage() {
  const [overview, setOverview] = useState<any>(null);
  const [recentDeposits, setRecentDeposits] = useState<any[]>([]);
  const [actionMsg, setActionMsg] = useState<string | null>(null);

  const load = async () => {
    try {
      const [ovRes, depRes] = await Promise.all([
        adminApi.getDashboardOverview(),
        adminApi.getRecentDeposits(10)
      ]);
      
      if (ovRes.success) setOverview(ovRes.data);
      if (depRes.success) setRecentDeposits(depRes.data?.deposits || depRes.data || []);
    } catch (error) {
      console.error('Failed to load tracking data:', error);
    }
  };

  useEffect(() => {
    load();
  }, []);

  return (
    <div className="ui-section">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="ui-card"
      >
        <div className="absolute inset-0 bg-gradient-to-r from-purple-600/90 via-purple-500/90 to-purple-400/90"></div>
        <div className="absolute inset-0 bg-gradient-to-br from-transparent via-purple-400/20 to-purple-600/30"></div>
        <div className="relative z-10 ui-card-body text-center">
          <div className="flex items-center justify-center gap-3 mb-4">
            <div className="p-3 bg-white/20 rounded-2xl">
              <Activity className="w-8 h-8 text-white" />
            </div>
            <h1 className="text-3xl font-bold text-white">System Tracking</h1>
          </div>
          <p className="text-white/80">Monitor deposits, wallet pools and system operations</p>
        </div>
      </motion.div>

      {/* System Overview */}
      <motion.div 
        initial={{ opacity: 0, y: 20 }} 
        animate={{ opacity: 1, y: 0 }} 
        transition={{ delay: 0.1 }}
        className="ui-card"
      >
        <div className="ui-card-header">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-yellow-500/20 rounded-xl">
                <BarChart3 className="w-6 h-6 text-yellow-300" />
              </div>
              <h2 className="text-xl font-semibold text-white">System Overview</h2>
            </div>
            <div className="flex gap-2">
              <button
                onClick={async () => {
                  const res = await adminApi.resetDepositScanPosition();
                  setActionMsg(res.message || res.data?.message || 'Reset requested');
                  setTimeout(() => setActionMsg(null), 3000);
                }}
                className="ui-btn bg-purple-600/20 hover:bg-purple-600/30 border-purple-500/30 text-purple-400"
              >
                <RefreshCw className="w-4 h-4" />
                Reset Scanner (-50)
              </button>
            </div>
          </div>
        </div>

        {actionMsg && (
          <div className="ui-card-body border-b border-white/10">
            <div className="ui-card bg-yellow-500/10 border border-yellow-500/20">
              <div className="ui-card-body">
                <div className="text-yellow-300">{actionMsg}</div>
              </div>
            </div>
          </div>
        )}

        <div className="ui-card-body">
          <div className="ui-grid grid-cols-1 md:grid-cols-3 gap-6">
            <div className="ui-card bg-white/5">
              <div className="ui-card-body">
                <div className="text-gray-400 text-sm mb-2">Wallet Address</div>
                <div className="text-white text-sm font-mono break-all bg-white/5 p-3 rounded-lg border border-white/10">
                  {overview?.masterWallet?.address || '—'}
                </div>
              </div>
            </div>
            <div className="ui-card bg-white/5">
              <div className="ui-card-body text-center">
                <div className="text-gray-400 text-sm mb-1">TRX Balance</div>
                <div className={`text-2xl font-bold ${
                  overview?.masterWallet?.isLowTrxBalance ? 'text-red-400' : 'text-white'
                }`}>
                  {typeof overview?.masterWallet?.trxBalance === 'number' 
                    ? overview.masterWallet.trxBalance.toFixed(2) 
                    : '0.00'}
                </div>
                <div className="flex justify-center mt-2">
                  <Zap className="w-5 h-5 text-blue-300" />
                </div>
              </div>
            </div>
            <div className="ui-card bg-white/5">
              <div className="ui-card-body text-center">
                <div className="text-gray-400 text-sm mb-1">USDT Balance</div>
                <div className={`text-2xl font-bold ${
                  overview?.masterWallet?.isLowUsdtBalance ? 'text-red-400' : 'text-yellow-300'
                }`}>
                  {typeof overview?.masterWallet?.usdtBalance === 'number' 
                    ? overview.masterWallet.usdtBalance.toFixed(2) 
                    : '0.00'}
                </div>
                <div className="flex justify-center mt-2">
                  <DollarSign className="w-5 h-5 text-yellow-300" />
                </div>
              </div>
            </div>
          </div>
        </div>
      </motion.div>

      {/* Recent Deposits */}
      <motion.div 
        initial={{ opacity: 0, y: 20 }} 
        animate={{ opacity: 1, y: 0 }} 
        transition={{ delay: 0.2 }}
        className="ui-card"
      >
        <div className="ui-card-header">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-green-500/20 rounded-xl">
              <TrendingUp className="w-6 h-6 text-green-300" />
            </div>
            <h2 className="text-xl font-semibold text-white">Recent Deposits</h2>
          </div>
        </div>
        <div className="ui-card-body">
          {recentDeposits.length ? (
            <div className="space-y-4">
              {recentDeposits.slice(0, 10).map((d: any, idx: number) => (
                <div key={d.id || idx} className="ui-card bg-white/5 hover:bg-white/10 transition-colors">
                  <div className="ui-card-body">
                    <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
                      <div className="flex-1 min-w-0">
                        <div className="text-white font-mono text-sm break-all mb-1">
                          {d.address || d.toAddress || d.from || d.to || '—'}
                        </div>
                        <div className="text-gray-400 text-xs">
                          {d.amount ?? d.value} {d.asset ?? 'USDT'} • {new Date(d.createdAt || d.created_at || d.timestamp || Date.now()).toLocaleString()}
                        </div>
                      </div>
                      <div className="flex items-center gap-3">
                        <div className="text-yellow-300 font-semibold text-lg">
                          {d.amount ?? d.value ?? '—'}
                        </div>
                        <button 
                          onClick={() => { 
                            const tx = d.txHash || d.transactionHash || d.tx || d.hash; 
                            if (tx) { 
                              window.open(`https://nile.tronscan.org/#/transaction/${tx}`, '_blank'); 
                            } 
                          }} 
                          className="ui-btn bg-blue-600/20 hover:bg-blue-600/30 border-blue-500/30 text-blue-400"
                        >
                          <Search className="w-4 h-4" />
                          View
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-8">
              <Wallet className="w-12 h-12 text-gray-500 mx-auto mb-4" />
              <div className="text-gray-400 text-lg mb-2">No recent deposits available</div>
              <div className="text-gray-500 text-sm">Deposits will appear here when they are detected</div>
            </div>
          )}
        </div>
      </motion.div>

      {/* Sweep Actions */}
      <motion.div 
        initial={{ opacity: 0, y: 20 }} 
        animate={{ opacity: 1, y: 0 }} 
        transition={{ delay: 0.3 }}
        className="ui-card"
      >
        <div className="ui-card-header">
          <div className="flex items-center gap-3">
            <div className="p-2 bg-purple-500/20 rounded-xl">
              <Target className="w-6 h-6 text-purple-300" />
            </div>
            <h2 className="text-xl font-semibold text-white">Sweep Actions</h2>
          </div>
        </div>
        <div className="ui-card-body">
          <div className="ui-card bg-white/5 mb-6">
            <div className="ui-card-body">
              <p className="text-gray-400 mb-2">Sweep stats endpoint is disabled in this frontend; you can still trigger sweep on a specific address.</p>
            </div>
          </div>
          <ManualSweepCard onRun={async (addr) => { 
            const r = await adminApi.sweepAddress(addr); 
            setActionMsg(r.message || r.data?.message || 'Sweep address triggered'); 
            await load(); 
          }} />
        </div>
      </motion.div>

      {/* Wallet Pool & Controls */}
      <motion.div 
        initial={{ opacity: 0, y: 20 }} 
        animate={{ opacity: 1, y: 0 }} 
        transition={{ delay: 0.4 }}
        className="ui-card"
      >
        <div className="ui-card-header">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-blue-500/20 rounded-xl">
                <Wallet className="w-6 h-6 text-blue-300" />
              </div>
              <h2 className="text-xl font-semibold text-white">Wallet Pool</h2>
            </div>
            <div className="flex gap-2">
              <button 
                disabled={!overview}
                onClick={async () => { 
                  const r = await adminApi.retryFailedWithdrawals(); 
                  setActionMsg(r.message || r.data?.message || 'Triggered'); 
                }} 
                className={`ui-btn ${!overview ? 'bg-gray-600/20 border-gray-600/30 text-gray-500 cursor-not-allowed' : 'bg-blue-600/20 hover:bg-blue-600/30 border-blue-500/30 text-blue-400'}`}
              >
                <RefreshCw className="w-4 h-4" />
                Retry Failed
              </button>
            </div>
          </div>
        </div>
        <div className="ui-card-body">
          {overview?.walletPool ? (
            <div className="ui-grid-kpi">
              <div className="ui-card bg-white/5">
                <div className="ui-card-body text-center">
                  <div className="text-gray-400 text-sm mb-1">Total Wallets</div>
                  <div className="text-white text-2xl font-bold">{overview.walletPool.total ?? 0}</div>
                </div>
              </div>
              <div className="ui-card bg-white/5">
                <div className="ui-card-body text-center">
                  <div className="text-gray-400 text-sm mb-1">Free</div>
                  <div className="text-green-400 text-2xl font-bold">{overview.walletPool.free ?? 0}</div>
                </div>
              </div>
              <div className="ui-card bg-white/5">
                <div className="ui-card-body text-center">
                  <div className="text-gray-400 text-sm mb-1">Assigned</div>
                  <div className="text-yellow-400 text-2xl font-bold">{overview.walletPool.assigned ?? 0}</div>
                </div>
              </div>
              <div className="ui-card bg-white/5">
                <div className="ui-card-body text-center">
                  <div className="text-gray-400 text-sm mb-1">Active</div>
                  <div className="text-blue-400 text-2xl font-bold">{overview.walletPool.active ?? 0}</div>
                </div>
              </div>
            </div>
          ) : (
            <div className="text-center py-8">
              <Shield className="w-12 h-12 text-gray-500 mx-auto mb-4" />
              <div className="text-gray-400 text-lg">No pool stats available</div>
            </div>
          )}
        </div>
      </motion.div>
    </div>
  );
}

function ManualSweepCard({ onRun }: { onRun: (address: string) => Promise<void> }) {
  const [address, setAddress] = useState('');
  const [running, setRunning] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!address.trim() || running) return;
    
    setRunning(true);
    try {
      await onRun(address.trim());
      setAddress('');
    } finally {
      setRunning(false);
    }
  };

  return (
    <div className="ui-card bg-white/5">
      <div className="ui-card-header">
        <h3 className="text-lg font-semibold text-white">Manual Sweep Address</h3>
      </div>
      <div className="ui-card-body">
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-gray-300 mb-2">
              Wallet Address
            </label>
            <input
              type="text"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              className="ui-input"
              placeholder="Enter wallet address to sweep"
              disabled={running}
            />
          </div>
          <button
            type="submit"
            disabled={!address.trim() || running}
            className={`ui-btn w-full ${
              address.trim() && !running 
                ? 'bg-green-600/20 hover:bg-green-600/30 border-green-500/30 text-green-400'
                : 'bg-gray-600/20 border-gray-600/30 text-gray-500 cursor-not-allowed'
            }`}
          >
            {running ? (
              <>
                <RefreshCw className="w-4 h-4 animate-spin" />
                Sweeping...
              </>
            ) : (
              <>
                <Target className="w-4 h-4" />
                Sweep Address
              </>
            )}
          </button>
        </form>
      </div>
    </div>
  );
}