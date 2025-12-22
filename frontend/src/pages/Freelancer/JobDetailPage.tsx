// frontend/src/pages/Freelancer/JobDetailPage.tsx
import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { 
  ArrowLeft, DollarSign, Clock, Calendar, 
  Briefcase, Send, Users, AlertCircle 
} from 'lucide-react';
import { jobApi } from '../../services/jobApi';
import { SubmitProposalModal } from '../../components/Proposals/SubmitProposalModal';

export function JobDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  
  const [job, setJob] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showProposalModal, setShowProposalModal] = useState(false);

  useEffect(() => {
    if (id) {
      loadJobDetails();
    }
  }, [id]);

  const loadJobDetails = async () => {
    try {
      setLoading(true);
      setError(null);
      
      console.log('🔍 Loading job with ID:', id);
      
      const response = await jobApi.getJobById(id!);
      
      console.log('📦 Raw API response:', response);
      console.log('📦 Response keys:', Object.keys(response));
      
      // ✅ FIX: Extract job data properly
      let jobData;
      if (response.success && response.data) {
        // Response is ApiResponse<JobResponse>
        jobData = response.data;
      } else if (response.id) {
        // Response is already JobResponse
        jobData = response;
      } else {
        throw new Error('Invalid response structure');
      }
      
      console.log('✅ Extracted job data:', jobData);
      console.log('📋 Job fields:', {
        id: jobData.id,
        title: jobData.title,
        description: jobData.description?.substring(0, 50) + '...',
        budget: jobData.budget,
        skills: jobData.skills,
        type: jobData.type,
        employerName: jobData.employerName
      });
      
      if (!jobData || !jobData.id) {
        throw new Error('Invalid job data - missing required fields');
      }
      
      setJob(jobData);
    } catch (err: any) {
      console.error('❌ Failed to load job:', err);
      const errorMessage = err.response?.data?.message || 
                          err.message || 
                          'Failed to load job details';
      setError(errorMessage);
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

  if (error) {
    return (
      <div className="p-6">
        <button
          onClick={() => navigate('/freelancer/jobs')}
          className="flex items-center gap-2 text-gray-400 hover:text-white transition-colors mb-4"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to Jobs
        </button>
        <div className="bg-red-500/10 border border-red-500/30 rounded-lg p-6 flex items-start gap-3">
          <AlertCircle className="w-6 h-6 text-red-400 shrink-0 mt-1" />
          <div>
            <h3 className="text-red-400 font-semibold mb-1">Error Loading Job</h3>
            <p className="text-red-300 text-sm">{error}</p>
          </div>
        </div>
      </div>
    );
  }

  if (!job) {
    return (
      <div className="p-6">
        <button
          onClick={() => navigate('/freelancer/jobs')}
          className="flex items-center gap-2 text-gray-400 hover:text-white transition-colors mb-4"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to Jobs
        </button>
        <div className="text-center py-12">
          <AlertCircle className="w-16 h-16 text-gray-400 mx-auto mb-4" />
          <p className="text-gray-400">Job not found</p>
        </div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* Back button */}
      <button
        onClick={() => navigate('/freelancer/jobs')}
        className="flex items-center gap-2 text-gray-400 hover:text-white transition-colors"
      >
        <ArrowLeft className="w-4 h-4" />
        Back to Jobs
      </button>

      {/* Job Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative overflow-hidden rounded-2xl border border-white/10 bg-gradient-to-br from-white/10 via-white/5 to-transparent backdrop-blur-sm p-8"
      >
        <h1 className="text-3xl font-bold text-white mb-4">{job.title}</h1>

        <div className="flex flex-wrap gap-6 text-sm text-gray-300 mb-6">
          <div className="flex items-center gap-2">
            <DollarSign className="w-5 h-5 text-yellow-400" />
            <span className="text-white font-semibold">{job.budget} {job.currency || 'USDT'}</span>
          </div>
          <div className="flex items-center gap-2">
            <Briefcase className="w-5 h-5 text-blue-400" />
            <span>{job.type?.replace('_', ' ') || 'FIXED_PRICE'}</span>
          </div>
          {job.duration && (
            <div className="flex items-center gap-2">
              <Clock className="w-5 h-5 text-green-400" />
              <span>{job.duration}</span>
            </div>
          )}
          <div className="flex items-center gap-2">
            <Calendar className="w-5 h-5 text-purple-400" />
            <span>Posted {new Date(job.createdAt).toLocaleDateString()}</span>
          </div>
          <div className="flex items-center gap-2">
            <Users className="w-5 h-5 text-orange-400" />
            <span>{job.proposalCount || 0} Proposals</span>
          </div>
        </div>

        {/* Skills */}
        {job.skills && job.skills.length > 0 && (
          <div className="flex flex-wrap gap-2 mb-6">
            {job.skills.map((skill: string, index: number) => (
              <span
                key={index}
                className="px-3 py-1 bg-yellow-400/10 text-yellow-400 rounded-full text-sm border border-yellow-400/30"
              >
                {skill}
              </span>
            ))}
          </div>
        )}

        {/* Submit Proposal Button */}
        <button
          onClick={() => setShowProposalModal(true)}
          className="relative overflow-hidden rounded-lg px-8 py-3 transition-all"
        >
          <div className="absolute inset-0 bg-gradient-to-r from-yellow-600 via-yellow-500 to-yellow-400"></div>
          <div className="relative z-10 text-black font-medium flex items-center gap-2">
            <Send className="w-5 h-5" />
            Submit Proposal
          </div>
        </button>
      </motion.div>

      {/* Job Description */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="bg-white/5 backdrop-blur-sm border border-white/10 rounded-xl p-6"
      >
        <h2 className="text-xl font-semibold text-white mb-4">Job Description</h2>
        <p className="text-gray-300 whitespace-pre-wrap">
          {job.description || 'No description available'}
        </p>
      </motion.div>

      {/* Employer Info */}
      {job.employerName && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="bg-white/5 backdrop-blur-sm border border-white/10 rounded-xl p-6"
        >
          <h2 className="text-xl font-semibold text-white mb-4">Employer</h2>
          <p className="text-gray-300">{job.employerName}</p>
        </motion.div>
      )}

      {/* Submit Proposal Modal */}
      <SubmitProposalModal
        job={job}
        isOpen={showProposalModal}
        onClose={() => setShowProposalModal(false)}
        onSuccess={() => {
          loadJobDetails();
        }}
      />
    </div>
  );
}