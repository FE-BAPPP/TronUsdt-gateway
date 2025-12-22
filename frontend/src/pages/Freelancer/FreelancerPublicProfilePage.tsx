import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { 
  ArrowLeft, User, Star, Briefcase, DollarSign, 
  Clock, Award, ExternalLink, Github, Linkedin, 
  Globe, Mail, MapPin, Calendar
} from 'lucide-react';
import { userApi } from '../../services/api';

export function FreelancerPublicProfilePage() {
  const { freelancerId } = useParams<{ freelancerId: string }>();
  const navigate = useNavigate();

  const [profile, setProfile] = useState<any>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (freelancerId) {
      loadProfile();
    }
  }, [freelancerId]);

  const loadProfile = async () => {
    try {
      setLoading(true);
      const response = await userApi.getFreelancerProfileById(freelancerId!);
      
      console.log('📋 Freelancer Profile Response:', response);
      
      const data = response.data || response;
      setProfile(data);
    } catch (error) {
      console.error('Failed to load freelancer profile:', error);
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

  if (!profile) {
    return (
      <div className="p-6 text-center">
        <p className="text-gray-400">Profile not found</p>
        <button
          onClick={() => navigate(-1)}
          className="mt-4 text-yellow-400 hover:text-yellow-300"
        >
          Go Back
        </button>
      </div>
    );
  }

  return (
    <div className="p-6 space-y-6 max-w-5xl mx-auto">
      {/* Back Button */}
      <button
        onClick={() => navigate(-1)}
        className="flex items-center gap-2 text-gray-400 hover:text-white transition-colors"
      >
        <ArrowLeft className="w-4 h-4" />
        Back
      </button>

      {/* Profile Header */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative overflow-hidden rounded-2xl border border-white/10 bg-gradient-to-br from-purple-500/10 via-blue-500/10 to-transparent backdrop-blur-sm p-8"
      >
        <div className="flex items-start gap-6">
          {/* Avatar */}
          <div className="relative">
            <div className="w-32 h-32 rounded-full bg-gradient-to-br from-purple-500 to-blue-500 p-1">
              <div className="w-full h-full rounded-full bg-gray-900 flex items-center justify-center">
                {profile.avatar ? (
                  <img 
                    src={profile.avatar} 
                    alt={profile.userName}
                    className="w-full h-full rounded-full object-cover"
                  />
                ) : (
                  <User className="w-16 h-16 text-gray-400" />
                )}
              </div>
            </div>
            
            {/* Availability Badge */}
            <div className={`absolute bottom-2 right-2 w-6 h-6 rounded-full border-4 border-gray-900 ${
              profile.availability === 'AVAILABLE' ? 'bg-green-500' : 
              profile.availability === 'BUSY' ? 'bg-yellow-500' : 'bg-red-500'
            }`} />
          </div>

          {/* Info */}
          <div className="flex-1">
            <h1 className="text-3xl font-bold text-white mb-2">
              {profile.userName || 'Freelancer'}
            </h1>
            
            {profile.professionalTitle && (
              <p className="text-xl text-blue-400 mb-4">
                {profile.professionalTitle}
              </p>
            )}

            {/* Stats Row */}
            <div className="flex items-center gap-6 mb-4">
              <div className="flex items-center gap-2">
                <Star className="w-5 h-5 text-yellow-400" />
                <span className="text-white font-semibold">
                  {profile.avgRating ? profile.avgRating.toFixed(1) : '0.0'}
                </span>
                <span className="text-gray-400 text-sm">rating</span>
              </div>

              <div className="flex items-center gap-2">
                <Briefcase className="w-5 h-5 text-green-400" />
                <span className="text-white font-semibold">
                  {profile.jobsCompleted || 0}
                </span>
                <span className="text-gray-400 text-sm">jobs completed</span>
              </div>

              <div className="flex items-center gap-2">
                <DollarSign className="w-5 h-5 text-purple-400" />
                <span className="text-white font-semibold">
                  ${profile.totalEarnings || 0}
                </span>
                <span className="text-gray-400 text-sm">earned</span>
              </div>
            </div>

            {/* Availability Status */}
            <div className="flex items-center gap-2">
              <Clock className="w-4 h-4 text-gray-400" />
              <span className={`text-sm font-medium ${
                profile.availability === 'AVAILABLE' ? 'text-green-400' :
                profile.availability === 'BUSY' ? 'text-yellow-400' : 'text-red-400'
              }`}>
                {profile.availability === 'AVAILABLE' ? '✓ Available for work' :
                 profile.availability === 'BUSY' ? '⚠ Currently busy' : '✗ Not available'}
              </span>
            </div>
          </div>

          {/* Hourly Rate */}
          {profile.hourlyRate && (
            <div className="text-right">
              <p className="text-gray-400 text-sm mb-1">Hourly Rate</p>
              <p className="text-3xl font-bold text-yellow-400">
                ${profile.hourlyRate}
                <span className="text-sm text-gray-400">/hr</span>
              </p>
            </div>
          )}
        </div>
      </motion.div>

      {/* Bio Section */}
      {profile.bio && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="ui-card"
        >
          <div className="ui-card-header">
            <h2 className="text-xl font-bold text-white">About</h2>
          </div>
          <div className="ui-card-body">
            <p className="text-gray-300 leading-relaxed whitespace-pre-wrap">
              {profile.bio}
            </p>
          </div>
        </motion.div>
      )}

      {/* Links & Contact */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2 }}
        className="ui-card"
      >
        <div className="ui-card-header">
          <h2 className="text-xl font-bold text-white">Links & Contact</h2>
        </div>
        <div className="ui-card-body">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {profile.userEmail && (
              <div className="flex items-center gap-3 p-3 bg-white/5 rounded-lg">
                <Mail className="w-5 h-5 text-blue-400" />
                <div>
                  <p className="text-gray-400 text-xs">Email</p>
                  <p className="text-white">{profile.userEmail}</p>
                </div>
              </div>
            )}

            {profile.portfolioUrl && (
              <a
                href={profile.portfolioUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-3 p-3 bg-white/5 rounded-lg hover:bg-white/10 transition-colors group"
              >
                <Globe className="w-5 h-5 text-purple-400" />
                <div className="flex-1">
                  <p className="text-gray-400 text-xs">Portfolio</p>
                  <p className="text-white group-hover:text-purple-400 transition-colors">
                    View Portfolio
                  </p>
                </div>
                <ExternalLink className="w-4 h-4 text-gray-400 group-hover:text-purple-400" />
              </a>
            )}

            {profile.githubUrl && (
              <a
                href={profile.githubUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-3 p-3 bg-white/5 rounded-lg hover:bg-white/10 transition-colors group"
              >
                <Github className="w-5 h-5 text-gray-400" />
                <div className="flex-1">
                  <p className="text-gray-400 text-xs">GitHub</p>
                  <p className="text-white group-hover:text-gray-300 transition-colors">
                    View GitHub
                  </p>
                </div>
                <ExternalLink className="w-4 h-4 text-gray-400" />
              </a>
            )}

            {profile.linkedinUrl && (
              <a
                href={profile.linkedinUrl}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center gap-3 p-3 bg-white/5 rounded-lg hover:bg-white/10 transition-colors group"
              >
                <Linkedin className="w-5 h-5 text-blue-500" />
                <div className="flex-1">
                  <p className="text-gray-400 text-xs">LinkedIn</p>
                  <p className="text-white group-hover:text-blue-400 transition-colors">
                    View LinkedIn
                  </p>
                </div>
                <ExternalLink className="w-4 h-4 text-gray-400" />
              </a>
            )}
          </div>
        </div>
      </motion.div>

      {/* Work History Stats */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.3 }}
        className="ui-card"
      >
        <div className="ui-card-header">
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <Award className="w-6 h-6 text-yellow-400" />
            Work History
          </h2>
        </div>
        <div className="ui-card-body">
          <div className="grid grid-cols-3 gap-6 text-center">
            <div className="bg-white/5 rounded-xl p-6">
              <div className="text-4xl font-bold text-green-400 mb-2">
                {profile.jobsCompleted || 0}
              </div>
              <div className="text-gray-400">Jobs Completed</div>
            </div>

            <div className="bg-white/5 rounded-xl p-6">
              <div className="text-4xl font-bold text-purple-400 mb-2">
                ${profile.totalEarnings || 0}
              </div>
              <div className="text-gray-400">Total Earned</div>
            </div>

            <div className="bg-white/5 rounded-xl p-6">
              <div className="text-4xl font-bold text-yellow-400 mb-2">
                {profile.activeProjects || 0}
              </div>
              <div className="text-gray-400">Active Projects</div>
            </div>
          </div>
        </div>
      </motion.div>

      {/* Member Since */}
      {profile.createdAt && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          className="text-center text-gray-400 text-sm flex items-center justify-center gap-2"
        >
          <Calendar className="w-4 h-4" />
          <span>
            Member since {new Date(profile.createdAt).toLocaleDateString('en-US', { 
              month: 'long', 
              year: 'numeric' 
            })}
          </span>
        </motion.div>
      )}
    </div>
  );
}