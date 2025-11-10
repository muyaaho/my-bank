'use client';

import { usePathname } from 'next/navigation';
import { MobileHeader } from './MobileHeader';
import { BottomNav } from './BottomNav';

const PUBLIC_ROUTES = ['/login', '/register', '/auth'];

export function AppLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();

  // Check if current route is a public route (no nav needed)
  const isPublicRoute = PUBLIC_ROUTES.some(route => pathname?.startsWith(route));

  if (isPublicRoute) {
    // Auth pages - no header or bottom nav
    return <>{children}</>;
  }

  // App pages - with header and bottom nav
  return (
    <div className="flex flex-col h-screen bg-gray-50">
      {/* Mobile Header */}
      <MobileHeader />

      {/* Main Content with padding for fixed header and bottom nav */}
      <main className="flex-1 overflow-y-auto pt-16 pb-20 px-4">
        {children}
      </main>

      {/* Bottom Navigation */}
      <BottomNav />
    </div>
  );
}
