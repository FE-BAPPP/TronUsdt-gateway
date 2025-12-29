import { useState, useEffect } from "react"
import { motion } from "framer-motion"
import { Plus, Tag, Trash2, Search, AlertCircle, CheckCircle } from "lucide-react"
import { skillApi, Skill } from "../../services/skillApi"

export function AdminSkillsPage() {
  const [skills, setSkills] = useState<Skill[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState("")
  const [searchTerm, setSearchTerm] = useState("")
  const [showAddModal, setShowAddModal] = useState(false)
  const [newSkillName, setNewSkillName] = useState("")
  const [submitting, setSubmitting] = useState(false)
  const [successMessage, setSuccessMessage] = useState("")

  useEffect(() => {
    loadSkills()
  }, [])

  const loadSkills = async () => {
    try {
      setLoading(true)
      const data = await skillApi.getAllSkills()
      setSkills(data)
      setError("")
    } catch (err: any) {
      setError(err.message || "Failed to load skills")
    } finally {
      setLoading(false)
    }
  }

  const handleAddSkill = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!newSkillName.trim()) return

    try {
      setSubmitting(true)
      await skillApi.createSkill(newSkillName.trim())
      setSuccessMessage(`✅ Skill "${newSkillName}" added successfully`)
      setNewSkillName("")
      setShowAddModal(false)
      loadSkills()
      
      setTimeout(() => setSuccessMessage(""), 3000)
    } catch (err: any) {
      setError(err.message || "Failed to add skill")
    } finally {
      setSubmitting(false)
    }
  }

  const handleDeleteSkill = async (skill: Skill) => {
    if (!confirm(`Are you sure you want to delete "${skill.name}"?`)) return

    try {
      await skillApi.deleteSkill(skill.id)
      setSuccessMessage(`✅ Skill "${skill.name}" deleted successfully`)
      loadSkills()
      
      setTimeout(() => setSuccessMessage(""), 3000)
    } catch (err: any) {
      setError(err.message || "Failed to delete skill")
    }
  }

  const filteredSkills = skills.filter(skill =>
    skill.name.toLowerCase().includes(searchTerm.toLowerCase())
  )

  if (loading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <div className="animate-spin rounded-full h-32 w-32 border-b-2 border-blue-500"></div>
      </div>
    )
  }

  return (
    <div className="p-6 max-w-6xl mx-auto space-y-6">
      {/* Header */}
      <motion.div
        initial={{ opacity: 0, y: -20 }}
        animate={{ opacity: 1, y: 0 }}
        className="ui-card"
      >
        <div className="absolute inset-0 bg-gradient-to-r from-purple-600/90 via-blue-600/90 to-indigo-600/90"></div>
        <div className="relative z-10 p-8">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Tag className="w-8 h-8 text-white" />
              <div>
                <h1 className="text-3xl font-bold text-white">Skills Management</h1>
                <p className="text-white/80">Manage platform skills</p>
              </div>
            </div>
            <button
              onClick={() => setShowAddModal(true)}
              className="ui-btn ui-btn-primary"
            >
              <Plus className="w-5 h-5 mr-2" />
              Add Skill
            </button>
          </div>
        </div>
      </motion.div>

      {/* Success/Error Messages */}
      {successMessage && (
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          className="p-4 bg-green-500/10 border border-green-500/30 rounded-lg flex items-center gap-2 text-green-400"
        >
          <CheckCircle className="w-5 h-5" />
          {successMessage}
        </motion.div>
      )}

      {error && (
        <motion.div
          initial={{ opacity: 0, y: -10 }}
          animate={{ opacity: 1, y: 0 }}
          className="p-4 bg-red-500/10 border border-red-500/30 rounded-lg flex items-center gap-2 text-red-400"
        >
          <AlertCircle className="w-5 h-5" />
          {error}
        </motion.div>
      )}

      {/* Search */}
      <div className="ui-card">
        <div className="ui-card-body">
          <div className="relative">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
            <input
              type="text"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              placeholder="Search skills..."
              className="ui-input pl-10"
            />
          </div>
        </div>
      </div>

      {/* Skills Grid */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        {filteredSkills.map((skill) => (
          <motion.div
            key={skill.id}
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            className="ui-card group"
          >
            <div className="ui-card-body flex items-center justify-between">
              <span className="text-gray-200 font-medium">{skill.name}</span>
              <button
                onClick={() => handleDeleteSkill(skill)}
                className="opacity-0 group-hover:opacity-100 transition-opacity text-red-400 hover:text-red-300"
              >
                <Trash2 className="w-4 h-4" />
              </button>
            </div>
          </motion.div>
        ))}
      </div>

      {filteredSkills.length === 0 && (
        <div className="text-center py-12 text-gray-400">
          <Tag className="w-16 h-16 mx-auto mb-4 opacity-30" />
          <p>No skills found</p>
        </div>
      )}

      {/* Add Skill Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
          <motion.div
            initial={{ opacity: 0, scale: 0.9 }}
            animate={{ opacity: 1, scale: 1 }}
            className="ui-card max-w-md w-full"
          >
            <div className="ui-card-body">
              <h2 className="text-xl font-bold text-white mb-4">Add New Skill</h2>
              
              <form onSubmit={handleAddSkill} className="space-y-4">
                <div>
                  <label className="block text-gray-300 text-sm font-medium mb-2">
                    Skill Name
                  </label>
                  <input
                    type="text"
                    value={newSkillName}
                    onChange={(e) => setNewSkillName(e.target.value)}
                    placeholder="e.g., React, Python, Node.js"
                    className="ui-input"
                    required
                    autoFocus
                  />
                </div>

                <div className="flex gap-3">
                  <button
                    type="button"
                    onClick={() => {
                      setShowAddModal(false)
                      setNewSkillName("")
                    }}
                    className="ui-btn ui-btn-secondary flex-1"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={submitting}
                    className="ui-btn ui-btn-primary flex-1 disabled:opacity-50"
                  >
                    {submitting ? "Adding..." : "Add Skill"}
                  </button>
                </div>
              </form>
            </div>
          </motion.div>
        </div>
      )}

      {/* Stats */}
      <div className="ui-card">
        <div className="ui-card-body">
          <div className="text-center">
            <p className="text-4xl font-bold text-blue-400">{skills.length}</p>
            <p className="text-gray-400 mt-1">Total Skills</p>
          </div>
        </div>
      </div>
    </div>
  )
}

export default AdminSkillsPage
