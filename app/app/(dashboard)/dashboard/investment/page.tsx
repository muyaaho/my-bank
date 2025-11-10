'use client';

import { useState } from 'react';
import { useInvestmentSummary, useRoundUpToggle } from '@/lib/hooks/useInvestments';
import { useAssets } from '@/lib/hooks/useAssets';
import { Card } from '@/components/ui/Card';
import { Button } from '@/components/ui/Button';
import { Loading } from '@/components/ui/Loading';
import { formatCurrency, formatDateTime } from '@/lib/utils/format';
import { InvestmentType } from '@/types/api';

const INVESTMENT_TYPE_LABELS: Record<InvestmentType, string> = {
  ROUNDUP: 'Round-up Savings',
  MANUAL: 'Manual Investment',
  AUTO: 'Auto Investment',
};

const INVESTMENT_TYPE_COLORS: Record<InvestmentType, string> = {
  ROUNDUP: 'bg-blue-100 text-blue-800',
  MANUAL: 'bg-green-100 text-green-800',
  AUTO: 'bg-purple-100 text-purple-800',
};

export default function InvestmentPage() {
  const [selectedAccountId, setSelectedAccountId] = useState<string>('');
  const { data: investments, isLoading: investmentsLoading } = useInvestmentSummary();
  const { data: assets, isLoading: assetsLoading } = useAssets();
  const { enableRoundUp, disableRoundUp, isLoading: toggleLoading } = useRoundUpToggle();

  const handleToggleRoundUp = (enabled: boolean) => {
    if (!selectedAccountId) {
      alert('Please select an account first');
      return;
    }

    if (enabled) {
      enableRoundUp(selectedAccountId);
    } else {
      disableRoundUp(selectedAccountId);
    }
  };

  if (investmentsLoading || assetsLoading) {
    return <Loading message="Loading investments..." />;
  }

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">투자</h1>
        <p className="text-sm text-gray-600 mt-1">자동 투자로 자산을 늘려보세요</p>
      </div>

      {/* Summary Cards */}
      <div className="grid grid-cols-1 gap-3">
        <Card>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-gray-600">총 투자금액</p>
              <p className="text-2xl font-bold text-gray-900 mt-1">
                {formatCurrency(investments?.totalInvested || 0)}
              </p>
            </div>
            <div className="bg-green-100 p-2.5 rounded-full">
              <svg className="w-6 h-6 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6" />
              </svg>
            </div>
          </div>
        </Card>

        <Card>
          <div className="flex items-center justify-between">
            <div>
              <p className="text-xs font-medium text-gray-600">거스름돈 투자</p>
              <p className="text-2xl font-bold text-gray-900 mt-1">
                {formatCurrency(investments?.totalRoundedUp || 0)}
              </p>
            </div>
            <div className="bg-blue-100 p-2.5 rounded-full">
              <svg className="w-6 h-6 text-blue-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 8h6m-5 0a3 3 0 110 6H9l3 3m-3-6h6m6 1a9 9 0 11-18 0 9 9 0 0118 0z" />
              </svg>
            </div>
          </div>
          <p className="text-xs text-gray-500 mt-2">
            {investments?.totalRoundUpTransactions || 0}회 투자
          </p>
        </Card>
      </div>

      {/* Round-up Settings */}
      <Card title="거스름돈 투자" description="자동으로 잔돈을 투자하세요">
        <div className="space-y-3">
          <p className="text-sm text-gray-700">
            결제 시 금액을 천 원 단위로 올려서 거스름돈을 자동으로 투자합니다.
            예: 3,450원 결제 시 550원이 자동 투자됩니다.
          </p>

          <div>
            <label className="block text-sm font-medium text-gray-700 mb-1.5">
              투자 계좌 선택
            </label>
            <select
              className="w-full px-3 py-2.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
              value={selectedAccountId}
              onChange={(e) => setSelectedAccountId(e.target.value)}
            >
              <option value="">계좌 선택</option>
              {assets?.assets.map((asset) => (
                <option key={asset.id} value={asset.id}>
                  {asset.accountName} - {formatCurrency(asset.balance)}
                </option>
              ))}
            </select>
          </div>

          <div className="flex gap-2">
            <Button
              onClick={() => handleToggleRoundUp(true)}
              isLoading={toggleLoading}
              disabled={!selectedAccountId}
              className="flex-1"
            >
              투자 시작
            </Button>
            <Button
              variant="danger"
              onClick={() => handleToggleRoundUp(false)}
              isLoading={toggleLoading}
              disabled={!selectedAccountId}
              className="flex-1"
            >
              투자 중단
            </Button>
          </div>
        </div>
      </Card>

      {/* Investment History */}
      <Card title="최근 투자 내역" description="최근 투자 활동">
        {investments && investments.recentInvestments.length > 0 ? (
          <div className="space-y-2">
            {investments.recentInvestments.map((investment) => (
              <div
                key={investment.investmentId}
                className="flex items-center justify-between p-3 border border-gray-200 rounded-lg active:bg-gray-50 transition-colors"
              >
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-2 mb-0.5">
                    <p className="font-medium text-sm text-gray-900 truncate">{investment.productName}</p>
                    <span className={`px-1.5 py-0.5 rounded text-xs font-medium flex-shrink-0 ${INVESTMENT_TYPE_COLORS[investment.investmentType]}`}>
                      {INVESTMENT_TYPE_LABELS[investment.investmentType]}
                    </span>
                  </div>
                  <p className="text-xs text-gray-500">
                    {formatDateTime(new Date(investment.investedAt))}
                  </p>
                </div>
                <div className="text-right flex-shrink-0 ml-2">
                  <p className="text-base font-semibold text-green-600">
                    +{formatCurrency(investment.amount)}
                  </p>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <p className="text-center text-sm text-gray-500 py-8">투자 내역이 없습니다</p>
        )}
      </Card>

      {/* Investment Tips */}
      <Card title="투자 팁">
        <div className="grid grid-cols-1 gap-2">
          <div className="p-3 bg-blue-50 rounded-lg">
            <h4 className="font-semibold text-sm text-blue-900 mb-1">작게 시작하기</h4>
            <p className="text-xs text-blue-700">
              거스름돈 투자로 부담 없이 시작하세요. 작은 금액도 모이면 큰돈이 됩니다!
            </p>
          </div>
          <div className="p-3 bg-green-50 rounded-lg">
            <h4 className="font-semibold text-sm text-green-900 mb-1">꾸준함 유지</h4>
            <p className="text-xs text-green-700">
              작은 금액이라도 꾸준히 투자하면 복리 효과로 크게 성장할 수 있습니다.
            </p>
          </div>
        </div>
      </Card>
    </div>
  );
}
