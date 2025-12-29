import { Calendar, CheckCircle, Clock, FileText, Upload, AlertCircle, Paperclip, Download, Play, X } from 'lucide-react';

interface MilestoneCardProps {
  milestone: any;
  index: number;
  isFreelancer: boolean;
  isEmployer: boolean;
  onStartMilestone: (milestoneId: string) => void;
  onSubmitWork: (milestone: any) => void;
  onApprove: (milestoneId: string) => void;
  onReject: (milestone: any) => void;
}

export function MilestoneCard({
  milestone,
  index,
  isFreelancer,
  isEmployer,
  onStartMilestone,
  onSubmitWork,
  onApprove,
  onReject
}: MilestoneCardProps) {
  
  const getMilestoneStatusColor = (status: string) => {
    switch (status) {
      case 'RELEASED':
      case 'APPROVED': return 'bg-green-500/20 text-green-400 border-green-500/30';
      case 'PENDING': return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
      case 'IN_PROGRESS': return 'bg-blue-500/20 text-blue-400 border-blue-500/30';
      case 'SUBMITTED': return 'bg-yellow-500/20 text-yellow-400 border-yellow-500/30';
      case 'REJECTED': return 'bg-red-500/20 text-red-400 border-red-500/30';
      default: return 'bg-gray-500/20 text-gray-400 border-gray-500/30';
    }
  };

  return (
    <div className="relative overflow-hidden rounded-xl border border-white/10 bg-white/5 backdrop-blur-sm p-6">
      <div className="flex items-start justify-between">
        <div className="flex-1">
          {/* Milestone Header */}
          <div className="flex items-center gap-3 mb-2">
            <span className="text-sm font-medium text-gray-400">
              Milestone #{milestone.sequenceOrder || index + 1}
            </span>
            <span className={`px-3 py-1 rounded-full text-xs font-medium border ${getMilestoneStatusColor(milestone.status)}`}>
              {milestone.status}
            </span>
          </div>

          <h3 className="text-lg font-semibold text-white mb-2">
            {milestone.title}
          </h3>
          <p className="text-gray-400 text-sm mb-4">
            {milestone.description}
          </p>

          {/* Deliverables */}
          {milestone.deliverables && (
            <div className="mb-3 p-3 bg-blue-500/10 border border-blue-500/30 rounded-lg">
              <h4 className="text-sm font-semibold text-blue-400 mb-1 flex items-center gap-2">
                <Upload className="w-3 h-3" />
                Deliverables Submitted:
              </h4>
              <p className="text-gray-300 text-xs whitespace-pre-wrap">{milestone.deliverables}</p>
              {milestone.completionNotes && (
                <p className="text-gray-400 text-xs mt-1">Notes: {milestone.completionNotes}</p>
              )}
            </div>
          )}

          {/* Rejection Reason */}
          {milestone.rejectionReason && milestone.status === 'IN_PROGRESS' && (
            <div className="mb-3 p-3 bg-red-500/10 border border-red-500/30 rounded-lg">
              <h4 className="text-sm font-semibold text-red-400 mb-1 flex items-center gap-2">
                <AlertCircle className="w-3 h-3" />
                Rejection Feedback:
              </h4>
              <p className="text-gray-300 text-xs">{milestone.rejectionReason}</p>
            </div>
          )}

          {/* Attachments */}
          {milestone.attachments && milestone.attachments.length > 0 && (
            <div className="mb-3 p-3 bg-purple-500/10 border border-purple-500/30 rounded-lg">
              <h4 className="text-sm font-semibold text-purple-400 mb-2 flex items-center gap-2">
                <Paperclip className="w-3 h-3" />
                Attachments ({milestone.attachments.length}):
              </h4>
              <div className="space-y-1">
                {milestone.attachments.map((file: any) => (
                  <a
                    key={file.id}
                    href={`http://localhost:8080${file.fileUrl}`}
                    target="_blank"
                    rel="noopener noreferrer"
                    className="flex items-center gap-2 p-2 bg-white/5 rounded hover:bg-white/10 transition-colors text-xs group"
                  >
                    <FileText className="w-4 h-4 text-purple-400 flex-shrink-0" />
                    <span className="flex-1 text-white truncate">{file.fileName}</span>
                    <span className="text-gray-400">{(file.fileSize / 1024).toFixed(1)} KB</span>
                    <Download className="w-3 h-3 text-gray-400 group-hover:text-purple-400 flex-shrink-0" />
                  </a>
                ))}
              </div>
            </div>
          )}

          {/* Due Date */}
          {milestone.dueDate && (
            <div className="flex items-center gap-2 text-sm text-gray-400">
              <Calendar className="w-4 h-4" />
              <span>Due: {new Date(milestone.dueDate).toLocaleDateString()}</span>
            </div>
          )}
        </div>

        <div className="ml-4 flex flex-col items-end gap-3">
          <p className="text-2xl font-bold text-yellow-400">
            {milestone.amount} {milestone.currency}
          </p>

          {/* Freelancer Actions */}
          {isFreelancer && (
            <FreelancerActions
              milestone={milestone}
              onStartMilestone={onStartMilestone}
              onSubmitWork={onSubmitWork}
            />
          )}

          {/* Employer Actions */}
          {isEmployer && (
            <EmployerActions
              milestone={milestone}
              onApprove={onApprove}
              onReject={onReject}
            />
          )}
        </div>
      </div>
    </div>
  );
}

// Freelancer Actions Component
function FreelancerActions({ milestone, onStartMilestone, onSubmitWork }: any) {
  switch (milestone.status) {
    case 'PENDING':
      return (
        <button
          onClick={() => onStartMilestone(milestone.id)}
          className="px-4 py-2 bg-blue-500/20 text-blue-400 rounded-lg hover:bg-blue-500/30 transition-colors flex items-center gap-2 text-sm font-medium"
        >
          <Play className="w-4 h-4" />
          Start Working
        </button>
      );

    case 'IN_PROGRESS':
      return (
        <button
          onClick={() => onSubmitWork(milestone)}
          className="px-4 py-2 bg-gradient-to-r from-yellow-600 to-yellow-500 text-black rounded-lg hover:shadow-lg hover:shadow-yellow-500/50 transition-all flex items-center gap-2 text-sm font-semibold"
        >
          <Upload className="w-4 h-4" />
          Submit Work
        </button>
      );

    case 'SUBMITTED':
      return (
        <div className="flex items-center gap-2 text-yellow-400 text-sm px-4 py-2 bg-yellow-500/10 rounded-lg">
          <Clock className="w-4 h-4" />
          <span>Awaiting Review</span>
        </div>
      );

    case 'APPROVED':
    case 'RELEASED':
      return (
        <div className="flex items-center gap-2 text-green-400 text-sm px-4 py-2 bg-green-500/10 rounded-lg">
          <CheckCircle className="w-4 h-4" />
          <span>Payment Released</span>
        </div>
      );

    default:
      return null;
  }
}

// Employer Actions Component
function EmployerActions({ milestone, onApprove, onReject }: any) {
  switch (milestone.status) {
    case 'SUBMITTED':
      return (
        <div className="flex gap-2">
          <button
            onClick={() => onApprove(milestone.id)}
            className="px-4 py-2 bg-green-500/20 text-green-400 rounded-lg hover:bg-green-500/30 transition-colors flex items-center gap-2 text-sm font-medium"
          >
            <CheckCircle className="w-4 h-4" />
            Approve & Pay
          </button>
          <button
            onClick={() => onReject(milestone)}
            className="px-4 py-2 bg-red-500/20 text-red-400 rounded-lg hover:bg-red-500/30 transition-colors flex items-center gap-2 text-sm font-medium"
          >
            <X className="w-4 h-4" />
            Reject
          </button>
        </div>
      );

    case 'PENDING':
      return (
        <div className="text-gray-400 text-sm px-4 py-2 bg-gray-500/10 rounded-lg">
          Waiting for freelancer to start
        </div>
      );

    case 'IN_PROGRESS':
      return (
        <div className="flex items-center gap-2 text-blue-400 text-sm px-4 py-2 bg-blue-500/10 rounded-lg">
          <Clock className="w-4 h-4" />
          <span>In Progress</span>
        </div>
      );

    case 'APPROVED':
    case 'RELEASED':
      return (
        <div className="flex items-center gap-2 text-green-400 text-sm px-4 py-2 bg-green-500/10 rounded-lg">
          <CheckCircle className="w-4 h-4" />
          <span>Payment Released</span>
        </div>
      );

    default:
      return null;
  }
}
