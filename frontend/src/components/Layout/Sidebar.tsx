"use client"

import { NavLink } from "react-router-dom"
import {
  HomeIcon,
  WalletIcon,
  ArrowsRightLeftIcon,
  ClockIcon,
  UserGroupIcon,
  CogIcon,
  ShieldCheckIcon,
  ChartBarIcon,
} from "@heroicons/react/24/outline"
import { motion, AnimatePresence } from "framer-motion"
import clsx from "clsx"
import { useAuth } from "../../hooks/useAuth"

interface SidebarProps {
  isOpen: boolean
  onClose: () => void
}

function buildNav(isAdmin: boolean) {
  const base = isAdmin ? "/admin" : "/user"
  const nav = [{ name: "Dashboard", href: `${base}/dashboard`, icon: HomeIcon }]
  if (!isAdmin) {
    nav.push(
      { name: "Wallet", href: `${base}/wallet`, icon: WalletIcon },
      { name: "P2P Transfer", href: `${base}/p2p`, icon: ArrowsRightLeftIcon },
      { name: "History", href: `${base}/transactions`, icon: ClockIcon },
    )
  }
  // Admin extras
  if (isAdmin) {
    nav.push(
      { name: "Withdrawals", href: `${base}/withdrawals`, icon: ClockIcon },
      { name: "Tracking", href: `${base}/tracking`, icon: ChartBarIcon },
    )
  } else {
    nav.push({ name: "Analytics", href: `${base}/dashboard`, icon: ChartBarIcon })
  }
  const settings = [
    { name: "Profile", href: `${base}/profile`, icon: UserGroupIcon },
    { name: "Security", href: `${base}/profile`, icon: ShieldCheckIcon },
    { name: "Settings", href: `${base}/profile`, icon: CogIcon },
  ]
  return { nav, settings }
}

export function Sidebar({ isOpen, onClose }: SidebarProps) {
  const { isAdmin } = useAuth()
  const { nav: navigation, settings } = buildNav(isAdmin)
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
              Binance
              <span className="bg-gradient-to-r from-white via-gray-100 to-white bg-clip-text text-transparent ml-1">
                Pay
              </span>
            </div>
          </div>

          {/* Navigation */}
          <nav className="flex-1 px-4 py-6 space-y-2 overflow-y-auto">
            <div>
              <h3 className="px-3 text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">Main</h3>
              {navigation.map((item) => (
                <NavLink
                  key={item.name}
                  to={item.href}
                  onClick={onClose}
                  className={({ isActive }) =>
                    clsx(
                      "group flex items-center px-3 py-3 text-sm font-medium rounded-xl transition-all duration-300 relative overflow-hidden",
                      isActive ? "text-yellow-300 shadow-lg shadow-yellow-400/25" : "text-gray-300 hover:text-white",
                    )
                  }
                >
                  {({ isActive }) => (
                    <>
                      {isActive && (
                        <>
                          <div className="absolute inset-0 bg-gradient-to-r from-yellow-400/20 via-yellow-300/20 to-yellow-400/20"></div>
                          <div className="absolute inset-0 bg-gradient-to-b from-yellow-400/10 via-transparent to-yellow-400/10"></div>
                          <div className="absolute right-0 top-0 bottom-0 w-1 bg-gradient-to-b from-yellow-400 to-yellow-500 rounded-l-full"></div>
                        </>
                      )}
                      {!isActive && (
                        <div className="absolute inset-0 bg-gradient-to-r from-purple-700/0 via-purple-700/0 to-indigo-700/0 group-hover:from-purple-700/20 group-hover:via-indigo-700/20 group-hover:to-purple-700/20 transition-all duration-300"></div>
                      )}
                      <item.icon className="relative z-10 mr-3 h-5 w-5" />
                      <span className="relative z-10">{item.name}</span>
                    </>
                  )}
                </NavLink>
              ))}
            </div>

            <div className="pt-6">
              <h3 className="px-3 text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">Account</h3>
              {settings.map((item) => (
                <NavLink
                  key={item.name}
                  to={item.href}
                  onClick={onClose}
                  className={({ isActive }) =>
                    clsx(
                      "group flex items-center px-3 py-3 text-sm font-medium rounded-xl transition-all duration-300 relative overflow-hidden",
                      isActive ? "text-yellow-300 shadow-lg shadow-yellow-400/25" : "text-gray-300 hover:text-white",
                    )
                  }
                >
                  {({ isActive }) => (
                    <>
                      {isActive && (
                        <>
                          <div className="absolute inset-0 bg-gradient-to-r from-yellow-400/20 via-yellow-300/20 to-yellow-400/20"></div>
                          <div className="absolute inset-0 bg-gradient-to-b from-yellow-400/10 via-transparent to-yellow-400/10"></div>
                          <div className="absolute right-0 top-0 bottom-0 w-1 bg-gradient-to-b from-yellow-400 to-yellow-500 rounded-l-full"></div>
                        </>
                      )}
                      {!isActive && (
                        <div className="absolute inset-0 bg-gradient-to-r from-purple-700/0 via-purple-700/0 to-indigo-700/0 group-hover:from-purple-700/20 group-hover:via-indigo-700/20 group-hover:to-purple-700/20 transition-all duration-300"></div>
                      )}
                      <item.icon className="relative z-10 mr-3 h-5 w-5" />
                      <span className="relative z-10">{item.name}</span>
                    </>
                  )}
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
