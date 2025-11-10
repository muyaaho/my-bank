'use client';

import { useState } from 'react';
import { useTransactions } from '@/lib/hooks/useTransactions';
import { Card } from '@/components/ui/Card';
import { Loading } from '@/components/ui/Loading';
import { formatCurrency, formatDate } from '@/lib/utils/format';
import { TransactionType } from '@/types/api';

const TRANSACTION_ICONS: Record<TransactionType, string> = {
  DEPOSIT: '↓',
  WITHDRAWAL: '↑',
  TRANSFER: '→',
  PAYMENT: '→',
};

const TRANSACTION_COLORS: Record<TransactionType, string> = {
  DEPOSIT: 'text-green-600 bg-green-100',
  WITHDRAWAL: 'text-red-600 bg-red-100',
  TRANSFER: 'text-blue-600 bg-blue-100',
  PAYMENT: 'text-purple-600 bg-purple-100',
};

export default function AccountsPage() {
  const [page, setPage] = useState(0);
  const { data, isLoading } = useTransactions(page, 20);

  if (isLoading) {
    return <Loading message="Loading transactions..." />;
  }

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">거래 내역</h1>
        <p className="text-sm text-gray-600 mt-1">최근 거래를 확인하세요</p>
      </div>

      <Card title="거래 내역">
        {data && data.transactions.length > 0 ? (
          <>
            <div className="space-y-2">
              {data.transactions.map((transaction) => (
                <div
                  key={transaction.transactionId}
                  className="flex items-center justify-between p-3 border border-gray-200 rounded-lg active:bg-gray-50 transition-colors"
                >
                  <div className="flex items-center space-x-3 flex-1 min-w-0">
                    <div className={`w-10 h-10 rounded-full flex items-center justify-center flex-shrink-0 ${TRANSACTION_COLORS[transaction.type]}`}>
                      <span className="text-base font-bold">
                        {TRANSACTION_ICONS[transaction.type]}
                      </span>
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-sm text-gray-900 truncate">
                        {transaction.merchantName || transaction.category}
                      </p>
                      <p className="text-xs text-gray-500">
                        {formatDate(new Date(transaction.transactionDate))}
                      </p>
                    </div>
                  </div>
                  <div className="text-right flex-shrink-0 ml-2">
                    <p className={`text-base font-semibold ${
                      transaction.type === 'DEPOSIT' ? 'text-green-600' : 'text-red-600'
                    }`}>
                      {transaction.type === 'DEPOSIT' ? '+' : '-'}
                      {formatCurrency(transaction.amount)}
                    </p>
                  </div>
                </div>
              ))}
            </div>

            <div className="flex justify-center items-center space-x-3 mt-4">
              <button
                onClick={() => setPage((p) => Math.max(0, p - 1))}
                disabled={page === 0}
                className="px-3 py-2 text-sm bg-gray-200 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed active:bg-gray-300"
              >
                이전
              </button>
              <span className="text-sm text-gray-600">
                {page + 1} / {data.totalPages}
              </span>
              <button
                onClick={() => setPage((p) => Math.min(data.totalPages - 1, p + 1))}
                disabled={page >= data.totalPages - 1}
                className="px-3 py-2 text-sm bg-gray-200 rounded-lg disabled:opacity-50 disabled:cursor-not-allowed active:bg-gray-300"
              >
                다음
              </button>
            </div>
          </>
        ) : (
          <p className="text-center text-sm text-gray-500 py-8">거래 내역이 없습니다</p>
        )}
      </Card>
    </div>
  );
}
