'use client';

import { useQuery } from '@tanstack/react-query';
import { Card } from '@/components/ui/Card';
import { pfmApi } from '@/lib/api/endpoints';
import { formatCurrency } from '@/lib/utils/format';
import { TrendingUp, Wallet, CreditCard, Building2 } from 'lucide-react';
import { AssetType } from '@/types/api';

const COLORS = {
  BANK: '#0ea5e9',
  CARD: '#8b5cf6',
  SECURITIES: '#10b981',
  INSURANCE: '#f59e0b',
  FINTECH: '#ef4444',
};

const ASSET_ICONS = {
  BANK: Building2,
  CARD: CreditCard,
  SECURITIES: TrendingUp,
  INSURANCE: Wallet,
  FINTECH: Wallet,
};

export default function DashboardPage() {
  const { data: assets, isLoading, error } = useQuery({
    queryKey: ['assets'],
    queryFn: async () => {
      const response = await pfmApi.getAssets();
      return response.data;
    },
  });

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="text-gray-500">로딩 중...</div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded-lg">
        자산 정보를 불러오는 중 오류가 발생했습니다.
      </div>
    );
  }

  const chartData = assets?.categoryBreakdown.map((item) => ({
    name: getAssetTypeName(item.assetType),
    value: item.totalValue,
    color: COLORS[item.assetType as keyof typeof COLORS],
  })) || [];

  // Get bank accounts only
  const bankAccounts = assets?.assets.filter((asset) => asset.assetType === 'BANK') || [];

  return (
    <div className="space-y-6">
      {/* Total Balance Card */}
      <Card className="bg-gradient-to-br from-primary-500 to-primary-700 text-white">
        <div className="flex items-center justify-between">
          <div>
            <p className="text-primary-100 text-sm">총 자산</p>
            <h2 className="text-3xl font-bold mt-2">
              {formatCurrency(assets?.totalBalance || 0)}
            </h2>
          </div>
          <Wallet className="w-12 h-12 opacity-50" />
        </div>
      </Card>

      {/* Bank Accounts Section */}
      <div>
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-lg font-bold text-gray-900">내 통장</h2>
          <button className="text-sm text-primary-600 font-medium">
            전체보기
          </button>
        </div>

        {bankAccounts.length > 0 ? (
          <div className="space-y-3">
            {bankAccounts.slice(0, 3).map((account) => (
              <div
                key={account.id}
                className="bg-white rounded-xl p-4 border border-gray-200 shadow-sm"
              >
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-3">
                    <div className="w-12 h-12 bg-primary-100 rounded-full flex items-center justify-center">
                      <Building2 className="w-6 h-6 text-primary-600" />
                    </div>
                    <div>
                      <p className="font-semibold text-gray-900">
                        {account.institutionName}
                      </p>
                      <p className="text-sm text-gray-500">{account.accountName}</p>
                    </div>
                  </div>
                  <div className="text-right">
                    <p className="font-bold text-gray-900">
                      {formatCurrency(account.currentValue)}
                    </p>
                  </div>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="bg-gray-50 rounded-xl p-6 text-center">
            <Building2 className="w-12 h-12 text-gray-400 mx-auto mb-2" />
            <p className="text-gray-500">등록된 은행 계좌가 없습니다</p>
            <button className="mt-3 text-sm text-primary-600 font-medium">
              계좌 연결하기
            </button>
          </div>
        )}
      </div>

      {/* Asset Categories */}
      <div>
        <h2 className="text-lg font-bold text-gray-900 mb-4">자산 분류</h2>
        <div className="grid grid-cols-2 gap-3">
          {assets?.categoryBreakdown.map((category) => {
            const Icon = ASSET_ICONS[category.assetType as keyof typeof ASSET_ICONS];
            const totalBalance = assets?.totalBalance || 1;
            const percentage = ((category.totalValue / totalBalance) * 100).toFixed(0);

            return (
              <div
                key={category.assetType}
                className="bg-white rounded-xl p-4 border border-gray-200"
              >
                <div className="flex items-center mb-2">
                  <div
                    className="p-2 rounded-lg mr-2"
                    style={{ backgroundColor: `${COLORS[category.assetType as keyof typeof COLORS]}20` }}
                  >
                    <Icon
                      className="w-5 h-5"
                      style={{ color: COLORS[category.assetType as keyof typeof COLORS] }}
                    />
                  </div>
                  <span className="text-sm font-medium text-gray-700">
                    {getAssetTypeName(category.assetType)}
                  </span>
                </div>
                <p className="text-lg font-bold text-gray-900">
                  {formatCurrency(category.totalValue)}
                </p>
                <p className="text-xs text-gray-500 mt-1">
                  {percentage}% · {category.count}개
                </p>
              </div>
            );
          })}
        </div>
      </div>

      {/* Recent Transactions or Quick Actions */}
      <div>
        <h2 className="text-lg font-bold text-gray-900 mb-4">빠른 메뉴</h2>
        <div className="grid grid-cols-4 gap-3">
          <button className="flex flex-col items-center p-3 bg-white rounded-xl border border-gray-200">
            <div className="w-12 h-12 bg-primary-100 rounded-full flex items-center justify-center mb-2">
              <CreditCard className="w-6 h-6 text-primary-600" />
            </div>
            <span className="text-xs font-medium text-gray-700">송금</span>
          </button>
          <button className="flex flex-col items-center p-3 bg-white rounded-xl border border-gray-200">
            <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center mb-2">
              <TrendingUp className="w-6 h-6 text-green-600" />
            </div>
            <span className="text-xs font-medium text-gray-700">투자</span>
          </button>
          <button className="flex flex-col items-center p-3 bg-white rounded-xl border border-gray-200">
            <div className="w-12 h-12 bg-purple-100 rounded-full flex items-center justify-center mb-2">
              <Wallet className="w-6 h-6 text-purple-600" />
            </div>
            <span className="text-xs font-medium text-gray-700">계좌</span>
          </button>
          <button className="flex flex-col items-center p-3 bg-white rounded-xl border border-gray-200">
            <div className="w-12 h-12 bg-orange-100 rounded-full flex items-center justify-center mb-2">
              <Building2 className="w-6 h-6 text-orange-600" />
            </div>
            <span className="text-xs font-medium text-gray-700">연결</span>
          </button>
        </div>
      </div>
    </div>
  );
}

function getAssetTypeName(type: AssetType): string {
  const names: Record<AssetType, string> = {
    BANK: '은행',
    CARD: '카드',
    SECURITIES: '증권',
    INSURANCE: '보험',
    FINTECH: '핀테크',
  };
  return names[type] || type;
}
