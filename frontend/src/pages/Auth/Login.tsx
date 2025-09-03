"use client"

import type React from "react"
import { useState, useEffect } from "react"
import { useNavigate, Link } from "react-router-dom"
import { motion } from "framer-motion"
import { useAuth } from "../../hooks/useAuth"
import { LogIn, User, Lock, Eye, EyeOff, AlertCircle, Loader2 } from "lucide-react"

export function Login() {
  const navigate = useNavigate()
  const { login, isLoggedIn, isUser, isAdmin } = useAuth()

  const [formData, setFormData] = useState({
    username: "",
    password: "",
  })
  const [showPassword, setShowPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")

  useEffect(() => {
    if (isLoggedIn) {
      if (isAdmin) {
        navigate("/admin/dashboard", { replace: true })
      } else if (isUser) {
        navigate("/user/dashboard", { replace: true })
      }
    }
  }, [isLoggedIn, isAdmin, isUser, navigate])

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError("")
    setLoading(true)

    try {
      await login(formData.username, formData.password)
      // Navigation will be handled by useEffect
    } catch (err: any) {
      setError(err.message || "Login failed")
    } finally {
      setLoading(false)
    }
  }

  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    })
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 flex items-center justify-center px-4">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -right-40 w-80 h-80 bg-purple-500 rounded-full mix-blend-multiply filter blur-xl opacity-20 animate-pulse"></div>
        <div className="absolute -bottom-40 -left-40 w-80 h-80 bg-indigo-500 rounded-full mix-blend-multiply filter blur-xl opacity-20 animate-pulse animation-delay-2000"></div>
        <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-violet-500 rounded-full mix-blend-multiply filter blur-xl opacity-10 animate-pulse animation-delay-4000"></div>
      </div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="w-full max-w-md relative z-10"
      >
        <div className="relative overflow-hidden rounded-2xl">
          <div className="absolute inset-0 bg-gradient-to-br from-white/15 via-white/10 to-transparent"></div>
          <div className="absolute inset-0 backdrop-blur-xl border border-white/20 rounded-2xl shadow-2xl shadow-purple-900/50"></div>
          <div className="relative z-10 p-8">
            <div className="text-center mb-8">
              <div className="flex items-center justify-center gap-3 mb-4">
                <LogIn className="w-8 h-8 text-yellow-400" />
                <h1 className="text-3xl font-bold text-white">Welcome Back</h1>
              </div>
              <p className="text-gray-300">Sign in to your account</p>
            </div>

            <form onSubmit={handleSubmit} className="space-y-6">
              <div>
                <label
                  htmlFor="username"
                  className="block text-sm font-medium text-gray-300 mb-2 flex items-center gap-2"
                >
                  <User className="w-4 h-4" />
                  Username
                </label>
                <input
                  id="username"
                  name="username"
                  type="text"
                  required
                  value={formData.username}
                  onChange={handleInputChange}
                  className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-yellow-400/50 backdrop-blur-sm transition-all duration-300"
                  placeholder="Enter your username"
                />
              </div>

              <div>
                <label
                  htmlFor="password"
                  className="block text-sm font-medium text-gray-300 mb-2 flex items-center gap-2"
                >
                  <Lock className="w-4 h-4" />
                  Password
                </label>
                <div className="relative">
                  <input
                    id="password"
                    name="password"
                    type={showPassword ? "text" : "password"}
                    required
                    value={formData.password}
                    onChange={handleInputChange}
                    className="w-full px-4 py-3 bg-white/10 border border-white/20 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:border-yellow-400/50 backdrop-blur-sm transition-all duration-300 pr-12"
                    placeholder="Enter your password"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-300 transition-colors"
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              {error && (
                <motion.div
                  initial={{ opacity: 0, y: -10 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="relative overflow-hidden rounded-xl"
                >
                  <div className="absolute inset-0 bg-gradient-to-br from-red-500/20 via-red-400/10 to-transparent"></div>
                  <div className="absolute inset-0 backdrop-blur-sm border border-red-400/30 rounded-xl"></div>
                  <div className="relative z-10 p-3 flex items-center gap-2">
                    <AlertCircle className="w-4 h-4 text-red-400" />
                    <p className="text-red-400 text-sm">{error}</p>
                  </div>
                </motion.div>
              )}

              <button
                type="submit"
                disabled={loading}
                className="w-full relative overflow-hidden rounded-xl py-3 px-4 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <div className="absolute inset-0 bg-gradient-to-r from-yellow-600 via-yellow-500 to-yellow-400"></div>
                <div className="absolute inset-0 bg-gradient-to-br from-transparent via-yellow-400/20 to-yellow-600/30"></div>
                <div className="relative z-10 text-black font-medium">
                  {loading ? (
                    <span className="flex items-center justify-center gap-2">
                      <Loader2 className="w-4 h-4 animate-spin" />
                      Signing in...
                    </span>
                  ) : (
                    <span className="flex items-center justify-center gap-2">
                      <LogIn className="w-4 h-4" />
                      Sign in
                    </span>
                  )}
                </div>
              </button>
            </form>

            <div className="mt-6 text-center">
              <p className="text-gray-300">
                Don't have an account?{" "}
                <Link to="/register" className="text-yellow-400 hover:text-yellow-300 font-medium transition-colors">
                  Sign up
                </Link>
              </p>
            </div>

            <div className="mt-8 pt-6 border-t border-white/20">
              <div className="text-center">
                <p className="text-sm text-gray-300">Crypto Payment Gateway USDT</p>
                <p className="text-xs text-gray-400 mt-1">Secure. Fast. Reliable.</p>
              </div>
            </div>
          </div>
        </div>
      </motion.div>
    </div>
  )
}
