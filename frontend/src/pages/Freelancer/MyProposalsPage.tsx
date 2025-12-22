// frontend/src/pages/Freelancer/MyProposalsPage.tsx
import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { FileText, DollarSign, Clock, Calendar, AlertCircle } from 'lucide-react';
import { proposalApi, ProposalResponse } from '../../services/proposalApi';

export function MyProposalsPage() {
  const [proposals, setProposals] = useState<ProposalResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    loadProposals();
  }, [page]);

  const loadProposals = async () => {
    try {
      setLoading(true);
      
      const response = await proposalApi.getMyProposals(page, 20);
      console.log('📦 My Proposals Response:', response);
      
      // ✅ FIX: Extract data properly from ApiResponse wrapper
      let pageData;
      if (response.data) {
        // Response is { success: true, data: Page<Proposal> }
        pageData = response.data;
      } else if (response.content) {
        // Response is already Page<Proposal>
        pageData = response;
      } else {
        pageData = { content: [], totalPages: 0 };
      }
      
      console.log('✅ Proposals extracted:', pageData);
      console.log('📋 Proposals count:', pageData.content?.length || 0);
      
      setProposals(pageData.content || []);
      setTotalPages(pageData.totalPages || 0);
      
    } catch (error: any) {
      console.error('❌ Failed to load proposals:', error);
      console.error('Error details:', error.message);
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING': return 'text-yellow-400 bg-yellow-400/10';
      case 'ACCEPTED': 
      case 'AWARDED': return 'text-green-400 bg-green-400/10';
      case 'REJECTED': return 'text-red-400 bg-red-400/10';
      default: return 'text-gray-400 bg-gray-400/10';
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-yellow-400 border-t-transparent"></div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div>
        <h1 className="text-3xl font-bold text-white flex items-center gap-3">
          <FileText className="w-8 h-8 text-yellow-400" />
          My Proposals
        </h1>
        <p className="text-gray-400 mt-2">Track all your submitted proposals</p>
      </div>

      {/* Proposals List */}
      {proposals.length === 0 ? (
        <div className="text-center py-12 bg-white/5 backdrop-blur-sm border border-white/10 rounded-xl">
          <AlertCircle className="w-16 h-16 text-gray-600 mx-auto mb-4" />
          <p className="text-gray-400">No proposals yet. Start browsing jobs!</p>
        </div>
      ) : (
        <div className="space-y-4">
          {proposals.map((proposal) => (
            <motion.div
              key={proposal.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="relative overflow-hidden rounded-xl border border-white/10 bg-white/5 backdrop-blur-sm p-6 hover:border-yellow-400/30 transition-all"
            >
              <div className="flex items-start justify-between gap-4">
                <div className="flex-1">
                  <h3 className="text-xl font-semibold text-white mb-2">
                    {proposal.jobTitle || 'Untitled Job'}
                  </h3>

                  <div className="flex flex-wrap gap-4 text-sm text-gray-400 mb-4">
                    <div className="flex items-center gap-1">
                      <DollarSign className="w-4 h-4" />
                      <span className="text-white font-medium">{proposal.proposedAmount} USDT</span>
                    </div>
                    <div className="flex items-center gap-1">
                      <Clock className="w-4 h-4" />
                      <span>{proposal.estimatedDurationDays} days</span>
                    </div>
                    <div className="flex items-center gap-1">
                      <Calendar className="w-4 h-4" />
                      <span>{new Date(proposal.createdAt).toLocaleDateString()}</span>
                    </div>
                  </div>

                  <p className="text-gray-300 line-clamp-2 mb-4">
                    {proposal.coverLetter}
                  </p>
                </div>

                <div>
                  <span className={`px-3 py-1 rounded-full text-xs font-medium ${getStatusColor(proposal.status)}`}>
                    {proposal.status}
                  </span>
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex justify-center gap-2">
          <button
            onClick={() => setPage(p => Math.max(0, p - 1))}
            disabled={page === 0}
            className="px-4 py-2 bg-white/10 hover:bg-white/20 disabled:opacity-50 rounded-lg transition-all text-white"
          >
            Previous
          </button>
          <span className="px-4 py-2 text-white">
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => setPage(p => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            className="px-4 py-2 bg-white/10 hover:bg-white/20 disabled:opacity-50 rounded-lg transition-all text-white"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}