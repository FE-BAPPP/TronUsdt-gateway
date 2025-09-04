import { useState } from "react";
import { Link } from "react-router-dom";
import { userApi } from "../../services/api";

export function ForgotPassword() {
  const [email, setEmail] = useState("");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setMessage(null);
    setError(null);
    setLoading(true);
    try {
      const res = await userApi.forgotPassword(email.trim());
      if (!res.success) throw new Error(res.message || "Request failed");
      setMessage("If the email exists, we've sent reset instructions.");
    } catch (err: any) {
      setMessage("If the email exists, we've sent reset instructions.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-gray-900 flex items-center justify-center px-4">
      <div className="w-full max-w-md bg-gray-800/60 border border-white/10 rounded-xl p-6">
        <h1 className="text-xl font-semibold text-white mb-2">Forgot Password</h1>
        <p className="text-sm text-gray-300 mb-6">
          Enter your email and we'll send you a link to reset your password.
        </p>

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-sm text-gray-300 mb-2">Email</label>
            <input
              type="email"
              required
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-yellow-400/50"
              placeholder="you@example.com"
            />
          </div>

          {message && <div className="text-sm text-yellow-300">{message}</div>}
          {error && <div className="text-sm text-red-400">{error}</div>}

          <button
            type="submit"
            disabled={loading}
            className="w-full px-4 py-3 bg-yellow-500 text-black rounded-lg disabled:opacity-50"
          >
            {loading ? "Sending..." : "Send reset link"}
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

