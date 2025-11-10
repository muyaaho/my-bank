'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '@/components/ui/Button';
import { Input } from '@/components/ui/Input';
import { Card } from '@/components/ui/Card';
import { authApi } from '@/lib/api/endpoints';
import { apiClient } from '@/lib/api/client';
import { useAuthStore } from '@/stores/authStore';
import Link from 'next/link';

const registerSchema = z.object({
  name: z.string().min(2, '이름은 최소 2자 이상이어야 합니다'),
  email: z.string().email('올바른 이메일을 입력해주세요'),
  phoneNumber: z.string().regex(/^[0-9]{10,11}$/, '휴대폰 번호는 10-11자리 숫자여야 합니다'),
  password: z.string().min(8, '비밀번호는 최소 8자 이상이어야 합니다'),
  confirmPassword: z.string(),
}).refine((data) => data.password === data.confirmPassword, {
  message: "비밀번호가 일치하지 않습니다",
  path: ['confirmPassword'],
});

type RegisterFormData = z.infer<typeof registerSchema>;

export default function RegisterPage() {
  const router = useRouter();
  const { setUser } = useAuthStore();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState('');

  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterFormData>({
    resolver: zodResolver(registerSchema),
  });

  const onSubmit = async (data: RegisterFormData) => {
    setIsLoading(true);
    setError('');

    try {
      const { confirmPassword, ...registerData } = data;
      const response = await authApi.register(registerData);

      if (response.success) {
        const { accessToken, refreshToken, user } = response.data;

        // Store tokens
        apiClient.setAuth(accessToken, refreshToken);

        // Update auth store
        setUser(user);

        // Redirect to dashboard
        router.push('/dashboard');
      } else {
        setError(response.error || '회원가입에 실패했습니다');
      }
    } catch (err: any) {
      // Use backend error messages directly for consistent user experience
      let errorMessage = '회원가입에 실패했습니다. 잠시 후 다시 시도해주세요.';

      if (err.response?.data?.message) {
        // Backend provides Korean error messages
        errorMessage = err.response.data.message;
      } else if (err.request) {
        errorMessage = '네트워크 연결을 확인해주세요.';
      }

      setError(errorMessage);
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex flex-col justify-center bg-gradient-to-br from-primary-50 to-primary-100 px-5 py-8">
      <div className="w-full">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-gray-900 mb-2">MyBank 360</h1>
          <p className="text-sm text-gray-600">새로운 계정 만들기</p>
        </div>

        <Card>
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
            {error && (
              <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded">
                {error}
              </div>
            )}

            <Input
              label="이름"
              placeholder="홍길동"
              error={errors.name?.message}
              {...register('name')}
            />

            <Input
              label="이메일"
              type="email"
              placeholder="example@mybank.com"
              error={errors.email?.message}
              {...register('email')}
            />

            <Input
              label="휴대폰 번호"
              type="tel"
              placeholder="01012345678"
              error={errors.phoneNumber?.message}
              {...register('phoneNumber')}
            />

            <Input
              label="비밀번호"
              type="password"
              placeholder="최소 8자 이상"
              error={errors.password?.message}
              {...register('password')}
            />

            <Input
              label="비밀번호 확인"
              type="password"
              placeholder="비밀번호를 다시 입력하세요"
              error={errors.confirmPassword?.message}
              {...register('confirmPassword')}
            />

            <Button
              type="submit"
              className="w-full"
              isLoading={isLoading}
            >
              회원가입
            </Button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-sm text-gray-600">
              이미 계정이 있으신가요?{' '}
              <Link href="/login" className="text-primary-600 hover:text-primary-700 font-medium">
                로그인
              </Link>
            </p>
          </div>
        </Card>
      </div>
    </div>
  );
}
