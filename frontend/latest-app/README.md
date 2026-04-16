# Frontend (latest-app)

Aplicação React do SmartTrafficFlow para visualização operacional e análise de tráfego.

## Stack

- React 18
- Vite 5
- TypeScript
- Recharts
- Leaflet + React Leaflet
- Vitest + Testing Library

## Executar localmente

```bash
npm install
npm run dev
```

A aplicação sobe na porta `5174`.

## Build e preview

```bash
npm run build
npm run preview
```

## Testes

```bash
npm test
```

## Configuração de ambiente

Use `./.env.production.example` como base.  
Variável principal:

- `VITE_API_BASE_URL` (ex.: `http://localhost:8080/api`)
