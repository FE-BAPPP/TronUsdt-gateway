"use client"

import { ReactNode } from "react"
import { Header } from "./Header"
import { Sidebar } from "./Sidebar"
import { useAuth } from "../../hooks/useAuth"

interface LayoutProps {
  children: ReactNode
}

export function Layout({ children }: LayoutProps) {
  const { role } = useAuth()

  return (
    <div className="min-h-screen bg-gray-50">
      <Header />

      <div className="flex">
        <Sidebar />

        <main className="flex-1 ml-0 lg:ml-64 transition-all duration-300">
          <div className="container-app section">{children}</div>
        </main>
      </div>
    </div>
  )
}
