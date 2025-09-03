"use client"

import type React from "react"
import { useState, useEffect } from "react"
import { useNavigate, Link } from "react-router-dom"
import { motion } from "framer-motion"
import { useAuth } from "../../hooks/useAuth"
import { userApi } from "../../services/api"
import { UserPlus, User, Mail, UserCircle, Lock, Eye, EyeOff, AlertCircle, Loader2, CheckCircle } from "lucide-react"

interface FormData {
  username: string
  email: string
  fullName: string
  password: string
  confirmPassword: string
}

interface ValidationErrors {
  username?: string
  email?: string
  fullName?: string
  password?: string
  confirmPassword?: string
}

export function Register() {
  const { user } = useAuth()
  const navigate = useNavigate()

  const [formData, setFormData] = useState<FormData>({
    username: "",
    email: "",
    fullName: "",
    password: "",
    confirmPassword: "",
  })

  const [showPassword, setShowPassword] = useState(false)
  const [showConfirmPassword, setShowConfirmPassword] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const [validationErrors, setValidationErrors] = useState<ValidationErrors>({})
  const [checkingAvailability] = useState({
    username: false,
    email: false,
  })

  useEffect(() => {
    if (user) {
      navigate("/", { replace: true })
    }
  }, [user, navigate])

  const validateForm = (): ValidationErrors => {
    const errors: ValidationErrors = {}

    if (!formData.username) {
      errors.username = "Username is required"
    } else if (formData.username.length < 3) {
      errors.username = "Username must be at least 3 characters"
    }

    if (!formData.email) {
      errors.email = "Email is required"
    } else if (!/\S+@\S+\.\S+/.test(formData.email)) {
      errors.email = "Email is invalid"
    }

    if (!formData.fullName) {
      errors.fullName = "Full name is required"
    }

    if (!formData.password) {
      errors.password = "Password is required"
    } else if (formData.password.length < 6) {
      errors.password = "Password must be at least 6 characters"
    }

    if (!formData.confirmPassword) {
      errors.confirmPassword = "Please confirm your password"
    } else if (formData.password !== formData.confirmPassword) {
      errors.confirmPassword = "Passwords do not match"
    }

    return errors
  }

  const handleInputChange = (field: keyof FormData, value: string) => {
    setFormData((prev) => ({ ...prev, [field]: value }))
    setError("")

    if (validationErrors[field]) {
      setValidationErrors((prev) => {
        const { [field]: _, ...rest } = prev
        return rest
      })
    }
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError("")

    const errors = validateForm()
    if (Object.keys(errors).length > 0) {
      setValidationErrors(errors)
      return
    }

    setLoading(true)

    try {
      const response = await userApi.register({
        username: formData.username,
        email: formData.email,
        fullName: formData.fullName,
        password: formData.password,
        confirmPassword: formData.confirmPassword,
      })

      if (response.success) {
        navigate("/login", {
          state: { message: "Registration successful! Please sign in." },
        })
      } else {
        setError(response.message || "Registration failed")
      }
    } catch (err: any) {
      setError(err.message || "Registration failed")
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900 flex items-center justify-center px-4 py-8">
      <div className="fixed inset-0 overflow-hidden pointer-events-none">
        <div className="absolute -top-40 -right-40 w-80 h-80 bg-purple-500 rounded-full mix-blend-multiply filter blur-xl opacity-20 animate-pulse"></div>
        <div className="absolute -bottom-40 -left-40 w-80 h-80 bg-indigo-500 rounded-full mix-blend-multiply filter blur-xl opacity-20 animate-pulse animation-delay-2000"></div>
        <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-violet-500 rounded-full mix-blend-multiply filter blur-xl opacity-10 animate-pulse animation-delay-4000"></div>
      </div>

      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="relative z-10 w-full max-w-md"
      >
        <div className="relative overflow-hidden rounded-2xl">
          <div className="absolute inset-0 bg-gradient-to-br from-white/15 via-white/10 to-transparent"></div>
          <div className="absolute inset-0 backdrop-blur-xl border border-white/20 rounded-2xl shadow-2xl shadow-purple-900/50"></div>
          <div className="relative z-10 p-8">
            <div className="text-center mb-8">
              <div className="flex items-center justify-center gap-3 mb-4">
                <UserPlus className="w-8 h-8 text-yellow-400" />
                <h1 className="text-3xl font-bold text-white">Create Account</h1>
              </div>
              <p className="text-gray-300">Join the USDT wallet ecosystem</p>
            </div>

            {error && (
              <motion.div
                initial={{ opacity: 0, scale: 0.95 }}
                animate={{ opacity: 1, scale: 1 }}
                className="relative overflow-hidden rounded-xl mb-4"
              >
                <div className="absolute inset-0 bg-gradient-to-br from-red-500/20 via-red-400/10 to-transparent"></div>
                <div className="absolute inset-0 backdrop-blur-sm border border-red-400/30 rounded-xl"></div>
                <div className="relative z-10 p-3 flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 text-red-400" />
                  <p className="text-red-400 text-sm">{error}</p>
                </div>
              </motion.div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              {/* Username */}
              <div>
                <label className="block text-gray-300 text-sm font-medium mb-2 flex items-center gap-2">
                  <User className="w-4 h-4" />
                  Username
                </label>
                <div className="relative">
                  <input
                    type="text"
                    value={formData.username}
                    onChange={(e) => handleInputChange("username", e.target.value)}
                    className={`w-full px-4 py-3 bg-white/10 border rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-1 transition-all duration-300 backdrop-blur-sm pr-10 ${
                      validationErrors.username
                        ? "border-red-500/50 focus:border-red-400/50 focus:ring-red-400/50"
                        : "border-white/20 focus:border-yellow-400/50 focus:ring-yellow-400/50"
                    }`}
                    placeholder="Choose a username"
                    required
                    disabled={loading}
                  />
                  {checkingAvailability.username && (
                    <div className="absolute right-3 top-1/2 transform -translate-y-1/2">
                      <Loader2 className="w-4 h-4 animate-spin text-yellow-400" />
                    </div>
                  )}
                </div>
                {validationErrors.username && (
                  <p className="text-red-400 text-xs mt-1 flex items-center gap-1">
                    <AlertCircle className="w-3 h-3" />
                    {validationErrors.username}
                  </p>
                )}
              </div>

              {/* Email */}
              <div>
                <label className="block text-gray-300 text-sm font-medium mb-2 flex items-center gap-2">
                  <Mail className="w-4 h-4" />
                  Email
                </label>
                <div className="relative">
                  <input
                    type="email"
                    value={formData.email}
                    onChange={(e) => handleInputChange("email", e.target.value)}
                    className={`w-full px-4 py-3 bg-white/10 border rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-1 transition-all duration-300 backdrop-blur-sm pr-10 ${
                      validationErrors.email
                        ? "border-red-500/50 focus:border-red-400/50 focus:ring-red-400/50"
                        : "border-white/20 focus:border-yellow-400/50 focus:ring-yellow-400/50"
                    }`}
                    placeholder="Enter your email"
                    required
                    disabled={loading}
                  />
                  {checkingAvailability.email && (
                    <div className="absolute right-3 top-1/2 transform -translate-y-1/2">
                      <Loader2 className="w-4 h-4 animate-spin text-yellow-400" />
                    </div>
                  )}
                </div>
                {validationErrors.email && (
                  <p className="text-red-400 text-xs mt-1 flex items-center gap-1">
                    <AlertCircle className="w-3 h-3" />
                    {validationErrors.email}
                  </p>
                )}
              </div>

              {/* Full Name */}
              <div>
                <label className="block text-gray-300 text-sm font-medium mb-2 flex items-center gap-2">
                  <UserCircle className="w-4 h-4" />
                  Full Name
                </label>
                <input
                  type="text"
                  value={formData.fullName}
                  onChange={(e) => handleInputChange("fullName", e.target.value)}
                  className={`w-full px-4 py-3 bg-white/10 border rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-1 transition-all duration-300 backdrop-blur-sm ${
                    validationErrors.fullName
                      ? "border-red-500/50 focus:border-red-400/50 focus:ring-red-400/50"
                      : "border-white/20 focus:border-yellow-400/50 focus:ring-yellow-400/50"
                  }`}
                  placeholder="Enter your full name"
                  required
                  disabled={loading}
                />
                {validationErrors.fullName && (
                  <p className="text-red-400 text-xs mt-1 flex items-center gap-1">
                    <AlertCircle className="w-3 h-3" />
                    {validationErrors.fullName}
                  </p>
                )}
              </div>

              {/* Password */}
              <div>
                <label className="block text-gray-300 text-sm font-medium mb-2 flex items-center gap-2">
                  <Lock className="w-4 h-4" />
                  Password
                </label>
                <div className="relative">
                  <input
                    type={showPassword ? "text" : "password"}
                    value={formData.password}
                    onChange={(e) => handleInputChange("password", e.target.value)}
                    className={`w-full px-4 py-3 bg-white/10 border rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-1 transition-all duration-300 backdrop-blur-sm pr-12 ${
                      validationErrors.password
                        ? "border-red-500/50 focus:border-red-400/50 focus:ring-red-400/50"
                        : "border-white/20 focus:border-yellow-400/50 focus:ring-yellow-400/50"
                    }`}
                    placeholder="Create a password"
                    required
                    disabled={loading}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-300 transition-colors"
                    disabled={loading}
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                {validationErrors.password && (
                  <p className="text-red-400 text-xs mt-1 flex items-center gap-1">
                    <AlertCircle className="w-3 h-3" />
                    {validationErrors.password}
                  </p>
                )}
              </div>

              {/* Confirm Password */}
              <div>
                <label className="block text-gray-300 text-sm font-medium mb-2 flex items-center gap-2">
                  <CheckCircle className="w-4 h-4" />
                  Confirm Password
                </label>
                <div className="relative">
                  <input
                    type={showConfirmPassword ? "text" : "password"}
                    value={formData.confirmPassword}
                    onChange={(e) => handleInputChange("confirmPassword", e.target.value)}
                    className={`w-full px-4 py-3 bg-white/10 border rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-1 transition-all duration-300 backdrop-blur-sm pr-12 ${
                      validationErrors.confirmPassword
                        ? "border-red-500/50 focus:border-red-400/50 focus:ring-red-400/50"
                        : "border-white/20 focus:border-yellow-400/50 focus:ring-yellow-400/50"
                    }`}
                    placeholder="Confirm your password"
                    required
                    disabled={loading}
                  />
                  <button
                    type="button"
                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                    className="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-300 transition-colors"
                    disabled={loading}
                  >
                    {showConfirmPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
                {validationErrors.confirmPassword && (
                  <p className="text-red-400 text-xs mt-1 flex items-center gap-1">
                    <AlertCircle className="w-3 h-3" />
                    {validationErrors.confirmPassword}
                  </p>
                )}
              </div>

              <button
                type="submit"
                disabled={loading || Object.keys(validationErrors).length > 0}
                className="w-full relative overflow-hidden rounded-xl py-3 transition-all duration-300 disabled:opacity-50 disabled:cursor-not-allowed mt-6"
              >
                <div className="absolute inset-0 bg-gradient-to-r from-yellow-600 via-yellow-500 to-yellow-400"></div>
                <div className="absolute inset-0 bg-gradient-to-br from-transparent via-yellow-400/20 to-yellow-600/30"></div>
                <div className="relative z-10 text-black font-medium">
                  {loading ? (
                    <div className="flex items-center justify-center gap-2">
                      <Loader2 className="w-4 h-4 animate-spin" />
                      Creating Account...
                    </div>
                  ) : (
                    <div className="flex items-center justify-center gap-2">
                      <UserPlus className="w-4 h-4" />
                      Create Account
                    </div>
                  )}
                </div>
              </button>
            </form>

            <div className="mt-6 text-center">
              <p className="text-gray-300">
                Already have an account?{" "}
                <Link to="/login" className="text-yellow-400 hover:text-yellow-300 font-medium transition-colors">
                  Sign in
                </Link>
              </p>
            </div>
          </div>
        </div>
      </motion.div>
    </div>
  )
}
