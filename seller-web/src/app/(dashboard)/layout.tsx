"use client";

import { AuthGuard } from "@/components/auth/auth-guard";
import { Sidebar } from "@/components/layout/sidebar";
import { QueryProvider } from "@/lib/providers/query-provider";
import { Toaster } from "sonner";

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <QueryProvider>
      <AuthGuard>
        <div className="flex h-screen">
          {/* Sidebar - hidden on mobile */}
          <div className="hidden lg:block">
            <Sidebar />
          </div>

          {/* Main content */}
          <main className="flex-1 overflow-y-auto bg-muted/30">{children}</main>
        </div>
        <Toaster position="top-right" richColors />
      </AuthGuard>
    </QueryProvider>
  );
}
