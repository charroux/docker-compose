# Rental Service

## Description
Microservice REST pour la gestion de locations de voitures.

## Structure du projet
```
docker-compose/
├── RentalService/          # Code source du microservice
│   ├── src/
│   ├── build.gradle
│   ├── Dockerfile
│   └── ...
└── docker-compose.yml      # Orchestration Docker
```

## Prérequis
- Docker
- Docker Compose

## Démarrage

### Avec Docker Compose
```bash
docker-compose up --build
```

### Accès à l'API
Le service sera disponible sur `http://localhost:8080`

### Endpoints
- `GET /api/cars` - Retourne la liste des voitures disponibles

### Exemple de réponse
```json
[
  {
    "plateNumber": "AA-123-BB",
    "brand": "Renault",
    "price": 45.0
  },
  {
    "plateNumber": "CC-456-DD",
    "brand": "Peugeot",
    "price": 50.0
  }
]
```

## Arrêt du service
```bash
docker-compose down
```
