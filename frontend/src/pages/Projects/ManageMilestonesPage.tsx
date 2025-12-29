import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { 
  ArrowLeft, Plus, Edit, Trash2, Check, X, 
  Clock, DollarSign, AlertCircle, CheckCircle, FileText,
  Upload, Eye, Download, Paperclip
} from 'lucide-react';
import { projectApi } from '../../services/api';
import { useAuth } from '../../hooks/useAuth';

interface FileAttachment {
  id: string;
  uploadedBy: string;
  uploaderName: string;
  entityType: string;
  entityId: string;
  fileUrl: string;
  fileName: string;
  fileSize: number;
  mimeType: string;
  createdAt: string;
}

interface Milestone {
  id: string;
  projectId: string;
  title: string;
  description: string;
  amount: number;
  currency: string;
  sequenceOrder: number;
  status: 'PENDING' | 'IN_PROGRESS' | 'SUBMITTED' | 'APPROVED' | 'REJECTED' | 'RELEASED';
  dueDate?: string;
  submittedAt?: string;
  approvedAt?: string;
  releasedAt?: string;
  createdAt: string;
  deliverables?: string;
  completionNotes?: string;
  rejectionReason?: string;
  attachments?: FileAttachment[];
}

export function ManageMilestonesPage() {
  const { projectId } = useParams<{ projectId: string }>();
  const navigate = useNavigate();
  const { role } = useAuth();
  
  const [project, setProject] = useState<any>(null);
  const [milestones, setMilestones] = useState<Milestone[]>([]);
  const [loading, setLoading] = useState(true);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [editingMilestone, setEditingMilestone] = useState<Milestone | null>(null);
  const [showSubmitModal, setShowSubmitModal] = useState(false);
  const [submittingMilestone, setSubmittingMilestone] = useState<Milestone | null>(null);
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [rejectingMilestone, setRejectingMilestone] = useState<Milestone | null>(null);
  
  const isEmployer = role === 'EMPLOYER';
  const isFreelancer = role === 'FREELANCER';
  
  useEffect(() => {
    if (projectId) {
      loadData();
    }
  }, [projectId]);
  
  const loadData = async () => {
    try {
      setLoading(true);
      
      const projectRes = await projectApi.getProjectById(projectId!);
      setProject(projectRes.data || projectRes);
      
      const milestonesRes = await projectApi.getProjectMilestones(projectId!);
      setMilestones(milestonesRes.data || milestonesRes || []);
      
    } catch (error: any) {
      console.error('Failed to load data:', error);
    } finally {
      setLoading(false);
    }
  };
  
  const handleDeleteMilestone = async (milestoneId: string) => {
    if (!confirm('Are you sure you want to delete this milestone?')) {
      return;
    }
    
    try {
      await projectApi.deleteMilestone(milestoneId);
      loadData();
    } catch (error: any) {
      console.error('Failed to delete milestone:', error);
      alert(error.response?.data?.message || 'Failed to delete milestone');
    }
  };
  
  const handleSubmitMilestone = async (milestoneId: string, deliverables: string, notes: string) => {
    try {
      await projectApi.submitMilestone(milestoneId, { deliverables, notes });
      setShowSubmitModal(false);
      setSubmittingMilestone(null);
      loadData();
    } catch (error: any) {
      console.error('Failed to submit milestone:', error);
      alert(error.response?.data?.message || 'Failed to submit milestone');
    }
  };
  
  const handleApproveMilestone = async (milestoneId: string) => {
    if (!confirm('Approve this milestone and release payment?')) {
      return;
    }
    
    try {
      await projectApi.approveMilestone(milestoneId);
      loadData();
    } catch (error: any) {
      console.error('Failed to approve milestone:', error);
      alert(error.response?.data?.message || 'Failed to approve milestone');
    }
  };
  
  const handleRejectMilestone = async (milestoneId: string, reason: string) => {
    try {
      await projectApi.rejectMilestone(milestoneId, { reason });
      setShowRejectModal(false);
      setRejectingMilestone(null);
      loadData();
    } catch (error: any) {
      console.error('Failed to reject milestone:', error);
      alert(error.response?.data?.message || 'Failed to reject milestone');
    }
  };
  
  const getStatusBadge = (status: string) => {
    const badges = {
      PENDING: 'bg-gray-500/20 text-gray-400',
      IN_PROGRESS: 'bg-blue-500/20 text-blue-400',
      SUBMITTED: 'bg-yellow-500/20 text-yellow-400',
      APPROVED: 'bg-green-500/20 text-green-400',
      REJECTED: 'bg-red-500/20 text-red-400',
      RELEASED: 'bg-purple-500/20 text-purple-400',
    };
    
    return badges[status as keyof typeof badges] || badges.PENDING;
  };

  const getStatusText = (status: string) => {
    const texts = {
      PENDING: 'Pending',
      IN_PROGRESS: 'In Progress',
      SUBMITTED: 'Awaiting Review',
      APPROVED: 'Approved',
      REJECTED: 'Rejected',
      RELEASED: 'Paid',
    };
    
    return texts[status as keyof typeof texts] || status;
  };
  
  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-yellow-400 border-t-transparent"></div>
      </div>
    );
  }
  
  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-gray-800 to-gray-900 p-6">
      <div className="max-w-6xl mx-auto space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate(`/projects/${projectId}`)}
              className="flex items-center gap-2 text-gray-400 hover:text-white transition-colors"
            >
              <ArrowLeft className="w-4 h-4" />
              Back to Project
            </button>
            <h1 className="text-3xl font-bold text-white">Manage Milestones</h1>
          </div>
          
          {isEmployer && project?.status === 'IN_PROGRESS' && (
            <button
              onClick={() => setShowCreateModal(true)}
              className="flex items-center gap-2 px-6 py-3 bg-gradient-to-r from-yellow-600 via-yellow-500 to-yellow-400 text-black font-semibold rounded-lg hover:shadow-lg hover:shadow-yellow-500/50 transition-all"
            >
              <Plus className="w-5 h-5" />
              Add Milestone
            </button>
          )}
        </div>
        
        {/* Project Info */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-white/5 backdrop-blur-sm border border-white/10 rounded-xl p-6"
        >
          <h2 className="text-2xl font-semibold text-white mb-2">{project?.title}</h2>
          <div className="flex gap-6 text-sm text-gray-400">
            <span>Budget: {project?.agreedAmount} {project?.currency}</span>
            <span>Status: {project?.status}</span>
            <span>Total Milestones: {milestones.length}</span>
          </div>
        </motion.div>
        
        {/* Milestones List */}
        <div className="space-y-4">
          {milestones.length === 0 ? (
            <motion.div
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              className="bg-white/5 backdrop-blur-sm border border-white/10 rounded-xl text-center py-16"
            >
              <FileText className="w-20 h-20 mx-auto mb-4 text-gray-600" />
              <p className="text-gray-400 text-lg mb-4">No milestones yet</p>
              {isEmployer && (
                <button
                  onClick={() => setShowCreateModal(true)}
                  className="px-6 py-3 bg-gradient-to-r from-yellow-600 via-yellow-500 to-yellow-400 text-black font-semibold rounded-lg hover:shadow-lg hover:shadow-yellow-500/50 transition-all"
                >
                  Create First Milestone
                </button>
              )}
            </motion.div>
          ) : (
            milestones.map((milestone, index) => (
              <motion.div
                key={milestone.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.1 }}
                className="bg-white/5 backdrop-blur-sm border border-white/10 rounded-xl p-6 hover:bg-white/10 transition-all"
              >
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-3">
                      <span className="text-gray-400 font-mono text-sm">#{milestone.sequenceOrder}</span>
                      <h3 className="text-xl font-semibold text-white">{milestone.title}</h3>
                      <span className={`px-3 py-1 rounded-full text-sm font-medium ${getStatusBadge(milestone.status)}`}>
                        {getStatusText(milestone.status)}
                      </span>
                    </div>
                    
                    <p className="text-gray-400 mb-4 whitespace-pre-wrap">{milestone.description}</p>
                    
                    {/* ✅ Show Deliverables (if submitted) */}
                    {milestone.deliverables && (
                      <div className="mb-4 p-4 bg-blue-500/10 border border-blue-500/30 rounded-lg">
                        <h4 className="text-sm font-semibold text-blue-400 mb-2 flex items-center gap-2">
                          <Upload className="w-4 h-4" />
                          Deliverables Submitted:
                        </h4>
                        <p className="text-gray-300 text-sm whitespace-pre-wrap">{milestone.deliverables}</p>
                        {milestone.completionNotes && (
                          <div className="mt-2 pt-2 border-t border-blue-500/20">
                            <p className="text-gray-400 text-xs">Notes: {milestone.completionNotes}</p>
                          </div>
                        )}
                      </div>
                    )}
                    
                    {/* ✅ Show Rejection Reason (if rejected) */}
                    {milestone.rejectionReason && milestone.status === 'IN_PROGRESS' && (
                      <div className="mb-4 p-4 bg-red-500/10 border border-red-500/30 rounded-lg">
                        <h4 className="text-sm font-semibold text-red-400 mb-2 flex items-center gap-2">
                          <AlertCircle className="w-4 h-4" />
                          Rejection Feedback:
                        </h4>
                        <p className="text-gray-300 text-sm whitespace-pre-wrap">{milestone.rejectionReason}</p>
                      </div>
                    )}
                    
                    {/* ✅ Show File Attachments */}
                    {milestone.attachments && milestone.attachments.length > 0 && (
                      <div className="mb-4 p-4 bg-purple-500/10 border border-purple-500/30 rounded-lg">
                        <h4 className="text-sm font-semibold text-purple-400 mb-3 flex items-center gap-2">
                          <Paperclip className="w-4 h-4" />
                          Attachments ({milestone.attachments.length}):
                        </h4>
                        <div className="space-y-2">
                          {milestone.attachments.map((file) => (
                            <a
                              key={file.id}
                              href={`http://localhost:8080${file.fileUrl}`}
                              target="_blank"
                              rel="noopener noreferrer"
                              className="flex items-center gap-3 p-3 bg-white/5 rounded-lg hover:bg-white/10 transition-colors group"
                            >
                              <FileText className="w-5 h-5 text-purple-400 flex-shrink-0" />
                              <div className="flex-1 min-w-0">
                                <p className="text-white text-sm font-medium truncate">{file.fileName}</p>
                                <p className="text-gray-400 text-xs">
                                  {(file.fileSize / 1024).toFixed(1)} KB • Uploaded by {file.uploaderName}
                                </p>
                              </div>
                              <Download className="w-4 h-4 text-gray-400 group-hover:text-purple-400 transition-colors flex-shrink-0" />
                            </a>
                          ))}
                        </div>
                      </div>
                    )}
                    
                    <div className="flex flex-wrap gap-6 text-sm mb-4">
                      <div className="flex items-center gap-2 text-gray-300">
                        <DollarSign className="w-4 h-4 text-green-400" />
                        <span className="font-semibold">{milestone.amount} {milestone.currency}</span>
                      </div>
                      {milestone.dueDate && (
                        <div className="flex items-center gap-2 text-gray-300">
                          <Clock className="w-4 h-4 text-yellow-400" />
                          <span>Due: {new Date(milestone.dueDate).toLocaleDateString()}</span>
                        </div>
                      )}
                      {milestone.submittedAt && (
                        <div className="flex items-center gap-2 text-yellow-300">
                          <Upload className="w-4 h-4" />
                          <span>Submitted: {new Date(milestone.submittedAt).toLocaleDateString()}</span>
                        </div>
                      )}
                      {milestone.approvedAt && (
                        <div className="flex items-center gap-2 text-green-300">
                          <CheckCircle className="w-4 h-4" />
                          <span>Approved: {new Date(milestone.approvedAt).toLocaleDateString()}</span>
                        </div>
                      )}
                    </div>
                  </div>
                  
                  {/* Action Buttons */}
                  <div className="flex gap-2 ml-4">
                    {/* Employer Actions */}
                    {isEmployer && (
                      <>
                        {milestone.status === 'PENDING' && (
                          <>
                            <button
                              onClick={() => setEditingMilestone(milestone)}
                              className="p-2 text-blue-400 hover:bg-blue-500/10 rounded-lg transition-colors"
                              title="Edit"
                            >
                              <Edit className="w-4 h-4" />
                            </button>
                            <button
                              onClick={() => handleDeleteMilestone(milestone.id)}
                              className="p-2 text-red-400 hover:bg-red-500/10 rounded-lg transition-colors"
                              title="Delete"
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          </>
                        )}
                        
                        {milestone.status === 'SUBMITTED' && (
                          <>
                            <button
                              onClick={() => handleApproveMilestone(milestone.id)}
                              className="flex items-center gap-2 px-4 py-2 bg-green-500/20 text-green-400 rounded-lg hover:bg-green-500/30 transition-colors"
                            >
                              <CheckCircle className="w-4 h-4" />
                              Approve & Pay
                            </button>
                            <button
                              onClick={() => {
                                setRejectingMilestone(milestone);
                                setShowRejectModal(true);
                              }}
                              className="flex items-center gap-2 px-4 py-2 bg-red-500/20 text-red-400 rounded-lg hover:bg-red-500/30 transition-colors"
                            >
                              <X className="w-4 h-4" />
                              Reject
                            </button>
                          </>
                        )}
                      </>
                    )}
                    
                    {/* Freelancer Actions */}
                    {isFreelancer && (
                      <>
                        {milestone.status === 'IN_PROGRESS' && (
                          <button
                            onClick={() => {
                              setSubmittingMilestone(milestone);
                              setShowSubmitModal(true);
                            }}
                            className="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-yellow-600 via-yellow-500 to-yellow-400 text-black font-semibold rounded-lg hover:shadow-lg hover:shadow-yellow-500/50 transition-all"
                          >
                            <Upload className="w-4 h-4" />
                            Submit Work
                          </button>
                        )}
                        
                        {milestone.status === 'REJECTED' && (
                          <button
                            onClick={() => {
                              setSubmittingMilestone(milestone);
                              setShowSubmitModal(true);
                            }}
                            className="flex items-center gap-2 px-4 py-2 bg-yellow-500/20 text-yellow-400 rounded-lg hover:bg-yellow-500/30 transition-colors"
                          >
                            <Upload className="w-4 h-4" />
                            Resubmit
                          </button>
                        )}
                        
                        {milestone.status === 'SUBMITTED' && (
                          <div className="flex items-center gap-2 text-yellow-400 text-sm px-4 py-2 bg-yellow-500/10 rounded-lg">
                            <Clock className="w-4 h-4" />
                            <span>Awaiting Review</span>
                          </div>
                        )}
                        
                        {milestone.status === 'APPROVED' && (
                          <div className="flex items-center gap-2 text-green-400 text-sm px-4 py-2 bg-green-500/10 rounded-lg">
                            <CheckCircle className="w-4 h-4" />
                            <span>Payment Released</span>
                          </div>
                        )}
                      </>
                    )}
                  </div>
                </div>
              </motion.div>
            ))
          )}
        </div>
      </div>
      
      {/* Create/Edit Modal */}
      {(showCreateModal || editingMilestone) && (
        <MilestoneModal
          projectId={projectId!}
          project={project}
          milestone={editingMilestone}
          onClose={() => {
            setShowCreateModal(false);
            setEditingMilestone(null);
          }}
          onSuccess={() => {
            setShowCreateModal(false);
            setEditingMilestone(null);
            loadData();
          }}
        />
      )}
      
      {/* Submit Work Modal */}
      {showSubmitModal && submittingMilestone && (
        <SubmitWorkModal
          milestone={submittingMilestone}
          onClose={() => {
            setShowSubmitModal(false);
            setSubmittingMilestone(null);
          }}
          onSubmit={(deliverables, notes) => {
            handleSubmitMilestone(submittingMilestone.id, deliverables, notes);
          }}
        />
      )}
      
      {/* Reject Modal */}
      {showRejectModal && rejectingMilestone && (
        <RejectModal
          milestone={rejectingMilestone}
          onClose={() => {
            setShowRejectModal(false);
            setRejectingMilestone(null);
          }}
          onReject={(reason) => {
            handleRejectMilestone(rejectingMilestone.id, reason);
          }}
        />
      )}
    </div>
  );
}

// ===== MILESTONE MODAL (CREATE/EDIT) =====
interface MilestoneModalProps {
  projectId: string;
  project: any;
  milestone: Milestone | null;
  onClose: () => void;
  onSuccess: () => void;
}

function MilestoneModal({ projectId, project, milestone, onClose, onSuccess }: MilestoneModalProps) {
  const [formData, setFormData] = useState({
    title: milestone?.title || '',
    description: milestone?.description || '',
    amount: milestone?.amount || 0,
    dueDate: milestone?.dueDate ? new Date(milestone.dueDate).toISOString().split('T')[0] : '',
  });
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    
    // Validation
    if (!formData.title || formData.title.trim().length < 3) {
      setError('Title must be at least 3 characters');
      return;
    }
    
    if (!formData.amount || formData.amount <= 0) {
      setError('Amount must be greater than 0');
      return;
    }
    
    try {
      setLoading(true);
      
      const payload = {
        ...formData,
        dueDate: formData.dueDate ? new Date(formData.dueDate).toISOString() : undefined,
      };
      
      if (milestone) {
        await projectApi.updateMilestone(milestone.id, payload);
      } else {
        await projectApi.createMilestone(projectId, payload);
      }
      
      onSuccess();
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to save milestone');
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <motion.div
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        className="bg-gray-800 rounded-xl p-6 max-w-md w-full"
      >
        <h2 className="text-2xl font-bold text-white mb-4">
          {milestone ? 'Edit Milestone' : 'Create Milestone'}
        </h2>
        
        {error && (
          <div className="mb-4 p-3 bg-red-500/20 border border-red-500/50 rounded-lg text-red-400 text-sm flex items-start gap-2">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{error}</span>
          </div>
        )}
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-gray-300 text-sm font-medium mb-2">
              Title *
            </label>
            <input
              type="text"
              value={formData.title}
              onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-yellow-400/50 transition-all"
              placeholder="e.g., Database Design"
              required
              disabled={loading}
            />
          </div>
          
          <div>
            <label className="block text-gray-300 text-sm font-medium mb-2">
              Description
            </label>
            <textarea
              value={formData.description}
              onChange={(e) => setFormData({ ...formData, description: e.target.value })}
              className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-yellow-400/50 transition-all"
              rows={3}
              placeholder="Describe the milestone deliverables..."
              disabled={loading}
            />
          </div>
          
          <div>
            <label className="block text-gray-300 text-sm font-medium mb-2">
              Amount ({project?.currency || 'USDT'}) *
            </label>
            <input
              type="number"
              step="0.01"
              value={formData.amount}
              onChange={(e) => setFormData({ ...formData, amount: parseFloat(e.target.value) || 0 })}
              className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-yellow-400/50 transition-all"
              placeholder="Enter amount"
              required
              min="0.01"
              disabled={loading}
            />
          </div>
          
          <div>
            <label className="block text-gray-300 text-sm font-medium mb-2">
              Due Date
            </label>
            <input
              type="date"
              value={formData.dueDate}
              onChange={(e) => setFormData({ ...formData, dueDate: e.target.value })}
              className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-white focus:outline-none focus:border-yellow-400/50 transition-all"
              disabled={loading}
            />
          </div>
          
          <div className="flex gap-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-6 py-3 bg-gray-700 text-white rounded-lg hover:bg-gray-600 transition-colors"
              disabled={loading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="flex-1 px-6 py-3 bg-gradient-to-r from-yellow-600 via-yellow-500 to-yellow-400 text-black font-semibold rounded-lg hover:shadow-lg hover:shadow-yellow-500/50 transition-all disabled:opacity-50"
              disabled={loading}
            >
              {loading ? 'Saving...' : 'Save'}
            </button>
          </div>
        </form>
      </motion.div>
    </div>
  );
}

// ===== SUBMIT WORK MODAL (FREELANCER) =====
interface SubmitWorkModalProps {
  milestone: Milestone;
  onClose: () => void;
  onSubmit: (deliverables: string, notes: string) => void;
}

function SubmitWorkModal({ milestone, onClose, onSubmit }: SubmitWorkModalProps) {
  const [deliverables, setDeliverables] = useState('');
  const [notes, setNotes] = useState('');
  const [files, setFiles] = useState<File[]>([]);
  const [uploading, setUploading] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  
  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const newFiles = Array.from(e.target.files);
      setFiles(prev => [...prev, ...newFiles]);
    }
  };
  
  const removeFile = (index: number) => {
    setFiles(prev => prev.filter((_, i) => i !== index));
  };
  
  const uploadFiles = async () => {
    if (files.length === 0) return;
    
    setUploading(true);
    try {
      for (const file of files) {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('entityType', 'MILESTONE');
        formData.append('entityId', milestone.id);
        
        await fetch('http://localhost:8080/api/files/upload', {
          method: 'POST',
          headers: {
            'Authorization': `Bearer ${localStorage.getItem('userToken') || localStorage.getItem('token')}`
          },
          body: formData
        });
      }
    } catch (err: any) {
      console.error('File upload error:', err);
      throw new Error('Failed to upload files: ' + err.message);
    } finally {
      setUploading(false);
    }
  };
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    
    if (!deliverables.trim()) {
      setError('Please describe your work/deliverables');
      return;
    }
    
    if (deliverables.trim().length < 20) {
      setError('Deliverables description must be at least 20 characters');
      return;
    }
    
    setLoading(true);
    try {
      // Upload files first
      await uploadFiles();
      
      // Then submit milestone
      await onSubmit(deliverables, notes);
    } catch (err: any) {
      setError(err.message || 'Failed to submit work');
      setLoading(false);
    }
  };
  
  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <motion.div
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        className="bg-gray-800 rounded-xl p-6 max-w-2xl w-full"
      >
        <h2 className="text-2xl font-bold text-white mb-2">Submit Work for Review</h2>
        <p className="text-gray-400 mb-6">Milestone: {milestone.title}</p>
        
        {error && (
          <div className="mb-4 p-3 bg-red-500/20 border border-red-500/50 rounded-lg text-red-400 text-sm flex items-start gap-2">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{error}</span>
          </div>
        )}
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-gray-300 text-sm font-medium mb-2">
              Deliverables / Work Completed *
            </label>
            <textarea
              value={deliverables}
              onChange={(e) => setDeliverables(e.target.value)}
              className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-yellow-400/50 transition-all"
              rows={6}
              placeholder="Describe what you have completed:&#10;- Feature implemented&#10;- Links to demo/repository&#10;- Screenshots/files uploaded&#10;- Any relevant notes..."
              required
              disabled={loading}
            />
            <p className="text-xs text-gray-500 mt-1">
              Minimum 20 characters. Be specific about what you've delivered.
            </p>
          </div>
          
          <div>
            <label className="block text-gray-300 text-sm font-medium mb-2">
              Additional Notes (Optional)
            </label>
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-yellow-400/50 transition-all"
              rows={3}
              placeholder="Any additional information for the employer..."
              disabled={loading}
            />
          </div>
          
          {/* ✅ File Upload Section */}
          <div>
            <label className="block text-gray-300 text-sm font-medium mb-2">
              Attach Files (Optional)
            </label>
            <div className="border-2 border-dashed border-white/20 rounded-lg p-6 text-center hover:border-yellow-400/50 transition-all">
              <input
                type="file"
                onChange={handleFileChange}
                multiple
                className="hidden"
                id="file-upload"
                accept=".pdf,.doc,.docx,.xls,.xlsx,.png,.jpg,.jpeg,.gif,.webp,.zip,.txt"
                disabled={loading || uploading}
              />
              <label htmlFor="file-upload" className="cursor-pointer">
                <Upload className="w-8 h-8 mx-auto mb-2 text-gray-400" />
                <p className="text-gray-400 text-sm">
                  Click to upload files or drag and drop
                </p>
                <p className="text-gray-500 text-xs mt-1">
                  PDF, DOC, Images, ZIP (Max 10MB per file)
                </p>
              </label>
            </div>
            
            {/* Selected Files List */}
            {files.length > 0 && (
              <div className="mt-3 space-y-2">
                {files.map((file, index) => (
                  <div key={index} className="flex items-center justify-between p-3 bg-white/5 rounded-lg">
                    <div className="flex items-center gap-3 flex-1 min-w-0">
                      <FileText className="w-4 h-4 text-purple-400 flex-shrink-0" />
                      <div className="flex-1 min-w-0">
                        <p className="text-white text-sm truncate">{file.name}</p>
                        <p className="text-gray-400 text-xs">
                          {(file.size / 1024).toFixed(1)} KB
                        </p>
                      </div>
                    </div>
                    <button
                      type="button"
                      onClick={() => removeFile(index)}
                      className="p-1 text-red-400 hover:bg-red-500/10 rounded transition-colors"
                      disabled={loading || uploading}
                    >
                      <X className="w-4 h-4" />
                    </button>
                  </div>
                ))}
              </div>
            )}
          </div>
          
          <div className="bg-blue-500/10 border border-blue-500/30 rounded-lg p-4">
            <div className="flex items-start gap-3">
              <AlertCircle className="w-5 h-5 text-blue-400 shrink-0 mt-0.5" />
              <div className="text-sm text-blue-300">
                <p className="font-semibold mb-1">Before submitting:</p>
                <ul className="list-disc list-inside space-y-1 text-blue-200">
                  <li>Ensure all deliverables meet the requirements</li>
                  <li>Test your work thoroughly</li>
                  <li>Provide clear documentation/links</li>
                  <li>Payment will be released upon employer approval</li>
                </ul>
              </div>
            </div>
          </div>
          
          <div className="flex gap-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-6 py-3 bg-gray-700 text-white rounded-lg hover:bg-gray-600 transition-colors"
              disabled={loading || uploading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="flex-1 px-6 py-3 bg-gradient-to-r from-green-600 to-green-500 text-white font-semibold rounded-lg hover:shadow-lg hover:shadow-green-500/50 transition-all disabled:opacity-50 flex items-center justify-center gap-2"
              disabled={loading || uploading}
            >
              {uploading ? (
                <>
                  <div className="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent" />
                  Uploading Files...
                </>
              ) : loading ? (
                <>
                  <div className="animate-spin rounded-full h-4 w-4 border-2 border-white border-t-transparent" />
                  Submitting...
                </>
              ) : (
                <>
                  <Upload className="w-5 h-5" />
                  Submit for Review
                </>
              )}
            </button>
          </div>
        </form>
      </motion.div>
    </div>
  );
}

// ===== REJECT MODAL (EMPLOYER) =====
interface RejectModalProps {
  milestone: Milestone;
  onClose: () => void;
  onReject: (reason: string) => void;
}

function RejectModal({ milestone, onClose, onReject }: RejectModalProps) {
  const [reason, setReason] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  
  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    
    if (!reason.trim()) {
      setError('Please provide a reason for rejection');
      return;
    }
    
    if (reason.trim().length < 10) {
      setError('Reason must be at least 10 characters');
      return;
    }
    
    setLoading(true);
    try {
      await onReject(reason);
    } catch (err: any) {
      setError(err.message || 'Failed to reject milestone');
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <motion.div
        initial={{ opacity: 0, scale: 0.9 }}
        animate={{ opacity: 1, scale: 1 }}
        className="bg-gray-800 rounded-xl p-6 max-w-md w-full"
      >
        <h2 className="text-2xl font-bold text-white mb-2">Reject Milestone</h2>
        <p className="text-gray-400 mb-6">Milestone: {milestone.title}</p>
        
        {error && (
          <div className="mb-4 p-3 bg-red-500/20 border border-red-500/50 rounded-lg text-red-400 text-sm flex items-start gap-2">
            <AlertCircle className="w-4 h-4 shrink-0 mt-0.5" />
            <span>{error}</span>
          </div>
        )}
        
        <form onSubmit={handleSubmit} className="space-y-4">
          <div>
            <label className="block text-gray-300 text-sm font-medium mb-2">
              Reason for Rejection *
            </label>
            <textarea
              value={reason}
              onChange={(e) => setReason(e.target.value)}
              className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:border-yellow-400/50 transition-all"
              rows={5}
              placeholder="Explain why the work doesn't meet requirements:&#10;- What needs to be fixed&#10;- Specific issues found&#10;- What you expect in the resubmission..."
              required
              disabled={loading}
            />
            <p className="text-xs text-gray-500 mt-1">
              Minimum 10 characters. Be clear so freelancer can improve.
            </p>
          </div>
          
          <div className="bg-yellow-500/10 border border-yellow-500/30 rounded-lg p-4">
            <div className="flex items-start gap-3">
              <AlertCircle className="w-5 h-5 text-yellow-400 shrink-0 mt-0.5" />
              <div className="text-sm text-yellow-300">
                <p>The freelancer will be able to fix and resubmit the work. Payment will remain in escrow until you approve.</p>
              </div>
            </div>
          </div>
          
          <div className="flex gap-3 pt-4">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 px-6 py-3 bg-gray-700 text-white rounded-lg hover:bg-gray-600 transition-colors"
              disabled={loading}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="flex-1 px-6 py-3 bg-gradient-to-r from-red-600 to-red-500 text-white font-semibold rounded-lg hover:shadow-lg hover:shadow-red-500/50 transition-all disabled:opacity-50 flex items-center justify-center gap-2"
              disabled={loading}
            >
              {loading ? (
                'Rejecting...'
              ) : (
                <>
                  <X className="w-5 h-5" />
                  Reject Milestone
                </>
              )}
            </button>
          </div>
        </form>
      </motion.div>
    </div>
  );
}