import { useState } from 'react';
import { projectApi } from '../services/api';

export function useMilestoneActions(onRefresh: () => void) {
  const [showSubmitModal, setShowSubmitModal] = useState(false);
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [selectedMilestone, setSelectedMilestone] = useState<any>(null);

  const handleStartMilestone = async (milestoneId: string) => {
    if (!window.confirm('Start working on this milestone?')) {
      return;
    }

    try {
      await projectApi.startMilestone(milestoneId);
      alert('Milestone started! You can now begin working.');
      onRefresh();
    } catch (error: any) {
      alert(error.response?.data?.message || 'Failed to start milestone');
    }
  };

  const handleOpenSubmitModal = (milestone: any) => {
    setSelectedMilestone(milestone);
    setShowSubmitModal(true);
  };

  const handleSubmitWork = async (deliverables: string, notes: string) => {
    if (!selectedMilestone) return;

    try {
      await projectApi.submitMilestone(selectedMilestone.id, { deliverables, notes });
      alert('Work submitted for review!');
      setShowSubmitModal(false);
      setSelectedMilestone(null);
      onRefresh();
    } catch (error: any) {
      alert(error.response?.data?.message || 'Failed to submit work');
    }
  };

  const handleApproveMilestone = async (milestoneId: string) => {
    if (!window.confirm('Approve this milestone and release payment?')) {
      return;
    }

    try {
      await projectApi.approveMilestone(milestoneId);
      alert('Milestone approved! Payment released to freelancer.');
      onRefresh();
    } catch (error: any) {
      alert(error.response?.data?.message || 'Failed to approve milestone');
    }
  };

  const handleOpenRejectModal = (milestone: any) => {
    setSelectedMilestone(milestone);
    setShowRejectModal(true);
  };

  const handleRejectMilestone = async (reason: string) => {
    if (!selectedMilestone) return;

    try {
      await projectApi.rejectMilestone(selectedMilestone.id, { reason });
      alert('Milestone rejected with feedback.');
      setShowRejectModal(false);
      setSelectedMilestone(null);
      onRefresh();
    } catch (error: any) {
      alert(error.response?.data?.message || 'Failed to reject milestone');
    }
  };

  const closeModals = () => {
    setShowSubmitModal(false);
    setShowRejectModal(false);
    setSelectedMilestone(null);
  };

  return {
    showSubmitModal,
    showRejectModal,
    selectedMilestone,
    handleStartMilestone,
    handleOpenSubmitModal,
    handleSubmitWork,
    handleApproveMilestone,
    handleOpenRejectModal,
    handleRejectMilestone,
    closeModals,
  };
}
