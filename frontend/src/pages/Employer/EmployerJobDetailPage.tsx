import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { 
  ArrowLeft, DollarSign, Clock, Calendar,
  Briefcase, Users, AlertCircle
} from 'lucide-react';
import { jobApi, Job } from '../../services/jobApi';

export function EmployerJobDetailPage() {
  const { jobId } = useParams<{ jobId: string }>();
  const navigate = useNavigate();

  const [job, setJob] = useState<Job | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (jobId) {
      loadJobDetails();
    }
  }, [jobId]);

  const loadJobDetails = async () => {
    try {
      setLoading(true);
      setError(null);

      const response = await jobApi.getJobById(jobId!);

      // Handle both { success, data } and plain Job
      const jobData: Job | null = (response as any).data || (response as any);

      if (!jobData || !jobData.id) {
        throw new Error('Invalid job data');
      }

      setJob(jobData);
    } catch (err: any) {
      console.error('Failed to load employer job detail:', err);
      setError(err.message || 'Failed to load job details');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-blue-400 border-t-transparent"></div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="p-6">
        <button
          onClick={() => navigate('/employer/my-jobs')}
          className="flex items-center gap-2 text-gray-400 hover:text-white transition-colors mb-4"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to My Jobs
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
          onClick={() => navigate('/employer/my-jobs')}
          className="flex items-center gap-2 text-gray-400 hover:text-white transition-colors mb-4"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to My Jobs
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
        className="relative overflow-hidden rounded-2xl border border-white/10 bg-gradient-to-br from-white/10 via-white/5 to-transparent backdrop-blur-sm p-8"
      >
        <h1 className="text-3xl font-bold text-white mb-4">{job.title}</h1>

        <div className="flex flex-wrap gap-6 text-sm text-gray-300 mb-6">
          <div className="flex items-center gap-2">
            <DollarSign className="w-5 h-5 text-yellow-400" />
            <span className="text-white font-semibold">{job.budget} {job.currency}</span>
          </div>
          <div className="flex items-center gap-2">
            <Briefcase className="w-5 h-5 text-blue-400" />
            <span>{job.type?.replace('_', ' ')}</span>
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
            {job.skills.map((skill) => (
              <span
                key={skill}
                className="px-3 py-1 bg-blue-500/10 text-blue-300 rounded-full text-sm border border-blue-400/30"
              >
                {skill}
              </span>
            ))}
          </div>
        )}

        {/* Quick action to view proposals */}
        <button
          onClick={() => navigate(`/employer/jobs/${job.id}/proposals`)}
          className="relative overflow-hidden rounded-lg px-8 py-3 transition-all"
        >
          <div className="absolute inset-0 bg-gradient-to-r from-blue-600 via-blue-500 to-blue-400"></div>
          <div className="relative z-10 text-white font-medium flex items-center gap-2">
            <Users className="w-5 h-5" />
            View Proposals
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
    </div>
  );
}
