"use client"

import type React from "react"
import { useState } from "react"
import { motion } from "framer-motion"
import { usePoints } from "../../hooks/useApi"
import { userApi } from "../../services/api"
import { useAuth } from "../../hooks/useAuth"
import {
  ArrowRightLeft,
  History,
  Send,
  Receipt as Receive,
  User,
  DollarSign,
  MessageSquare,
  AlertCircle,
  Clock,
  RefreshCw,
  Wallet,
} from "lucide-react"

export function P2PPage() {
  const { data: pointsData, loading, error, refetch } = usePoints()
  const [activeTab, setActiveTab] = useState<"transfer" | "history">("transfer")
  const { user } = useAuth()

  const [transferForm, setTransferForm] = useState({
    toUserId: "",
    amount: "",
    description: "",
  })
  const [transferLoading, setTransferLoading] = useState(false)
  const [transferError, setTransferError] = useState<string | null>(null)

  const handleTransfer = async (e: React.FormEvent) => {
    e.preventDefault()
    setTransferError(null)
    setTransferLoading(true)

    try {
      const response = await userApi.transferPoints({
        toUserId: transferForm.toUserId,
        amount: Number.parseFloat(transferForm.amount),
        description: transferForm.description || undefined,
      })

      if (response.success) {
        setTransferForm({ toUserId: "", amount: "", description: "" })
        refetch() 
        alert("Points transferred successfully!")
      } else {
        setTransferError(response.message || "Transfer failed")
      }
    } catch (err: any) {
      setTransferError(err.message || "Transfer failed")
    } finally {
      setTransferLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-96">
        <div className="animate-spin rounded-full h-12 w-12 border-2 border-transparent border-t-yellow-400 border-r-yellow-400"></div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="relative overflow-hidden rounded-2xl">
        <div className="absolute inset-0 bg-gradient-to-br from-red-500/20 via-red-400/10 to-transparent"></div>
        <div className="absolute inset-0 backdrop-blur-sm border border-red-400/30 rounded-2xl"></div>
        <div className="relative z-10 p-4">
          <p className="text-red-400">Error loading points data: {error}</p>
          <button
            onClick={refetch}
            className="mt-2 flex items-center gap-2 px-4 py-2 bg-red-600/20 hover:bg-red-600/30 border border-red-500/30 text-red-400 rounded transition-all duration-300"
          >
            <RefreshCw className="w-4 h-4" />
            Retry
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-6 md:space-y-8">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative overflow-hidden rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl"
      >
        <div className="absolute inset-0 bg-gradient-to-r from-yellow-600/20 via-yellow-500/20 to-yellow-400/20"></div>
        <div className="relative z-10 p-6 sm:p-8 text-center">
          <div className="flex items-center justify-center gap-3 mb-4">
            <div className="p-3 bg-yellow-500/20 rounded-2xl">
              <ArrowRightLeft className="w-8 h-8 text-yellow-300" />
            </div>
            <h1 className="text-3xl font-bold text-white">Points Transfer</h1>
          </div>
          <p className="text-white/80">Send points to other users instantly</p>
        </div>
      </motion.div>

      {/* Balance Display */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative overflow-hidden rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl"
      >
        <div className="absolute inset-0 bg-gradient-to-r from-yellow-600/20 via-yellow-500/15 to-yellow-400/20"></div>
        <div className="relative z-10 p-6 sm:p-8 text-center">
          <div className="flex items-center justify-center gap-2 text-yellow-300 text-sm mb-2">
            <Wallet className="w-5 h-5" />
            Your Points Balance
          </div>
          <div className="text-white text-3xl font-bold mb-2">{pointsData?.balance?.balance || 0}</div>
          <div className="text-yellow-200/70 text-sm">Available for transfer</div>
        </div>
      </motion.div>

      {/* Tabs */}
      <div className="flex flex-wrap sm:flex-nowrap gap-2 p-2 bg-white/5 rounded-2xl border border-white/10">
        {[
          { id: "transfer", label: "Send Points", icon: Send },
          { id: "history", label: "Transfer History", icon: History },
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

      {/* Tab Content */}
      {activeTab === "transfer" && (
        <motion.div 
          initial={{ opacity: 0 }} 
          animate={{ opacity: 1 }} 
          className="relative overflow-hidden rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl"
        >
          <div className="absolute inset-0 bg-gradient-to-br from-white/15 via-white/10 to-transparent"></div>
          <div className="relative z-10 p-6">
            <h2 className="text-xl font-semibold text-white mb-6 flex items-center gap-2">
              <Send className="w-6 h-6 text-yellow-400" />
              Transfer Points to User
            </h2>

            <form onSubmit={handleTransfer} className="space-y-6">
              <div>
                <label className="block text-gray-300 text-sm font-medium mb-2 flex items-center gap-2">
                  <User className="w-4 h-4" />
                  Recipient User ID
                </label>
                <input
                  type="text"
                  value={transferForm.toUserId}
                  onChange={(e) => setTransferForm((prev) => ({ ...prev, toUserId: e.target.value }))}
                  className="w-full p-4 bg-white/10 border border-white/20 rounded-xl text-white placeholder-gray-400 focus:outline-none focus:border-yellow-400/50 focus:bg-white/15 transition-all duration-300"
                  placeholder="Enter user ID"
                  required
                />
              </div>

              <div>
                <label className="block text-gray-300 text-sm font-medium mb-2 flex items-center gap-2">
                  <DollarSign className="w-4 h-4" />
                  Amount (Points)
                </label>
                <input
                  type="number"
                  min="1"
                  max={pointsData?.balance?.balance || 0}
                  value={transferForm.amount}
                  onChange={(e) => setTransferForm((prev) => ({ ...prev, amount: e.target.value }))}
                  className="w-full p-4 bg-white/10 border border-white/20 rounded-xl text-white placeholder-gray-400 focus:outline-none focus:border-yellow-400/50 focus:bg-white/15 transition-all duration-300"
                  placeholder="Enter amount"
                  required
                />
                <p className="text-gray-400 text-sm mt-1">Available: {pointsData?.balance?.balance || 0} points</p>
              </div>

              <div>
                <label className="block text-gray-300 text-sm font-medium mb-2 flex items-center gap-2">
                  <MessageSquare className="w-4 h-4" />
                  Description (Optional)
                </label>
                <textarea
                  value={transferForm.description}
                  onChange={(e) => setTransferForm((prev) => ({ ...prev, description: e.target.value }))}
                  className="w-full p-4 bg-white/10 border border-white/20 rounded-xl text-white placeholder-gray-400 focus:outline-none focus:border-yellow-400/50 focus:bg-white/15 transition-all duration-300"
                  placeholder="Add a note for this transfer"
                  rows={3}
                />
              </div>

              {transferError && (
                <div className="relative overflow-hidden rounded-xl border border-red-500/30 bg-red-500/10">
                  <div className="p-4 flex items-center gap-2">
                    <AlertCircle className="w-4 h-4 text-red-400" />
                    <p className="text-red-400 text-sm">{transferError}</p>
                  </div>
                </div>
              )}

              <div className="relative overflow-hidden rounded-xl">
                <div className="absolute inset-0 bg-gradient-to-br from-blue-500/10 via-blue-400/5 to-transparent"></div>
                <div className="absolute inset-0 bg-gradient-to-br from-transparent via-blue-400/20 to-blue-500/30"></div>
                <div className="relative z-10 p-4">
                  <h4 className="text-blue-400 font-medium mb-2 flex items-center gap-2">
                    <AlertCircle className="w-4 h-4" />
                    Transfer Information:
                  </h4>
                  <ul className="text-blue-300 text-sm space-y-1">
                    <li>• Minimum transfer: 1 point</li>
                    <li>• Transfer fee: None</li>
                    <li>• Processing time: Instant</li>
                    <li>• Transfers are irreversible</li>
                  </ul>
                </div>
              </div>

              <button
                type="submit"
                disabled={transferLoading || !transferForm.toUserId || !transferForm.amount}
                className="w-full relative overflow-hidden rounded-xl py-3 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <div className="absolute inset-0 bg-gradient-to-r from-yellow-600 via-yellow-500 to-yellow-400"></div>
                <div className="absolute inset-0 bg-gradient-to-br from-transparent via-yellow-400/20 to-yellow-600/30"></div>
                <div className="relative z-10 text-black font-medium">
                  {transferLoading ? (
                    <div className="flex items-center justify-center gap-2">
                      <RefreshCw className="w-4 h-4 animate-spin" />
                      Processing...
                    </div>
                  ) : (
                    <div className="flex items-center justify-center gap-2">
                      <Send className="w-4 h-4" />
                      Transfer Points
                    </div>
                  )}
                </div>
              </button>
            </form>
          </div>
        </motion.div>
      )}

      {activeTab === "history" && (
        <motion.div 
          initial={{ opacity: 0 }} 
          animate={{ opacity: 1 }} 
          className="relative overflow-hidden rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl"
        >
          <div className="absolute inset-0 bg-gradient-to-br from-white/15 via-white/10 to-transparent"></div>
          <div className="relative z-10 p-6">
            <h2 className="text-xl font-semibold text-white mb-6 flex items-center gap-2">
              <History className="w-6 h-6 text-blue-400" />
              Transfer History
            </h2>

            <div className="space-y-4">
              {pointsData?.history?.content?.length > 0 ? (
                pointsData.history.content.map((t: any) => {
                  const pick = (...keys: string[]) => {
                    for (const k of keys) {
                      const v = t?.[k]
                      if (v !== undefined && v !== null && v !== "") return v
                    }
                    return undefined
                  }

                  const toDisplay = (val: any): string | undefined => {
                    if (val === undefined || val === null) return undefined
                    if (typeof val === "string" || typeof val === "number") return String(val)
                    if (typeof val === "object") {
                      const userLike = val as any
                      const cand =
                        userLike.username ||
                        userLike.name ||
                        userLike.email ||
                        userLike.id ||
                        userLike.userId ||
                        userLike.user_id
                      if (cand !== undefined && cand !== null && cand !== "") return String(cand)
                    }
                    return undefined
                  }

                  const currentUserId = user?.id ? String(user.id) : undefined
                  const txType = String(
                    pick("transactionType", "transaction_type", "type", "direction") || "",
                  ).toUpperCase()
                  const ledgerUserId = pick("userId", "user_id")
                  const fromIdRaw = pick("fromUserId", "from_user_id")
                  const toIdRaw = pick("toUserId", "to_user_id")
                  const fromId = fromIdRaw != null ? String(fromIdRaw) : undefined
                  const toId = toIdRaw != null ? String(toIdRaw) : undefined
                  const amountNum =
                    typeof t.amount === "number" ? t.amount : Number(pick("amount", "points", "value") || 0)

                  let isOutgoing: boolean | undefined
                  if (currentUserId) {
                    if (fromId === currentUserId) {
                      isOutgoing = true
                    } else if (toId === currentUserId) {
                      isOutgoing = false
                    }
                  }
                  if (isOutgoing === undefined) {
                    if (txType === "P2P_SEND" || txType === "SEND" || txType === "OUT" || txType === "DEBIT")
                      isOutgoing = true
                    if (txType === "P2P_RECEIVE" || txType === "RECEIVE" || txType === "IN" || txType === "CREDIT")
                      isOutgoing = false
                  }
                  if (isOutgoing === undefined && currentUserId && ledgerUserId === currentUserId) {
                    const entry = String(pick("entry", "entryType", "entry_type", "direction") || "").toUpperCase()
                    if (["DEBIT", "OUT", "SEND"].includes(entry)) isOutgoing = true
                    if (["CREDIT", "IN", "RECEIVE"].includes(entry)) isOutgoing = false
                  }
                  if (isOutgoing === undefined) isOutgoing = false

                  let otherRaw = isOutgoing
                    ? toId || pick("toUsername", "to_username", "toUser", "to_user")
                    : fromId || pick("fromUsername", "from_username", "fromUser", "from_user")
                  let showName = toDisplay(otherRaw)

                  if (!showName) {
                    otherRaw = pick(
                      "counterpartyId",
                      "counterparty_id",
                      "counterparty",
                      "peerUserId",
                      "peer_user_id",
                      "peer",
                      "otherUserId",
                      "other_user_id",
                      "otherUser",
                      "other_user",
                      "targetUserId",
                      "target_user_id",
                      "sourceUserId",
                      "source_user_id",
                    )
                    showName = toDisplay(otherRaw)
                  }

                  if (!showName) {
                    const candidates = [fromId, toId, ledgerUserId].filter(Boolean).map(String)
                    const unique = Array.from(new Set(candidates))
                    const other = currentUserId ? unique.find((id) => id !== currentUserId) : unique[0]
                    if (other) showName = String(other)
                  }
                  const absAmount = Math.abs(amountNum || t.points || t.value || 0)

                  const full = (v: string | undefined) => v ?? ""

                  const fromLabel =
                    toDisplay(fromId) ||
                    toDisplay(pick("fromUsername", "from_username", "fromUser", "from_user")) ||
                    "N/A"
                  const toLabel =
                    toDisplay(toId) || toDisplay(pick("toUsername", "to_username", "toUser", "to_user")) || "N/A"

                  return (
                    <div
                      key={t.id || `${t.createdAt}-${absAmount}-${fromId || ""}-${toId || ""}`}
                      className="relative overflow-hidden rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl hover:bg-white/10 transition-all duration-200"
                    >
                      <div className="p-6">
                        <div className="flex items-center justify-between">
                          <div className="space-y-2">
                            <div className="flex items-center gap-3">
                              <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${
                                isOutgoing ? "bg-red-500/20 border border-red-500/30" : "bg-green-500/20 border border-green-500/30"
                              }`}>
                                {isOutgoing ? (
                                  <Send className="w-5 h-5 text-red-400" />
                                ) : (
                                  <Receive className="w-5 h-5 text-green-400" />
                                )}
                              </div>
                              <div>
                                <p className="text-white font-semibold text-base">
                                  {isOutgoing ? "Sent to" : "Received from"} {full(showName || "ID unavailable")}
                                </p>
                                <p className="text-gray-400 text-sm">
                                  {pick("note", "description") ||
                                    (isOutgoing ? "P2P transfer sent" : "P2P transfer received")}
                                </p>
                              </div>
                            </div>
                            <div className="pl-13 space-y-1">
                              <p className="text-gray-500 text-sm">
                                From: {full(String(fromLabel))} | To: {full(String(toLabel))}
                              </p>
                              <p className="text-gray-500 text-sm flex items-center gap-2">
                                <Clock className="w-4 h-4" />
                                {new Date(t.createdAt || t.time || Date.now()).toLocaleString()}
                              </p>
                            </div>
                          </div>
                          <div className="text-right">
                            <p className={`font-bold text-2xl ${isOutgoing ? "text-red-400" : "text-green-400"}`}>
                              {isOutgoing ? "-" : "+"}
                              {absAmount}
                            </p>
                            <p className="text-gray-400 text-sm font-medium">points</p>
                          </div>
                        </div>
                      </div>
                    </div>
                  )
                })
              ) : (
                <div className="text-center py-16">
                  <div className="w-20 h-20 rounded-2xl bg-gray-500/20 border border-gray-500/30 flex items-center justify-center mx-auto mb-6">
                    <History className="w-10 h-10 text-gray-400" />
                  </div>
                  <h3 className="text-white font-semibold text-lg mb-2">No transfer history</h3>
                  <p className="text-gray-400">Your point transfers will appear here</p>
                </div>
              )}
            </div>
          </div>
        </motion.div>
      )}
    </div>
  )
}
