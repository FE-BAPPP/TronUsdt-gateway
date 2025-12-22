import React, { useState, useEffect } from 'react';
import { motion } from 'framer-motion';
import { Search, Briefcase, Clock, DollarSign, Calendar } from 'lucide-react';
import { jobApi, Job } from '../../services/jobApi';
import { useNavigate } from 'react-router-dom';

export function BrowseJobsPage() {
  const navigate = useNavigate();
  const [jobs, setJobs] = useState<Job[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [searchKeyword, setSearchKeyword] = useState('');

  useEffect(() => {
    loadJobs();
  }, [page]);

  const loadJobs = async () => {
    try {
      setLoading(true);
      const response = searchKeyword 
        ? await jobApi.searchJobs(searchKeyword, page, 10)
        : await jobApi.browseJobs(page, 10);
      
      console.log('📋 Jobs loaded:', response); // Debug log
      
      setJobs(response.content);
      setTotalPages(response.totalPages);
    } catch (error) {
      console.error('❌ Failed to load jobs:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSearch = () => {
    setPage(0);
    loadJobs();
  };

  const handleJobClick = (job: Job) => {
    console.log('🖱️ Job clicked:', job); // Debug log
    console.log('📍 Navigating to:', `/freelancer/jobs/${job.id}`); // Debug log
    navigate(`/freelancer/jobs/${job.id}`);
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
          <h1 className="text-3xl font-bold text-white">Browse Jobs</h1>
          <p className="text-gray-400 mt-1">Find your next opportunity</p>
        </div>
      </div>

      {/* Search Bar */}
      <div className="ui-card">
        <div className="flex gap-4">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-3.5 w-5 h-5 text-gray-400" />
            <input
              type="text"
              placeholder="Search jobs by title or keywords..."
              value={searchKeyword}
              onChange={(e) => setSearchKeyword(e.target.value)}
              onKeyPress={(e) => e.key === 'Enter' && handleSearch()}
              className="w-full pl-10 pr-4 py-3 bg-white/5 border border-white/10 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-blue-500"
            />
          </div>
          <button
            onClick={handleSearch}
            className="px-6 py-3 bg-gradient-to-r from-blue-500 to-purple-500 rounded-lg font-medium hover:opacity-90 transition-opacity"
          >
            Search
          </button>
        </div>
      </div>

      {/* Jobs List */}
      <div className="space-y-4">
        {jobs.length === 0 ? (
          <div className="ui-card text-center py-12">
            <Briefcase className="w-16 h-16 text-gray-400 mx-auto mb-4" />
            <p className="text-gray-400 text-lg">No jobs found</p>
          </div>
        ) : (
          jobs.map((job) => (
            <motion.div
              key={job.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="ui-card hover:border-blue-500/50 transition-all cursor-pointer"
              onClick={() => handleJobClick(job)}
            >
              <div className="flex justify-between items-start">
                <div className="flex-1">
                  <h3 className="text-xl font-bold text-white mb-2">{job.title}</h3>
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

                <div className="text-right ml-4">
                  <span className="inline-block px-3 py-1 bg-green-500/20 text-green-400 rounded-full text-sm mb-2">
                    {job.type.replace('_', ' ')}
                  </span>
                  <p className="text-gray-400 text-sm mt-2">
                    {job.proposalCount || 0} proposals
                  </p>
                  <p className="text-gray-500 text-xs mt-1">
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