import { formatCurrency, formatNumber, formatDate, formatDateTime } from '@/lib/utils/format'

describe('format utils', () => {
  describe('formatCurrency', () => {
    it('should format number as KRW currency', () => {
      expect(formatCurrency(1000000)).toBe('₩1,000,000')
    })

    it('should handle decimal numbers', () => {
      expect(formatCurrency(1000000.50)).toBe('₩1,000,001')
    })

    it('should handle zero', () => {
      expect(formatCurrency(0)).toBe('₩0')
    })

    it('should handle negative numbers', () => {
      expect(formatCurrency(-1000000)).toContain('-')
      expect(formatCurrency(-1000000)).toContain('1,000,000')
    })
  })

  describe('formatNumber', () => {
    it('should format number with thousand separators', () => {
      expect(formatNumber(1000000)).toBe('1,000,000')
    })

    it('should handle small numbers', () => {
      expect(formatNumber(100)).toBe('100')
    })
  })

  describe('formatDate', () => {
    it('should format date string', () => {
      const date = new Date('2024-01-15')
      const formatted = formatDate(date)
      expect(formatted).toContain('2024')
      expect(formatted).toContain('1')
      expect(formatted).toContain('15')
    })
  })

  describe('formatDateTime', () => {
    it('should format datetime string', () => {
      const date = new Date('2024-01-15T10:30:00')
      const formatted = formatDateTime(date)
      expect(formatted).toContain('2024')
      expect(formatted).toContain('10')
      expect(formatted).toContain('30')
    })
  })
})
