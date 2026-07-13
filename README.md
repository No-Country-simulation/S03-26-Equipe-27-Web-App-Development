# SmartTrafficFlow

Este é um projeto de hackathon desenvolvido no contexto da comunidade **NoCountry**.  
O objetivo é centralizar análise de tráfego urbano com:

- coleta e cadastro de registros de tráfego,
- visualização de mapa e indicadores,
- geração de insights e exportações,
- simulação de cenários.

## Demonstração & Infraestrutura

A aplicação está publicada em uma instância **Oracle Cloud Infrastructure (OCI)**, na região São Paulo:

- **Demo:** [http://168.138.135.46:5174/](http://168.138.135.46:5174/)

O deploy da stack completa é orquestrado via **Docker Compose**, subindo quatro serviços em containers:

| Serviço | Descrição |
|---|---|
| `smarttraffic-db` | PostgreSQL + PostGIS (banco geoespacial) |
| `smarttraffic-backend` | API Spring Boot |
| `smarttraffic-frontend` | Aplicação web (Node/Vite) |
| `pgadmin` | Interface de administração do banco |

> Infraestrutura e deploy (provisionamento da instância OCI e configuração do Docker Compose): **André Teixeira**.
> A demo é mantida em ambiente de avaliação e pode ficar indisponível eventualmente.

## Arquitetura

- `backend/`: API Spring Boot (Java 21) com JPA, Flyway e PostgreSQL/PostGIS.
- `frontend/latest-app/`: aplicação React + Vite + TypeScript.
- `infra/`: `docker-compose.yml` para banco, backend, frontend e pgAdmin (perfil opcional).

## Como rodar localmente

### 1. Backend

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### 2. Frontend

```bash
cd frontend/latest-app
npm install
npm run dev
```

### 3. Infra com Docker Compose (opcional)

```bash
cd infra
docker compose --env-file .env up -d --build
```

Para subir também o pgAdmin:

```bash
docker compose --env-file .env --profile admin up -d --build
```

## Variáveis de ambiente

- Backend: use `backend/.env.example` como base.
- Frontend: use `frontend/latest-app/.env.production.example` como base.
- Compose: use `infra/.env.example` como base.

## Fonte do arquivo `export.geojson`

O arquivo de ruas usado na importação do backend **não é versionado** no repositório (o arquivo completo pode ser grande, ex.: ~150 MB).

Ele pode ser gerado pelo Overpass Turbo:

- URL: `https://overpass-turbo.eu/`
- Passos:
1. Acesse a URL.
2. Cole a query abaixo.
3. Ajuste o mapa para a região desejada.
4. Execute (`Run`).
5. Exporte em `Export -> GeoJSON`.
6. Salve como `export.geojson`.

Query exemplo:

```overpass
[out:json][timeout:180];
  area["name"="São Paulo"]["admin_level"="8"]->.sp;
  (
    way["highway"](area.sp);
  );
  out ids tags geom;
```

## Testes

### Backend

```bash
cd backend
./mvnw test
```

### Frontend

```bash
cd frontend/latest-app
npm test
```
