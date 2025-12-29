import { useState } from 'react';
import { motion } from 'framer-motion';
import { AlertCircle } from 'lucide-react';

interface RejectMilestoneModalProps {
  milestone: any;
  onClose: () => void;
  onReject: (reason: string) => Promise<void>;
}

export function RejectMilestoneModal({ milestone, onClose, onReject }: RejectMilestoneModalProps) {
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');

    if (!reason.trim() || reason.trim().length < 10) {
      setError('Please provide at least 10 characters explaining why you are rejecting');
      return;
    }

    setLoading(true);
    try {
      await onReject(reason);
    } catch (err: any) {
      setError(err.message || 'Failed to reject milestone');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4" onClick={onClose}>
      <motion.div
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        onClick={(e) => e.stopPropagation()}
        className="bg-gray-800 rounded-xl p-6 max-w-xl w-full"
      >
        <h2 className="text-2xl font-bold text-white mb-2">Reject Milestone</h2>
        <p className="text-gray-400 mb-6">Milestone: {milestone.title}</p>

        {error && (
          <div className="mb-4 p-3 bg-red-500/20 border border-red-500/50 rounded-lg text-red-400 text-sm flex items-start gap-2">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{error}</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-gray-300 text-sm font-medium mb-2">
              Rejection Reason *
            </label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-red-400/50 transition-all"
              rows={5}
              placeholder="Please explain what needs to be improved or corrected..."
              required
              disabled={loading}
            />
            <p className="text-gray-500 text-xs mt-2">
              The freelancer will see this feedback and can resubmit after making corrections.
            </p>
          </div>

          <div className="flex gap-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-6 py-3 bg-gray-700 text-white rounded-lg hover:bg-gray-600 transition-colors"
              disabled={loading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="flex-1 px-6 py-3 bg-red-600 text-white font-semibold rounded-lg hover:bg-red-700 transition-all disabled:opacity-50"
              disabled={loading}
            >
              {loading ? 'Rejecting...' : 'Reject Milestone'}
            </button>
          </div>
        </form>
      </motion.div>
    </div>
  );
}
