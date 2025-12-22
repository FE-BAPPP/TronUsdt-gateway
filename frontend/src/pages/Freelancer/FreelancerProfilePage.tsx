import { useEffect, useState } from 'react';
import { motion } from 'framer-motion';
import { User, Briefcase, DollarSign, Star, Link, Save, Edit } from 'lucide-react';
import { userApi } from '../../services/api';

export function FreelancerProfilePage() {
  const [profile, setProfile] = useState<any>(null);
  const [editing, setEditing] = useState(false);
  const [formData, setFormData] = useState<any>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    try {
      setLoading(true);
      const response = await userApi.getFreelancerProfile();
      const data = response.data || response;
      setProfile(data);
      setFormData({
        professionalTitle: data.professionalTitle || '',
        bio: data.bio || '',
        hourlyRate: data.hourlyRate || 0,
        availability: data.availability || 'AVAILABLE',
        portfolioUrl: data.portfolioUrl || '',
        linkedinUrl: data.linkedinUrl || '',
        githubUrl: data.githubUrl || '',
      });
    } catch (error) {
      console.error('Failed to load profile:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async () => {
    try {
      setSaving(true);
      await userApi.updateFreelancerProfile(formData);
      await loadProfile();
      setEditing(false);
      alert('Profile updated successfully!');
    } catch (error: any) {
      alert(error.response?.data?.message || 'Failed to update profile');
    } finally {
      setSaving(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-12 w-12 border-4 border-yellow-400 border-t-transparent"></div>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6 max-w-4xl mx-auto">
      {/* Header */}
      <div className="flex items-center justify-between">
        <h1 className="text-3xl font-bold text-white">My Profile</h1>
        <button
          onClick={() => editing ? handleSave() : setEditing(true)}
          disabled={saving}
          className="ui-btn ui-btn-primary flex items-center gap-2"
        >
          {editing ? (
            <>
              <Save className="w-4 h-4" />
              {saving ? 'Saving...' : 'Save Changes'}
            </>
          ) : (
            <>
              <Edit className="w-4 h-4" />
              Edit Profile
            </>
          )}
        </button>
      </div>

      {/* Profile Stats */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="grid grid-cols-1 md:grid-cols-4 gap-4"
      >
        <div className="ui-card text-center">
          <Star className="w-8 h-8 text-yellow-400 mx-auto mb-2" />
          <p className="text-2xl font-bold text-white">{profile.avgRating?.toFixed(1) || '0.0'}</p>
          <p className="text-gray-400 text-sm">Rating</p>
        </div>

        <div className="ui-card text-center">
          <Briefcase className="w-8 h-8 text-blue-400 mx-auto mb-2" />
          <p className="text-2xl font-bold text-white">{profile.jobsCompleted || 0}</p>
          <p className="text-gray-400 text-sm">Jobs Completed</p>
        </div>

        <div className="ui-card text-center">
          <DollarSign className="w-8 h-8 text-green-400 mx-auto mb-2" />
          <p className="text-2xl font-bold text-white">{profile.totalEarnings || '0'}</p>
          <p className="text-gray-400 text-sm">Total Earned (USDT)</p>
        </div>

        <div className="ui-card text-center">
          <User className="w-8 h-8 text-purple-400 mx-auto mb-2" />
          <p className="text-2xl font-bold text-white">{profile.activeProjects || 0}</p>
          <p className="text-gray-400 text-sm">Active Projects</p>
        </div>
      </motion.div>

      {/* Profile Form */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="ui-card"
      >
        <div className="ui-card-header">
          <h2 className="text-xl font-bold text-white">Professional Information</h2>
        </div>
        <div className="ui-card-body space-y-4">
          {/* Professional Title */}
          <div>
            <label className="block text-sm font-medium text-gray-400 mb-2">
              Professional Title
            </label>
            <input
              type="text"
              value={formData.professionalTitle}
              onChange={(e) => setFormData({ ...formData, professionalTitle: e.target.value })}
              disabled={!editing}
              className="w-full px-4 py-2 bg-white/5 border border-white/10 rounded-lg text-white disabled:opacity-50"
              placeholder="e.g., Full-stack Developer"
            />
          </div>

          {/* Bio */}
          <div>
            <label className="block text-sm font-medium text-gray-400 mb-2">
              Bio
            </label>
            <textarea
              value={formData.bio}
              onChange={(e) => setFormData({ ...formData, bio: e.target.value })}
              disabled={!editing}
              rows={4}
              className="w-full px-4 py-2 bg-white/5 border border-white/10 rounded-lg text-white disabled:opacity-50"
              placeholder="Tell us about yourself..."
            />
          </div>

          {/* Hourly Rate & Availability */}
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label className="block text-sm font-medium text-gray-400 mb-2">
                Hourly Rate (USDT)
              </label>
              <input
                type="number"
                value={formData.hourlyRate}
                onChange={(e) => setFormData({ ...formData, hourlyRate: parseFloat(e.target.value) })}
                disabled={!editing}
                className="w-full px-4 py-2 bg-white/5 border border-white/10 rounded-lg text-white disabled:opacity-50"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-400 mb-2">
                Availability
              </label>
              <select
                value={formData.availability}
                onChange={(e) => setFormData({ ...formData, availability: e.target.value })}
                disabled={!editing}
                className="w-full px-4 py-2 bg-white/5 border border-white/10 rounded-lg text-white disabled:opacity-50"
              >
                <option value="AVAILABLE">Available</option>
                <option value="BUSY">Busy</option>
                <option value="UNAVAILABLE">Unavailable</option>
              </select>
            </div>
          </div>

          {/* Links */}
          <div className="space-y-3">
            <h3 className="text-lg font-semibold text-white flex items-center gap-2">
              <Link className="w-5 h-5 text-blue-400" />
              Links
            </h3>

            <div>
              <label className="block text-sm font-medium text-gray-400 mb-2">Portfolio URL</label>
              <input
                type="url"
                value={formData.portfolioUrl}
                onChange={(e) => setFormData({ ...formData, portfolioUrl: e.target.value })}
                disabled={!editing}
                className="w-full px-4 py-2 bg-white/5 border border-white/10 rounded-lg text-white disabled:opacity-50"
                placeholder="https://yourportfolio.com"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-400 mb-2">LinkedIn URL</label>
              <input
                type="url"
                value={formData.linkedinUrl}
                onChange={(e) => setFormData({ ...formData, linkedinUrl: e.target.value })}
                disabled={!editing}
                className="w-full px-4 py-2 bg-white/5 border border-white/10 rounded-lg text-white disabled:opacity-50"
                placeholder="https://linkedin.com/in/yourprofile"
              />
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-400 mb-2">GitHub URL</label>
              <input
                type="url"
                value={formData.githubUrl}
                onChange={(e) => setFormData({ ...formData, githubUrl: e.target.value })}
                disabled={!editing}
                className="w-full px-4 py-2 bg-white/5 border border-white/10 rounded-lg text-white disabled:opacity-50"
                placeholder="https://github.com/yourusername"
              />
            </div>
          </div>
        </div>
      </motion.div>
    </div>
  );
}