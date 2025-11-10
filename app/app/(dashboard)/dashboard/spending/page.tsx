'use client';

import { useState } from 'react';
import { useSpendingAnalysis } from '@/lib/hooks/useSpending';
import { Card } from '@/components/ui/Card';
import { Loading } from '@/components/ui/Loading';
import { formatCurrency } from '@/lib/utils/format';
import { BarChart, Bar, XAxis, YAxis, Tooltip, CartesianGrid, ResponsiveContainer, PieChart, Pie, Cell, Legend } from 'recharts';

const COLORS = ['#3B82F6', '#10B981', '#F59E0B', '#EF4444', '#8B5CF6', '#EC4899', '#14B8A6', '#F97316'];

export default function SpendingPage() {
  const [daysBack, setDaysBack] = useState(30);
  const { data: spending, isLoading } = useSpendingAnalysis(daysBack);

  if (isLoading) {
    return <Loading message="Loading spending analysis..." />;
  }

  const categoryChartData = spending?.categoryBreakdown.map((item, index) => ({
    name: item.category,
    amount: item.amount,
    count: item.transactionCount,
    average: item.averageAmount,
    color: COLORS[index % COLORS.length],
  })) || [];

  const pieChartData = spending?.categoryBreakdown.slice(0, 8).map((item, index) => ({
    name: item.category,
    value: item.amount,
    color: COLORS[index % COLORS.length],
  })) || [];

  return (
    <div className="space-y-4">
      <div>
        <h1 className="text-2xl font-bold text-gray-900">소비 분석</h1>
        <p className="text-sm text-gray-600 mt-1">소비 패턴을 확인하세요</p>
      </div>

      <div>
        <select
          className="w-full px-3 py-2.5 text-sm border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-primary-500"
          value={daysBack}
          onChange={(e) => setDaysBack(Number(e.target.value))}
        >
          <option value={7}>최근 7일</option>
          <option value={30}>최근 30일</option>
          <option value={90}>최근 90일</option>
          <option value={365}>최근 1년</option>
        </select>
      </div>

      <Card>
        <div className="flex items-center justify-between">
          <div>
            <p className="text-xs font-medium text-gray-600">총 소비금액</p>
            <p className="text-3xl font-bold text-gray-900 mt-1">
              {formatCurrency(spending?.totalSpending || 0)}
            </p>
            <p className="text-xs text-gray-500 mt-1">{spending?.period}</p>
          </div>
          <div className="bg-red-100 p-3 rounded-full">
            <svg className="w-8 h-8 text-red-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3 3v8a3 3 0 003 3z" />
            </svg>
          </div>
        </div>
      </Card>

      <Card title="카테고리별 소비" description="막대 그래프">
        {categoryChartData.length > 0 ? (
          <ResponsiveContainer width="100%" height={250}>
            <BarChart data={categoryChartData}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" angle={-45} textAnchor="end" height={80} style={{ fontSize: '11px' }} />
              <YAxis style={{ fontSize: '11px' }} />
              <Tooltip
                formatter={(value) => formatCurrency(Number(value))}
                contentStyle={{ fontSize: '12px' }}
              />
              <Bar dataKey="amount">
                {categoryChartData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.color} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        ) : (
          <p className="text-center text-sm text-gray-500 py-8">소비 데이터가 없습니다</p>
        )}
      </Card>

      <Card title="카테고리 분포" description="비율 차트">
        {pieChartData.length > 0 ? (
          <ResponsiveContainer width="100%" height={250}>
            <PieChart>
              <Pie
                data={pieChartData}
                cx="50%"
                cy="50%"
                labelLine={false}
                label={({ name, percent }) => `${name}: ${(percent * 100).toFixed(0)}%`}
                outerRadius={70}
                fill="#8884d8"
                dataKey="value"
                style={{ fontSize: '11px' }}
              >
                {pieChartData.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.color} />
                ))}
              </Pie>
              <Tooltip
                formatter={(value) => formatCurrency(Number(value))}
                contentStyle={{ fontSize: '12px' }}
              />
            </PieChart>
          </ResponsiveContainer>
        ) : (
          <p className="text-center text-sm text-gray-500 py-8">데이터가 없습니다</p>
        )}
      </Card>

      <Card title="카테고리 상세" description="상세한 소비 내역">
        {categoryChartData.length > 0 ? (
          <div className="space-y-2">
            {categoryChartData.map((category, index) => {
              const percentage = ((category.amount / (spending?.totalSpending || 1)) * 100).toFixed(0);
              return (
                <div key={index} className="p-3 border border-gray-200 rounded-lg">
                  <div className="flex items-center justify-between mb-2">
                    <div className="flex items-center flex-1 min-w-0">
                      <div
                        className="w-3 h-3 rounded-full mr-2 flex-shrink-0"
                        style={{ backgroundColor: category.color }}
                      ></div>
                      <span className="font-medium text-sm text-gray-900 truncate">{category.name}</span>
                    </div>
                    <span className="text-sm font-semibold text-gray-900 ml-2 flex-shrink-0">
                      {formatCurrency(category.amount)}
                    </span>
                  </div>
                  <div className="flex justify-between text-xs text-gray-600 ml-5">
                    <span>{category.count}건</span>
                    <span>평균 {formatCurrency(category.average)}</span>
                    <span>{percentage}%</span>
                  </div>
                </div>
              );
            })}
          </div>
        ) : (
          <p className="text-center text-sm text-gray-500 py-8">카테고리 데이터가 없습니다</p>
        )}
      </Card>
    </div>
  );
}
