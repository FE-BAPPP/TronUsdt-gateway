"use client"

import type React from "react"
import { useMemo, useState } from "react"
import { motion } from "framer-motion"
import { useWallet, useDeposits, useWithdrawals } from "../../hooks/useApi"
import { userApi } from "../../services/api"
import { QRCodeCanvas } from "qrcode.react"
import {
  BarChart3,
  ArrowUpCircle,
  ArrowDownCircle,
  WalletIcon,
  Copy,
  RefreshCw,
  Search,
  AlertCircle,
  CheckCircle,
  Clock,
  X,
  CreditCard,
  Shield,
  Zap
} from "lucide-react"

export function WalletPage() {
  const [activeTab, setActiveTab] = useState("overview")

  const { data: walletData, loading: walletLoading, refetch: refetchWallet } = useWallet()
  const {
    data: depositsData,
    loading: depositsLoading,
    error: depositsError,
    refetch: refetchDeposits,
  } = useDeposits(0, 10)
  const { data: withdrawalsData } = useWithdrawals(0, 10)

  const tabs = [
    { id: "overview", label: "Overview", icon: BarChart3 },
    { id: "deposit", label: "Deposit", icon: ArrowDownCircle },
    { id: "withdraw", label: "Withdraw", icon: ArrowUpCircle },
  ]

  if (walletLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="animate-spin rounded-full h-16 w-16 border-2 border-transparent border-t-yellow-400 border-r-yellow-400"></div>
      </div>
    )
  }

  return (
    <div className="ui-section">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        className="ui-card"
      >
        <div className="ui-card-body text-center">
          <div className="flex items-center justify-center gap-3 mb-4">
            <div className="p-3 bg-yellow-400/20 rounded-2xl">
              <WalletIcon className="w-8 h-8 text-yellow-300" />
            </div>
            <h1 className="text-3xl font-bold text-white">My Wallet</h1>
          </div>
          <p className="text-gray-300">Manage your digital assets securely</p>
        </div>
      </motion.div>

      {/* Balance Card */}
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="ui-card"
      >
        <div className="absolute inset-0 bg-gradient-to-r from-yellow-600/20 via-yellow-500/20 to-yellow-400/20"></div>
        <div className="ui-card-body text-center">
          <div className="mb-6">
            <div className="text-gray-300 text-sm mb-2">Available Balance</div>
            <div className="text-4xl md:text-5xl font-bold text-white mb-2">
              {walletData?.pointsBalance || "0.00"}
            </div>
            <div className="text-yellow-300 font-medium">POINTS</div>
            <div className="text-gray-400 text-sm mt-2">1 point = 1 USDT equivalent</div>
          </div>
          
          <div className="grid grid-cols-3 gap-4">
            <div className="text-center">
              <div className="p-3 bg-green-500/20 rounded-xl mb-2 mx-auto w-fit">
                <Shield className="w-5 h-5 text-green-400" />
              </div>
              <div className="text-xs text-gray-400">Secured</div>
            </div>
            <div className="text-center">
              <div className="p-3 bg-blue-500/20 rounded-xl mb-2 mx-auto w-fit">
                <Zap className="w-5 h-5 text-blue-400" />
              </div>
              <div className="text-xs text-gray-400">Instant</div>
            </div>
            <div className="text-center">
              <div className="p-3 bg-purple-500/20 rounded-xl mb-2 mx-auto w-fit">
                <CreditCard className="w-5 h-5 text-purple-400" />
              </div>
              <div className="text-xs text-gray-400">Verified</div>
            </div>
          </div>
        </div>
      </motion.div>

      {/* Tabs */}
      <motion.div
        initial={{ opacity: 0, y: 16 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2 }}
        className="ui-tabs"
      >
        {tabs.map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id)}
            className={`ui-tab ${activeTab === tab.id ? 'ui-tab-active' : 'ui-tab-inactive'}`}
          >
            <tab.icon className="w-5 h-5" />
            <span className="hidden sm:inline">{tab.label}</span>
          </button>
        ))}
      </motion.div>

      {/* Tab Content */}
      <motion.div
        key={activeTab}
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
      >
        {activeTab === "overview" && <WalletOverview depositsData={depositsData} withdrawalsData={withdrawalsData} />}
        {activeTab === "deposit" && (
          <DepositSection
            depositsData={depositsData}
            loading={depositsLoading}
            error={depositsError}
            onRefresh={refetchDeposits}
          />
        )}
        {activeTab === "withdraw" && (
          <WithdrawSection walletData={walletData} withdrawalsData={withdrawalsData} onSuccess={refetchWallet} />
        )}
      </motion.div>
    </div>
  )
}

function WalletOverview({ depositsData, withdrawalsData }: any) {
  const statusBadge = (status: string) => {
    const s = String(status || '').toLowerCase()
    if (s.includes('completed') || s.includes('success') || s === 'confirmed') {
      return { className: 'status-success', icon: <CheckCircle className="w-3 h-3" />, text: 'COMPLETED' }
    }
    if (s.includes('pending') || s.includes('processing')) {
      return { className: 'status-warning', icon: <Clock className="w-3 h-3" />, text: 'PENDING' }
    }
    if (s.includes('failed') || s.includes('error') || s.includes('cancel')) {
      return { className: 'status-error', icon: <X className="w-3 h-3" />, text: s.includes('cancel') ? 'CANCELED' : 'FAILED' }
    }
    return { className: 'status-info', icon: <Clock className="w-3 h-3" />, text: status || 'UNKNOWN' }
  }

  return (
    <div className="ui-grid-2col">
      {/* Recent Deposits */}
      <div className="ui-card">
        <div className="ui-card-header">
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <ArrowDownCircle className="w-6 h-6 text-green-400" />
            Recent Deposits
          </h2>
        </div>
        <div className="ui-card-body">
          {depositsData?.history?.content?.length > 0 ? (
            <div className="space-y-3">
              {depositsData.history.content.slice(0, 5).map((deposit: any, index: number) => (
                <div key={index} className="flex justify-between items-center p-4 bg-white/5 rounded-xl border border-white/10">
                  <div>
                    <div className="font-semibold text-white">{deposit.amount} USDT</div>
                    <div className="text-sm text-gray-400">{new Date(deposit.createdAt).toLocaleDateString()}</div>
                  </div>
                  <div className={`ui-badge ${statusBadge(deposit.status).className}`}>
                    {statusBadge(deposit.status).icon}
                    {statusBadge(deposit.status).text}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-8">
              <WalletIcon className="w-12 h-12 text-gray-500 mx-auto mb-4" />
              <p className="text-gray-400">No deposits yet</p>
            </div>
          )}
        </div>
      </div>

      {/* Recent Withdrawals */}
      <div className="ui-card">
        <div className="ui-card-header">
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <ArrowUpCircle className="w-6 h-6 text-red-400" />
            Recent Withdrawals
          </h2>
        </div>
        <div className="ui-card-body">
          {withdrawalsData?.history?.content?.length > 0 ? (
            <div className="space-y-3">
              {withdrawalsData.history.content.slice(0, 5).map((withdrawal: any, index: number) => (
                <div key={index} className="flex justify-between items-center p-4 bg-white/5 rounded-xl border border-white/10">
                  <div>
                    <div className="font-semibold text-white">{withdrawal.amount} USDT</div>
                    <div className="text-sm text-gray-400">{new Date(withdrawal.createdAt).toLocaleDateString()}</div>
                  </div>
                  <div className="flex items-center gap-2">
                    <div className={`ui-badge ${statusBadge(withdrawal.status).className}`}>
                      {statusBadge(withdrawal.status).icon}
                      {statusBadge(withdrawal.status).text}
                    </div>
                    {withdrawal.status === "PENDING" && (
                      <button
                        onClick={async () => {
                          const r = await userApi.cancelWithdrawal(withdrawal.id)
                          alert(r.message || "Cancel sent")
                        }}
                        className="ui-btn ui-btn-ghost px-2 py-1 text-xs"
                      >
                        Cancel
                      </button>
                    )}
                  </div>
                </div>
              ))}
            </div>
          ) : (
            <div className="text-center py-8">
              <WalletIcon className="w-12 h-12 text-gray-500 mx-auto mb-4" />
              <p className="text-gray-400">No withdrawals yet</p>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function DepositSection({ depositsData, loading, error, onRefresh }: any) {
  const [txHash, setTxHash] = useState("")
  const [statusResult, setStatusResult] = useState<any>(null)
  const [checking, setChecking] = useState(false)
  
  const address: string = useMemo(() => {
    const raw = depositsData?.address
    if (!raw) return ""
    if (typeof raw === "string") return raw
    return raw.address || raw.depositAddress || raw.usdtAddress || raw.trc20 || raw.tron || ""
  }, [depositsData])

  if (loading) return <div className="text-center py-8 text-gray-400">Loading deposit information...</div>
  if (error) return <div className="text-red-400 text-center py-8">Error: {error}</div>

  const handleCheckStatus = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!txHash) return
    setChecking(true)
    setStatusResult(null)
    try {
      const res = await userApi.checkDepositStatus(txHash.trim())
      setStatusResult(res)
    } finally {
      setChecking(false)
    }
  }

  return (
    <div className="ui-section">
      {/* Deposit Address */}
      <div className="ui-card">
        <div className="ui-card-header">
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <ArrowDownCircle className="w-6 h-6 text-green-400" />
            Deposit USDT (TRC20)
          </h2>
        </div>
        <div className="ui-card-body">
          <div className="ui-grid-2col items-center">
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-300 mb-3">Your Deposit Address</label>
                <div className="p-4 bg-white/10 border border-white/20 rounded-xl">
                  <div className="flex items-center justify-between gap-3">
                    <code className="text-sm break-all flex-1 min-w-0 text-white font-mono">
                      {address || "Loading..."}
                    </code>
                    <div className="flex gap-2 shrink-0">
                      <button
                        onClick={() => address && navigator.clipboard.writeText(address)}
                        disabled={!address}
                        className="ui-btn ui-btn-secondary px-3 py-2 text-xs"
                      >
                        <Copy className="w-3 h-3" />
                        Copy
                      </button>
                      <button
                        onClick={() => onRefresh && onRefresh()}
                        className="ui-btn ui-btn-secondary px-3 py-2 text-xs"
                      >
                        <RefreshCw className="w-3 h-3" />
                        Refresh
                      </button>
                    </div>
                  </div>
                </div>
              </div>

              <div className="ui-card">
                <div className="ui-card-body">
                  <h3 className="font-semibold text-blue-400 mb-3 flex items-center gap-2">
                    <AlertCircle className="w-4 h-4" />
                    Important Notes:
                  </h3>
                  <ul className="text-sm text-blue-300 space-y-2">
                    <li className="flex items-start gap-2">
                      <div className="w-1.5 h-1.5 bg-blue-400 rounded-full mt-2 shrink-0"></div>
                      Only send USDT (TRC20) to this address
                    </li>
                    <li className="flex items-start gap-2">
                      <div className="w-1.5 h-1.5 bg-blue-400 rounded-full mt-2 shrink-0"></div>
                      Minimum deposit: 10 USDT
                    </li>
                    <li className="flex items-start gap-2">
                      <div className="w-1.5 h-1.5 bg-blue-400 rounded-full mt-2 shrink-0"></div>
                      Deposits are credited after 1 confirmation
                    </li>
                  </ul>
                </div>
              </div>
            </div>

            {address && (
              <div className="flex justify-center">
                <div className="ui-card">
                  <div className="ui-card-body text-center">
                    <div className="bg-white p-4 rounded-xl mb-3">
                      <QRCodeCanvas value={address} size={180} includeMargin />
                    </div>
                    <div className="text-sm text-gray-400">Scan to deposit USDT</div>
                  </div>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Pending Deposits */}
      {depositsData?.pending?.length > 0 && (
        <div className="ui-card">
          <div className="ui-card-header">
            <h2 className="text-xl font-bold text-white flex items-center gap-2">
              <Clock className="w-6 h-6 text-yellow-400" />
              Pending Deposits
            </h2>
          </div>
          <div className="ui-card-body">
            <div className="space-y-3">
              {depositsData.pending.map((deposit: any, index: number) => (
                <div key={index} className="flex justify-between items-center p-4 bg-yellow-500/10 border border-yellow-500/30 rounded-xl">
                  <div>
                    <div className="font-semibold text-white">{deposit.amount} USDT</div>
                    <div className="text-sm text-gray-400">Confirmations: {deposit.confirmations}/1</div>
                  </div>
                  <div className="ui-badge status-warning">
                    <Clock className="w-3 h-3" />
                    Pending
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Check Status */}
      <div className="ui-card">
        <div className="ui-card-header">
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <Search className="w-6 h-6 text-blue-400" />
            Check Deposit Status
          </h2>
        </div>
        <div className="ui-card-body">
          <form onSubmit={handleCheckStatus} className="space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Transaction Hash</label>
              <input
                type="text"
                value={txHash}
                onChange={(e) => setTxHash(e.target.value)}
                className="ui-input"
                placeholder="Enter Tron transaction hash"
              />
            </div>
            <button
              type="submit"
              disabled={checking || !txHash}
              className="ui-btn ui-btn-primary py-3 px-6 w-full disabled:opacity-50"
            >
              {checking ? <RefreshCw className="w-4 h-4 animate-spin" /> : <Search className="w-4 h-4" />}
              {checking ? "Checking..." : "Check Status"}
            </button>
          </form>

          {statusResult && (
            <div className="mt-6 p-4 bg-white/5 border border-white/10 rounded-xl">
              <pre className="text-sm overflow-x-auto text-gray-300">
                {JSON.stringify(statusResult.data || statusResult, null, 2)}
              </pre>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

function WithdrawSection({ walletData, withdrawalsData, onSuccess }: any) {
  const [withdrawForm, setWithdrawForm] = useState({ amount: "", toAddress: "" })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")

  const availablePoints = useMemo(() => {
    const balance = walletData?.pointsBalance ?? walletData?.points ?? walletData?.balance
    const n = typeof balance === "number" ? balance : Number(balance || 0)
    return Number.isFinite(n) ? n : 0
  }, [walletData])

  const limits = withdrawalsData?.limits

  const handleWithdraw = async (e: React.FormEvent) => {
    e.preventDefault()
    setError("")

    const amount = parseFloat(withdrawForm.amount)
    if (!withdrawForm.amount || !withdrawForm.toAddress) {
      setError("Please fill in all required fields")
      return
    }

    if (amount < (limits?.minAmount || 10)) {
      setError(`Minimum withdrawal amount is ${limits?.minAmount || 10} PTS`)
      return
    }

    if (amount > (limits?.maxAmount || 10000)) {
      setError(`Maximum withdrawal amount is ${limits?.maxAmount || 10000} PTS`)
      return
    }

    if (amount > availablePoints) {
      setError("Insufficient balance")
      return
    }

    setLoading(true)

    try {
      const response = await userApi.createWithdrawal({
        amount,
        toAddress: withdrawForm.toAddress,
      })

      if (response.success) {
        setWithdrawForm({ amount: "", toAddress: "" })
        onSuccess()
        alert("Withdrawal request submitted successfully")
      } else {
        setError(response.message || "Withdrawal failed")
      }
    } catch (err: any) {
      setError(err.message || "Network error")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="ui-section">
      <div className="ui-card">
        <div className="ui-card-header">
          <h2 className="text-xl font-bold text-white flex items-center gap-2">
            <ArrowUpCircle className="w-6 h-6 text-red-400" />
            Withdraw USDT
          </h2>
        </div>
        <div className="ui-card-body">
          <form onSubmit={handleWithdraw} className="space-y-6">
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Amount (PTS)</label>
              <input
                type="number"
                step="0.01"
                value={withdrawForm.amount}
                onChange={(e) => setWithdrawForm({ ...withdrawForm, amount: e.target.value })}
                className="ui-input"
                placeholder="Enter amount"
              />
              <div className="text-sm text-gray-400 mt-2">Available: {availablePoints.toFixed(2)} PTS</div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">Destination Address</label>
              <input
                type="text"
                value={withdrawForm.toAddress}
                onChange={(e) => setWithdrawForm({ ...withdrawForm, toAddress: e.target.value })}
                className="ui-input"
                placeholder="Enter USDT (TRC20) address"
              />
            </div>

            {error && (
              <div className="ui-card">
                <div className="ui-card-body">
                  <div className="text-red-400 text-sm">{error}</div>
                </div>
              </div>
            )}

            {limits && (
              <div className="ui-card">
                <div className="ui-card-body">
                  <h3 className="font-semibold text-gray-300 mb-3">Withdrawal Limits:</h3>
                  <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 text-sm">
                    <div>
                      <div className="text-gray-400">Minimum</div>
                      <div className="text-white font-medium">{limits.minAmount || 10} PTS</div>
                    </div>
                    <div>
                      <div className="text-gray-400">Maximum</div>
                      <div className="text-white font-medium">{limits.maxAmount || 10000} PTS</div>
                    </div>
                    <div>
                      <div className="text-gray-400">Daily Limit</div>
                      <div className="text-white font-medium">{limits.dailyLimit || 50000} PTS</div>
                    </div>
                  </div>
                </div>
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="ui-btn ui-btn-primary py-4 px-6 w-full text-lg disabled:opacity-50"
            >
              {loading ? "Processing..." : "Submit Withdrawal"}
            </button>
          </form>
        </div>
      </div>
    </div>
  )
}