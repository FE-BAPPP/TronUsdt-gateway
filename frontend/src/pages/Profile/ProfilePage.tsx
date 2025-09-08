"use client"

import { useEffect, useState } from "react"
import { motion } from "framer-motion"
import { useAuth } from "../../hooks/useAuth"
import { userApi } from "../../services/api"
import { QRCodeCanvas } from "qrcode.react"
import {
  User,
  Edit3,
  Copy,
  Check,
  Mail,
  Calendar,
  Shield,
  Key,
  Smartphone,
  LogOut,
  Trash2,
  UserCircle,
  AlertTriangle,
} from "lucide-react"

export function ProfilePage() {
  const { user, logout } = useAuth()
  const [isEditing, setIsEditing] = useState(false)
  const [copied, setCopied] = useState(false)
  const [profileData, setProfileData] = useState({
    fullName: user?.fullName || "",
    email: user?.email || "",
  })

  // 2FA state
  const [twoFAEnabled, setTwoFAEnabled] = useState<boolean>(false)
  const [setupModal, setSetupModal] = useState(false)
  const [otpauthUrl, setOtpauthUrl] = useState<string>("")
  const [setupCode, setSetupCode] = useState("")
  const [disableModal, setDisableModal] = useState(false)
  const [disablePwd, setDisablePwd] = useState("")
  const [disableCode, setDisableCode] = useState("")
  const [secMsg, setSecMsg] = useState<string | null>(null)

  useEffect(() => {
    // fetch profile to get 2FA flag
    ;(async () => {
      try {
        const res = await userApi.getProfile()
        if (res.success) {
          const p: any = res.data || {}
          setTwoFAEnabled(!!p.twoFactorEnabled)
        }
      } catch {}
    })()
  }, [])

  const handleSave = async () => {
    try {
      const res = await userApi.updateProfile({
        fullName: profileData.fullName,
        email: profileData.email,
      })
      if (!res.success) throw new Error(res.message || "Update failed")
      setIsEditing(false)
      alert("Profile updated successfully!")
    } catch (e: any) {
      alert(e.message || "Failed to update profile")
    }
  }

  const handleLogout = () => {
    if (confirm("Are you sure you want to logout?")) {
      logout()
    }
  }

  const startSetup2FA = async () => {
    setSecMsg(null)
    const r = await userApi.startTwoFactorSetup()
    if (!r.success) {
      setSecMsg(r.message || "Failed to start 2FA setup")
      return
    }
    const data: any = r.data || {}
    setOtpauthUrl(data.otpauthUrl || "")
    setSetupCode("")
    setSetupModal(true)
  }

  const confirmEnable2FA = async () => {
    if (!setupCode || setupCode.trim().length !== 6) {
      setSecMsg("Enter 6-digit code from Google Authenticator")
      return
    }
    const r = await userApi.enableTwoFactor(setupCode.trim())
    if (!r.success) {
      setSecMsg(r.message || "Failed to enable 2FA")
      return
    }
    setTwoFAEnabled(true)
    setSetupModal(false)
    alert("Two-factor authentication enabled")
  }

  const confirmDisable2FA = async () => {
    if (!disablePwd) {
      setSecMsg("Password is required to disable 2FA")
      return
    }
    const r = await userApi.disableTwoFactor(disablePwd, disableCode?.trim() || undefined)
    if (!r.success) {
      setSecMsg(r.message || "Failed to disable 2FA")
      return
    }
    setTwoFAEnabled(false)
    setDisableModal(false)
    setDisablePwd("")
    setDisableCode("")
    alert("Two-factor authentication disabled")
  }

  return (
    <div className="space-y-6 md:space-y-8">
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative overflow-hidden rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl"
      >
        <div className="absolute inset-0 bg-gradient-to-r from-indigo-600/20 via-purple-600/20 to-indigo-700/20"></div>
        <div className="relative z-10 p-6 sm:p-8 text-center">
          <div className="flex items-center justify-center gap-3 mb-4">
            <div className="p-3 bg-indigo-500/20 rounded-2xl">
              <UserCircle className="w-8 h-8 text-indigo-300" />
            </div>
            <h1 className="text-3xl font-bold text-white">Profile</h1>
          </div>
          <p className="text-white/80">Manage your account information</p>
        </div>
      </motion.div>

      {/* Profile Information */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative overflow-hidden rounded-2xl border border-white/10 bg-white/5 backdrop-blur-xl"
      >
        <div className="p-6">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-xl font-semibold text-white flex items-center gap-2">
              <User className="w-6 h-6 text-blue-400" />
              Account Information
            </h2>
            <button
              onClick={() => setIsEditing(!isEditing)}
              className="flex items-center gap-2 px-4 py-2 bg-yellow-600/20 hover:bg-yellow-600/30 border border-yellow-500/30 text-yellow-400 rounded-lg transition-all duration-300"
            >
              <Edit3 className="w-4 h-4" />
              {isEditing ? "Cancel" : "Edit Profile"}
            </button>
          </div>

          <div className="space-y-4">
            <div>
              <label className="block text-gray-300 text-sm font-medium mb-2 flex items-center gap-2">
                <User className="w-4 h-4" />
                Username
              </label>
              <input
                type="text"
                value={user?.username || ""}
                disabled
                className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-gray-400 cursor-not-allowed backdrop-blur-sm"
              />
              <p className="text-gray-500 text-xs mt-1">Username cannot be changed</p>
            </div>

            {/* User ID for P2P transfers */}
            <div>
              <label className="block text-gray-300 text-sm font-medium mb-2 flex items-center gap-2">
                <Shield className="w-4 h-4" />
                User ID
              </label>
              <div className="relative">
                <input
                  type="text"
                  value={user?.id || ""}
                  disabled
                  className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-gray-300 pr-28 cursor-not-allowed font-mono backdrop-blur-sm"
                />
                <button
                  type="button"
                  onClick={async () => {
                    if (!user?.id) return
                    try {
                      await navigator.clipboard.writeText(user.id)
                      setCopied(true)
                      setTimeout(() => setCopied(false), 2000)
                    } catch {}
                  }}
                  className="absolute right-2 top-1/2 -translate-y-1/2 flex items-center gap-1 px-3 py-2 bg-white/10 hover:bg-white/20 border border-white/20 text-white rounded-md disabled:opacity-50 transition-all duration-300"
                  disabled={!user?.id}
                >
                  {copied ? <Check className="w-3 h-3" /> : <Copy className="w-3 h-3" />}
                  {copied ? "Copied!" : "Copy ID"}
                </button>
              </div>
              <p className="text-gray-500 text-xs mt-1">Share this ID to receive points transfers.</p>
            </div>

            <div>
              <label className="block text-gray-300 text-sm font-medium mb-2 flex items-center gap-2">
                <UserCircle className="w-4 h-4" />
                Full Name
              </label>
              <input
                type="text"
                value={profileData.fullName}
                onChange={(e) => setProfileData((prev) => ({ ...prev, fullName: e.target.value }))}
                disabled={!isEditing}
                className={`w-full px-4 py-3 border rounded-lg backdrop-blur-sm ${
                  isEditing
                    ? "bg-white/10 border-white/20 text-white focus:outline-none focus:border-yellow-400/50"
                    : "bg-white/10 border-white/20 text-gray-400 cursor-not-allowed"
                }`}
              />
            </div>

            <div>
              <label className="block text-gray-300 text-sm font-medium mb-2 flex items-center gap-2">
                <Mail className="w-4 h-4" />
                Email Address
              </label>
              <input
                type="email"
                value={profileData.email}
                onChange={(e) => setProfileData((prev) => ({ ...prev, email: e.target.value }))}
                disabled={!isEditing}
                className={`w-full px-4 py-3 border rounded-lg backdrop-blur-sm ${
                  isEditing
                    ? "bg-white/10 border-white/20 text-white focus:outline-none focus:border-yellow-400/50"
                    : "bg-white/10 border-white/20 text-gray-400 cursor-not-allowed"
                }`}
              />
            </div>

            <div>
              <label className="block text-gray-300 text-sm font-medium mb-2 flex items-center gap-2">
                <Shield className="w-4 h-4" />
                Account Role
              </label>
              <div className="flex items-center">
                <span
                  className={`flex items-center gap-1 px-3 py-1 rounded-full text-sm font-medium ${
                    user?.role === "ADMIN"
                      ? "bg-red-500/20 text-red-400 border border-red-500/30"
                      : "bg-blue-500/20 text-blue-400 border border-blue-500/30"
                  }`}
                >
                  <Shield className="w-3 h-3" />
                  {user?.role}
                </span>
              </div>
            </div>

            <div>
              <label className="block text-gray-300 text-sm font-medium mb-2 flex items-center gap-2">
                <Calendar className="w-4 h-4" />
                Member Since
              </label>
              <input
                type="text"
                value={user?.createdAt ? new Date(user.createdAt).toLocaleDateString() : "Unknown"}
                disabled
                className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-gray-400 cursor-not-allowed backdrop-blur-sm"
              />
            </div>

            {isEditing && (
              <div className="flex space-x-4">
                <button
                  onClick={handleSave}
                  className="flex items-center gap-2 px-6 py-2 bg-green-600/20 hover:bg-green-600/30 border border-green-500/30 text-green-400 rounded-lg transition-all duration-300"
                >
                  <Check className="w-4 h-4" />
                  Save Changes
                </button>
                <button
                  onClick={() => setIsEditing(false)}
                  className="flex items-center gap-2 px-6 py-2 bg-gray-600/20 hover:bg-gray-600/30 border border-gray-500/30 text-gray-400 rounded-lg transition-all duration-300"
                >
                  Cancel
                </button>
              </div>
            )}
          </div>
        </div>
      </motion.div>

      {/* Security Section */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.1 }}
        className="relative overflow-hidden rounded-2xl"
      >
        <div className="absolute inset-0 bg-gradient-to-br from-white/15 via-white/10 to-transparent"></div>
        <div className="absolute inset-0 backdrop-blur-sm border border-white/30 rounded-2xl"></div>
        <div className="relative z-10 p-6">
          <h2 className="text-xl font-semibold text-white mb-4 flex items-center gap-2">
            <Shield className="w-6 h-6 text-green-400" />
            Security
          </h2>
          <div className="space-y-4">
            <button className="w-full p-4 bg-white/10 hover:bg-white/20 border border-white/20 rounded-lg text-left transition-all duration-300 backdrop-blur-sm">
              <div className="flex justify-between items-center">
                <div className="flex items-center gap-3">
                  <Key className="w-5 h-5 text-yellow-400" />
                  <div>
                    <h3 className="text-white font-medium">Change Password</h3>
                    <p className="text-gray-400 text-sm">Update your account password</p>
                  </div>
                </div>
                <span className="text-gray-400">→</span>
              </div>
            </button>

            <div className="w-full p-4 bg-white/10 border border-white/20 rounded-lg backdrop-blur-sm">
              <div className="flex justify-between items-center">
                <div className="flex items-center gap-3">
                  <Smartphone className="w-5 h-5 text-blue-400" />
                  <div>
                    <h3 className="text-white font-medium">Two-Factor Authentication</h3>
                    <p className="text-gray-400 text-sm">{twoFAEnabled ? 'Enabled' : 'Disabled'}</p>
                  </div>
                </div>
                {twoFAEnabled ? (
                  <button
                    onClick={() => { setDisableModal(true); setSecMsg(null); }}
                    className="px-4 py-2 bg-red-600/20 hover:bg-red-600/30 border border-red-500/30 text-red-400 rounded-lg"
                  >
                    Disable
                  </button>
                ) : (
                  <button
                    onClick={startSetup2FA}
                    className="px-4 py-2 bg-blue-600/20 hover:bg-blue-600/30 border border-blue-500/30 text-blue-400 rounded-lg"
                  >
                    Enable
                  </button>
                )}
              </div>
              {secMsg && <div className="text-sm text-red-400 mt-2">{secMsg}</div>}
            </div>
          </div>
        </div>
      </motion.div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ delay: 0.2 }}
        className="relative overflow-hidden rounded-2xl"
      >
        <div className="absolute inset-0 bg-gradient-to-br from-red-500/20 via-red-400/10 to-transparent"></div>
        <div className="absolute inset-0 backdrop-blur-sm border border-red-400/30 rounded-2xl"></div>
        <div className="relative z-10 p-6">
          <h2 className="text-xl font-semibold text-red-400 mb-4 flex items-center gap-2">
            <AlertTriangle className="w-6 h-6" />
            Danger Zone
          </h2>
          <div className="space-y-4">
            <button
              onClick={handleLogout}
              className="flex items-center gap-2 px-6 py-2 bg-red-600/20 hover:bg-red-600/30 border border-red-500/30 text-red-400 rounded-lg transition-all duration-300"
            >
              <LogOut className="w-4 h-4" />
              Logout
            </button>
            <button className="flex items-center gap-2 px-6 py-2 bg-red-800/20 hover:bg-red-800/30 border border-red-700/30 text-red-400 rounded-lg transition-all duration-300 ml-4">
              <Trash2 className="w-4 h-4" />
              Delete Account
            </button>
          </div>
          <p className="text-red-300 text-sm mt-2 flex items-center gap-2">
            <AlertTriangle className="w-4 h-4" />
            These actions cannot be undone. Please be careful.
          </p>
        </div>
      </motion.div>

      {/* 2FA Setup Modal */}
      {setupModal && (
        <div className="fixed inset-0 z-30 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-md relative overflow-hidden rounded-2xl">
            <div className="absolute inset-0 bg-gradient-to-br from-white/15 via-white/10 to-transparent"></div>
            <div className="absolute inset-0 backdrop-blur-xl border border-white/20 rounded-2xl"></div>
            <div className="relative z-10 p-6">
              <h3 className="text-white text-lg font-semibold mb-2">Enable Two-Factor Authentication</h3>
              <p className="text-gray-400 text-sm mb-4">Scan the QR with Google Authenticator, then enter the 6-digit code.</p>
              <div className="bg-white p-3 rounded-xl mb-3 w-fit mx-auto">
                {otpauthUrl ? <QRCodeCanvas value={otpauthUrl} size={170} includeMargin /> : null}
              </div>
              <input
                type="text"
                value={setupCode}
                onChange={(e) => setSetupCode(e.target.value)}
                className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-yellow-400/50"
                placeholder="Enter 6-digit code"
                maxLength={6}
                inputMode="numeric"
                pattern="[0-9]*"
              />
              <div className="flex justify-end gap-2 mt-4">
                <button onClick={() => setSetupModal(false)} className="px-4 py-2 text-gray-300 hover:text-white">Cancel</button>
                <button onClick={confirmEnable2FA} className="px-4 py-2 bg-yellow-500 text-black rounded-lg">Enable</button>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* 2FA Disable Modal */}
      {disableModal && (
        <div className="fixed inset-0 z-30 flex items-center justify-center bg-black/60 p-4">
          <div className="w-full max-w-md relative overflow-hidden rounded-2xl">
            <div className="absolute inset-0 bg-gradient-to-br from-white/15 via-white/10 to-transparent"></div>
            <div className="absolute inset-0 backdrop-blur-xl border border-white/20 rounded-2xl"></div>
            <div className="relative z-10 p-6">
              <h3 className="text-white text-lg font-semibold mb-2">Disable Two-Factor Authentication</h3>
              <p className="text-gray-400 text-sm mb-4">Enter your password and, if requested, a 2FA code.</p>
              <input
                type="password"
                value={disablePwd}
                onChange={(e) => setDisablePwd(e.target.value)}
                className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-yellow-400/50 mb-3"
                placeholder="Password"
              />
              <input
                type="text"
                value={disableCode}
                onChange={(e) => setDisableCode(e.target.value)}
                className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-yellow-400/50"
                placeholder="2FA code (optional)"
                maxLength={6}
                inputMode="numeric"
                pattern="[0-9]*"
              />
              {secMsg && <div className="text-sm text-red-400 mt-2">{secMsg}</div>}
              <div className="flex justify-end gap-2 mt-4">
                <button onClick={() => setDisableModal(false)} className="px-4 py-2 text-gray-300 hover:text-white">Cancel</button>
                <button onClick={confirmDisable2FA} className="px-4 py-2 bg-red-500 text-black rounded-lg">Disable</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
