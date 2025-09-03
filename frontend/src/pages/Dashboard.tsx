"use client"

import { useEffect, useMemo, useState } from "react"
import { motion } from "framer-motion"
import { userApi } from "../services/api"
import { useWallet, useTransactions, useWithdrawalHistory } from "../hooks/useApi"
import { useNavigate } from "react-router-dom"
import { 
  Wallet, 
  ArrowUpCircle, 
  ArrowDownCircle, 
  History, 
  TrendingUp, 
  TrendingDown, 
  BarChart3,
  CreditCard,
  Users,
  ArrowRight
} from "lucide-react"

export function Dashboard() {
  const { data: walletData, loading, error } = useWallet()
  const [summary, setSummary] = useState<any>(null)
  const navigate = useNavigate()
  const pageSize = 100
  const { data: txs } = useTransactions(0, pageSize)
  const { data: withdraws } = useWithdrawalHistory(0, pageSize)

  useEffect(() => {
    ;(async () => {
      const res = await userApi.getTransactionSummary(30)
      if (res.success) setSummary(res.data)
    })()
  }, [])

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-96">
        <div className="animate-spin rounded-full h-12 w-12 border-2 border-transparent border-t-yellow-400 border-r-yellow-400"></div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex items-center justify-center min-h-96">
        <div className="text-red-400">Error: {error}</div>
      </div>
    )
  }

  return <UserDashboard wallet={walletData} summary={summary} onNavigate={navigate} txs={txs} withdraws={withdraws} />
}

function UserDashboard({
  wallet,
  summary,
  onNavigate,
  txs,
  withdraws,
}: { wallet: any; summary: any; onNavigate: ReturnType<typeof useNavigate>; txs: any; withdraws: any }) {
  
  const pointsBalance = useMemo(() => {
    const pb = wallet?.pointsBalance ?? wallet?.points ?? wallet?.balance
    const n = typeof pb === "number" ? pb : Number(pb || 0)
    return Number.isFinite(n) ? n : 0
  }, [wallet])

  const toNum = (v: any) => (typeof v === "number" ? v : Number(v || 0))
  const pickNumber = (obj: any, keys: string[]) => {
    if (!obj) return Number.NaN
    for (const k of keys) {
      if (Object.prototype.hasOwnProperty.call(obj, k) && obj[k] != null) {
        const n = Number(obj[k])
        if (Number.isFinite(n)) return n
      }
    }
    return Number.NaN
  }

  const sumArrayByFields = (arr: any, fields: string[]) => {
    const list = Array.isArray(arr) ? arr : (arr?.content ?? arr?.items ?? arr?.records ?? arr?.data ?? null)
    if (!Array.isArray(list)) return Number.NaN
    return list.reduce((acc, item) => {
      for (const f of fields) {
        if (typeof item?.[f] !== "undefined") {
          const n = Number(item[f])
          return acc + (Number.isFinite(n) ? n : 0)
        }
      }
      const n = Number(item)
      return acc + (Number.isFinite(n) ? n : 0)
    }, 0)
  }

  const getTime = (obj: any) => {
    const t = obj?.createdAt ?? obj?.created_at ?? obj?.time ?? obj?.timestamp ?? obj?.date
    const n = typeof t === "number" ? t : Date.parse(t || "")
    return Number.isFinite(n) ? n : 0
  }

  const getType = (obj: any) => String(obj?.transactionType ?? obj?.type ?? obj?.kind ?? "").toUpperCase()
  const daysBack = 30
  const cutoff = useMemo(() => Date.now() - daysBack * 24 * 60 * 60 * 1000, [])

  const localDepositsSum = useMemo(() => {
    const content = txs?.content || []
    return content
      .filter((t: any) => getType(t).includes("DEPOSIT"))
      .filter((t: any) => getTime(t) >= cutoff)
      .reduce((acc: number, t: any) => acc + toNum(typeof t.amount !== "undefined" ? t.amount : t.netAmount), 0)
  }, [txs, cutoff])

  const localWithdrawSum = useMemo(() => {
    const list = withdraws?.content || []
    return list
      .filter((w: any) => getTime(w) >= cutoff)
      .reduce((acc: number, w: any) => acc + toNum(typeof w.netAmount !== "undefined" ? w.netAmount : w.amount), 0)
  }, [withdraws, cutoff])

  const depositsSum = useMemo(() => {
    let v = pickNumber(summary, ["depositsSum", "totalDeposited", "totalDepositAmount", "depositsTotal", "depositSum"])
    if (Number.isFinite(v)) return v
    v = sumArrayByFields(summary?.deposits ?? summary?.depositList ?? summary?.depositHistory ?? summary?.deposits30d, [
      "amount", "netAmount", "points", "value", "total",
    ])
    if (Number.isFinite(v)) return v
    return localDepositsSum
  }, [summary, localDepositsSum])

  const withdrawalsSum = useMemo(() => {
    let v = pickNumber(summary, ["withdrawalsSum", "totalWithdrawn", "totalWithdrawalAmount", "withdrawalsTotal", "withdrawalSum"])
    if (Number.isFinite(v)) return v
    v = sumArrayByFields(summary?.withdrawals ?? summary?.withdrawalList ?? summary?.withdrawalHistory ?? summary?.withdrawals30d, [
      "amount", "netAmount", "points", "value", "total",
    ])
    if (Number.isFinite(v)) return v
    return localWithdrawSum
  }, [summary, localWithdrawSum])

  const netVolume = useMemo(() => depositsSum - withdrawalsSum, [depositsSum, withdrawalsSum])

  const formatPts = (n: number) => {
    if (!Number.isFinite(n)) return "0.00"
    const abs = Math.abs(n)
    const formatter = new Intl.NumberFormat(undefined, {
      minimumFractionDigits: abs > 0 && abs < 1 ? 4 : 2,
      maximumFractionDigits: 6,
    })
    return formatter.format(n)
  }

  const netColor = netVolume > 0 ? "text-green-400" : netVolume < 0 ? "text-red-400" : "text-gray-300"

  return (
    <div className="ui-section">
      {/* Welcome Hero */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="ui-card"
      >
        <div className="absolute inset-0 bg-gradient-to-r from-yellow-600/90 via-yellow-500/90 to-yellow-400/90"></div>
        <div className="absolute inset-0 bg-gradient-to-br from-transparent via-yellow-400/20 to-yellow-600/30"></div>
        <div className="relative z-10 ui-card-body text-center">
          <h1 className="text-3xl md:text-4xl font-bold text-black mb-2">
            {formatPts(pointsBalance)} PTS
          </h1>
          <p className="text-black/80 text-lg font-medium mb-1">TOTAL BALANCE</p>
          <p className="text-black/70 text-sm">1 point = 1 USDT equivalent</p>
        </div>
      </motion.div>

      {/* KPI Cards */}
      <div className="ui-grid-kpi">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.1 }}
          className="ui-card"
        >
          <div className="absolute inset-0 bg-gradient-to-br from-green-500/20 via-green-400/10 to-transparent"></div>
          <div className="ui-card-body">
            <div className="flex items-center gap-3 mb-3">
              <div className="p-2 bg-green-500/20 rounded-xl">
                <TrendingUp className="w-6 h-6 text-green-400" />
              </div>
              <div>
                <div className="text-green-400 text-sm font-medium">Deposited (30d)</div>
                <div className="text-white text-xl font-bold">{formatPts(depositsSum)} PTS</div>
              </div>
            </div>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.2 }}
          className="ui-card"
        >
          <div className="absolute inset-0 bg-gradient-to-br from-red-500/20 via-red-400/10 to-transparent"></div>
          <div className="ui-card-body">
            <div className="flex items-center gap-3 mb-3">
              <div className="p-2 bg-red-500/20 rounded-xl">
                <TrendingDown className="w-6 h-6 text-red-400" />
              </div>
              <div>
                <div className="text-red-400 text-sm font-medium">Withdrawn (30d)</div>
                <div className="text-white text-xl font-bold">{formatPts(withdrawalsSum)} PTS</div>
              </div>
            </div>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.3 }}
          className="ui-card"
        >
          <div className="absolute inset-0 bg-gradient-to-br from-purple-500/20 via-purple-400/10 to-transparent"></div>
          <div className="ui-card-body">
            <div className="flex items-center gap-3 mb-3">
              <div className="p-2 bg-purple-500/20 rounded-xl">
                <BarChart3 className="w-6 h-6 text-purple-400" />
              </div>
              <div>
                <div className="text-purple-400 text-sm font-medium">Net Volume (30d)</div>
                <div className={`text-xl font-bold ${netColor}`}>{formatPts(netVolume)} PTS</div>
              </div>
            </div>
          </div>
        </motion.div>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          className="ui-card"
        >
          <div className="absolute inset-0 bg-gradient-to-br from-blue-500/20 via-blue-400/10 to-transparent"></div>
          <div className="ui-card-body">
            <div className="flex items-center gap-3 mb-3">
              <div className="p-2 bg-blue-500/20 rounded-xl">
                <CreditCard className="w-6 h-6 text-blue-400" />
              </div>
              <div>
                <div className="text-blue-400 text-sm font-medium">Total Transactions</div>
                <div className="text-white text-xl font-bold">{txs?.totalElements || 0}</div>
              </div>
            </div>
          </div>
        </motion.div>
      </div>

      {/* Quick Actions */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.5 }}
        className="ui-card"
      >
        <div className="ui-card-header">
          <h2 className="text-2xl font-bold text-white text-center">Quick Actions</h2>
        </div>
        <div className="ui-card-body">
          <div className="ui-grid-responsive">
            <ActionCard
              icon={<ArrowDownCircle className="w-8 h-8" />}
              title="Deposit"
              description="Add funds to wallet"
              gradient="from-green-500/20 to-emerald-500/20"
              iconColor="text-green-400"
              onClick={() => onNavigate("/user/wallet")}
            />
            <ActionCard
              icon={<ArrowUpCircle className="w-8 h-8" />}
              title="Withdraw"
              description="Send funds out"
              gradient="from-red-500/20 to-rose-500/20"
              iconColor="text-red-400"
              onClick={() => onNavigate("/user/wallet")}
            />
            <ActionCard
              icon={<Users className="w-8 h-8" />}
              title="Transfer"
              description="Send to other users"
              gradient="from-purple-500/20 to-violet-500/20"
              iconColor="text-purple-400"
              onClick={() => onNavigate("/user/p2p")}
            />
            <ActionCard
              icon={<History className="w-8 h-8" />}
              title="History"
              description="View transactions"
              gradient="from-blue-500/20 to-cyan-500/20"
              iconColor="text-blue-400"
              onClick={() => onNavigate("/user/transactions")}
            />
          </div>
        </div>
      </motion.div>
    </div>
  )
}

function ActionCard({ 
  icon, 
  title, 
  description, 
  gradient, 
  iconColor, 
  onClick 
}: {
  icon: React.ReactNode
  title: string
  description: string
  gradient: string
  iconColor: string
  onClick: () => void
}) {
  return (
    <button
      onClick={onClick}
      className="group relative overflow-hidden rounded-2xl p-6 transition-all duration-300 hover:scale-105 hover:shadow-2xl text-left"
    >
      <div className={`absolute inset-0 bg-gradient-to-br ${gradient} group-hover:opacity-80 transition-opacity`}></div>
      <div className="absolute inset-0 backdrop-blur-sm border border-white/20 rounded-2xl group-hover:border-white/40 transition-colors"></div>
      <div className="relative z-10">
        <div className={`${iconColor} mb-4 group-hover:scale-110 transition-transform`}>
          {icon}
        </div>
        <div className="text-white font-semibold mb-1 group-hover:text-yellow-300 transition-colors">
          {title}
        </div>
        <div className="text-gray-400 text-sm mb-3">{description}</div>
        <div className="flex items-center text-gray-300 group-hover:text-white transition-colors">
          <span className="text-sm">Get started</span>
          <ArrowRight className="w-4 h-4 ml-2 group-hover:translate-x-1 transition-transform" />
        </div>
      </div>
    </button>
  )
}