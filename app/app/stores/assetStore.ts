import { create } from 'zustand';
import { AssetSummary } from '@/types/api';

interface AssetState {
  assets: AssetSummary | null;
  isLoading: boolean;
  error: string | null;
  setAssets: (assets: AssetSummary) => void;
  setLoading: (isLoading: boolean) => void;
  setError: (error: string | null) => void;
  clearAssets: () => void;
}

export const useAssetStore = create<AssetState>((set) => ({
  assets: null,
  isLoading: false,
  error: null,

  setAssets: (assets) => set({ assets, error: null }),
  setLoading: (isLoading) => set({ isLoading }),
  setError: (error) => set({ error }),
  clearAssets: () => set({ assets: null, error: null, isLoading: false }),
}));
