# Seller Dashboard

Web dashboard for poultry sellers to manage orders, products, settlements, and business analytics.

## Tech Stack

- **Framework:** Next.js 16 (App Router) with React 19
- **Language:** TypeScript 5
- **Styling:** Tailwind CSS 4, Radix UI primitives, shadcn/ui patterns
- **State:** Zustand for auth/global state, TanStack React Query for server state
- **Forms:** React Hook Form + Zod validation
- **Charts:** Recharts
- **HTTP:** Axios with JWT auth interceptors

## Getting Started

```bash
npm ci
npm run dev
```

Open http://localhost:3000.

### Environment

Create `.env.local`:

```
NEXT_PUBLIC_API_URL=http://localhost:8082/api
```

The backend must be running on port 8082. See the root [SETUP.md](../SETUP.md) for full instructions.

## Pages

| Route | Description |
|-------|-------------|
| `/login` | Seller authentication |
| `/forgot-password` | Password recovery |
| `/reset-password` | Password reset |
| `/` | Dashboard home (redirects) |
| `/orders` | Order management |
| `/products` | Product catalog |
| `/settlements` | Settlements, wallet, and payouts |
| `/analytics` | Business analytics |
| `/disputes` | Dispute management |
| `/settings` | Profile, bank accounts, notifications, security |

## Scripts

```bash
npm run dev       # Development server
npm run build     # Production build
npm run start     # Start production server
npm run lint      # ESLint
npm test          # Jest tests
```

## Project Structure

```
src/
  app/            Next.js App Router pages and layouts
  components/     UI components (dashboard, shared, ui primitives)
  lib/
    api/          API client and endpoint modules
    hooks/        React Query hooks (useOrders, useSettlements, etc.)
    store/        Zustand stores (auth)
    errors.ts     Error handling utilities
  types/          TypeScript type definitions
```
