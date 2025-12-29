"use client"

import { useState, useEffect } from "react"
import { motion } from "framer-motion"
import { useNavigate } from "react-router-dom"
import { userApi } from "../../services/api"
import { skillApi, Skill } from "../../services/skillApi"
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
  
  // 🆕 Skills state
  const [allSkills, setAllSkills] = useState<Skill[]>([])
  const [selectedSkills, setSelectedSkills] = useState<string[]>([])
  const [skillsLoading, setSkillsLoading] = useState(true)

  const [formData, setFormData] = useState({
    title: "",
    description: "",
    budget: "",
    type: "FIXED_PRICE",
    duration: "",
  })
  
  // 🆕 Load skills on mount
  useEffect(() => {
    loadSkills()
  }, [])
  
  const loadSkills = async () => {
    try {
      setSkillsLoading(true)
      const data = await skillApi.getAllSkills()
      setAllSkills(data)
    } catch (err: any) {
      console.error("Failed to load skills:", err)
      setError("Failed to load skills. You can still post the job without selecting skills.")
    } finally {
      setSkillsLoading(false)
    }
  }
  
  const toggleSkill = (skillId: string) => {
    setSelectedSkills(prev =>
      prev.includes(skillId)
        ? prev.filter(id => id !== skillId)
        : [...prev, skillId]
    )
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError("")
    setLoading(true)

    try {
      const response = await userApi.request("/api/jobs", {
        method: "POST",
        body: JSON.stringify({
          title: formData.title,
          description: formData.description,
          budget: parseFloat(formData.budget),
          type: formData.type,
          duration: formData.duration,
          skillIds: selectedSkills,  // ✅ Send selected skill UUIDs
        }),
      })

      if (response.success) {
        const jobId = response.data?.id || response.data

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

            {/* Skills - Multi-select with database */}
            <div>
              <label className="block text-gray-300 text-sm font-medium mb-2">
                <Tag className="w-4 h-4 inline mr-2" />
                Required Skills {selectedSkills.length > 0 && `(${selectedSkills.length} selected)`}
              </label>
              
              {skillsLoading ? (
                <div className="ui-input flex items-center justify-center py-4">
                  <div className="animate-spin rounded-full h-5 w-5 border-b-2 border-blue-400"></div>
                  <span className="ml-2 text-gray-400">Loading skills...</span>
                </div>
              ) : (
                <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-2 p-4 bg-white/5 rounded-xl border border-white/10 max-h-60 overflow-y-auto">
                  {allSkills.length === 0 ? (
                    <div className="col-span-full text-center py-4 text-gray-400">
                      No skills available. Contact admin to add skills.
                    </div>
                  ) : (
                    allSkills.map((skill) => (
                      <button
                        key={skill.id}
                        type="button"
                        onClick={() => toggleSkill(skill.id)}
                        className={`px-3 py-2 rounded-lg text-sm font-medium transition-all ${
                          selectedSkills.includes(skill.id)
                            ? "bg-blue-500/20 border-2 border-blue-400 text-blue-300"
                            : "bg-white/5 border border-white/20 text-gray-300 hover:bg-white/10"
                        }`}
                      >
                        {skill.name}
                      </button>
                    ))
                  )}
                </div>
              )}
              
              {selectedSkills.length === 0 && !skillsLoading && (
                <p className="text-sm text-gray-400 mt-2">
                  💡 Select at least one skill to help freelancers find your job
                </p>
              )}
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