"use client"

import { useState, useEffect, useMemo } from "react"
import { motion } from "framer-motion"
import { useNavigate } from "react-router-dom"
import { useAuth } from "../../hooks/useAuth"
import { useWallet, usePoints, useTransactions } from "../../hooks/useApi"
import {
  Building,
  TrendingUp,
  TrendingDown,
  DollarSign,
  Briefcase,
  Users,
  FileText,
  ArrowUpCircle,
  ArrowDownCircle,
  PlusCircle,
  History,
  Clock,
  CheckCircle,
  XCircle,
  Wallet,
  Copy,
  ExternalLink,
} from "lucide-react"

export function EmployerDashboard() {
  const navigate = useNavigate()
  const { user } = useAuth()
  const { data: wallet, loading: walletLoading, refetch: refetchWallet } = useWallet()
  const { data: pointsData, loading: pointsLoading } = usePoints()
  const { data: txs } = useTransactions(0, 5)

  const [stats, setStats] = useState<any>(null)
  const [activeJobs, setActiveJobs] = useState<any[]>([])
  const [recentProjects, setRecentProjects] = useState<any[]>([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    const loadEmployerData = async () => {
      try {
        setLoading(true)
        
        // TODO: Replace with actual API calls when backend is ready
        setStats({
          totalSpent: 0,
          activeJobs: 0,
          totalProjects: 0,
          avgProjectCost: 0,
        })
        
        setActiveJobs([])
        setRecentProjects([])
        
      } catch (error) {
        console.error("Failed to load employer data:", error)
      } finally {
        setLoading(false)
      }
    }

    loadEmployerData()
  }, [])

  const pointsBalance = useMemo(() => {
    const balance = pointsData?.balance?.balance ?? 
                   pointsData?.balance?.points ?? 
                   pointsData?.balance ?? 
                   wallet?.pointsBalance ?? 
                   0
    return Number(balance) || 0
  }, [pointsData, wallet])

  const availablePoints = useMemo(() => {
    const available = pointsData?.balance?.available ?? 
                     wallet?.pointsAvailable ?? 
                     pointsBalance
    return Number(available) || 0
  }, [pointsData, wallet, pointsBalance])

  const lockedPoints = useMemo(() => {
    const locked = pointsData?.balance?.locked ?? 
                  wallet?.pointsLocked ?? 
                  0
    return Number(locked) || 0
  }, [pointsData, wallet])

  // ✅ FIX: Same address resolution logic
  const walletAddress = useMemo(() => {
    const address = wallet?.address || 
                   wallet?.walletAddress || 
                   wallet?.depositAddress ||
                   user?.address || 
                   user?.walletAddress || 
                   ''
    
    console.log('Wallet data:', wallet)
    console.log('Resolved address:', address)
    
    return address
  }, [wallet, user])

  const [copied, setCopied] = useState(false)

  const copyAddress = () => {
    if (walletAddress) {
      navigator.clipboard.writeText(walletAddress)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    }
  }

  const formatPts = (val: number) => {
    return typeof val === 'number' && isFinite(val) ? val.toFixed(2) : '0.00'
  }

  if (loading || walletLoading || pointsLoading) {
    return (
      <div className="min-h-screen bg-gray-900 flex items-center justify-center">
        <div className="animate-spin rounded-full h-16 w-16 border-2 border-transparent border-t-blue-400 border-r-blue-400"></div>
      </div>
    )
  }

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative overflow-hidden rounded-2xl"
      >
        <div className="absolute inset-0 bg-gradient-to-r from-blue-600/90 via-blue-500/90 to-blue-400/90"></div>
        <div className="absolute inset-0 bg-gradient-to-br from-transparent via-blue-400/20 to-blue-600/30"></div>
        <div className="relative z-10 p-8 text-center">
          <div className="flex items-center justify-center gap-3 mb-4">
            <div className="p-3 bg-white/20 rounded-2xl">
              <Building className="w-8 h-8 text-white" />
            </div>
            <h1 className="text-3xl font-bold text-white">Employer Dashboard</h1>
          </div>
          <p className="text-white/90 text-lg">Hire top talent, {user?.fullName || user?.username}!</p>

          {/* Wallet Address */}
          {walletAddress && (
            <div className="mt-4 flex items-center justify-center gap-2">
              <div className="bg-white/20 rounded-lg px-4 py-2 flex items-center gap-2">
                <Wallet className="w-4 h-4 text-white" />
                <span className="text-white font-mono text-sm">
                  {walletAddress.slice(0, 6)}...{walletAddress.slice(-6)}
                </span>
                <button
                  onClick={copyAddress}
                  className="text-white/80 hover:text-white transition-colors"
                >
                  <Copy className="w-4 h-4" />
                </button>
                <a
                  href={`https://nile.tronscan.org/#/address/${walletAddress}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-white/80 hover:text-white transition-colors"
                >
                  <ExternalLink className="w-4 h-4" />
                </a>
              </div>
            </div>
          )}
        </div>
      </motion.div>

      {/* Balance Card */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="ui-card"
      >
        <div className="absolute inset-0 bg-gradient-to-r from-purple-600/90 via-purple-500/90 to-purple-400/90"></div>
        <div className="absolute inset-0 bg-gradient-to-br from-transparent via-purple-400/20 to-purple-600/30"></div>
        <div className="relative z-10 ui-card-body text-center">
          <h2 className="text-3xl md:text-4xl font-bold text-white mb-2">
            {formatPts(availablePoints)} PTS
          </h2>
          <p className="text-white/90 text-lg font-medium mb-1">AVAILABLE BUDGET</p>
          <p className="text-white/80 text-sm">1 point = 1 USDT equivalent</p>
          
          {lockedPoints > 0 && (
            <div className="mt-3 text-white/80 text-sm">
              Locked in escrow: {formatPts(lockedPoints)} PTS
            </div>
          )}
        </div>
      </motion.div>

      {/* Quick Actions */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <ActionCard
          icon={<PlusCircle className="w-6 h-6" />}
          title="Post a Job"
          description="Find freelancers"
          gradient="from-blue-500/20 to-blue-400/10"
          iconColor="text-blue-400"
          onClick={() => navigate("/employer/post-job")}
        />
        <ActionCard
          icon={<ArrowDownCircle className="w-6 h-6" />}
          title="Add Funds"
          description="Deposit USDT"
          gradient="from-green-500/20 to-green-400/10"
          iconColor="text-green-400"
          onClick={() => navigate("/employer/wallet")}
        />
        <ActionCard
          icon={<Briefcase className="w-6 h-6" />}
          title="My Jobs"
          description="Manage postings"
          gradient="from-purple-500/20 to-purple-400/10"
          iconColor="text-purple-400"
          onClick={() => navigate("/employer/my-jobs")}
        />
        <ActionCard
          icon={<FileText className="w-6 h-6" />}
          title="Projects"
          description="Active contracts"
          gradient="from-yellow-500/20 to-yellow-400/10"
          iconColor="text-yellow-400"
          onClick={() => navigate("/employer/my-projects")} // ✅ Updated route
        />
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          icon={<DollarSign className="w-6 h-6 text-purple-400" />}
          title="Total Spent"
          value={`${formatPts(stats?.totalSpent || 0)} PTS`}
          bgGradient="from-purple-500/20"
        />
        <StatCard
          icon={<Briefcase className="w-6 h-6 text-blue-400" />}
          title="Active Jobs"
          value={stats?.activeJobs || 0}
          bgGradient="from-blue-500/20"
        />
        <StatCard
          icon={<CheckCircle className="w-6 h-6 text-green-400" />}
          title="Total Projects"
          value={stats?.totalProjects || 0}
          bgGradient="from-green-500/20"
        />
        <StatCard
          icon={<TrendingUp className="w-6 h-6 text-yellow-400" />}
          title="Avg Project Cost"
          value={`${formatPts(stats?.avgProjectCost || 0)} PTS`}
          bgGradient="from-yellow-500/20"
        />
      </div>

      {/* Active Jobs & Recent Projects */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Active Jobs */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="ui-card"
        >
          <div className="ui-card-header flex justify-between items-center">
            <h2 className="text-xl font-bold text-white flex items-center gap-2">
              <Briefcase className="w-6 h-6 text-blue-400" />
              Active Job Posts
            </h2>
            <button
              onClick={() => navigate("/employer/post-job")}
              className="ui-btn ui-btn-primary ui-btn-sm"
            >
              <PlusCircle className="w-4 h-4" />
              Post Job
            </button>
          </div>
          <div className="ui-card-body">
            {activeJobs.length === 0 ? (
              <div className="text-center py-12 text-gray-400">
                <Briefcase className="w-16 h-16 mx-auto mb-4 opacity-30" />
                <p>No active job posts</p>
                <button
                  onClick={() => navigate("/employer/post-job")}
                  className="mt-4 ui-btn ui-btn-primary"
                >
                  Post Your First Job
                </button>
              </div>
            ) : (
              <div className="space-y-3">
                {activeJobs.map((job: any, idx: number) => (
                  <div
                    key={idx}
                    className="p-4 bg-white/5 rounded-xl border border-white/10 hover:bg-white/10 transition-colors cursor-pointer"
                  >
                    <div className="flex justify-between items-start mb-2">
                      <h3 className="text-white font-semibold">{job.title}</h3>
                      <span className="ui-badge ui-badge-success">Open</span>
                    </div>
                    <p className="text-gray-400 text-sm mb-3 line-clamp-2">{job.description}</p>
                    <div className="flex justify-between items-center text-sm">
                      <span className="text-gray-300">Budget: {job.budget} PTS</span>
                      <span className="text-gray-400">{job.proposalCount} proposals</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </motion.div>

        {/* Recent Projects */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="ui-card"
        >
          <div className="ui-card-header">
            <h2 className="text-xl font-bold text-white flex items-center gap-2">
              <FileText className="w-6 h-6 text-purple-400" />
              Recent Projects
            </h2>
          </div>
          <div className="ui-card-body">
            {recentProjects.length === 0 ? (
              <div className="text-center py-12 text-gray-400">
                <FileText className="w-16 h-16 mx-auto mb-4 opacity-30" />
                <p>No projects yet</p>
                <p className="text-sm mt-2">Post a job and hire freelancers</p>
              </div>
            ) : (
              <div className="space-y-3">
                {recentProjects.map((project: any, idx: number) => (
                  <div
                    key={idx}
                    className="p-4 bg-white/5 rounded-xl border border-white/10"
                  >
                    <div className="flex justify-between items-start mb-2">
                      <h3 className="text-white font-semibold">{project.title}</h3>
                      <span className={`ui-badge ${
                        project.status === 'COMPLETED' ? 'ui-badge-success' :
                        project.status === 'IN_PROGRESS' ? 'ui-badge-warning' :
                        'ui-badge-info'
                      }`}>
                        {project.status}
                      </span>
                    </div>
                    <p className="text-gray-400 text-sm mb-3">Freelancer: {project.freelancerName}</p>
                    <div className="flex justify-between items-center text-sm text-gray-400">
                      <span>Budget: {project.budget} PTS</span>
                      <span>{new Date(project.startedAt).toLocaleDateString()}</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </motion.div>
      </div>

      {/* Recent Transactions */}
      {txs?.content?.length > 0 && (
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          className="ui-card"
        >
          <div className="ui-card-header flex justify-between items-center">
            <h2 className="text-xl font-bold text-white flex items-center gap-2">
              <History className="w-6 h-6 text-gray-400" />
              Recent Transactions
            </h2>
            <button
              onClick={() => navigate("/employer/wallet")}
              className="text-blue-400 hover:text-blue-300 text-sm flex items-center gap-1"
            >
              View All
              <ArrowUpCircle className="w-4 h-4" />
            </button>
          </div>
          <div className="ui-card-body">
            <div className="space-y-3">
              {txs.content.slice(0, 5).map((tx: any, idx: number) => (
                <div
                  key={idx}
                  className="flex justify-between items-center p-3 bg-white/5 rounded-lg border border-white/10"
                >
                  <div className="flex items-center gap-3">
                    {tx.transactionType === 'DEPOSIT' ? (
                      <ArrowDownCircle className="w-5 h-5 text-green-400" />
                    ) : (
                      <ArrowUpCircle className="w-5 h-5 text-red-400" />
                    )}
                    <div>
                      <div className="text-white font-medium">{tx.transactionType}</div>
                      <div className="text-gray-400 text-sm">
                        {new Date(tx.createdAt).toLocaleString()}
                      </div>
                    </div>
                  </div>
                  <div className="text-right">
                    <div className={`font-semibold ${
                      tx.transactionType === 'DEPOSIT' ? 'text-green-400' : 'text-red-400'
                    }`}>
                      {tx.transactionType === 'DEPOSIT' ? '+' : '-'}{tx.amount} USDT
                    </div>
                    <div className="text-gray-400 text-sm">{tx.status}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </motion.div>
      )}
    </div>
  )
}

// Helper Components (same as FreelancerDashboard)
function ActionCard({ icon, title, description, gradient, iconColor, onClick }: any) {
  return (
    <motion.div
      whileHover={{ scale: 1.02 }}
      whileTap={{ scale: 0.98 }}
      onClick={onClick}
      className="relative overflow-hidden rounded-xl border border-white/10 bg-white/5 backdrop-blur-xl cursor-pointer group"
    >
      <div className={`absolute inset-0 bg-gradient-to-br ${gradient}`}></div>
      <div className="relative z-10 p-6">
        <div className={`${iconColor} mb-3 group-hover:scale-110 transition-transform`}>
          {icon}
        </div>
        <h3 className="text-white font-semibold mb-1">{title}</h3>
        <p className="text-gray-400 text-sm">{description}</p>
      </div>
    </motion.div>
  )
}

function StatCard({ icon, title, value, bgGradient }: any) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      className="ui-card"
    >
      <div className={`absolute inset-0 bg-gradient-to-br ${bgGradient} via-transparent to-transparent`}></div>
      <div className="relative z-10 ui-card-body">
        <div className="flex items-center gap-3 mb-3">
          <div className="p-2 bg-white/10 rounded-xl">
            {icon}
          </div>
          <div>
            <div className="text-gray-400 text-sm font-medium">{title}</div>
            <div className="text-white text-xl font-bold">{value}</div>
          </div>
        </div>
      </div>
    </motion.div>
  )
}