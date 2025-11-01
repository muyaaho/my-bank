import { renderHook, act } from '@testing-library/react'
import { useAuthStore } from '@/stores/authStore'

describe('authStore', () => {
  beforeEach(() => {
    // Reset store before each test
    useAuthStore.setState({ user: null, isAuthenticated: false })
  })

  it('should set user and authenticate', () => {
    const { result } = renderHook(() => useAuthStore())

    act(() => {
      result.current.setUser({
        id: 'user-123',
        email: 'test@mybank.com',
        name: 'Test User',
      })
    })

    expect(result.current.user).toEqual({
      id: 'user-123',
      email: 'test@mybank.com',
      name: 'Test User',
    })
    expect(result.current.isAuthenticated).toBe(true)
  })

  it('should clear user and deauthenticate', () => {
    const { result } = renderHook(() => useAuthStore())

    // First set a user
    act(() => {
      result.current.setUser({
        id: 'user-123',
        email: 'test@mybank.com',
        name: 'Test User',
      })
    })

    // Then clear
    act(() => {
      result.current.clearUser()
    })

    expect(result.current.user).toBeNull()
    expect(result.current.isAuthenticated).toBe(false)
  })
})
