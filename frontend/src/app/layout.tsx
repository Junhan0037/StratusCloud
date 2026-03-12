import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "StratusCloud Console",
  description: "StratusCloud 운영 콘솔"
};

function Sidebar() {
  return (
    <aside className="fixed inset-y-0 left-0 w-64 bg-slate-900 text-slate-100 flex flex-col border-r border-slate-800">
      <div className="h-16 flex items-center px-6 border-b border-slate-800">
        <span className="text-lg font-bold tracking-tight text-white">StratusCloud</span>
      </div>
      <nav className="flex-1 px-4 py-6 space-y-1 overflow-y-auto">
        <a href="/" className="flex items-center px-2 py-2 text-sm font-medium rounded-md bg-slate-800 text-white">
          Overview
        </a>
        <div className="pt-4 pb-2">
          <p className="px-2 text-xs font-semibold text-slate-400 uppercase tracking-wider">Resources</p>
        </div>
        <a href="/console/iam" className="flex items-center px-2 py-2 text-sm font-medium rounded-md text-slate-300 hover:bg-slate-800 hover:text-white">
          IAM & Security
        </a>
        <a href="/console/compute" className="flex items-center px-2 py-2 text-sm font-medium rounded-md text-slate-300 hover:bg-slate-800 hover:text-white">
          Compute
        </a>
        <a href="/console/network" className="flex items-center px-2 py-2 text-sm font-medium rounded-md text-slate-300 hover:bg-slate-800 hover:text-white">
          Network
        </a>
        <a href="/console/storage" className="flex items-center px-2 py-2 text-sm font-medium rounded-md text-slate-300 hover:bg-slate-800 hover:text-white">
          Storage
        </a>
        <div className="pt-4 pb-2">
          <p className="px-2 text-xs font-semibold text-slate-400 uppercase tracking-wider">Management</p>
        </div>
        <a href="/console/governance" className="flex items-center px-2 py-2 text-sm font-medium rounded-md text-slate-300 hover:bg-slate-800 hover:text-white">
          Governance
        </a>
        <a href="/console/audit" className="flex items-center px-2 py-2 text-sm font-medium rounded-md text-slate-300 hover:bg-slate-800 hover:text-white">
          Audit Logs
        </a>
      </nav>
      <div className="p-4 border-t border-slate-800">
        <div className="flex items-center">
          <div className="h-8 w-8 rounded-full bg-indigo-500 flex items-center justify-center text-white text-xs font-bold">
            JD
          </div>
          <div className="ml-3">
            <p className="text-sm font-medium text-white">Jane Doe</p>
            <p className="text-xs text-slate-400">View Profile</p>
          </div>
        </div>
      </div>
    </aside>
  );
}

function Header() {
  return (
    <header className="h-16 bg-white border-b border-slate-200 flex items-center justify-between px-6 sticky top-0 z-10">
      <div className="flex items-center">
        <h1 className="text-lg font-semibold text-slate-800">Dashboard</h1>
      </div>
      <div className="flex items-center space-x-4">
        <button className="p-2 text-slate-400 hover:text-slate-500">
          <span className="sr-only">Notifications</span>
          <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9" />
          </svg>
        </button>
        <div className="h-8 w-8 rounded-full bg-slate-200 border border-slate-300 flex items-center justify-center text-slate-500 text-xs">
          Help
        </div>
      </div>
    </header>
  );
}

// 모든 페이지에 공통 레이아웃을 적용한다.
export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ko" className="h-full bg-slate-50">
      <body className="h-full antialiased">
        <div className="flex h-screen overflow-hidden">
          <Sidebar />
          <div className="flex flex-col flex-1 overflow-hidden">
            <Header />
            <main className="flex-1 overflow-y-auto bg-slate-50 p-6">
              <div className="max-w-7xl mx-auto">
                {children}
              </div>
            </main>
          </div>
        </div>
      </body>
    </html>
  );
}
