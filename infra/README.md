# Infraestrutura (Docker Compose)

Orquestração local/runtime dos serviços do SmartTrafficFlow via Docker Compose.

## Serviços

- `smarttraffic-db`: PostgreSQL com PostGIS
- `smarttraffic-backend`: API Spring Boot
- `smarttraffic-frontend`: app React em modo dev
- `pgadmin`: interface administrativa (opcional via profile)

## Subir stack

```bash
docker compose --env-file .env up -d --build
```

Com pgAdmin:

```bash
docker compose --env-file .env --profile admin up -d --build
```

## Derrubar stack

```bash
docker compose down
```

## Variáveis de ambiente

Use `./.env.example` como base e ajuste o arquivo `./.env`.

Variáveis principais:

- `SMARTTRAFFIC_BACKEND_ENV_FILE`
- `SMARTTRAFFIC_FRONTEND_ENV_FILE`
- `SMARTTRAFFIC_DATA_DIR`
- `SMARTTRAFFIC_DB_USER`
- `SMARTTRAFFIC_DB_PASSWORD`
- `SMARTTRAFFIC_DB_NAME`
- `SMARTTRAFFIC_PGADMIN_EMAIL`
- `SMARTTRAFFIC_PGADMIN_PASSWORD`

## GeoJSON de ruas

O backend espera ler o arquivo de ruas por mount em `/runtime-data/export.geojson`.  
No Compose, esse mount vem de `SMARTTRAFFIC_DATA_DIR`.

### Como gerar o arquivo sem versionar no Git

Use o Overpass Turbo para gerar o arquivo localmente:

- `https://overpass-turbo.eu/`

Query exemplo:

```overpass
[out:json][timeout:180];
  area["name"="São Paulo"]["admin_level"="8"]->.sp;
  (
    way["highway"](area.sp);
  );
  out ids tags geom;
```

Depois de executar:

1. Exporte em `Export -> GeoJSON`
2. Salve como `export.geojson`
3. Coloque o arquivo no diretório apontado por `SMARTTRAFFIC_DATA_DIR`
