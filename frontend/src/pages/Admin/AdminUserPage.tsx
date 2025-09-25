import { useEffect, useState } from "react";
import { getAllUsers, updateUserStatus, getUserStats } from "../../services/adminApi";

export function AdminUsersPage() {
  const [users, setUsers] = useState<any[]>([]);
  const [page, setPage] = useState(0);
  const [size] = useState(20);
  const [search, setSearch] = useState("");
  const [loading, setLoading] = useState(false);
  const [msg, setMsg] = useState<string | null>(null);

  const load = async () => {
    setLoading(true);
    try {
      const res: any = await getAllUsers({ page, size, search });
      if (res?.success) {
        const raw = res.data?.content || res.data?.users || res.data || [];
        const normalized = (raw || []).map((u: any) => ({
          ...u,
          id: u.id?.toString ? u.id.toString() : u.id,
          username: u.username || u.userName || u.name,
          email: u.email || u.mail,
          role: (u.role || (u.isAdmin ? 'ADMIN' : 'USER') || '').toString(),
          isActive: (u.isActive !== undefined && u.isActive !== null) ? u.isActive : (u.active !== undefined ? u.active : !!u.enabled || false)
        }));
        // Hide ADMIN users on the UI (they remain in the database)
        const visible = normalized.filter((u: any) => (u.role || '').toUpperCase() !== 'ADMIN');
        setUsers(visible);
      } else {
        setMsg(res?.message || "Failed to load");
      }
    } catch (e: any) {
      setMsg(e.message || "Error");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { load(); }, [page, search]);

  const toggleActive = async (u: any) => {
    const newActive = !u.isActive;
    const res: any = await updateUserStatus(u.id, { isActive: newActive });
    if (res?.success) {
      setMsg("Updated");
      // update local state quickly
      setUsers(prev => prev.map(x => x.id === u.id ? { ...x, isActive: newActive } : x));
    } else {
      setMsg(res?.message || "Failed");
    }
    setTimeout(() => setMsg(null), 3000);
  };

  // Modal for user details/stats
  const [modalUser, setModalUser] = useState<any | null>(null);

  const showStats = async (u: any) => {
  // show loading indicator in button via attached state if needed
    try {
      const res: any = await getUserStats(u.id);
      if (res?.success) {
        const s = res.data || {};
        // attach stats to the user locally
        setUsers(prev => prev.map(x => x.id === u.id ? { ...x, _stats: s } : x));
        // open modal with the latest stats
        setModalUser({ ...u, _stats: s });
      } else {
        setModalUser(null);
      }
    } catch (err) {
      console.error('Failed to fetch stats', err);
      setModalUser(null);
    } finally {
      // noop
    }
  };

  return (
    <div className="ui-section">
      <div className="ui-card rounded-xl border border-white/10 backdrop-blur-md bg-gradient-to-r from-white/3 to-white/2 p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-2xl font-bold text-white">User Management</h2>
              <p className="text-sm text-gray-400">Manage and review user accounts</p>
            </div>
            <div className="flex items-center gap-3">
              <input
                className="ui-input max-w-sm"
                placeholder="Search username..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
              />
              <button className="ui-btn bg-violet-600/20 hover:bg-violet-600/30" onClick={() => load()}>Search</button>
            </div>
          </div>

        {msg && <div className="text-sm text-yellow-300 mb-2">{msg}</div>}

        <table className="w-full text-left text-sm divide-y divide-white/5">
          <thead>
            <tr className="text-gray-400">
              <th className="pl-4">Username</th>
              <th>Email</th>
              <th>Role</th>
              <th>Status</th>
              <th className="text-right pr-4">Actions</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr><td colSpan={5}>Loading...</td></tr>
            ) : users.length === 0 ? (
              <tr><td colSpan={5}>No users</td></tr>
            ) : users.map((u: any) => (
              <tr key={u.id} className="hover:bg-white/3 transition-colors">
                <td className="py-4 pl-4">
                  <div className="font-medium text-white">{u.username}</div>
                  <div className="text-xs text-gray-400">ID: {u.id}</div>
                </td>
                <td className="py-4 text-gray-300">{u.email}</td>
                <td className="py-4 text-sm">
                  <span className="inline-flex items-center px-2 py-1 rounded-full text-xs bg-white/5 text-gray-200">{u.role}</span>
                </td>
                <td className="py-4">
                  <span className={`inline-flex items-center px-2 py-1 rounded-full text-xs ${u.isActive ? 'bg-green-600/20 text-green-200' : 'bg-red-600/20 text-red-200'}`}>{u.isActive ? 'Active' : 'Disabled'}</span>
                </td>
                <td className="py-4 text-right pr-4">
                  <div className="flex items-center justify-end gap-3">
                    <button className={`px-3 py-1 rounded text-sm font-medium ${u.isActive ? 'bg-red-600/20 text-red-300' : 'bg-green-600/20 text-green-300'}`} onClick={() => toggleActive(u)}>
                      {u.isActive ? 'Disable' : 'Enable'}
                    </button>
                    <button className="px-3 py-1 rounded text-sm border border-white/10" onClick={() => showStats(u)}>Details</button>
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>

        <div className="mt-4 flex justify-between">
          <button className="ui-btn bg-gray-600/20" onClick={() => setPage(Math.max(0, page - 1))}>Prev</button>
          <div className="text-sm text-gray-400">Page {page + 1}</div>
          <button className="ui-btn bg-gray-600/20" onClick={() => setPage(page + 1)}>Next</button>
        </div>
      </div>
      {/* Modal for user details/stats */}
      {modalUser && (
        <div className="fixed inset-0 z-30 flex items-center justify-center">
          <div className="absolute inset-0 bg-black/50" onClick={() => setModalUser(null)} />
          <div className="relative z-40 w-full max-w-lg mx-4">
            <div className="ui-card">
              <div className="ui-card-header flex items-center justify-between">
                <h3 className="text-lg font-semibold">{modalUser.username} — Details</h3>
                <button className="ui-btn ui-btn-ghost" onClick={() => setModalUser(null)}>Close</button>
              </div>
              <div className="ui-card-body">
                <div className="mb-2 text-sm text-gray-300">Email: {modalUser.email}</div>
                <div className="grid grid-cols-2 gap-3">
                  <div className="p-3 bg-white/5 rounded">
                    <div className="text-xs text-gray-400">Deposits</div>
                    <div className="text-white font-semibold text-lg">{modalUser._stats?.depositCount ?? 0}</div>
                    <div className="text-sm text-gray-300">Total: {modalUser._stats?.totalDeposits ?? 0}</div>
                  </div>
                  <div className="p-3 bg-white/5 rounded">
                    <div className="text-xs text-gray-400">Withdrawals</div>
                    <div className="text-white font-semibold text-lg">{modalUser._stats?.withdrawalCount ?? 0}</div>
                    <div className="text-sm text-gray-300">Total: {modalUser._stats?.totalWithdrawals ?? 0}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

export default AdminUsersPage;