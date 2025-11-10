import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { assetApi } from '../api/endpoints';
import { useAssetStore } from '@/stores/assetStore';

export const useAssets = () => {
  const { setAssets, setLoading, setError } = useAssetStore();

  return useQuery({
    queryKey: ['assets'],
    queryFn: async () => {
      setLoading(true);
      try {
        const response = await assetApi.getAssets();
        if (response.success) {
          setAssets(response.data);
          return response.data;
        }
        throw new Error(response.error || 'Failed to fetch assets');
      } catch (error) {
        const errorMessage = error instanceof Error ? error.message : 'Failed to fetch assets';
        setError(errorMessage);
        throw error;
      } finally {
        setLoading(false);
      }
    },
    staleTime: 5 * 60 * 1000, // 5 minutes
    refetchOnWindowFocus: true,
  });
};

export const useSyncAssets = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => assetApi.syncAssets(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['assets'] });
    },
  });
};
