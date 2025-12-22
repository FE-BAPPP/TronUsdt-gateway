"use client"

import { useState, useEffect, useMemo } from "react"
import { motion } from "framer-motion"
import { useNavigate } from "react-router-dom"
import { useAuth } from "../../hooks/useAuth"
import { useWallet, usePoints, useTransactions } from "../../hooks/useApi"
import {
  Briefcase,
  TrendingUp,
  DollarSign,
  Award,
  FileText,
  ArrowUpCircle,
  ArrowDownCircle,
  Wallet,
  History,
  CheckCircle,
  Star,
  Copy,
  ExternalLink,
  FolderOpen,
  User,
} from "lucide-react"

export function FreelancerDashboard() {
  const navigate = useNavigate()
  const { user } = useAuth()
  
  const { data: wallet, loading: walletLoading, refetch: refetchWallet } = useWallet()
  const { data: pointsData, loading: pointsLoading, refetch: refetchPoints } = usePoints()
  const { data: txs } = useTransactions(0, 5)

  const [stats, setStats] = useState<any>(null)
  const [activeProjects, setActiveProjects] = useState<any[]>([])
  const [recentProposals, setRecentProposals] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [copied, setCopied] = useState(false)

  // ✅ FIX: Lấy address từ wallet API response (giống WalletPage)
  const walletAddress = useMemo(() => {
    // Ưu tiên address từ wallet response
    const address = wallet?.address || 
                   wallet?.walletAddress || 
                   wallet?.depositAddress ||
                   user?.address || 
                   user?.walletAddress || 
                   ''
    
    console.log('Wallet data:', wallet) // Debug
    console.log('Resolved address:', address) // Debug
    
    return address
  }, [wallet, user])

  // Load freelancer-specific stats
  useEffect(() => {
    const loadFreelancerData = async () => {
      try {
        setLoading(true)
        
        // TODO: Replace with actual API calls when backend is ready
        setStats({
          totalEarnings: 0,
          activeProjects: 0,
          completedJobs: 0,
          avgRating: 0,
          pendingProposals: 0,
        })
        
        setActiveProjects([])
        setRecentProposals([])
        
      } catch (error) {
        console.error("Failed to load freelancer data:", error)
      } finally {
        setLoading(false)
      }
    }

    loadFreelancerData()
  }, [])

  // Calculate points balance
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

  const formatPts = (val: number) => {
    return typeof val === 'number' && isFinite(val) ? val.toFixed(2) : '0.00'
  }

  const copyAddress = () => {
    if (walletAddress) {
      navigator.clipboard.writeText(walletAddress)
      setCopied(true)
      setTimeout(() => setCopied(false), 2000)
    }
  }

  if (loading || walletLoading || pointsLoading) {
    return (
      <div className="min-h-screen bg-gray-900 flex items-center justify-center">
        <div className="animate-spin rounded-full h-16 w-16 border-2 border-transparent border-t-yellow-400 border-r-yellow-400"></div>
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
        <div className="absolute inset-0 bg-gradient-to-r from-yellow-600/90 via-yellow-500/90 to-yellow-400/90"></div>
        <div className="absolute inset-0 bg-gradient-to-br from-transparent via-yellow-400/20 to-yellow-600/30"></div>
        <div className="relative z-10 p-8 text-center">
          <div className="flex items-center justify-center gap-3 mb-4">
            <div className="p-3 bg-white/20 rounded-2xl">
              <Briefcase className="w-8 h-8 text-white" />
            </div>
            <h1 className="text-3xl font-bold text-black">Freelancer Dashboard</h1>
          </div>
          <p className="text-black/80 text-lg">Welcome back, {user?.fullName || user?.username}!</p>
          
          {/* ✅ ENHANCED WALLET ADDRESS DISPLAY */}
          {walletAddress ? (
            <div className="mt-4 flex items-center justify-center gap-2">
              <div className="bg-black/20 rounded-lg px-4 py-2 flex items-center gap-2 backdrop-blur-sm border border-white/10">
                <Wallet className="w-4 h-4 text-white" />
                <span className="text-white font-mono text-sm">
                  {walletAddress.slice(0, 6)}...{walletAddress.slice(-6)}
                </span>
                <button
                  onClick={copyAddress}
                  className="text-white/80 hover:text-white transition-colors"
                  title={copied ? "Copied!" : "Copy address"}
                >
                  {copied ? (
                    <CheckCircle className="w-4 h-4 text-green-300" />
                  ) : (
                    <Copy className="w-4 h-4" />
                  )}
                </button>
                <a
                  href={`https://nile.tronscan.org/#/address/${walletAddress}`}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-white/80 hover:text-white transition-colors"
                  title="View on Tronscan"
                >
                  <ExternalLink className="w-4 h-4" />
                </a>
              </div>
            </div>
          ) : (
            <div className="mt-4 text-yellow-200 text-sm">
              ⚠️ Wallet address not available
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
        <div className="absolute inset-0 bg-gradient-to-r from-green-600/90 via-green-500/90 to-green-400/90"></div>
        <div className="absolute inset-0 bg-gradient-to-br from-transparent via-green-400/20 to-green-600/30"></div>
        <div className="relative z-10 ui-card-body text-center">
          <h2 className="text-3xl md:text-4xl font-bold text-black mb-2">
            {formatPts(availablePoints)} PTS
          </h2>
          <p className="text-black/80 text-lg font-medium mb-1">AVAILABLE BALANCE</p>
          <p className="text-black/70 text-sm">1 point = 1 USDT equivalent</p>
          
          {lockedPoints > 0 && (
            <div className="mt-3 text-black/70 text-sm">
              Locked: {formatPts(lockedPoints)} PTS
            </div>
          )}
        </div>
      </motion.div>

      {/* Quick Actions */}
      <div className="grid grid-cols-1 md:grid-cols-4 gap-4">
        <ActionCard
          icon={<ArrowDownCircle className="w-6 h-6" />}
          title="Deposit USDT"
          description="Add funds to wallet"
          gradient="from-green-500/20 to-green-400/10"
          iconColor="text-green-400"
          onClick={() => navigate("/freelancer/wallet")}
        />
        <ActionCard
          icon={<ArrowUpCircle className="w-6 h-6" />}
          title="Withdraw"
          description="Cash out earnings"
          gradient="from-red-500/20 to-red-400/10"
          iconColor="text-red-400"
          onClick={() => navigate("/freelancer/wallet")}
        />
        <ActionCard
          icon={<Briefcase className="w-6 h-6" />}
          title="Browse Jobs"
          description="Find new projects"
          gradient="from-blue-500/20 to-blue-400/10"
          iconColor="text-blue-400"
          onClick={() => navigate("/freelancer/jobs")}
        />
        <ActionCard
          icon={<FileText className="w-6 h-6" />}
          title="My Proposals"
          description="Track applications"
          gradient="from-purple-500/20 to-purple-400/10"
          iconColor="text-purple-400"
          onClick={() => navigate("/freelancer/my-proposals")}
        />
        {/* Add "My Projects" action card */}
        <ActionCard
          icon={<FolderOpen className="w-6 h-6" />}
          title="My Projects"
          description="View active projects"
          gradient="from-yellow-500/20 to-yellow-400/10"
          iconColor="text-yellow-400"
          onClick={() => navigate("/freelancer/my-projects")}
        />
        <ActionCard
          icon={<User className="w-6 h-6" />}
          title="My Profile"
          description="Update your professional info"
          gradient="from-green-500/20 to-teal-500/20"
          iconColor="text-green-400"
          onClick={() => navigate('/freelancer/freelancer-profile')}
        />
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <StatCard
          icon={<DollarSign className="w-6 h-6 text-green-400" />}
          title="Total Earnings"
          value={`${formatPts(stats?.totalEarnings || 0)} PTS`}
          bgGradient="from-green-500/20"
        />
        <StatCard
          icon={<Briefcase className="w-6 h-6 text-blue-400" />}
          title="Active Projects"
          value={stats?.activeProjects || 0}
          bgGradient="from-blue-500/20"
        />
        <StatCard
          icon={<CheckCircle className="w-6 h-6 text-purple-400" />}
          title="Completed Jobs"
          value={stats?.completedJobs || 0}
          bgGradient="from-purple-500/20"
        />
        <StatCard
          icon={<Star className="w-6 h-6 text-yellow-400" />}
          title="Average Rating"
          value={stats?.avgRating > 0 ? stats.avgRating.toFixed(1) : "N/A"}
          bgGradient="from-yellow-500/20"
        />
      </div>

      {/* Active Projects & Recent Activity */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        {/* Active Projects */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="ui-card"
        >
          <div className="ui-card-header">
            <h2 className="text-xl font-bold text-white flex items-center gap-2">
              <Briefcase className="w-6 h-6 text-blue-400" />
              Active Projects
            </h2>
          </div>
          <div className="ui-card-body">
            {activeProjects.length === 0 ? (
              <div className="text-center py-12 text-gray-400">
                <Briefcase className="w-16 h-16 mx-auto mb-4 opacity-30" />
                <p>No active projects yet</p>
                <button
                  onClick={() => navigate("/freelancer/jobs")}
                  className="mt-4 ui-btn ui-btn-primary"
                >
                  Browse Available Jobs
                </button>
              </div>
            ) : (
              <div className="space-y-3">
                {activeProjects.map((project: any, idx: number) => (
                  <div
                    key={idx}
                    className="p-4 bg-white/5 rounded-xl border border-white/10 hover:bg-white/10 transition-colors cursor-pointer"
                  >
                    <div className="flex justify-between items-start mb-2">
                      <h3 className="text-white font-semibold">{project.title}</h3>
                      <span className="ui-badge ui-badge-success">Active</span>
                    </div>
                    <p className="text-gray-400 text-sm mb-3">{project.employer}</p>
                    <div className="flex justify-between items-center text-sm">
                      <span className="text-gray-300">Budget: {project.budget} PTS</span>
                      <span className="text-gray-400">Due: {project.deadline}</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </motion.div>

        {/* Recent Proposals */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="ui-card"
        >
          <div className="ui-card-header">
            <h2 className="text-xl font-bold text-white flex items-center gap-2">
              <FileText className="w-6 h-6 text-purple-400" />
              Recent Proposals
            </h2>
          </div>
          <div className="ui-card-body">
            {recentProposals.length === 0 ? (
              <div className="text-center py-12 text-gray-400">
                <FileText className="w-16 h-16 mx-auto mb-4 opacity-30" />
                <p>No proposals yet</p>
                <p className="text-sm mt-2">Start bidding on jobs to grow your business</p>
              </div>
            ) : (
              <div className="space-y-3">
                {recentProposals.map((proposal: any, idx: number) => (
                  <div
                    key={idx}
                    className="p-4 bg-white/5 rounded-xl border border-white/10"
                  >
                    <div className="flex justify-between items-start mb-2">
                      <h3 className="text-white font-semibold">{proposal.jobTitle}</h3>
                      <span className={`ui-badge ${
                        proposal.status === 'ACCEPTED' ? 'ui-badge-success' :
                        proposal.status === 'REJECTED' ? 'ui-badge-error' :
                        'ui-badge-warning'
                      }`}>
                        {proposal.status}
                      </span>
                    </div>
                    <div className="flex justify-between items-center text-sm text-gray-400">
                      <span>Your bid: {proposal.amount} PTS</span>
                      <span>{new Date(proposal.submittedAt).toLocaleDateString()}</span>
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
              onClick={() => navigate("/freelancer/wallet")}
              className="text-yellow-400 hover:text-yellow-300 text-sm flex items-center gap-1"
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

// Helper Components
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