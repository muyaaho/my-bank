'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { cn } from '@/lib/utils/cn';
import {
  Home,
  Wallet,
  CreditCard,
  TrendingUp,
  BarChart3,
} from 'lucide-react';

const navigation = [
  {
    name: '홈',
    href: '/dashboard',
    icon: Home,
  },
  {
    name: '계좌',
    href: '/dashboard/accounts',
    icon: Wallet,
  },
  {
    name: '송금',
    href: '/dashboard/payment',
    icon: CreditCard,
  },
  {
    name: '투자',
    href: '/dashboard/investment',
    icon: TrendingUp,
  },
  {
    name: '소비',
    href: '/dashboard/spending',
    icon: BarChart3,
  },
];

export function BottomNav() {
  const pathname = usePathname();

  return (
    <nav className="fixed bottom-0 left-0 right-0 z-40 bg-white border-t border-gray-200 safe-area-pb">
      <div className="flex items-center justify-around px-2 py-2">
        {navigation.map((item) => {
          const Icon = item.icon;
          const isActive = pathname === item.href;

          return (
            <Link
              key={item.name}
              href={item.href}
              className={cn(
                'flex flex-col items-center justify-center min-w-[64px] px-3 py-2 rounded-lg transition-all',
                isActive
                  ? 'text-primary-600'
                  : 'text-gray-500 hover:bg-gray-50'
              )}
            >
              <Icon
                className={cn(
                  'w-6 h-6 mb-1',
                  isActive ? 'stroke-[2.5]' : 'stroke-[2]'
                )}
              />
              <span
                className={cn(
                  'text-xs',
                  isActive ? 'font-semibold' : 'font-medium'
                )}
              >
                {item.name}
              </span>
            </Link>
          );
        })}
      </div>
    </nav>
  );
}
