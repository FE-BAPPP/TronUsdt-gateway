"use client"

import { useEffect, useState } from "react"
import { motion } from "framer-motion"
import { useTransactions, useWithdrawalHistory } from "../../hooks/useApi"
import { userApi, API_BASE_URL, authHelper } from "../../services/api"
import {
  History,
  ArrowUpCircle,
  ArrowDownCircle,
  FileText,
  TrendingUp,
  TrendingDown,
  BarChart3,
  ExternalLink,
  Download,
  CheckCircle,
  Clock,
  XCircle,
  AlertCircle,
  X, // added for modal close
} from "lucide-react"

export function TransactionsPage() {
  const [activeTab, setActiveTab] = useState<"all" | "deposits" | "withdrawals">("all")
  const [currentPage, setCurrentPage] = useState(0)
  const pageSize = 20

  const { data: allTransactions, loading: allLoading, error: allError } = useTransactions(currentPage, pageSize)
  const {
    data: withdrawalHistory,
    loading: withdrawalLoading,
    error: withdrawalError,
  } = useWithdrawalHistory(currentPage, pageSize)
  const [summary, setSummary] = useState<any>(null)
  const [exporting, setExporting] = useState(false)
  const [detailOpen, setDetailOpen] = useState(false)
  const [detailTx, setDetailTx] = useState<any | null>(null)

  useEffect(() => {
    // Load 30-day summary
    ;(async () => {
      const res = await userApi.getTransactionSummary(30)
      if (res.success) setSummary(res.data)
    })()
  }, [])

  const getStatusColor = (status: string) => {
    switch (status) {
      case "CONFIRMED":
        return "bg-green-500/20 text-green-400 border border-green-500/30"
      case "PENDING":
        return "bg-yellow-500/20 text-yellow-400 border border-yellow-500/30"
      case "FAILED":
        return "bg-red-500/20 text-red-400 border border-red-500/30"
      default:
        return "bg-gray-500/20 text-gray-400 border border-gray-500/30"
    }
  }

  const getStatusIcon = (status: string) => {
    switch (status) {
      case "CONFIRMED":
        return <CheckCircle className="w-3 h-3" />
      case "PENDING":
        return <Clock className="w-3 h-3" />
      case "FAILED":
        return <XCircle className="w-3 h-3" />
      default:
        return <AlertCircle className="w-3 h-3" />
    }
  }

  const getTypeIcon = (type: string) => {
    switch (type) {
      case "DEPOSIT":
        return <ArrowDownCircle className="w-5 h-5 text-green-400" />
      case "WITHDRAWAL":
        return <ArrowUpCircle className="w-5 h-5 text-red-400" />
      case "TRANSFER":
        return <History className="w-5 h-5 text-blue-400" />
      case "SWEEP":
        return <BarChart3 className="w-5 h-5 text-purple-400" />
      default:
        return <FileText className="w-5 h-5 text-gray-400" />
    }
  }

  // Simple formatter for points amounts
  const formatPts = (n: number) => {
    const num = typeof n === "number" ? n : Number(n || 0)
    return num.toLocaleString(undefined, { maximumFractionDigits: 6 })
  }

  const renderTransactionTable = (transactions: any[], loading: boolean, error: string | null) => {
    if (loading) {
      return (
        <div className="text-center py-8">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-yellow-400 mx-auto"></div>
          <p className="text-gray-400 mt-2">Loading transactions...</p>
        </div>
      )
    }

    if (error) {
      return (
        <div className="relative overflow-hidden rounded-xl">
          <div className="absolute inset-0 bg-gradient-to-br from-red-500/20 via-red-400/10 to-transparent"></div>
          <div className="absolute inset-0 backdrop-blur-sm border border-red-400/30 rounded-xl"></div>
          <div className="relative z-10 p-4 flex items-center gap-2">
            <AlertCircle className="w-5 h-5 text-red-400" />
            <p className="text-red-400">Error loading transactions: {error}</p>
          </div>
        </div>
      )
    }

    if (!transactions || transactions.length === 0) {
      return (
        <div className="text-center py-8">
          <History className="w-12 h-12 text-gray-500 mx-auto mb-4" />
          <p className="text-gray-400">No transactions found</p>
          <p className="text-gray-500 text-sm">Your transactions will appear here</p>
        </div>
      )
    }

    return (
      <div className="relative overflow-hidden rounded-2xl border border-white/10">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead className="bg-white/5">
              <tr className="border-b border-white/10">
                <th className="text-left py-4 px-6 text-gray-300 font-medium">Type</th>
                <th className="text-left py-4 px-6 text-gray-300 font-medium">Amount</th>
                <th className="text-left py-4 px-6 text-gray-300 font-medium">Status</th>
                <th className="text-left py-4 px-6 text-gray-300 font-medium">Date</th>
                <th className="text-left py-4 px-6 text-gray-300 font-medium">Transaction Hash / Address</th>
              </tr>
            </thead>
            <tbody className="bg-white/5 divide-y divide-white/10">
              {transactions.map((tx: any) => {
                const type = String(tx.transactionType || "WITHDRAWAL").toUpperCase()
                const clickable = type === "WITHDRAWAL"
                return (
                  <tr
                    key={tx.id}
                    className={`hover:bg-white/10 transition-colors ${clickable ? 'cursor-pointer' : ''}`}
                    onClick={() => {
                      if (clickable) {
                        setDetailTx(tx)
                        setDetailOpen(true)
                      }
                    }}
                  >
                    <td className="py-4 px-6">
                      <div className="flex items-center space-x-2">
                        {getTypeIcon(tx.transactionType || "WITHDRAWAL")}
                        <span className="text-white font-medium">{tx.transactionType || "WITHDRAWAL"}</span>
                      </div>
                    </td>
                    <td className="py-4 px-6">
                      <div className="text-white font-bold">
                        {/* Always display the gross requested amount if available */}
                        <span>{typeof tx.amount !== "undefined" ? tx.amount : (typeof tx.netAmount !== "undefined" ? tx.netAmount : 0)} pts</span>
                      </div>
                    </td>
                    <td className="py-4 px-6">
                      <span
                        className={`flex items-center gap-1 px-2 py-1 rounded-full text-xs ${getStatusColor(tx.status || tx.state || "")}`}
                      >
                        {getStatusIcon(tx.status || tx.state || "")}
                        {tx.status || tx.state || ""}
                      </span>
                    </td>
                    <td className="py-4 px-6">
                      <div>
                        <p className="text-white">{new Date(tx.createdAt || tx.time || Date.now()).toLocaleDateString()}</p>
                        <p className="text-gray-400 text-xs">
                          {new Date(tx.createdAt || tx.time || Date.now()).toLocaleTimeString()}
                        </p>
                      </div>
                    </td>
                    <td className="py-4 px-6">
                      {tx.txHash ? (
                        <div className="flex items-center space-x-2">
                          <span className="text-gray-400 font-mono text-xs">
                            {tx.txHash.substring(0, 10)}...{tx.txHash.substring(tx.txHash.length - 10)}
                          </span>
                          <button
                            onClick={(e) => { e.stopPropagation(); window.open(`https://nile.tronscan.org/#/transaction/${tx.txHash}`, "_blank") }}
                            className="flex items-center gap-1 text-yellow-400 hover:text-yellow-300 text-xs transition-colors"
                          >
                            <ExternalLink className="w-3 h-3" />
                            View
                          </button>
                        </div>
                      ) : (
                        <span className="text-gray-500 text-xs">{tx.toAddress || "Pending"}</span>
                      )}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>

        {/* Withdrawal Details Modal */}
        {detailOpen && detailTx && (
          <div className="fixed inset-0 z-40 flex items-center justify-center bg-black/60 p-4">
            <div className="w-full max-w-lg relative overflow-hidden rounded-2xl">
              <div className="absolute inset-0 bg-gradient-to-br from-white/15 via-white/10 to-transparent"></div>
              <div className="absolute inset-0 backdrop-blur-xl border border-white/20 rounded-2xl"></div>
              <div className="relative z-10 p-6">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-white text-lg font-semibold flex items-center gap-2">
                    <ArrowUpCircle className="w-5 h-5 text-red-400" />
                    Withdrawal Details
                  </h3>
                  <button onClick={() => setDetailOpen(false)} className="text-gray-300 hover:text-white">
                    <X className="w-5 h-5" />
                  </button>
                </div>

                {(() => {
                  const fee = typeof detailTx.fee !== 'undefined' ? Number(detailTx.fee) : undefined
                  const net = typeof detailTx.netAmount !== 'undefined' ? Number(detailTx.netAmount) : undefined
                  let amount: number
                  if (typeof detailTx.amount !== 'undefined') amount = Number(detailTx.amount)
                  else if (typeof net === 'number' && typeof fee === 'number') amount = net + fee
                  else amount = Number(net || 0)

                  return (
                    <div className="space-y-3 text-sm">
                      <div className="flex justify-between">
                        <span className="text-gray-400">Requested</span>
                        <span className="text-white font-medium">{amount} pts</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-gray-400">Fee</span>
                        <span className="text-white font-medium">{typeof fee === 'number' ? fee : '-'} pts</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-gray-400">Net received</span>
                        <span className="text-white font-medium">{typeof net === 'number' ? net : (amount && typeof fee === 'number' ? amount - fee : '-')} pts</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-gray-400">Status</span>
                        <span className="text-white font-medium">{detailTx.status || detailTx.state || '-'}</span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-gray-400">Date</span>
                        <span className="text-white font-medium">
                          {new Date(detailTx.createdAt || detailTx.time || Date.now()).toLocaleDateString()} {new Date(detailTx.createdAt || detailTx.time || Date.now()).toLocaleTimeString()}
                        </span>
                      </div>
                      <div className="flex justify-between">
                        <span className="text-gray-400">To Address</span>
                        <span className="text-gray-300 break-all text-right">{detailTx.toAddress || '-'}</span>
                      </div>
                      <div className="flex items-center justify-between">
                        <span className="text-gray-400">Transaction</span>
                        {detailTx.txHash ? (
                          <div className="flex items-center gap-2">
                            <span className="text-gray-400 font-mono text-xs">
                              {detailTx.txHash.substring(0, 10)}...{detailTx.txHash.substring(detailTx.txHash.length - 10)}
                            </span>
                            <button
                              onClick={() => window.open(`https://nile.tronscan.org/#/transaction/${detailTx.txHash}`, '_blank')}
                              className="flex items-center gap-1 text-yellow-400 hover:text-yellow-300 text-xs transition-colors"
                            >
                              <ExternalLink className="w-3 h-3" />
                              View
                            </button>
                          </div>
                        ) : (
                          <span className="text-gray-500 text-xs">Pending</span>
                        )}
                      </div>
                    </div>
                  )
                })()}

                <div className="flex justify-end mt-6">
                  <button onClick={() => setDetailOpen(false)} className="px-4 py-2 bg-white/10 hover:bg-white/20 border border-white/20 text-white rounded-lg">
                    Close
                  </button>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    )
  }

  const daysBack = 30
  const cutoff = Date.now() - daysBack * 24 * 60 * 60 * 1000
  const toNum = (v: any) => (typeof v === "number" ? v : Number(v || 0))
  const sumListByFields = (arr: any[], fields: string[]) =>
    arr.reduce((acc, item) => {
      const val = fields.find((f) => typeof item?.[f] !== "undefined")
      return acc + toNum(val ? item[val as keyof typeof item] : 0)
    }, 0)
  const parseMaybeArraySum = (val: any, preferFields: string[], fallback = 0) => {
    if (typeof val === "number") return val
    if (Array.isArray(val)) {
      if (val.length === 0) return 0
      // If array of numbers
      if (typeof val[0] === "number") return (val as number[]).reduce((a, b) => a + b, 0)
      // If array of objects
      return sumListByFields(val as any[], preferFields)
    }
    return fallback
  }

  // Compute 30d deposits/withdrawals sums
  const depositSumFromSummary = summary
    ? 
      toNum(summary.totalDepositAmount ?? summary.depositSum ?? summary.depositAmount ?? Number.NaN) ||
      // Or sum from an array if provided
      parseMaybeArraySum(summary.deposits, ["amount", "netAmount", "value", "points"], 0)
    : 0
  const withdrawalSumFromSummary = summary
    ? 
      toNum(
        summary.totalWithdrawalAmount ??
          summary.withdrawalSum ??
          summary.withdrawalAmount ??
          summary.totalWithdrawalAmount ??
          Number.NaN,
      ) ||
      // Or sum from an array if provided
      parseMaybeArraySum(summary.withdrawals, ["netAmount", "amount", "value", "points"], 0)
    : 0

  // Local fallback based on currently loaded data
  const localDepositSum = (() => {
    const txs = allTransactions?.content || []
    return txs
      .filter((tx: any) => (tx.transactionType || "").toUpperCase() === "DEPOSIT")
      .filter((tx: any) => new Date(tx.createdAt || tx.time || 0).getTime() >= cutoff)
      .reduce((acc: number, tx: any) => acc + toNum(typeof tx.amount !== "undefined" ? tx.amount : tx.netAmount), 0)
  })()
  const localWithdrawalSum = (() => {
    const list = withdrawalHistory?.content || []
    return list
      .filter((w: any) => new Date(w.createdAt || w.time || 0).getTime() >= cutoff)
      .reduce((acc: number, w: any) => acc + toNum(typeof w.amount !== "undefined" ? w.amount : w.netAmount), 0)
  })()

  // Sums to compute net volume
  const depositsSum30d =
    Number.isFinite(depositSumFromSummary) && depositSumFromSummary > 0 ? depositSumFromSummary : localDepositSum
  const withdrawalsSum30d =
    Number.isFinite(withdrawalSumFromSummary) && withdrawalSumFromSummary > 0
      ? withdrawalSumFromSummary
      : localWithdrawalSum
  const netVolume30d = depositsSum30d - withdrawalsSum30d

  const handleExportCsv = async () => {
    try {
      setExporting(true)
      const token = authHelper.getUserToken()
      const res = await fetch(`${API_BASE_URL}/api/transactions/export?format=csv`, {
        headers: token ? { Authorization: `Bearer ${token}` } : undefined,
      })
      const blob = await res.blob()
      const url = window.URL.createObjectURL(blob)
      const a = document.createElement("a")
      a.href = url
      a.download = "transactions.csv"
      document.body.appendChild(a)
      a.click()
      a.remove()
      window.URL.revokeObjectURL(url)
    } finally {
      setExporting(false)
    }
  }

  return (
    <div className="space-y-6 md:space-y-8">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative overflow-hidden rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl"
      >
        <div className="absolute inset-0 bg-gradient-to-r from-purple-600/20 via-purple-500/20 to-indigo-600/20"></div>
        <div className="relative z-10 p-6 sm:p-8 text-center">
          <div className="flex items-center justify-center gap-3 mb-4">
            <div className="p-3 bg-purple-500/20 rounded-2xl">
              <History className="w-8 h-8 text-purple-300" />
            </div>
            <h1 className="text-3xl font-bold text-white">Transaction History</h1>
          </div>
          <p className="text-white/80">View all your deposits, withdrawals, and transfers</p>
        </div>
      </motion.div>

      {/* Tab Navigation */}
      <div className="flex flex-wrap sm:flex-nowrap gap-2 p-2 bg-white/5 rounded-2xl border border-white/10">
        {[
          { id: "all", label: "All Transactions", icon: FileText },
          { id: "deposits", label: "Deposits", icon: ArrowDownCircle },
          { id: "withdrawals", label: "Withdrawals", icon: ArrowUpCircle },
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setActiveTab(tab.id as any)}
            className={`flex-1 py-3 px-4 rounded-xl transition-all duration-300 font-medium text-sm flex items-center justify-center gap-2 min-w-0 ${
              activeTab === tab.id
                ? "bg-gradient-to-r from-yellow-500/20 to-yellow-400/20 text-yellow-300 border border-yellow-400/30 shadow-lg"
                : "text-gray-300 hover:text-white hover:bg-white/10 border border-transparent"
            }`}
          >
            <tab.icon className="w-4 h-4" />
            <span className="hidden sm:inline">{tab.label}</span>
          </button>
        ))}
      </div>

      {/* Content */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative overflow-hidden rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl"
      >
        <div className="absolute inset-0 bg-gradient-to-br from-white/15 via-white/10 to-transparent"></div>
        <div className="relative z-10 p-6">
          <div className="flex items-center justify-between mb-6">
            <h2 className="text-xl font-semibold text-white flex items-center gap-2">
              <BarChart3 className="w-6 h-6 text-blue-400" />
              Overview
            </h2>
            <button
              onClick={handleExportCsv}
              disabled={exporting}
              className="flex items-center gap-2 px-4 py-2 bg-yellow-600/20 hover:bg-yellow-600/30 border border-yellow-500/30 text-yellow-400 rounded-xl disabled:opacity-50 transition-all duration-300"
            >
              <Download className="w-4 h-4" />
              {exporting ? "Exporting..." : "Export CSV"}
            </button>
          </div>
          {
            // Show cards whenever we can compute something meaningful
            (summary || allTransactions || withdrawalHistory) && (
              <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
                <div className="relative overflow-hidden rounded-2xl border border-green-500/20 bg-green-500/5 backdrop-blur-xl hover:bg-green-500/10 transition-all duration-200">
                  <div className="p-6">
                    <div className="flex items-center gap-3 mb-4">
                      <div className="w-12 h-12 rounded-xl bg-green-500/20 border border-green-500/30 flex items-center justify-center">
                        <TrendingUp className="w-6 h-6 text-green-400" />
                      </div>
                      <div>
                        <p className="text-green-400 text-sm font-medium">Total Deposited</p>
                        <p className="text-green-300/70 text-xs">Last 30 days</p>
                      </div>
                    </div>
                    <div className="text-green-400 text-3xl font-bold">{formatPts(depositsSum30d)}</div>
                    <div className="text-green-300/70 text-sm font-medium">pts</div>
                  </div>
                </div>
                <div className="relative overflow-hidden rounded-2xl border border-red-500/20 bg-red-500/5 backdrop-blur-xl hover:bg-red-500/10 transition-all duration-200">
                  <div className="p-6">
                    <div className="flex items-center gap-3 mb-4">
                      <div className="w-12 h-12 rounded-xl bg-red-500/20 border border-red-500/30 flex items-center justify-center">
                        <TrendingDown className="w-6 h-6 text-red-400" />
                      </div>
                      <div>
                        <p className="text-red-400 text-sm font-medium">Total Withdrawn</p>
                        <p className="text-red-300/70 text-xs">Last 30 days</p>
                      </div>
                    </div>
                    <div className="text-red-400 text-3xl font-bold">{formatPts(withdrawalsSum30d)}</div>
                    <div className="text-red-300/70 text-sm font-medium">pts</div>
                  </div>
                </div>
                <div className="relative overflow-hidden rounded-2xl border border-blue-500/20 bg-blue-500/5 backdrop-blur-xl hover:bg-blue-500/10 transition-all duration-200">
                  <div className="p-6">
                    <div className="flex items-center gap-3 mb-4">
                      <div className="w-12 h-12 rounded-xl bg-blue-500/20 border border-blue-500/30 flex items-center justify-center">
                        <BarChart3 className="w-6 h-6 text-blue-400" />
                      </div>
                      <div>
                        <p className="text-blue-400 text-sm font-medium">Net Volume</p>
                        <p className="text-blue-300/70 text-xs">Last 30 days</p>
                      </div>
                    </div>
                    <div className="text-blue-400 text-3xl font-bold">{formatPts(netVolume30d)}</div>
                    <div className="text-blue-300/70 text-sm font-medium">pts</div>
                  </div>
                </div>
              </div>
            )
          }
          {activeTab === "all" && (
            <>
              <h2 className="text-xl font-semibold text-white mb-4 flex items-center gap-2">
                <FileText className="w-6 h-6 text-gray-400" />
                All Transactions
              </h2>
              {renderTransactionTable(allTransactions?.content || [], allLoading, allError)}
            </>
          )}

          {activeTab === "deposits" && (
            <>
              <h2 className="text-xl font-semibold text-white mb-4 flex items-center gap-2">
                <ArrowDownCircle className="w-6 h-6 text-green-400" />
                Deposit History
              </h2>
              {renderTransactionTable(
                allTransactions?.content?.filter((tx: any) => tx.transactionType === "DEPOSIT") || [],
                allLoading,
                allError,
              )}
            </>
          )}

          {activeTab === "withdrawals" && (
            <>
              <h2 className="text-xl font-semibold text-white mb-4 flex items-center gap-2">
                <ArrowUpCircle className="w-6 h-6 text-red-400" />
                Withdrawal History
              </h2>
              {renderTransactionTable(withdrawalHistory?.content || [], withdrawalLoading, withdrawalError)}
            </>
          )}

          {/* Pagination */}
          {((activeTab === "all" && allTransactions?.totalPages > 1) ||
            (activeTab === "withdrawals" && withdrawalHistory?.totalPages > 1)) && (
            <div className="flex items-center justify-between mt-8 pt-6 border-t border-white/10">
              <button
                onClick={() => setCurrentPage((prev) => Math.max(0, prev - 1))}
                disabled={currentPage === 0}
                className="px-6 py-3 bg-white/10 hover:bg-white/20 border border-white/20 text-white rounded-xl disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200"
              >
                Previous
              </button>

              <span className="text-gray-300 font-medium">
                Page {currentPage + 1} of{" "}
                {activeTab === "withdrawals" ? withdrawalHistory?.totalPages || 1 : allTransactions?.totalPages || 1}
              </span>

              <button
                onClick={() => setCurrentPage((prev) => prev + 1)}
                disabled={
                  currentPage >=
                  (activeTab === "withdrawals"
                    ? (withdrawalHistory?.totalPages || 1) - 1
                    : (allTransactions?.totalPages || 1) - 1)
                }
                className="px-6 py-3 bg-white/10 hover:bg-white/20 border border-white/20 text-white rounded-xl disabled:opacity-50 disabled:cursor-not-allowed transition-all duration-200"
              >
                Next
              </button>
            </div>
          )}
        </div>
      </motion.div>
    </div>
  )
}
