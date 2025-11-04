# MyBank Frontend

Modern Next.js frontend for the MyBank personal finance management platform.

## Tech Stack

- **Next.js 14** - React framework with App Router
- **React 18** - UI library
- **TypeScript** - Type-safe development
- **Tailwind CSS** - Utility-first styling
- **TanStack React Query** - Server state management & caching
- **Zustand** - Client state management
- **Axios** - HTTP client with interceptors
- **Recharts** - Data visualization charts
- **React Hook Form** - Form state management
- **Zod** - Schema validation
- **Lucide React** - Icon library
- **date-fns** - Date formatting

## Features

### 🔐 Authentication
- Email/password registration with validation
- JWT-based login
- Kakao OAuth integration
- Automatic token refresh
- Secure session management
- Protected routes

### 📊 Dashboard
- Total assets overview with charts
- Asset distribution (Pie chart)
- Category breakdown by asset type
- Recent assets list
- Real-time balance display

### 💼 Accounts & Transactions
- Transaction history with pagination
- Transaction type filtering
- Balance tracking per transaction
- Visual transaction icons
- Date and category information

### 💳 Payments & Transfers
- Account-to-account transfers
- Recipient management
- Transfer form with validation
- Real-time payment status
- Payment history with status tracking

### 📈 Investments
- Investment portfolio summary
- Round-up savings automation
- Enable/disable round-up per account
- Investment history tracking
- Investment type categorization
- Investment tips and insights

### 💰 Spending Analytics
- Category-based spending breakdown
- Customizable time periods (7/30/90/365 days)
- Interactive bar and pie charts
- Detailed category table
- Anomalous transaction detection
- Daily spending average
- Spending insights

## Project Structure

```
app/
├── __tests__/                  # Jest tests
│   ├── components/ui/         # Component tests
│   ├── lib/utils/            # Utility tests
│   └── stores/               # Store tests
├── app/                       # Next.js App Router
│   ├── (auth)/               # Authentication routes
│   │   ├── login/page.tsx
│   │   └── register/page.tsx
│   ├── (dashboard)/          # Protected dashboard routes
│   │   ├── dashboard/
│   │   │   ├── page.tsx              # Main dashboard
│   │   │   ├── accounts/page.tsx     # Transactions
│   │   │   ├── payment/page.tsx      # Payments & Transfers
│   │   │   ├── investment/page.tsx   # Investments
│   │   │   └── spending/page.tsx     # Analytics
│   │   └── layout.tsx        # Dashboard layout with sidebar
│   ├── globals.css
│   ├── layout.tsx
│   ├── page.tsx              # Root redirect
│   └── providers.tsx         # React Query provider
├── components/               # Reusable components
│   ├── ui/                  # UI primitives
│   │   ├── Button.tsx
│   │   ├── Card.tsx
│   │   ├── Input.tsx
│   │   └── Loading.tsx
│   ├── layout/
│   │   └── Sidebar.tsx      # Navigation sidebar
│   └── ProtectedRoute.tsx   # Auth guard
├── lib/                     # Core libraries
│   ├── api/
│   │   ├── client.ts        # Axios client with interceptors
│   │   └── endpoints.ts     # API endpoint definitions
│   ├── hooks/               # React Query hooks
│   │   ├── useAssets.ts
│   │   ├── useInvestments.ts
│   │   ├── usePayments.ts
│   │   ├── useSpending.ts
│   │   └── useTransactions.ts
│   └── utils/
│       ├── cn.ts            # Class name utility
│       └── format.ts        # Format utilities
├── stores/                  # Zustand stores
│   ├── authStore.ts
│   ├── assetStore.ts
│   ├── investmentStore.ts
│   ├── paymentStore.ts
│   └── transactionStore.ts
├── types/                   # TypeScript types
│   └── api.ts              # API response types
├── jest.config.js
├── jest.setup.js
└── package.json
```

## Getting Started

### Development

```bash
# Install dependencies
npm install

# Run development server
npm run dev
```

The application will be available at http://localhost:3000

### Environment Variables

Create a `.env.local` file:

```env
NEXT_PUBLIC_API_URL=http://localhost:8080
```

### Build

```bash
# Build for production
npm run build

# Start production server
npm start
```

### Docker

```bash
# Build Docker image
docker build -t mybank/frontend:latest .

# Run container
docker run -p 3000:3000 \
  -e NEXT_PUBLIC_API_URL=http://api-gateway:8080 \
  mybank/frontend:latest
```

## API Integration

The frontend integrates with the following backend services via API Gateway (`https://api.mybank.com`):

### Auth Service (`/api/v1/auth/*`)
- `POST /register` - User registration
- `POST /login` - Email/password login
- `POST /kakao/callback` - Kakao OAuth callback
- `POST /logout` - User logout
- `POST /refresh` - Refresh access token
- `GET /me` - Get current user

### PFM Core Service (`/api/v1/pfm/*`)
- `GET /assets` - Get asset summary with category breakdown
- `GET /transactions` - Get transaction history (paginated)
- `GET /spending/analysis?daysBack={days}` - Get spending analysis
- `POST /assets/sync` - Sync assets from external sources

### Payment Service (`/api/v1/payment/*`)
- `POST /transfer` - Execute money transfer
- `GET /{paymentId}` - Get payment details
- `GET /history` - Get payment history (paginated)

### Investment Service (`/api/v1/invest/*`)
- `GET /summary` - Get investment summary
- `POST /roundup/enable/{accountId}` - Enable round-up for account
- `POST /roundup/disable/{accountId}` - Disable round-up for account

## Key Features Implementation

### JWT Authentication

The API client automatically:
- Adds JWT tokens to all requests
- Refreshes expired tokens
- Redirects to login on auth failure

```typescript
// Automatic token handling in lib/api/client.ts
apiClient.get('/api/v1/pfm/assets')
```

### Form Validation

All forms use Zod schemas for validation:

```typescript
const loginSchema = z.object({
  email: z.string().email(),
  password: z.string().min(8),
});
```

### Data Fetching

React Query handles all server state:

```typescript
const { data, isLoading, error } = useQuery({
  queryKey: ['assets'],
  queryFn: async () => {
    const response = await pfmApi.getAssets();
    return response.data;
  },
});
```

### Responsive Design

Mobile-first design with Tailwind CSS:
- Collapsible sidebar on mobile
- Responsive grid layouts
- Touch-friendly interactions

## Deployment

### Kubernetes

Deploy to Kubernetes cluster:

```bash
# Apply Kubernetes manifests
kubectl apply -f k8s/frontend-deployment.yaml

# Access via NodePort
http://localhost:30000
```

### Docker Compose

```bash
# Start all services including frontend
docker-compose up -d frontend
```

## Development Guidelines

### Code Style

- Use TypeScript for type safety
- Follow React best practices
- Use functional components and hooks
- Implement error boundaries
- Add loading states
- Handle errors gracefully

### Component Guidelines

- Keep components small and focused
- Use composition over inheritance
- Implement proper prop types
- Add JSDoc comments for complex logic

### State Management

- Use React Query for server state
- Use Zustand for client state (auth)
- Keep state as local as possible
- Avoid prop drilling with context

## Performance Optimization

- Next.js automatic code splitting
- Image optimization with next/image
- Route prefetching
- React Query caching
- Lazy loading components

## Security

- JWT token management
- XSS protection
- CSRF protection via SameSite cookies
- Secure HTTP-only cookies (when applicable)
- Input validation with Zod
- Output sanitization

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

## License

Private - MyBank Project
