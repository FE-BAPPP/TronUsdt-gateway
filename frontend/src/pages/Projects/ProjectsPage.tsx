import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { projectApi } from '../../services/api'; // ✅ CORRECT IMPORT
import { useAuth } from '../../hooks/useAuth';
import {
  Briefcase,
  Calendar,
  DollarSign,
  User,
  ArrowRight,
  RefreshCw,
  FolderOpen,
} from 'lucide-react';

export function ProjectsPage() {
  const navigate = useNavigate();
  const { role } = useAuth();

  const [projects, setProjects] = useState<any[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);

  const isEmployer = role === 'EMPLOYER';

  useEffect(() => {
    loadProjects();
  }, [page, role]); // ✅ Add role as dependency

  const loadProjects = async () => {
    try {
      setLoading(true);

      console.log('🔍 Loading projects for role:', role); // Debug
      console.log('📍 isEmployer:', isEmployer); // Debug

      const response = isEmployer
        ? await projectApi.getEmployerProjects(page, 20)
        : await projectApi.getFreelancerProjects(page, 20);

      console.log('📦 Projects Response:', response); // Debug

      if (response.success) {
        const pageData = response.data;
        console.log('✅ Projects Data:', pageData); // Debug
        
        setProjects(pageData.content || []);
        setTotalPages(pageData.totalPages || 0);
      } else {
        console.error('❌ API Error:', response.message);
      }

    } catch (error: any) {
      console.error('❌ Failed to load projects:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleProjectClick = (projectId: string) => {
    navigate(`/projects/${projectId}`);
  };

  const getStatusColor = (status: string) => {
    const colors: Record<string, string> = {
      IN_PROGRESS: 'bg-blue-500/20 text-blue-300 border-blue-500/30',
      COMPLETED: 'bg-green-500/20 text-green-300 border-green-500/30',
      CANCELLED: 'bg-red-500/20 text-red-300 border-red-500/30',
      DISPUTED: 'bg-yellow-500/20 text-yellow-300 border-yellow-500/30',
    };
    return colors[status] || 'bg-gray-500/20 text-gray-300 border-gray-500/30';
  };

  if (loading && projects.length === 0) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-16 w-16 border-2 border-transparent border-t-yellow-400 border-r-yellow-400"></div>
      </div>
    );
  }

  return (
    <div className="ui-section">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="ui-card mb-6"
      >
        <div className="absolute inset-0 bg-gradient-to-r from-purple-600/20 via-purple-500/20 to-indigo-600/20"></div>
        <div className="relative z-10 ui-card-body">
          <div className="flex items-center justify-between">
            <div>
              <div className="flex items-center gap-3 mb-2">
                <Briefcase className="w-6 h-6 text-purple-400" />
                <h1 className="text-2xl font-bold text-white">
                  {isEmployer ? 'My Projects (Employer)' : 'My Projects (Freelancer)'}
                </h1>
              </div>
              <p className="text-white/60">Manage active and completed projects</p>
            </div>

            <button onClick={loadProjects} className="ui-btn ui-btn-secondary">
              <RefreshCw className="w-4 h-4" />
              Refresh
            </button>
          </div>
        </div>
      </motion.div>

      {/* Projects Grid */}
      {projects.length === 0 ? (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="ui-card"
        >
          <div className="ui-card-body text-center py-12">
            <FolderOpen className="w-16 h-16 text-gray-500 mx-auto mb-4" />
            <p className="text-gray-400 text-lg">No projects found</p>
            <p className="text-gray-500 text-sm mt-2">
              {isEmployer
                ? 'Award a proposal to start a project'
                : 'Your awarded proposals will appear here as projects'}
            </p>
          </div>
        </motion.div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {projects.map((project, index) => (
            <motion.div
              key={project.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.1 }}
              onClick={() => handleProjectClick(project.id)}
              className="ui-card cursor-pointer hover:shadow-xl hover:scale-105 transition-all duration-300"
            >
              <div className="ui-card-body">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center gap-3">
                    <div className="p-2 bg-purple-500/20 rounded-lg">
                      <Briefcase className="w-5 h-5 text-purple-400" />
                    </div>
                    <div>
                      <h3 className="text-lg font-semibold text-white">
                        Project #{project.id.substring(0, 8)}
                      </h3>
                      <p className="text-sm text-gray-400">
                        Job ID: {project.jobId?.substring(0, 8)}
                      </p>
                    </div>
                  </div>

                  <span className={`ui-badge ${getStatusColor(project.status)}`}>
                    {project.status}
                  </span>
                </div>

                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2 text-gray-400">
                      <DollarSign className="w-4 h-4" />
                      <span className="text-sm">Amount</span>
                    </div>
                    <span className="text-white font-semibold">
                      {project.agreedAmount} {project.currency}
                    </span>
                  </div>

                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2 text-gray-400">
                      <Calendar className="w-4 h-4" />
                      <span className="text-sm">Started</span>
                    </div>
                    <span className="text-white text-sm">
                      {project.startedAt ? new Date(project.startedAt).toLocaleDateString() : 'N/A'}
                    </span>
                  </div>

                  <div className="flex items-center justify-between">
                    <div className="flex items-center gap-2 text-gray-400">
                      <User className="w-4 h-4" />
                      <span className="text-sm">{isEmployer ? 'Freelancer' : 'Employer'}</span>
                    </div>
                    <span className="text-white text-sm font-mono">
                      {isEmployer
                        ? project.freelancerId?.substring(0, 8)
                        : project.employerId?.substring(0, 8)}...
                    </span>
                  </div>
                </div>

                <div className="mt-4 pt-4 border-t border-white/10 flex items-center justify-between">
                  <span className="text-sm text-gray-400">View Details</span>
                  <ArrowRight className="w-4 h-4 text-yellow-400" />
                </div>
              </div>
            </motion.div>
          ))}
        </div>
      )}

      {/* Pagination */}
      {totalPages > 1 && (
        <div className="flex items-center justify-center gap-2 mt-8">
          <button
            onClick={() => setPage(Math.max(0, page - 1))}
            disabled={page === 0}
            className="ui-btn ui-btn-secondary disabled:opacity-50"
          >
            Previous
          </button>
          
          <span className="text-white px-4">
            Page {page + 1} of {totalPages}
          </span>
          
          <button
            onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
            disabled={page >= totalPages - 1}
            className="ui-btn ui-btn-secondary disabled:opacity-50"
          >
            Next
          </button>
        </div>
      )}
    </div>
  );
}