import { useEffect, useMemo, useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { userApi } from "../../services/api";

function useQuery() {
  const { search } = useLocation();
  return useMemo(() => new URLSearchParams(search), [search]);
}

export function ResetPassword() {
  const q = useQuery();
  const navigate = useNavigate();
  const [token, setToken] = useState<string>("");
  const [newPwd, setNewPwd] = useState("");
  const [confirmPwd, setConfirmPwd] = useState("");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const t = q.get("token") || "";
    setToken(t);
  }, [q]);

  const onSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setMessage(null);
    setError(null);

    if (!token) {
      setError("Missing or invalid token.");
      return;
    }
    if (!newPwd || newPwd.length < 6) {
      setError("Password must be at least 6 characters.");
      return;
    }
    if (newPwd !== confirmPwd) {
      setError("Passwords do not match.");
      return;
    }

    setLoading(true);
    try {
      const res = await userApi.resetPassword(token, newPwd);
      if (!res.success) throw new Error(res.message || "Reset failed");
      setMessage("Password reset successful. You can sign in now.");
      setTimeout(() => navigate("/login"), 1200);
    } catch (err: any) {
      setError(err.message || "Failed to reset password. The link may have expired.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-900 flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-gray-800/60 border border-white/10 rounded-xl p-6">
        <h1 className="text-xl font-semibold text-white mb-2">Reset Password</h1>
        <p className="text-sm text-gray-300 mb-6">
          Enter a new password for your account.
        </p>

        <form onSubmit={onSubmit} className="space-y-4">
          <div>
            <label className="block text-sm text-gray-300 mb-2">New Password</label>
            <input
              type="password"
              value={newPwd}
              onChange={(e) => setNewPwd(e.target.value)}
              required
              className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-yellow-400/50"
              placeholder="Enter new password"
            />
          </div>
          <div>
            <label className="block text-sm text-gray-300 mb-2">Confirm Password</label>
            <input
              type="password"
              value={confirmPwd}
              onChange={(e) => setConfirmPwd(e.target.value)}
              required
              className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-yellow-400/50"
              placeholder="Re-enter new password"
            />
          </div>

          {message && <div className="text-sm text-yellow-300">{message}</div>}
          {error && <div className="text-sm text-red-400">{error}</div>}

          <button
            type="submit"
            disabled={loading}
            className="w-full px-4 py-3 bg-yellow-500 text-black rounded-lg disabled:opacity-50"
          >
            {loading ? "Resetting..." : "Reset password"}
          </button>
        </form>

        <div className="mt-6 text-center">
          <Link to="/login" className="text-sm text-gray-300 hover:text-white">
            Back to Sign in
          </Link>
        </div>
      </div>
    </div>
  );
}

