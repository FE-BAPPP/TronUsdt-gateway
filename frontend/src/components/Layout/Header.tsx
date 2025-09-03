import { useState } from 'react';
import { Link, useLocation } from 'react-router-dom';
import { 
  BellIcon, 
  UserCircleIcon,
  ChevronDownIcon
} from '@heroicons/react/24/outline';
import { useAuth } from '../../hooks/useAuth';
import { motion, AnimatePresence } from 'framer-motion';

interface HeaderProps {
}

export function Header(_props: HeaderProps) {
  const { user, logout, isAdmin } = useAuth();
  const location = useLocation();
  const [isProfileOpen, setIsProfileOpen] = useState(false);
  const [notifications] = useState(3);
  const isActive = (path: string) => location.pathname.startsWith(path);

  const base = isAdmin ? '/admin' : '/user';
  const routes = {
    dashboard: `${base}/dashboard`,
    wallet: `${base}/wallet`,
    transactions: `${base}/transactions`,
    p2p: `${base}/p2p`,
    tracking: `/admin/tracking`,
    adminWithdrawals: `/admin/withdrawals`,
  };

  return (
    <header className="sticky top-0 z-50 bg-slate-950/80 backdrop-blur border-b border-white/10">
      <div className="container mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <div className="flex items-center">
            <Link to="/" className="flex items-center">
              <div className="text-xl font-semibold text-yellow-400">
                {isAdmin ? 'Admin' : 'Binance'}
                <span className="text-white ml-1">Pay</span>
              </div>
            </Link>
          </div>

          {/* Navigation */}
          <nav className="flex items-center gap-2">
            <Link
              to={routes.dashboard}
              className={`px-4 py-2 rounded-xl text-sm font-medium transition-all duration-300 ${
                isActive(routes.dashboard) 
                  ? 'text-yellow-300 bg-yellow-400/20 border border-yellow-400/30' 
                  : 'text-gray-300 hover:text-white hover:bg-white/10 border border-transparent'
              }`}
            >
              Dashboard
            </Link>
            {!isAdmin && (
              <Link
                to={routes.wallet}
                className={`px-4 py-2 rounded-xl text-sm font-medium transition-all duration-300 ${
                  isActive(routes.wallet) 
                    ? 'text-yellow-300 bg-yellow-400/20 border border-yellow-400/30' 
                    : 'text-gray-300 hover:text-white hover:bg-white/10 border border-transparent'
                }`}
              >
                Wallet
              </Link>
            )}
            <Link
              to={isAdmin ? routes.adminWithdrawals : routes.transactions}
              className={`px-4 py-2 rounded-xl text-sm font-medium transition-all duration-300 ${
                isActive(isAdmin ? routes.adminWithdrawals : routes.transactions) 
                  ? 'text-yellow-300 bg-yellow-400/20 border border-yellow-400/30' 
                  : 'text-gray-300 hover:text-white hover:bg-white/10 border border-transparent'
              }`}
            >
              {isAdmin ? 'Withdrawals' : 'History'}
            </Link>
            {!isAdmin && (
              <Link
                to={routes.p2p}
                className={`px-4 py-2 rounded-xl text-sm font-medium transition-all duration-300 ${
                  isActive(routes.p2p) 
                    ? 'text-yellow-300 bg-yellow-400/20 border border-yellow-400/30' 
                    : 'text-gray-300 hover:text-white hover:bg-white/10 border border-transparent'
                }`}
              >
                P2P Transfer
              </Link>
            )}
            {isAdmin && (
              <Link
                to={routes.tracking}
                className={`px-4 py-2 rounded-xl text-sm font-medium transition-all duration-300 ${
                  isActive(routes.tracking) 
                    ? 'text-yellow-300 bg-yellow-400/20 border border-yellow-400/30' 
                    : 'text-gray-300 hover:text-white hover:bg-white/10 border border-transparent'
                }`}
              >
                Tracking
              </Link>
            )}
          </nav>

          {/* Right side actions */}
          <div className="flex items-center space-x-4">
            {/* Notifications */}
            <button className="relative p-2 text-gray-400 hover:text-white hover:bg-white/10 rounded-md transition-colors">
              <BellIcon className="h-6 w-6" />
              {notifications > 0 && (
                <span className="absolute -top-1 -right-1 h-4 w-4 bg-red-500 text-white text-[10px] rounded-full flex items-center justify-center">
                  {notifications}
                </span>
              )}
            </button>

            {/* Profile dropdown */}
            <div className="relative">
              <button
                onClick={() => setIsProfileOpen(!isProfileOpen)}
                className="flex items-center gap-2 p-2 text-gray-400 hover:text-white hover:bg-white/10 rounded-md transition-colors"
              >
                <UserCircleIcon className="h-6 w-6" />
                <span className="hidden sm:block text-sm">
                  {isAdmin ? 'Admin' : user?.username}
                </span>
                <ChevronDownIcon className="h-4 w-4" />
              </button>

              <AnimatePresence>
                {isProfileOpen && (
                  <motion.div
                    initial={{ opacity: 0, y: -10 }}
                    animate={{ opacity: 1, y: 0 }}
                    exit={{ opacity: 0, y: -10 }}
                    className="absolute right-0 mt-2 w-48 bg-slate-900 rounded-md shadow-lg py-1 border border-white/10"
                  >
                    {!isAdmin && (
                      <>
                        <Link
                          to="/user/profile"
                          className="block px-4 py-2 text-sm text-gray-300 hover:text-white hover:bg-white/5 transition-colors"
                          onClick={() => setIsProfileOpen(false)}
                        >
                          Profile Settings
                        </Link>
                        <hr className="border-gray-700 my-1" />
                      </>
                    )}
                    <button
                      onClick={() => {
                        logout();
                        setIsProfileOpen(false);
                      }}
                      className="block w-full text-left px-4 py-2 text-sm text-red-400 hover:text-red-300 hover:bg-red-500/10 transition-colors"
                    >
                      Sign Out
                    </button>
                  </motion.div>
                )}
              </AnimatePresence>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
}