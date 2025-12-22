
import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Briefcase, DollarSign, Clock, Calendar, Eye, Edit, Trash2 } from 'lucide-react';
import { jobApi, Job } from '../../services/jobApi';
import { useNavigate } from 'react-router-dom';

export function MyJobsPage() {
  const navigate = useNavigate();
  const [jobs, setJobs] = useState<Job[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    loadJobs();
  }, [page]);

  const loadJobs = async () => {
    try {
      setLoading(true);
      const response = await jobApi.getMyJobs(page, 10);
      setJobs(response.content);
      setTotalPages(response.totalPages);
    } catch (error) {
      console.error('Failed to load jobs:', error);
    } finally {
      setLoading(false);
    }
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'OPEN': return 'bg-green-500/20 text-green-400';
      case 'IN_PROGRESS': return 'bg-blue-500/20 text-blue-400';
      case 'COMPLETED': return 'bg-purple-500/20 text-purple-400';
      case 'CANCELLED': return 'bg-red-500/20 text-red-400';
      default: return 'bg-gray-500/20 text-gray-400';
    }
  };

  if (loading && jobs.length === 0) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-500"></div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-white">My Posted Jobs</h1>
          <p className="text-gray-400 mt-1">Manage your job postings</p>
        </div>
        <button
          onClick={() => navigate('/employer/post-job')}
          className="px-6 py-3 bg-gradient-to-r from-blue-500 to-purple-500 rounded-lg font-medium hover:opacity-90 transition-opacity"
        >
          + Post New Job
        </button>
      </div>

      {/* Jobs List */}
      <div className="space-y-4">
        {jobs.length === 0 ? (
          <div className="ui-card text-center py-12">
            <Briefcase className="w-16 h-16 text-gray-400 mx-auto mb-4" />
            <p className="text-gray-400 text-lg mb-4">No jobs posted yet</p>
            <button
              onClick={() => navigate('/employer/post-job')}
              className="px-6 py-3 bg-gradient-to-r from-blue-500 to-purple-500 rounded-lg font-medium hover:opacity-90 transition-opacity"
            >
              Post Your First Job
            </button>
          </div>
        ) : (
          jobs.map((job) => (
            <motion.div
              key={job.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="ui-card hover:border-blue-500/50 transition-all"
            >
              <div className="flex justify-between items-start">
                <div className="flex-1">
                  <div className="flex items-start gap-4">
                    <div className="flex-1">
                      <div className="flex items-center gap-3 mb-2">
                        <h3 className="text-xl font-bold text-white">{job.title}</h3>
                        <span className={`px-3 py-1 rounded-full text-sm ${getStatusColor(job.status)}`}>
                          {job.status}
                        </span>
                      </div>
                      
                      <p className="text-gray-400 mb-4 line-clamp-2">{job.description}</p>
                      
                      <div className="flex flex-wrap gap-4 text-sm text-gray-400">
                        <div className="flex items-center gap-2">
                          <DollarSign className="w-4 h-4" />
                          <span className="text-white font-semibold">
                            {job.budget} {job.currency}
                          </span>
                        </div>
                        
                        {job.duration && (
                          <div className="flex items-center gap-2">
                            <Clock className="w-4 h-4" />
                            <span>{job.duration}</span>
                          </div>
                        )}
                        
                        {job.deadline && (
                          <div className="flex items-center gap-2">
                            <Calendar className="w-4 h-4" />
                            <span>Due: {new Date(job.deadline).toLocaleDateString()}</span>
                          </div>
                        )}

                        <div className="flex items-center gap-2">
                          <Briefcase className="w-4 h-4" />
                          <span className="text-blue-400 font-semibold">
                            {job.proposalCount} proposals
                          </span>
                        </div>
                      </div>

                      {job.skills && job.skills.length > 0 && (
                        <div className="flex flex-wrap gap-2 mt-4">
                          {job.skills.map((skill) => (
                            <span
                              key={skill}
                              className="px-3 py-1 bg-blue-500/20 text-blue-400 rounded-full text-sm"
                            >
                              {skill}
                            </span>
                          ))}
                        </div>
                      )}
                    </div>
                  </div>
                </div>

                <div className="flex flex-col gap-2 ml-4">
                  <button
                    onClick={() => navigate(`/employer/jobs/${job.id}/proposals`)}
                    className="px-4 py-2 bg-blue-500/20 text-blue-400 rounded-lg hover:bg-blue-500/30 transition-colors flex items-center gap-2"
                  >
                    <Eye className="w-4 h-4" />
                    View Proposals
                  </button>
                  
                  <button
                    onClick={() => navigate(`/employer/jobs/${job.id}`)}
                    className="px-4 py-2 bg-white/5 border border-white/10 rounded-lg hover:bg-white/10 transition-colors flex items-center gap-2"
                  >
                    <Eye className="w-4 h-4" />
                    Details
                  </button>

                  <p className="text-gray-500 text-xs text-center mt-2">
                    Posted {new Date(job.createdAt).toLocaleDateString()}
                  </p>
                </div>
              </div>
            </motion.div>
          ))
        )}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex justify-center gap-2">
          <button
            onClick={() => setPage(Math.max(0, page - 1))}
            disabled={page === 0}
            className="px-4 py-2 bg-white/5 border border-white/10 rounded-lg disabled:opacity-50"
          >
            Previous
          </button>
          <span className="px-4 py-2 text-white">
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
            disabled={page >= totalPages - 1}
            className="px-4 py-2 bg-white/5 border border-white/10 rounded-lg disabled:opacity-50"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}