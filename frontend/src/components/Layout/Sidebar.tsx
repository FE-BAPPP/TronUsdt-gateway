"use client"

import { NavLink } from "react-router-dom"
import { useAuth } from "../../hooks/useAuth"
import {
  LayoutDashboard,
  Briefcase,
  Search,
  FileText,
  Folder,
  User,
  Wallet,
  Users,
  Settings,
  BarChart3,
  Shield,
  Building,
  Send,
  MessageSquare,
  Tag,
} from "lucide-react"
import { cn } from "../../utils/cn"
import { motion, AnimatePresence } from "framer-motion"
import clsx from "clsx"

interface SidebarProps {
  isOpen: boolean
  onClose: () => void
}

export function Sidebar({ isOpen, onClose }: SidebarProps) {
  const { role } = useAuth()

  const getNavItems = () => {
    switch (role) {
      case "FREELANCER":
        return [
          { to: "/freelancer/dashboard", icon: LayoutDashboard, label: "Dashboard" },
          { to: "/freelancer/jobs", icon: Search, label: "Browse Jobs" },
          { to: "/freelancer/my-proposals", icon: FileText, label: "My Proposals" },
          { to: "/freelancer/my-projects", icon: Folder, label: "My Projects" },
          { to: "/chat", icon: MessageSquare, label: "Messages" },
          { to: "/freelancer/wallet", icon: Wallet, label: "Wallet" },
          { to: "/freelancer/freelancer-profile", icon: User, label: "Profile Settings" },
        ]

      case "EMPLOYER":
        return [
          { to: "/employer/dashboard", icon: LayoutDashboard, label: "Dashboard" },
          { to: "/employer/post-job", icon: Send, label: "Post a Job" },
          { to: "/employer/my-jobs", icon: Briefcase, label: "My Jobs" },
          { to: "/employer/my-projects", icon: Folder, label: "Projects" },
          { to: "/chat", icon: MessageSquare, label: "Messages" },
          { to: "/employer/wallet", icon: Wallet, label: "Wallet" },
          { to: "/profile", icon: User, label: "Profile Settings" },
        ]

      case "ADMIN":
        return [
          { to: "/admin/dashboard", icon: LayoutDashboard, label: "Dashboard" },
          { to: "/admin/users", icon: Users, label: "Users" },
          { to: "/admin/tracking", icon: BarChart3, label: "Deposits & Sweep" },
          { to: "/admin/withdrawals", icon: Shield, label: "Withdrawals" },
          { to: "/admin/skills", icon: Tag, label: "Skills" },
          { to: "/admin/wallet", icon: Wallet, label: "Wallet" },
        ]

      default: // USER
        return [
          { to: "/user/dashboard", icon: LayoutDashboard, label: "Dashboard" },
          { to: "/user/wallet", icon: Wallet, label: "Wallet" },
          { to: "/user/transactions", icon: BarChart3, label: "Transactions" },
          { to: "/profile", icon: User, label: "Profile" },
        ]
    }
  }

  const navItems = getNavItems()
  return (
    <>
      {/* Mobile backdrop */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            className="fixed inset-0 bg-black/50 z-40 md:hidden backdrop-blur-sm"
            onClick={onClose}
          />
        )}
      </AnimatePresence>

      {/* Sidebar */}
      <motion.div
        initial={{ x: -300 }}
        animate={{ x: 0 }}
        transition={{ type: "spring", damping: 25, stiffness: 200 }}
        className={clsx(
          "fixed left-0 top-0 h-full w-64 z-50 overflow-hidden",
          "md:fixed md:z-40",
          isOpen ? "translate-x-0" : "-translate-x-full md:translate-x-0",
        )}
      >
        <div className="absolute inset-0 bg-gradient-to-b from-purple-900/95 via-indigo-800/95 to-purple-900/95"></div>
        <div className="absolute inset-0 bg-gradient-to-r from-purple-700/20 via-transparent to-indigo-700/20"></div>
        <div className="absolute inset-0 backdrop-blur-xl border-r border-purple-500/30 shadow-2xl shadow-purple-900/50"></div>

        <div className="relative z-10 flex flex-col h-full">
          {/* Logo */}
          <div className="flex items-center h-16 px-6 border-b border-purple-500/30">
            <div className="text-xl font-bold bg-gradient-to-r from-yellow-400 via-yellow-300 to-yellow-500 bg-clip-text text-transparent">
              Freelance
              <span className="bg-gradient-to-r from-white via-gray-100 to-white bg-clip-text text-transparent ml-1">
                Pay
              </span>
            </div>
          </div>

          {/* Navigation */}
          <nav className="flex-1 px-4 py-6 space-y-2 overflow-y-auto">
            <div>
              <h3 className="px-3 text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">Main</h3>
              {navItems.map((item) => (
                <NavLink
                  key={item.label}
                  to={item.to}
                  onClick={onClose}
                  className={({ isActive }) =>
                    cn(
                      "flex items-center gap-3 px-4 py-3 rounded-lg transition-colors",
                      isActive ? "bg-teal-50 text-teal-700 font-medium" : "text-gray-700 hover:bg-gray-100",
                    )
                  }
                >
                  <item.icon className="w-5 h-5" />
                  <span>{item.label}</span>
                </NavLink>
              ))}
            </div>
          </nav>

          {/* Footer */}
          <div className="p-4 border-t border-purple-500/30">
            <div className="text-xs text-gray-400">Version 2.0.1</div>
          </div>
        </div>
      </motion.div>
    </>
  )
}
