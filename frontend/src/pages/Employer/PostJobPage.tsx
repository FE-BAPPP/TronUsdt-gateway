"use client"

import { useState } from "react"
import { motion } from "framer-motion"
import { useNavigate } from "react-router-dom"
import { userApi } from "../../services/api"
import {
  Briefcase,
  DollarSign,
  Clock,
  FileText,
  Tag,
  AlertCircle,
  CheckCircle,
} from "lucide-react"

export function PostJobPage() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState("")
  const [success, setSuccess] = useState(false)

  const [formData, setFormData] = useState({
    title: "",
    description: "",
    budget: "",
    type: "FIXED_PRICE",
    duration: "",
    skills: "",
  })

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError("")
    setLoading(true)

    try {
      const skillsArray = formData.skills
        .split(",")
        .map((s) => s.trim())
        .filter((s) => s.length > 0)

      const response = await userApi.request("/api/jobs", {
        method: "POST",
        body: JSON.stringify({
          title: formData.title,
          description: formData.description,
          budget: parseFloat(formData.budget),
          type: formData.type,
          duration: formData.duration,
          skills: skillsArray,
        }),
      })

      if (response.success) {
        setSuccess(true)
        setTimeout(() => {
          navigate("/employer/my-jobs")
        }, 2000)
      } else {
        setError(response.message || "Failed to post job")
      }
    } catch (err: any) {
      setError(err.message || "Failed to post job")
    } finally {
      setLoading(false)
    }
  }

  if (success) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <motion.div
          initial={{ opacity: 0, scale: 0.9 }}
          animate={{ opacity: 1, scale: 1 }}
          className="text-center"
        >
          <CheckCircle className="w-20 h-20 text-green-400 mx-auto mb-4" />
          <h2 className="text-2xl font-bold text-white mb-2">
            Job Posted Successfully!
          </h2>
          <p className="text-gray-400">Redirecting to your jobs...</p>
        </motion.div>
      </div>
    )
  }

  return (
    <div className="p-6 max-w-4xl mx-auto space-y-6">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="ui-card"
      >
        <div className="absolute inset-0 bg-gradient-to-r from-blue-600/90 via-blue-500/90 to-indigo-600/90"></div>
        <div className="relative z-10 p-8 text-center">
          <div className="flex items-center justify-center gap-3 mb-4">
            <Briefcase className="w-8 h-8 text-white" />
            <h1 className="text-3xl font-bold text-white">Post a New Job</h1>
          </div>
          <p className="text-white/80">Find talented freelancers for your project</p>
        </div>
      </motion.div>

      {/* Form */}
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        className="ui-card"
      >
        <div className="ui-card-body">
          {error && (
            <div className="mb-6 p-4 bg-red-500/10 border border-red-500/30 rounded-lg flex items-center gap-2 text-red-400">
              <AlertCircle className="w-5 h-5" />
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Title */}
            <div>
              <label className="block text-gray-300 text-sm font-medium mb-2">
                <FileText className="w-4 h-4 inline mr-2" />
                Job Title *
              </label>
              <input
                type="text"
                value={formData.title}
                onChange={(e) =>
                  setFormData({ ...formData, title: e.target.value })
                }
                className="ui-input"
                placeholder="e.g., Build a React Dashboard"
                required
                maxLength={200}
              />
            </div>

            {/* Description */}
            <div>
              <label className="block text-gray-300 text-sm font-medium mb-2">
                Description *
              </label>
              <textarea
                value={formData.description}
                onChange={(e) =>
                  setFormData({ ...formData, description: e.target.value })
                }
                className="ui-input min-h-[150px]"
                placeholder="Describe your project requirements..."
                required
              />
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              {/* Budget */}
              <div>
                <label className="block text-gray-300 text-sm font-medium mb-2">
                  <DollarSign className="w-4 h-4 inline mr-2" />
                  Budget (PTS) *
                </label>
                <input
                  type="number"
                  step="0.01"
                  min="10"
                  value={formData.budget}
                  onChange={(e) =>
                    setFormData({ ...formData, budget: e.target.value })
                  }
                  className="ui-input"
                  placeholder="e.g., 500"
                  required
                />
                <p className="text-sm text-gray-400 mt-1">Minimum: 10 PTS</p>
              </div>

              {/* Duration */}
              <div>
                <label className="block text-gray-300 text-sm font-medium mb-2">
                  <Clock className="w-4 h-4 inline mr-2" />
                  Duration
                </label>
                <input
                  type="text"
                  value={formData.duration}
                  onChange={(e) =>
                    setFormData({ ...formData, duration: e.target.value })
                  }
                  className="ui-input"
                  placeholder="e.g., 2 weeks"
                  maxLength={100}
                />
              </div>
            </div>

            {/* Type */}
            <div>
              <label className="block text-gray-300 text-sm font-medium mb-2">
                Project Type *
              </label>
              <div className="grid grid-cols-2 gap-4">
                <button
                  type="button"
                  onClick={() => setFormData({ ...formData, type: "FIXED_PRICE" })}
                  className={`p-4 rounded-xl border-2 transition-all ${
                    formData.type === "FIXED_PRICE"
                      ? "border-blue-400 bg-blue-400/10 text-blue-300"
                      : "border-white/20 bg-white/5 text-gray-300"
                  }`}
                >
                  <DollarSign className="w-6 h-6 mx-auto mb-2" />
                  <div className="font-semibold">Fixed Price</div>
                  <div className="text-xs mt-1">One-time payment</div>
                </button>

                <button
                  type="button"
                  onClick={() => setFormData({ ...formData, type: "HOURLY" })}
                  className={`p-4 rounded-xl border-2 transition-all ${
                    formData.type === "HOURLY"
                      ? "border-blue-400 bg-blue-400/10 text-blue-300"
                      : "border-white/20 bg-white/5 text-gray-300"
                  }`}
                >
                  <Clock className="w-6 h-6 mx-auto mb-2" />
                  <div className="font-semibold">Hourly</div>
                  <div className="text-xs mt-1">Pay per hour</div>
                </button>
              </div>
            </div>

            {/* Skills */}
            <div>
              <label className="block text-gray-300 text-sm font-medium mb-2">
                <Tag className="w-4 h-4 inline mr-2" />
                Required Skills
              </label>
              <input
                type="text"
                value={formData.skills}
                onChange={(e) =>
                  setFormData({ ...formData, skills: e.target.value })
                }
                className="ui-input"
                placeholder="e.g., React, Node.js, TypeScript (comma-separated)"
              />
              <p className="text-sm text-gray-400 mt-1">
                Separate skills with commas
              </p>
            </div>

            {/* Submit */}
            <div className="flex gap-4">
              <button
                type="button"
                onClick={() => navigate("/employer/dashboard")}
                className="ui-btn ui-btn-secondary flex-1"
              >
                Cancel
              </button>
              <button
                type="submit"
                disabled={loading}
                className="ui-btn ui-btn-primary flex-1 disabled:opacity-50"
              >
                {loading ? "Posting..." : "Post Job"}
              </button>
            </div>
          </form>
        </div>
      </motion.div>
    </div>
  )
}