// frontend/src/components/Proposals/SubmitProposalModal.tsx
import { useState, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { X, Send, DollarSign, Clock, FileText, AlertCircle, CheckCircle } from 'lucide-react';
import { proposalApi, ProposalCreateRequest } from '../../services/proposalApi';

interface SubmitProposalModalProps {
  job: any;
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

export function SubmitProposalModal({ job, isOpen, onClose, onSuccess }: SubmitProposalModalProps) {
  const [formData, setFormData] = useState<ProposalCreateRequest>({
    jobId: '',
    coverLetter: '',
    proposedAmount: 0,
    estimatedDurationDays: 1,
  });

  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);

  // ✅ FIX: Update jobId when modal opens
  useEffect(() => {
    if (isOpen && job?.id) {
      setFormData(prev => ({
        ...prev,
        jobId: job.id // Ensure jobId is set from job object
      }));
      setError(null);
      setSuccess(false);
    }
  }, [isOpen, job]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    setSuccess(false);

    console.log('📤 Submitting proposal with data:', formData); // Debug log

    // ✅ FIX: Better validation
    if (!formData.jobId) {
      setError('Job ID is missing. Please try again.');
      return;
    }

    if (!formData.coverLetter || formData.coverLetter.trim().length < 50) {
      setError('Cover letter must be at least 50 characters');
      return;
    }

    if (!formData.proposedAmount || formData.proposedAmount < 1) {
      setError('Proposed amount must be at least 1 USDT');
      return;
    }

    if (!formData.estimatedDurationDays || formData.estimatedDurationDays < 1) {
      setError('Estimated duration must be at least 1 day');
      return;
    }

    try {
      setLoading(true);
      
      // ✅ FIX: Ensure correct data types
      const payload: ProposalCreateRequest = {
        jobId: formData.jobId,
        coverLetter: formData.coverLetter.trim(),
        proposedAmount: Number(formData.proposedAmount),
        estimatedDurationDays: Number(formData.estimatedDurationDays)
      };

      console.log('📦 Sending payload:', payload); // Debug log

      const response = await proposalApi.submitProposal(payload);
      
      console.log('✅ Proposal submitted:', response); // Debug log
      
      setSuccess(true);
      
      // Reset form after 1.5 seconds
      setTimeout(() => {
        setFormData({ 
          jobId: job.id, 
          coverLetter: '', 
          proposedAmount: 0, 
          estimatedDurationDays: 1 
        });
        
        onSuccess?.();
        onClose();
      }, 1500);
      
    } catch (err: any) {
      console.error('❌ Failed to submit proposal:', err); // Debug log
      
      const errorMessage = err.response?.data?.message || 
                          err.message || 
                          'Failed to submit proposal. Please try again.';
      setError(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  if (!isOpen) return null;

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.95 }}
          className="w-full max-w-2xl relative overflow-hidden rounded-2xl"
        >
          {/* Background */}
          <div className="absolute inset-0 bg-gradient-to-br from-white/15 via-white/10 to-transparent"></div>
          <div className="absolute inset-0 backdrop-blur-xl bg-gray-900/95 border border-white/10 rounded-2xl"></div>

          {/* Content */}
          <div className="relative z-10 p-6 max-h-[90vh] overflow-y-auto">
            {/* Header */}
            <div className="flex items-center justify-between mb-6">
              <div>
                <h2 className="text-2xl font-bold text-white flex items-center gap-2">
                  <Send className="w-6 h-6 text-yellow-400" />
                  Submit Proposal
                </h2>
                <p className="text-gray-400 text-sm mt-1">
                  Job: {job?.title || 'Untitled'}
                </p>
              </div>
              <button
                onClick={onClose}
                disabled={loading}
                className="p-2 hover:bg-white/10 rounded-lg transition-colors disabled:opacity-50"
              >
                <X className="w-5 h-5 text-gray-400" />
              </button>
            </div>

            {/* Success message */}
            {success && (
              <motion.div
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                className="mb-4 p-4 bg-green-500/10 border border-green-500/30 rounded-lg flex items-start gap-3"
              >
                <CheckCircle className="w-5 h-5 text-green-400 shrink-0 mt-0.5" />
                <div>
                  <p className="text-green-400 font-medium">Proposal submitted successfully!</p>
                  <p className="text-green-300 text-sm mt-1">The employer will review your proposal soon.</p>
                </div>
              </motion.div>
            )}

            {/* Error message */}
            {error && (
              <motion.div
                initial={{ opacity: 0, y: -10 }}
                animate={{ opacity: 1, y: 0 }}
                className="mb-4 p-4 bg-red-500/10 border border-red-500/30 rounded-lg flex items-start gap-3"
              >
                <AlertCircle className="w-5 h-5 text-red-400 shrink-0 mt-0.5" />
                <p className="text-red-400 text-sm">{error}</p>
              </motion.div>
            )}

            <form onSubmit={handleSubmit} className="space-y-6">
              {/* Cover Letter */}
              <div>
                <label className="block text-gray-300 text-sm font-medium mb-2 flex items-center gap-2">
                  <FileText className="w-4 h-4" />
                  Cover Letter *
                </label>
                <textarea
                  value={formData.coverLetter}
                  onChange={(e) => setFormData({ ...formData, coverLetter: e.target.value })}
                  className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-yellow-400/50 transition-all min-h-[200px]"
                  placeholder="Explain why you're the best fit for this job..."
                  required
                  minLength={50}
                  maxLength={2000}
                  disabled={loading || success}
                />
                <p className="text-gray-500 text-xs mt-1">
                  {formData.coverLetter.length}/2000 characters (min 50)
                </p>
              </div>

              {/* Proposed Amount */}
              <div>
                <label className="block text-gray-300 text-sm font-medium mb-2 flex items-center gap-2">
                  <DollarSign className="w-4 h-4" />
                  Your Bid Amount (USDT) *
                </label>
                <input
                  type="number"
                  value={formData.proposedAmount || ''}
                  onChange={(e) => setFormData({ 
                    ...formData, 
                    proposedAmount: parseFloat(e.target.value) || 0 
                  })}
                  className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-yellow-400/50 transition-all"
                  placeholder="Enter your bid"
                  required
                  min="1"
                  step="0.01"
                  disabled={loading || success}
                />
                {job?.budget && (
                  <p className="text-gray-500 text-xs mt-1">
                    Job budget: {job.budget} USDT
                  </p>
                )}
              </div>

              {/* Estimated Duration */}
              <div>
                <label className="block text-gray-300 text-sm font-medium mb-2 flex items-center gap-2">
                  <Clock className="w-4 h-4" />
                  Estimated Duration (Days) *
                </label>
                <input
                  type="number"
                  value={formData.estimatedDurationDays}
                  onChange={(e) => setFormData({ 
                    ...formData, 
                    estimatedDurationDays: parseInt(e.target.value) || 1 
                  })}
                  className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-yellow-400/50 transition-all"
                  placeholder="How long will it take?"
                  required
                  min="1"
                  max="365"
                  disabled={loading || success}
                />
              </div>

              {/* Action buttons */}
              <div className="flex gap-3 pt-4">
                <button
                  type="button"
                  onClick={onClose}
                  disabled={loading}
                  className="flex-1 px-6 py-3 bg-gray-700/50 hover:bg-gray-700 border border-gray-600 text-white rounded-lg transition-all disabled:opacity-50"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={loading || success}
                  className="flex-1 relative overflow-hidden rounded-lg py-3 transition-all disabled:opacity-50"
                >
                  <div className="absolute inset-0 bg-gradient-to-r from-yellow-600 via-yellow-500 to-yellow-400"></div>
                  <div className="relative z-10 text-black font-medium flex items-center justify-center gap-2">
                    {loading ? (
                      <>
                        <div className="animate-spin rounded-full h-4 w-4 border-2 border-black border-t-transparent"></div>
                        Submitting...
                      </>
                    ) : success ? (
                      <>
                        <CheckCircle className="w-4 h-4" />
                        Submitted!
                      </>
                    ) : (
                      <>
                        <Send className="w-4 h-4" />
                        Submit Proposal
                      </>
                    )}
                  </div>
                </button>
              </div>
            </form>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
}