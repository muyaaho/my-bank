import { useQuery } from '@tanstack/react-query';
import { analyticsApi } from '../api/endpoints';

export const useSpendingAnalysis = (daysBack = 30) => {
  return useQuery({
    queryKey: ['spending-analysis', daysBack],
    queryFn: async () => {
      const response = await analyticsApi.getSpendingAnalysis(daysBack);
      if (response.success) {
        return response.data;
      }
      throw new Error(response.error || 'Failed to fetch spending analysis');
    },
    staleTime: 10 * 60 * 1000, // 10 minutes
  });
};
