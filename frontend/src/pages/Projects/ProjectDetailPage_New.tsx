import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { 
  ArrowLeft, User, CheckCircle, MessageSquare, FileText, Settings
} from 'lucide-react';
import { projectApi } from '../../services/api';
import { useAuth } from '../../hooks/useAuth';
import { ChatButton } from '../../components/Chat/ChatButton';
import { MilestoneCard } from '../../components/Milestones/MilestoneCard';
import { SubmitWorkModal } from '../../components/Milestones/SubmitWorkModal';
import { RejectMilestoneModal } from '../../components/Milestones/RejectMilestoneModal';
import { useMilestoneActions } from '../../hooks/useMilestoneActions';

export function ProjectDetailPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  const { role } = useAuth();

  const [project, setProject] = useState<any>(null);
  const [milestones, setMilestones] = useState<any[]>([]);
  const [stats, setStats] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  const isEmployer = role === 'EMPLOYER';
  const isFreelancer = role === 'FREELANCER';

  // Use custom hook for milestone actions
  const milestoneActions = useMilestoneActions(loadProjectDetails);

  useEffect(() => {
    if (projectId) {
      loadProjectDetails();
    }
  }, [projectId]);

  async function loadProjectDetails() {
    try {
      setLoading(true);

      // Load project
      const projectRes = await projectApi.getProjectById(projectId!);
      const projectData = projectRes.data || projectRes;
      setProject(projectData);

      // Load milestones
      const milestonesRes = await projectApi.getProjectMilestones(projectId!);
      setMilestones(milestonesRes.data || milestonesRes || []);

      // Calculate stats
      const totalMilestones = milestonesRes.data?.length || 0;
      const completedMilestones = milestonesRes.data?.filter((m: any) => 
        m.status === 'RELEASED' || m.status === 'COMPLETED'
      ).length || 0;

      setStats({
        totalMilestones,
        completedMilestones,
        progress: totalMilestones > 0 ? (completedMilestones / totalMilestones) * 100 : 0,
      });

    } catch (error) {
      console.error('Failed to load project details:', error);
    } finally {
      setLoading(false);
    }
  }

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED': return 'bg-green-500/20 text-green-400';
      case 'IN_PROGRESS': return 'bg-blue-500/20 text-blue-400';
      case 'CANCELLED': return 'bg-red-500/20 text-red-400';
      case 'DISPUTED': return 'bg-yellow-500/20 text-yellow-400';
      default: return 'bg-gray-500/20 text-gray-400';
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-yellow-400 border-t-transparent"></div>
      </div>
    );
  }

  if (!project) {
    return (
      <div className="p-6 text-center">
        <p className="text-gray-400">Project not found</p>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6">
      {/* Back Button */}
      <button
        onClick={() => navigate(isEmployer ? '/employer/my-projects' : '/freelancer/my-projects')}
        className="flex items-center gap-2 text-gray-400 hover:text-white transition-colors"
      >
        <ArrowLeft className="w-4 h-4" />
        Back to Projects
      </button>

      {/* Project Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative overflow-hidden rounded-2xl border border-white/10 bg-gradient-to-br from-purple-500/10 via-blue-500/10 to-transparent backdrop-blur-sm p-6"
      >
        <div className="flex items-start justify-between mb-4">
          <div className="flex-1">
            <h1 className="text-2xl font-bold text-white mb-2">
              {project.jobTitle || 'Project'}
            </h1>
            <div className="flex items-center gap-3">
              <span className={`px-3 py-1 rounded-full text-sm font-medium ${getStatusColor(project.status)}`}>
                {project.status}
              </span>
              <span className="text-gray-400 text-sm">
                Started: {new Date(project.startedAt).toLocaleDateString()}
              </span>
            </div>
          </div>

          <div className="text-right">
            <p className="text-gray-400 text-sm">Total Budget</p>
            <p className="text-3xl font-bold text-yellow-400">
              {project.agreedAmount} <span className="text-lg">{project.currency}</span>
            </p>
          </div>
        </div>

        {/* Participants */}
        <div className="grid grid-cols-2 gap-4 mt-6 pt-6 border-t border-white/10">
          <div>
            <div className="flex items-center gap-3 mb-2">
              <User className="w-5 h-5 text-blue-400" />
              <span className="text-gray-400">Employer</span>
            </div>
            <div className="text-lg font-semibold text-white">
              {project.employerName || `ID: ${project.employerId?.substring(0, 8)}...`}
            </div>
          </div>

          <div>
            <div className="flex items-center gap-3 mb-2">
              <User className="w-5 h-5 text-purple-400" />
              <span className="text-gray-400">Freelancer</span>
            </div>
            
            <button
              onClick={() => navigate(`/freelancer/profile/${project.freelancerId}`)}
              className="text-lg font-semibold text-white hover:text-purple-400 transition-colors text-left"
            >
              {project.freelancerName || `ID: ${project.freelancerId?.substring(0, 8)}...`}
            </button>
          </div>
        </div>

        {/* Action Buttons Row */}
        <div className="flex gap-3 mt-6">
          {/* Chat Button */}
          {project?.id && (
            <ChatButton
              projectId={project.id}
              projectTitle={project.jobTitle}
              className="flex-1"
            />
          )}

          {/* Manage Milestones (Employer only) */}
          {isEmployer && project?.status === 'IN_PROGRESS' && (
            <button
              onClick={() => navigate(`/projects/${projectId}/milestones`)}
              className="flex-1 flex items-center justify-center gap-2 px-6 py-3 bg-gradient-to-r from-blue-600 to-blue-500 text-white font-semibold rounded-lg hover:shadow-lg hover:shadow-blue-500/50 transition-all"
            >
              <Settings className="w-5 h-5" />
              Manage Milestones
            </button>
          )}
        </div>
      </motion.div>

      {/* Progress Stats */}
      {stats && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="ui-card"
        >
          <div className="ui-card-header">
            <h2 className="text-xl font-bold text-white">Project Progress</h2>
          </div>
          <div className="ui-card-body">
            <div className="mb-4">
              <div className="flex justify-between text-sm mb-2">
                <span className="text-gray-400">Completion</span>
                <span className="text-white font-semibold">{stats.progress.toFixed(0)}%</span>
              </div>
              <div className="w-full h-2 bg-gray-700 rounded-full overflow-hidden">
                <div 
                  className="h-full bg-gradient-to-r from-green-500 to-green-400 transition-all"
                  style={{ width: `${stats.progress}%` }}
                />
              </div>
            </div>

            <div className="grid grid-cols-3 gap-4 text-center">
              <div className="bg-white/5 rounded-lg p-4">
                <p className="text-2xl font-bold text-yellow-400">{stats.totalMilestones}</p>
                <p className="text-gray-400 text-sm">Total Milestones</p>
              </div>
              <div className="bg-white/5 rounded-lg p-4">
                <p className="text-2xl font-bold text-green-400">{stats.completedMilestones}</p>
                <p className="text-gray-400 text-sm">Completed</p>
              </div>
              <div className="bg-white/5 rounded-lg p-4">
                <p className="text-2xl font-bold text-blue-400">
                  {stats.totalMilestones - stats.completedMilestones}
                </p>
                <p className="text-gray-400 text-sm">Remaining</p>
              </div>
            </div>
          </div>
        </motion.div>
      )}

      {/* Milestones */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2 }}
        className="ui-card"
      >
        <div className="ui-card-header">
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <CheckCircle className="w-6 h-6 text-green-400" />
            Milestones
          </h2>
        </div>
        <div className="ui-card-body">
          {milestones.length === 0 ? (
            <div className="text-center py-12 text-gray-400">
              <FileText className="w-16 h-16 mx-auto mb-4 opacity-30" />
              <p>No milestones yet</p>
            </div>
          ) : (
            <div className="space-y-4">
              {milestones.map((milestone, index) => (
                <MilestoneCard
                  key={milestone.id}
                  milestone={milestone}
                  index={index}
                  isFreelancer={isFreelancer}
                  isEmployer={isEmployer}
                  onStartMilestone={milestoneActions.handleStartMilestone}
                  onSubmitWork={milestoneActions.handleOpenSubmitModal}
                  onApprove={milestoneActions.handleApproveMilestone}
                  onReject={milestoneActions.handleOpenRejectModal}
                />
              ))}
            </div>
          )}
        </div>
      </motion.div>

      {/* Communication Section */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.3 }}
        className="ui-card"
      >
        <div className="ui-card-header">
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <MessageSquare className="w-6 h-6 text-blue-400" />
            Communication
          </h2>
        </div>
        <div className="ui-card-body text-center py-12 text-gray-400">
          <MessageSquare className="w-16 h-16 mx-auto mb-4 opacity-30" />
          <p>Chat feature coming soon</p>
          <p className="text-sm mt-2">Communicate with your {isEmployer ? 'freelancer' : 'employer'} here</p>
        </div>
      </motion.div>

      {/* Modals */}
      {milestoneActions.showSubmitModal && milestoneActions.selectedMilestone && (
        <SubmitWorkModal
          milestone={milestoneActions.selectedMilestone}
          onClose={milestoneActions.closeModals}
          onSubmit={milestoneActions.handleSubmitWork}
        />
      )}

      {milestoneActions.showRejectModal && milestoneActions.selectedMilestone && (
        <RejectMilestoneModal
          milestone={milestoneActions.selectedMilestone}
          onClose={milestoneActions.closeModals}
          onReject={milestoneActions.handleRejectMilestone}
        />
      )}
    </div>
  );
}
