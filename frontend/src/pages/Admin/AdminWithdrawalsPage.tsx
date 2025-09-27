import { useEffect, useMemo, useState } from 'react';
import { motion } from 'framer-motion';
import {adminApi} from '../../services/api';
import { DollarSign, RefreshCw, Shield, Clock, TrendingDown, X } from 'lucide-react';

type UIStatus = 'PENDING' | 'COMPLETED' | 'FAILED' | 'UNKNOWN';
type StatusFilter = 'ALL' | UIStatus;

const normalizeStatus = (status?: string): UIStatus => {
  const s = (status || '').toUpperCase();
  if (s === 'CONFIRMED' || s === 'COMPLETED' || s === 'SUCCESS') return 'COMPLETED';
  if (s === 'PENDING' || s === 'PROCESSING' || s === 'IN_PROGRESS' || s === 'QUEUED') return 'PENDING';
  if (s === 'FAILED' || s === 'ERROR' || s === 'REJECTED') return 'FAILED';
  return 'UNKNOWN';
};

export function AdminWithdrawalsPage() {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [rawData, setRawData] = useState<any>(null);
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
  const [search, setSearch] = useState('');
  const [selected, setSelected] = useState<any | null>(null);

  const calcStats = (items: any[]) => {
    const counts = { confirmed: 0, processing: 0, failed: 0 };
    for (const w of items) {
      const st = normalizeStatus(w.status);
      if (st === 'COMPLETED') counts.confirmed++;
      else if (st === 'PENDING') counts.processing++;
      else if (st === 'FAILED') counts.failed++;
    }
    return counts;
  };

  const load = async () => {
    setLoading(true);
    setError(null);
    try {
      const recentRes = await adminApi.getRecentWithdrawals(size);
      if (recentRes.success) {
        const items = recentRes.data?.withdrawals ?? recentRes.data ?? [];
        setRawData({
          recentWithdrawals: { items, page: 0, size },
          processingStats: calcStats(items),
          totalPages: null, 
        });
      } else {
        const res = await adminApi.getWithdrawalsManagement( page, size );
        if (!res.success) throw new Error(res.message || res.error || 'Failed');
        const items =
          res.data?.content ??
          res.data?.items ??
          res.data?.recentWithdrawals?.items ??
          [];
        setRawData({
          ...res.data,
          processingStats: calcStats(items),
        });
      }
    } catch (e: any) {
      setError(e.message || 'Failed to load withdrawals');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    load();
  }, [page, size]);

  const list: any[] = useMemo(() => {
    const items =
      rawData?.content ||
      rawData?.items ||
      rawData?.recentWithdrawals?.items ||
      [];
    return Array.isArray(items) ? items : [];
  }, [rawData]);

  const filtered = useMemo(() => {
    let arr = list;
    if (statusFilter !== 'ALL') {
      arr = arr.filter((w) => normalizeStatus(w.status) === statusFilter);
    }
    if (search.trim()) {
      const s = search.trim().toLowerCase();
      arr = arr.filter(
        (w) =>
          (w.toAddress || '').toLowerCase().includes(s) ||
          (w.username || '').toLowerCase().includes(s) ||
          String(w.userId || '').includes(s) ||
          String(w.id || '').includes(s)
      );
    }
    return arr;
  }, [list, statusFilter, search]);

  const totalPages =
    rawData?.totalPages ??
    (filtered.length > size ? Math.ceil(filtered.length / size) : 1);

  const paged = useMemo(() => {
    if (rawData?.totalPages != null) return filtered;
    const start = page * size;
    return filtered.slice(start, start + size);
  }, [filtered, page, size, rawData]);

  const badgeClass = (w: any) => {
    const ns = normalizeStatus(w.status);
    return (
      // Đã chuẩn hóa badge: px-3 py-1
      'inline-flex items-center px-3 py-1 rounded-full text-xs font-medium border ' +
      (ns === 'PENDING'
        ? 'bg-yellow-500/20 text-yellow-300 border-yellow-500/30'
        : ns === 'COMPLETED'
        ? 'bg-green-500/20 text-green-300 border-green-500/30'
        : ns === 'FAILED'
        ? 'bg-red-500/20 text-red-300 border-red-500/30'
        : 'bg-gray-500/20 text-gray-300 border-gray-500/30')
    );
  };

  const badgeLabel = (w: any) => normalizeStatus(w.status);

  return (
    <div className="p-4 sm:p-6 md:p-8"> {/* Thêm padding cho toàn bộ container */}
      <motion.div initial={{ opacity: 0, y: -20 }} animate={{ opacity: 1, y: 0 }} className="mb-8"> {/* Tách phần header ra khỏi ui-section */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="p-3 bg-yellow-500/20 rounded-xl"> {/* Tăng p-2 lên p-3 */}
              <DollarSign className="w-6 h-6 text-yellow-300" /> {/* Tăng icon size lên w-6 h-6 */}
            </div>
            <div>
              <h1 className="text-3xl font-bold text-white">Withdrawals Management</h1>
              <p className="text-gray-400">Review and monitor withdrawal requests</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <select value={size} onChange={(e) => setSize(Number(e.target.value))} className="ui-input p-2 rounded-lg bg-white/5 border border-white/10 text-white">
              {[10, 20, 50].map((n) => (
                <option key={n} value={n} className="bg-gray-800 text-white">
                  {n}/page
                </option>
              ))}
            </select>
            {/* Thêm px-4 cho nút Refresh */}
            <button onClick={load} className="ui-btn bg-blue-600/20 hover:bg-blue-600/30 border-blue-500/30 text-blue-400 px-4 py-2 rounded-lg flex items-center">
              <RefreshCw className="w-4 h-4 mr-2" />
              Refresh
            </button>
          </div>
        </div>
      </motion.div>

      <div className="ui-card rounded-xl border border-white/10 backdrop-blur-md bg-gradient-to-r from-white/3 to-white/2 p-6 mb-8"> {/* Đã chuẩn hóa ui-card */}
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-3 mb-6">
          <div className="flex flex-wrap gap-2"> {/* Thay ui-tabs bằng flex flex-wrap gap-2 để dễ kiểm soát hơn */}
            {(['ALL', 'PENDING', 'COMPLETED', 'FAILED'] as const).map((s) => (
              <button
                key={s}
                onClick={() => {
                  setPage(0);
                  setStatusFilter(s);
                }}
                className={`px-4 py-2 rounded-lg text-sm font-medium transition-colors border ${ // Đã sửa class
                  statusFilter === s
                    ? 'bg-yellow-600/20 text-yellow-300 border-yellow-500/30'
                    : 'bg-white/5 text-gray-300 hover:bg-white/10 border-transparent'
                }`}
              >
                {s}
              </button>
            ))}
          </div>
          <input
            value={search}
            onChange={(e) => {
              setPage(0);
              setSearch(e.target.value);
            }}
            placeholder="Search by id, user, or address..."
            className="ui-input w-full md:w-80 p-2 rounded-lg bg-white/5 border border-white/10 text-white" // Đã chuẩn hóa ui-input
          />
        </div>
      </div>
      
      {/* Stat Cards đã được đưa lên trên theo flow tự nhiên */}
      {rawData && (
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-8"> {/* Thay ui-grid bằng grid grid-cols-... gap-4 */}
          <div className="p-4 rounded-xl border border-white/10 bg-white/5"> {/* Đã chuẩn hóa ui-card bg-white/5 */}
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-400 text-sm">Confirmed</p>
                <p className="text-green-400 text-3xl font-bold mt-1">{rawData.processingStats?.confirmed ?? 0}</p> {/* Tăng cỡ chữ lên 3xl */}
              </div>
              <div className="p-3 bg-green-500/20 rounded-xl">
                <Shield className="w-6 h-6 text-green-300" />
              </div>
            </div>
          </div>
          <div className="p-4 rounded-xl border border-white/10 bg-white/5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-400 text-sm">Processing</p>
                <p className="text-yellow-400 text-3xl font-bold mt-1">{rawData.processingStats?.processing ?? 0}</p>
              </div>
              <div className="p-3 bg-yellow-500/20 rounded-xl">
                <Clock className="w-6 h-6 text-yellow-300" />
              </div>
            </div>
          </div>
          <div className="p-4 rounded-xl border border-white/10 bg-white/5">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-gray-400 text-sm">Failed</p>
                <p className="text-red-400 text-3xl font-bold mt-1">{rawData.processingStats?.failed ?? 0}</p>
              </div>
              <div className="p-3 bg-red-500/20 rounded-xl">
                <TrendingDown className="w-6 h-6 text-red-300" />
              </div>
            </div>
          </div>
        </div>
      )}
      
      {/* Bắt đầu bảng chính */}
      <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="ui-card rounded-xl border border-white/10 backdrop-blur-md bg-gradient-to-r from-white/3 to-white/2 p-0 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-12">
            <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-yellow-400"></div>
          </div>
        ) : error ? (
          <div className="p-6 bg-red-500/5 border border-red-500/20 rounded-xl m-4">
            <p className="text-red-400">{error}</p>
          </div>
        ) : paged.length === 0 ? (
          <div className="p-6 text-center py-12">
            <div className="text-gray-400 text-lg mb-2">No withdrawals found</div>
            {rawData?.recentWithdrawals?.message && <div className="text-gray-500 text-sm">{rawData.recentWithdrawals.message}</div>}
          </div>
        ) : (
          <div className="overflow-x-auto"> {/* Thêm overflow-x-auto để bảng cuộn trên mobile */}
            <table className="min-w-full text-sm divide-y divide-white/5"> {/* Thêm min-w-full */}
              <thead>
                <tr className="bg-white/5 text-gray-400 uppercase tracking-wider"> {/* Thêm màu nền cho header */}
                  <th className="text-left py-3 px-4">ID</th>
                  <th className="text-left py-3 px-4">User</th>
                  <th className="text-left py-3 px-4">Amount</th>
                  <th className="text-left py-3 px-4">Address</th>
                  <th className="text-left py-3 px-4">Status</th>
                  <th className="text-left py-3 px-4">Created</th>
                  <th className="text-right py-3 px-4">Actions</th> {/* Align right */}
                </tr>
              </thead>
              <tbody>
                {paged.map((w: any) => (
                  <tr key={w.id} className="border-b border-white/5 hover:bg-white/5 transition-colors">
                    <td className="py-4 px-4 text-white font-mono text-xs">{w.id}</td> {/* Tăng py lên py-4 */}
                    <td className="py-4 px-4">
                      <div className="text-white font-medium">{w.username ?? `User #${w.userId ?? 'N/A'}`}</div>
                      <div className="text-gray-400 text-xs">{w.userId ? `ID: ${w.userId}` : ''}</div>
                    </td>
                    <td className="py-4 px-4 text-yellow-300 font-semibold">{w.amount} USDT</td>
                    <td className="py-4 px-4 text-gray-300 font-mono text-xs max-w-xs truncate">{w.toAddress}</td> {/* Thêm truncate */}
                    <td className="py-4 px-4">
                      <span className={badgeClass(w)}>{badgeLabel(w)}</span>
                    </td>
                    <td className="py-4 px-4 text-gray-300 text-xs">{w.createdAt ? new Date(w.createdAt).toLocaleString() : '-'}</td>
                    <td className="py-4 px-4">
                      <div className="flex items-center gap-2 justify-end"> {/* Align right */}
                        {/* Nút View đã được chuẩn hóa padding và hover */}
                        <button
                          onClick={() => {
                            const tx = w.txHash || w.transactionHash || w.tx || w.hash;
                            if (tx) {
                              window.open(`https://nile.tronscan.org/#/transaction/${tx}`, '_blank');
                            } else {
                              setSelected(w);
                            }
                          }}
                          className="px-3 py-1 rounded text-sm font-medium bg-gray-600/20 hover:bg-gray-600/30 border border-gray-500/30 text-gray-300 transition-colors"
                        >
                          View
                        </button>
                        {/* Nút Retry đã được chuẩn hóa padding và hover */}
                        <button
                          onClick={async () => {
                            const r = await adminApi.retryWithdrawal(w.id);
                            if (r.success) {
                              alert(r.message || r.data?.message || 'Retry triggered');
                              await load();
                            } else {
                              alert('Retry failed: ' + (r.message || r.error));
                            }
                          }}
                          className="px-3 py-1 rounded text-sm font-medium bg-blue-600/20 hover:bg-blue-600/30 border border-blue-500/30 text-blue-400 transition-colors"
                        >
                          Retry
                        </button>
                        {/* Nút Load Failed đã được chuẩn hóa padding và hover */}
                        <button
                          onClick={async () => {
                            const r = await adminApi.getFailedWithdrawals(50);
                            if (r.success) {
                              const items = r.data?.withdrawals ?? r.data ?? [];
                              setRawData({
                                recentWithdrawals: { items, page: 0, size: 50 },
                                processingStats: calcStats(items),
                                totalPages: null,
                              });
                              setPage(0);
                            } else {
                              alert('Failed to load failed list: ' + (r.message || r.error));
                            }
                          }}
                          className="px-3 py-1 rounded text-sm font-medium bg-red-600/20 hover:bg-red-600/30 border border-red-500/30 text-red-400 transition-colors"
                        >
                          Load Failed
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination */}
        {!loading && !error && (
          <div className="flex items-center justify-between p-4 sm:p-6 border-t border-white/10"> {/* Đã chuẩn hóa padding và border */}
            <button
              onClick={() => setPage((p) => Math.max(0, p - 1))}
              disabled={page === 0}
              className="px-4 py-2 rounded-lg text-sm font-medium bg-gray-600/20 hover:bg-gray-600/30 border border-gray-500/30 text-gray-300 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              Previous
            </button>
            <span className="text-gray-400 text-sm">Page {page + 1} of {totalPages ?? 1}</span>
            <button
              onClick={() => setPage((p) => p + 1)}
              disabled={rawData?.totalPages != null ? page >= (rawData.totalPages - 1) : filtered.length <= (page + 1) * size}
              className="px-4 py-2 rounded-lg text-sm font-medium bg-gray-600/20 hover:bg-gray-600/30 border border-gray-500/30 text-gray-300 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
            >
              Next
            </button>
          </div>
        )}
      </motion.div>

      {/* Detail Modal */}
      {selected && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div className="absolute inset-0 bg-black/60 backdrop-blur-sm" onClick={() => setSelected(null)} />
          <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="relative p-6 rounded-xl border border-white/10 bg-gradient-to-r from-gray-900/95 to-gray-900/90 w-full max-w-2xl z-10 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-6 border-b border-white/10 pb-4"> {/* Thêm border bottom */}
              <div className="flex items-center gap-3">
                <div className="p-3 bg-yellow-500/20 rounded-xl">
                  <DollarSign className="w-6 h-6 text-yellow-300" />
                </div>
                <h3 className="text-white text-2xl font-semibold">Withdrawal #{selected.id}</h3> {/* Tăng cỡ chữ */}
              </div>
              <button onClick={() => setSelected(null)} className="p-2 rounded-full bg-gray-700/50 hover:bg-gray-600/50 text-gray-400 transition-colors"> {/* Thay đổi nút Close */}
                <X className="w-5 h-5" />
              </button>
            </div>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm mb-6"> {/* Tăng khoảng cách gap-4 */}
              <div className="p-4 rounded-lg bg-white/5 border border-white/10"> {/* Đã chuẩn hóa ui-card bg-white/5 */}
                <div className="text-gray-400 text-sm mb-1">User</div>
                <div className="text-white font-medium">{selected.username ?? `ID: ${selected.userId}`}</div>
              </div>
              <div className="p-4 rounded-lg bg-white/5 border border-white/10">
                <div className="text-gray-400 text-sm mb-1">Amount</div>
                <div className="text-yellow-300 font-semibold text-xl">{selected.amount} USDT</div> {/* Tăng cỡ chữ */}
              </div>
              <div className="md:col-span-2 p-4 rounded-lg bg-white/5 border border-white/10">
                <div className="text-gray-400 text-sm mb-1">Address</div>
                <div className="text-white font-mono break-all text-sm">{selected.toAddress}</div>
              </div>
              <div className="p-4 rounded-lg bg-white/5 border border-white/10">
                <div className="text-gray-400 text-sm mb-1">Status</div>
                <span className={badgeClass(selected)}>{badgeLabel(selected)}</span>
              </div>
              <div className="p-4 rounded-lg bg-white/5 border border-white/10">
                <div className="text-gray-400 text-sm mb-1">Created</div>
                <div className="text-white text-sm">{selected.createdAt ? new Date(selected.createdAt).toLocaleString() : '-'}</div>
              </div>
            </div>
            <div className="p-4 rounded-lg bg-white/5 border border-white/10">
              <div className="text-gray-400 mb-3 font-medium">Raw Data</div>
              <pre className="text-xs text-gray-300 overflow-x-auto bg-black/20 p-3 rounded-lg border border-white/10">
                {JSON.stringify(selected, null, 2)}
              </pre>
            </div>
          </motion.div>
        </div>
      )}
    </div>
  );
}