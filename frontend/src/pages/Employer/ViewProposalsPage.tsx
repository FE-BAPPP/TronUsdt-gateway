// frontend/src/pages/Employer/ViewProposalsPage.tsx
import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { 
  ArrowLeft, Users, DollarSign, Clock, Calendar, 
  Star, Briefcase, MessageSquare, Award, X, AlertCircle, Download, Paperclip
} from 'lucide-react';
import { proposalApi, ProposalResponse } from '../../services/proposalApi';
import { jobApi } from '../../services/jobApi';
import { projectApi, FileResponse, API_BASE_URL } from '../../services/api';

export function ViewProposalsPage() {
  const { jobId } = useParams<{ jobId: string }>();
  const navigate = useNavigate();
  
  const [job, setJob] = useState<any>(null);
  const [proposals, setProposals] = useState<ProposalResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedProposal, setSelectedProposal] = useState<ProposalResponse | null>(null);
  const [showAwardModal, setShowAwardModal] = useState(false);

  useEffect(() => {
    if (jobId) {
      loadJobAndProposals();
    }
  }, [jobId]);

  const loadJobAndProposals = async () => {
    try {
      setLoading(true);
      
      console.log('📥 Loading job and proposals for jobId:', jobId);
      
      // Load job details
      const jobResponse = await jobApi.getJobById(jobId!);
      console.log('📦 Job response:', jobResponse);
      
      // ✅ FIX: Extract job data properly
      const jobData = jobResponse.data || jobResponse;
      console.log('✅ Job extracted:', {
        id: jobData.id,
        title: jobData.title,
        budget: jobData.budget
      });
      
      if (!jobData || !jobData.id) {
        throw new Error('Invalid job data');
      }
      
      setJob(jobData);
      
      // Load proposals
      const proposalsResponse = await proposalApi.getProposalsForJob(jobId!, 0, 50);
      console.log('📦 Proposals response:', proposalsResponse);
      
      // ✅ FIX: Extract proposals data properly
      let proposalsData;
      if (proposalsResponse.data) {
        // If response has { success, data: Page<Proposal> }
        proposalsData = proposalsResponse.data;
      } else if (proposalsResponse.content) {
        // If response is already Page<Proposal>
        proposalsData = proposalsResponse;
      } else {
        proposalsData = { content: [] };
      }
      
      console.log('✅ Proposals extracted:', proposalsData);
      console.log('📋 Proposals count:', proposalsData.content?.length || 0);
      
      setProposals(proposalsData.content || []);
      
    } catch (error: any) {
      console.error('❌ Failed to load data:', error);
      console.error('❌ Error details:', {
        message: error.message,
        response: error.response,
        stack: error.stack
      });
      
      const errorMessage = error.response?.data?.message || 
                          error.message || 
                          'Failed to load data';
      alert(errorMessage);
    } finally {
      setLoading(false);
    }
  };

  const handleAwardProposal = async (proposal: ProposalResponse) => {
    setSelectedProposal(proposal);
    setShowAwardModal(true);
  };

  const confirmAward = async () => {
    if (!selectedProposal) return;
    
    try {
      setLoading(true);
      await proposalApi.awardProposal(selectedProposal.id);
      
      setShowAwardModal(false);
      setSelectedProposal(null);
      
      // Show success message
      alert('Proposal awarded successfully!');
      
      // Refresh proposals
      loadJobAndProposals();
    } catch (error: any) {
      console.error('Failed to award proposal:', error);
      alert(error.response?.data?.message || 'Failed to award proposal');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-yellow-400 border-t-transparent"></div>
      </div>
    );
  }

  if (!job) {
    return (
      <div className="p-6 text-center">
        <p className="text-gray-400">Job not found</p>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* Back button */}
      <button
        onClick={() => navigate('/employer/my-jobs')}
        className="flex items-center gap-2 text-gray-400 hover:text-white transition-colors"
      >
        <ArrowLeft className="w-4 h-4" />
        Back to My Jobs
      </button>

      {/* Job Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative overflow-hidden rounded-2xl border border-white/10 bg-gradient-to-br from-blue-500/10 via-purple-500/10 to-transparent backdrop-blur-sm p-6"
      >
        <div className="flex items-start justify-between">
          <div className="flex-1">
            <h1 className="text-2xl font-bold text-white mb-2">{job.title}</h1>
            <p className="text-gray-400 mb-4 line-clamp-2">{job.description}</p>
            
            <div className="flex flex-wrap gap-4 text-sm">
              <div className="flex items-center gap-2 text-gray-300">
                <DollarSign className="w-4 h-4 text-yellow-400" />
                <span className="font-semibold">{job.budget} USDT</span>
              </div>
              <div className="flex items-center gap-2 text-gray-300">
                <Users className="w-4 h-4 text-blue-400" />
                <span className="font-semibold">{proposals.length} Proposals</span>
              </div>
            </div>
          </div>
        </div>
      </motion.div>

      {/* Proposals List */}
      <div>
        <h2 className="text-xl font-semibold text-white mb-4 flex items-center gap-2">
          <Users className="w-6 h-6 text-blue-400" />
          Proposals Received ({proposals.length})
        </h2>

        {proposals.length === 0 ? (
          <div className="text-center py-12 bg-white/5 backdrop-blur-sm border border-white/10 rounded-xl">
            <AlertCircle className="w-16 h-16 text-gray-600 mx-auto mb-4" />
            <p className="text-gray-400">No proposals received yet</p>
          </div>
        ) : (
          <div className="space-y-4">
            {proposals.map((proposal) => (
              <ProposalCard
                key={proposal.id}
                proposal={proposal}
                onAward={() => handleAwardProposal(proposal)}
              />
            ))}
          </div>
        )}
      </div>

      {/* Award Confirmation Modal */}
      <AwardModal
        isOpen={showAwardModal}
        proposal={selectedProposal}
        job={job}
        onConfirm={confirmAward}
        onClose={() => {
          setShowAwardModal(false);
          setSelectedProposal(null);
        }}
      />
    </div>
  );
}

// Proposal Card Component
interface ProposalCardProps {
  proposal: ProposalResponse;
  onAward: () => void;
}

function ProposalCard({ proposal, onAward }: ProposalCardProps) {
  const navigate = useNavigate();
  
  const [expanded, setExpanded] = useState(false);
  const [proposalFiles, setProposalFiles] = useState<FileResponse[]>([]);
  const [loadingFiles, setLoadingFiles] = useState(false);

  useEffect(() => {
    loadProposalFiles();
  }, [proposal.id]);

  const loadProposalFiles = async () => {
    try {
      setLoadingFiles(true);
      const files = await projectApi.getFiles('PROPOSAL', proposal.id);
      setProposalFiles(files);
    } catch (err) {
      console.error('Failed to load proposal files:', err);
    } finally {
      setLoadingFiles(false);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'PENDING': return 'bg-yellow-400/10 text-yellow-400';
      case 'AWARDED': return 'bg-green-400/10 text-green-400';
      case 'REJECTED': return 'bg-red-400/10 text-red-400';
      default: return 'bg-gray-400/10 text-gray-400';
    }
  };

  return (
    <div className="relative overflow-hidden rounded-xl border border-white/10 bg-white/5 backdrop-blur-sm hover:border-blue-400/30 transition-all">
      {/* Freelancer Info */}
      <div className="flex items-start gap-4 mb-4">
        <div className="w-12 h-12 rounded-full bg-gradient-to-br from-blue-500 to-purple-500 flex items-center justify-center text-white font-bold text-lg">
          {proposal.freelancerName?.charAt(0) || 'F'}
        </div>
        
        <div className="flex-1">
          {/* 🆕 Make freelancer name clickable */}
          <button
            onClick={() => navigate(`/freelancer/profile/${proposal.freelancerId}`)}
            className="text-lg font-semibold text-white hover:text-blue-400 transition-colors text-left"
          >
            {proposal.freelancerName || 'Freelancer'}
          </button>
          
          <div className="flex items-center gap-3 mt-1">
            <div className="flex items-center gap-1">
              <Star className="w-4 h-4 text-yellow-400" />
              <span className="text-sm text-gray-300">
                {proposal.freelancerRating?.toFixed(1) || '0.0'}
              </span>
            </div>
            <span className="text-gray-500">•</span>
            <span className="text-sm text-gray-400">
              {proposal.freelancerCompletedJobs || 0} jobs completed
            </span>
          </div>
        </div>
        
        {/* Status Badge */}
        <span className={`px-2 py-1 rounded-full text-xs font-medium ${getStatusColor(proposal.status)}`}>
          {proposal.status}
        </span>
      </div>

      {/* Bid Details */}
      <div className="flex flex-wrap gap-6 text-sm mb-4">
        <div className="flex items-center gap-2">
          <DollarSign className="w-5 h-5 text-yellow-400" />
          <div>
            <p className="text-gray-400 text-xs">Bid Amount</p>
            <p className="text-white font-semibold text-lg">{proposal.proposedAmount} USDT</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Clock className="w-5 h-5 text-green-400" />
          <div>
            <p className="text-gray-400 text-xs">Delivery Time</p>
            <p className="text-white font-medium">{proposal.estimatedDurationDays} days</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <Calendar className="w-5 h-5 text-blue-400" />
          <div>
            <p className="text-gray-400 text-xs">Submitted</p>
            <p className="text-white font-medium">
              {new Date(proposal.createdAt).toLocaleDateString()}
            </p>
          </div>
        </div>
      </div>

      {/* Cover Letter Preview */}
      <div className="bg-white/5 border border-white/10 rounded-lg p-4 mb-4">
        <h4 className="text-sm font-medium text-gray-400 mb-2 flex items-center gap-2">
          <MessageSquare className="w-4 h-4" />
          Cover Letter
        </h4>
        <p className={`text-gray-300 text-sm ${!expanded ? 'line-clamp-3' : ''}`}>
          {proposal.coverLetter}
        </p>
        {proposal.coverLetter && proposal.coverLetter.length > 200 && (
          <button
            onClick={() => setExpanded(!expanded)}
            className="text-blue-400 text-sm mt-2 hover:underline"
          >
            {expanded ? 'Show less' : 'Read more'}
          </button>
        )}
      </div>

      {/* Attached Files */}
      {proposalFiles.length > 0 && (
        <div className="bg-white/5 border border-white/10 rounded-lg p-4 mb-4">
          <h4 className="text-sm font-medium text-gray-400 mb-3 flex items-center gap-2">
            <Paperclip className="w-4 h-4" />
            Portfolio & Work Samples ({proposalFiles.length})
          </h4>
          <div className="space-y-2">
            {proposalFiles.map((file) => (
              <a
                key={file.id}
                href={`${API_BASE_URL}/api/files/download/PROPOSAL/${proposal.id}/${file.fileName}`}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center justify-between p-2 bg-white/5 rounded-lg hover:bg-white/10 transition-all group"
              >
                <div className="flex items-center gap-2">
                  <Paperclip className="w-4 h-4 text-yellow-400" />
                  <div>
                    <p className="text-sm text-white group-hover:text-yellow-400 transition-colors">
                      {file.fileName}
                    </p>
                    <p className="text-xs text-gray-400">
                      {(file.fileSize / 1024).toFixed(2)} KB
                    </p>
                  </div>
                </div>
                <Download className="w-4 h-4 text-gray-400 group-hover:text-yellow-400 transition-colors" />
              </a>
            ))}
          </div>
        </div>
      )}

      {/* Action Button */}
      {proposal.status === 'PENDING' && (
        <button
          onClick={onAward}
          className="relative overflow-hidden rounded-lg px-6 py-3 transition-all w-full"
        >
          <div className="absolute inset-0 bg-gradient-to-r from-green-600 via-green-500 to-green-400"></div>
          <div className="relative z-10 text-white font-medium flex items-center justify-center gap-2 whitespace-nowrap">
            <Award className="w-5 h-5" />
            Award Project
          </div>
        </button>
      )}
    </div>
  );
}

// Award Confirmation Modal
interface AwardModalProps {
  isOpen: boolean;
  proposal: ProposalResponse | null;
  job: any;
  onConfirm: () => void;
  onClose: () => void;
}

function AwardModal({ isOpen, proposal, job, onConfirm, onClose }: AwardModalProps) {
  if (!isOpen || !proposal) return null;

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
        <motion.div
          initial={{ opacity: 0, scale: 0.95 }}
          animate={{ opacity: 1, scale: 1 }}
          exit={{ opacity: 0, scale: 0.95 }}
          className="w-full max-w-md relative overflow-hidden rounded-2xl"
        >
          {/* Background */}
          <div className="absolute inset-0 bg-gradient-to-br from-white/15 via-white/10 to-transparent"></div>
          <div className="absolute inset-0 backdrop-blur-xl bg-gray-900/95 border border-white/10 rounded-2xl"></div>

          {/* Content */}
          <div className="relative z-10 p-6">
            <div className="flex items-center justify-between mb-6">
              <h2 className="text-2xl font-bold text-white flex items-center gap-2">
                <Award className="w-6 h-6 text-green-400" />
                Award Project
              </h2>
              <button
                onClick={onClose}
                className="p-2 hover:bg-white/10 rounded-lg transition-colors"
              >
                <X className="w-5 h-5 text-gray-400" />
              </button>
            </div>

            <div className="space-y-4 mb-6">
              <div className="bg-white/5 border border-white/10 rounded-lg p-4">
                <p className="text-gray-400 text-sm mb-2">Awarding to:</p>
                <p className="text-white font-semibold text-lg">{proposal.freelancerName}</p>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div className="bg-white/5 border border-white/10 rounded-lg p-4">
                  <p className="text-gray-400 text-xs mb-1">Bid Amount</p>
                  <p className="text-white font-semibold">{proposal.proposedAmount} USDT</p>
                </div>
                <div className="bg-white/5 border border-white/10 rounded-lg p-4">
                  <p className="text-gray-400 text-xs mb-1">Duration</p>
                  <p className="text-white font-semibold">{proposal.estimatedDurationDays} days</p>
                </div>
              </div>

              <div className="bg-yellow-500/10 border border-yellow-500/30 rounded-lg p-4">
                <p className="text-yellow-400 text-sm flex items-start gap-2">
                  <AlertCircle className="w-5 h-5 shrink-0 mt-0.5" />
                  <span>
                    <strong>{proposal.proposedAmount} USDT</strong> will be locked in escrow. 
                    The freelancer can start working immediately after award.
                  </span>
                </p>
              </div>
            </div>

            <div className="flex gap-3">
              <button
                onClick={onClose}
                className="flex-1 px-6 py-3 bg-gray-700/50 hover:bg-gray-700 border border-gray-600 text-white rounded-lg transition-all"
              >
                Cancel
              </button>
              <button
                onClick={onConfirm}
                className="flex-1 relative overflow-hidden rounded-lg py-3 transition-all"
              >
                <div className="absolute inset-0 bg-gradient-to-r from-green-600 via-green-500 to-green-400"></div>
                <div className="relative z-10 text-white font-medium flex items-center justify-center gap-2">
                  <Award className="w-5 h-5" />
                  Confirm Award
                </div>
              </button>
            </div>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
}