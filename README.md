# Projet Microservices avec Docker Compose

## Description
Projet pédagogique illustrant l'architecture microservices avec deux services REST :
- **CustomerService** : Gestion des clients (port 8081)
- **RentalService** : Gestion des locations de voitures (port 8080)

Ce projet démontre la communication inter-services et l'orchestration avec Docker Compose.

## Structure du projet
```
docker-compose/
├── CustomerService/         # Microservice de gestion des clients
│   ├── src/
│   ├── build.gradle
│   ├── Dockerfile
│   └── ...
├── RentalService/          # Microservice de gestion des locations
│   ├── src/
│   ├── build.gradle
│   ├── Dockerfile
│   └── ...
└── docker-compose.yml      # Configuration d'orchestration Docker
```

## Prérequis
- Docker (version 20.10+)
- Docker Compose (version 2.0+)

Vérifiez vos installations :
```bash
docker --version
docker-compose --version
```

---

## 📚 Comprendre Docker Compose

### Qu'est-ce que Docker Compose ?

**Docker Compose** est un outil qui permet de définir et d'exécuter des applications multi-conteneurs. Au lieu de gérer chaque conteneur individuellement avec des commandes `docker run`, Docker Compose utilise un fichier YAML pour configurer tous les services de votre application.

### Pourquoi utiliser Docker Compose ?

**Sans Docker Compose**, vous devriez :
1. Créer un réseau Docker manuellement
2. Lancer chaque conteneur avec de longues commandes
3. Gérer les dépendances entre services manuellement
4. Configurer les variables d'environnement pour chaque conteneur

**Avec Docker Compose**, tout cela est défini dans un seul fichier `docker-compose.yml` !

### Anatomie du fichier docker-compose.yml

Analysons notre fichier `docker-compose.yml` section par section :

```yaml
version: '3.8'  # Version de la syntaxe Docker Compose
```
La version définit quelles fonctionnalités sont disponibles.

#### 1. Définition des services

```yaml
services:
  customer-service:        # Nom du service (utilisé comme DNS dans le réseau)
    build:
      context: ./CustomerService    # Dossier contenant le Dockerfile
      dockerfile: Dockerfile        # Nom du Dockerfile à utiliser
    container_name: customer-service  # Nom du conteneur créé
    ports:
      - "8081:8081"        # Mapping: port_hôte:port_conteneur
```

**Explications clés :**
- **Nom du service** (`customer-service`) : sert de nom DNS pour la communication inter-conteneurs
- **build.context** : indique où trouver le code source et le Dockerfile
- **ports** : expose le service sur votre machine locale
  - `8081:8081` signifie : le port 8081 du conteneur est accessible via le port 8081 de votre machine

#### 2. Variables d'environnement

```yaml
    environment:
      - SPRING_APPLICATION_NAME=customer-service
      - SERVER_PORT=8081
      - CUSTOMER_SERVICE_URL=http://customer-service:8081
```

**Pourquoi des variables d'environnement ?**
- Elles permettent de configurer l'application sans modifier le code
- Elles sont injectées dans l'application Spring Boot au démarrage
- Notez l'URL : `http://customer-service:8081` utilise le **nom du service** comme hostname

#### 3. Réseau Docker

```yaml
networks:
  microservices-network:
    driver: bridge
```

**Le réseau virtuel** :
- Crée un réseau isolé pour vos conteneurs
- Les conteneurs peuvent se parler en utilisant leurs noms de service
- Exemple : `rental-service` peut joindre `customer-service` via `http://customer-service:8081`
- Le `driver: bridge` crée un réseau local sur votre machine

**Schéma de communication :**
```
┌─────────────────────────────────────────────────────┐
│          microservices-network (bridge)             │
│                                                       │
│  ┌─────────────────┐      ┌─────────────────┐      │
│  │ customer-service│◄─────┤ rental-service  │      │
│  │   port 8081     │      │   port 8080     │      │
│  └────────┬────────┘      └────────┬────────┘      │
└───────────┼──────────────────────────┼──────────────┘
            │                          │
         (8081)                     (8080)
            │                          │
       ┌────▼──────────────────────────▼────┐
       │      Votre machine (localhost)      │
       └─────────────────────────────────────┘
```

#### 4. Dépendances entre services

```yaml
    depends_on:
      - customer-service
```

**Ordre de démarrage** :
- `depends_on` garantit que `customer-service` démarre **avant** `rental-service`
- Important car `rental-service` a besoin de communiquer avec `customer-service`

#### 5. Politique de redémarrage

```yaml
    restart: unless-stopped
```

**Options de redémarrage :**
- `no` : ne jamais redémarrer (défaut)
- `always` : toujours redémarrer si le conteneur s'arrête
- `on-failure` : redémarrer uniquement en cas d'erreur
- `unless-stopped` : redémarrer sauf si vous l'arrêtez manuellement

---

## 🚀 Démarrage du projet

### 1. Construction et démarrage des services

```bash
docker-compose up --build
```

**Que se passe-t-il ?**
1. Docker lit le fichier `docker-compose.yml`
2. Construit les images Docker pour chaque service (grâce à `--build`)
3. Crée le réseau `microservices-network`
4. Démarre `customer-service` en premier
5. Puis démarre `rental-service`
6. Les logs des deux services s'affichent dans le terminal

**Options utiles :**
```bash
# Démarrer en arrière-plan (mode détaché)
docker-compose up -d

# Reconstruire les images avant de démarrer
docker-compose up --build

# Voir les logs en temps réel
docker-compose logs -f

# Voir les logs d'un seul service
docker-compose logs -f rental-service
```

### 2. Vérifier que les services fonctionnent

```bash
# Lister les conteneurs en cours d'exécution
docker-compose ps

# Vérifier les logs
docker-compose logs
```

### 3. Tester les endpoints

**CustomerService (port 8081) :**
```bash
# Tous les clients
curl http://localhost:8081/customers

# Adresse d'un client spécifique
curl http://localhost:8081/customers/Jean%20Dupont/address
```

**RentalService (port 8080) :**
```bash
# Toutes les voitures
curl http://localhost:8080/cars

# Communication inter-services : RentalService appelle CustomerService
curl http://localhost:8080/customer/Jean%20Dupont
```

### Exemple de communication inter-services

Lorsque vous appelez :
```
http://localhost:8080/customer/Jean%20Dupont
```

**Voici ce qui se passe :**
1. Votre navigateur envoie une requête à `rental-service`
2. `rental-service` exécute la méthode `bonjour()`
3. Cette méthode fait une requête HTTP GET vers :
   ```
   http://customer-service:8081/customers/Jean%20Dupont/address
   ```
4. `customer-service` répond avec l'adresse du client
5. `rental-service` retourne cette adresse au navigateur

**Point important :** Le `rental-service` utilise `http://customer-service:8081` (nom du service) et non `http://localhost:8081` car les conteneurs communiquent via le réseau Docker interne.

### Analyse du code : La méthode bonjour()

Examinons le code de la méthode `bonjour()` dans `RentalController.java` :

```java
@GetMapping("/customer/{name}")
public String bonjour(@PathVariable String name) {
    RestTemplate restTemplate = new RestTemplate();
    String url = customerServiceUrl + "/customers/" + name + "/address";
    logger.info("Requesting URL: " + url);
    String response = restTemplate.getForObject(url, String.class);
    return response;
}
```

**Décomposition ligne par ligne :**

1. **`@GetMapping("/customer/{name}")`**
   - Définit que cette méthode répond aux requêtes HTTP GET sur `/customer/{name}`
   - `{name}` est une variable de chemin qui sera extraite de l'URL

2. **`@PathVariable String name`**
   - Extrait la valeur de `{name}` depuis l'URL et l'injecte dans le paramètre `name`
   - Exemple : `/customer/Jean%20Dupont` → `name = "Jean Dupont"`

3. **`RestTemplate restTemplate = new RestTemplate()`**
   - Crée une instance de `RestTemplate`, un client HTTP fourni par Spring
   - `RestTemplate` permet d'effectuer des requêtes HTTP vers d'autres services

4. **`String url = customerServiceUrl + "/customers/" + name + "/address"`**
   - Construit l'URL complète pour appeler le CustomerService
   - `customerServiceUrl` est injecté depuis `application.properties` (`${customer.service.url}`)
   - En Docker : `http://customer-service:8081/customers/Jean Dupont/address`
   - En local : `http://localhost:8081/customers/Jean Dupont/address`

5. **`restTemplate.getForObject(url, String.class)`**
   - **Envoie une requête HTTP GET** vers l'URL construite
   - **Premier paramètre** : L'URL cible
   - **Deuxième paramètre** : Le type de la réponse attendue (`String.class`)
   - `getForObject` effectue la requête de manière **synchrone** (bloquante)
   - La méthode attend la réponse avant de continuer

6. **`return response`**
   - Retourne la réponse reçue du CustomerService au client HTTP initial

**Alternatives à RestTemplate :**

`RestTemplate` est un client HTTP classique mais d'autres options existent :

```java
// Avec WebClient (réactif, recommandé pour les nouvelles applications)
WebClient webClient = WebClient.create(customerServiceUrl);
String response = webClient.get()
    .uri("/customers/{name}/address", name)
    .retrieve()
    .bodyToMono(String.class)
    .block();

// Avec Feign (client HTTP déclaratif)
@FeignClient(name = "customer-service", url = "${customer.service.url}")
public interface CustomerClient {
    @GetMapping("/customers/{name}/address")
    String getCustomerAddress(@PathVariable String name);
}
```

**Gestion des erreurs :**

La méthode actuelle ne gère pas les erreurs. En production, il faudrait ajouter :

```java
@GetMapping("/customer/{name}")
public String bonjour(@PathVariable String name) {
    try {
        RestTemplate restTemplate = new RestTemplate();
        String url = customerServiceUrl + "/customers/" + name + "/address";
        logger.info("Requesting URL: " + url);
        String response = restTemplate.getForObject(url, String.class);
        return response;
    } catch (HttpClientErrorException e) {
        // Erreur 4xx (client)
        logger.error("Client error: " + e.getStatusCode());
        return "Error: Customer not found";
    } catch (HttpServerErrorException e) {
        // Erreur 5xx (serveur)
        logger.error("Server error: " + e.getStatusCode());
        return "Error: Service unavailable";
    } catch (ResourceAccessException e) {
        // Problème de connexion réseau
        logger.error("Connection error: " + e.getMessage());
        return "Error: Cannot connect to customer service";
    }
}
```

**Timeout et configuration :**

Par défaut, `RestTemplate` n'a pas de timeout. Il est recommandé de le configurer :

```java
@Bean
public RestTemplate restTemplate() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(3000);  // 3 secondes pour établir la connexion
    factory.setReadTimeout(3000);     // 3 secondes pour lire la réponse
    return new RestTemplate(factory);
}
```

---

## 🛠️ Commandes Docker Compose essentielles

### Gestion du cycle de vie

```bash
# Démarrer les services
docker-compose up

# Démarrer en arrière-plan
docker-compose up -d

# Arrêter les services (conteneurs restent créés)
docker-compose stop

# Redémarrer les services
docker-compose restart

# Arrêter et supprimer les conteneurs
docker-compose down

# Tout supprimer (conteneurs, réseaux, volumes)
docker-compose down -v
```

### Surveillance et débogage

```bash
# Afficher les conteneurs actifs
docker-compose ps

# Voir les logs
docker-compose logs

# Suivre les logs en temps réel
docker-compose logs -f

# Logs d'un service spécifique
docker-compose logs -f rental-service

# Exécuter une commande dans un conteneur
docker-compose exec rental-service bash

# Voir les ressources utilisées
docker stats
```

### Construction et mise à jour

```bash
# Reconstruire les images
docker-compose build

# Reconstruire et redémarrer
docker-compose up --build

# Reconstruire sans cache
docker-compose build --no-cache
```

---

## 🔧 Exercices pratiques

### Exercice 1 : Observer la communication réseau
1. Démarrez les services avec `docker-compose up`
2. Dans un autre terminal, accédez au conteneur rental-service :
   ```bash
   docker-compose exec rental-service bash
   ```
3. Testez la résolution DNS :
   ```bash
   ping customer-service
   curl http://customer-service:8081/customers
   ```

### Exercice 2 : Modifier une variable d'environnement
1. Dans `docker-compose.yml`, changez `CUSTOMER_SERVICE_URL`
2. Redémarrez : `docker-compose up --build`
3. Observez l'impact sur la communication

### Exercice 3 : Analyser les logs
1. Générez du trafic en appelant les endpoints
2. Observez les logs : `docker-compose logs -f`
3. Identifiez les requêtes entre services

---

## 📊 Architecture du projet

```
┌──────────────────────────────────────────────────────┐
│                    Navigateur                         │
│              http://localhost:8080/8081              │
└────────────┬─────────────────────┬───────────────────┘
             │                     │
             │                     │
    ┌────────▼─────────┐  ┌───────▼──────────┐
    │ RentalService    │  │ CustomerService   │
    │ (port 8080)      │  │ (port 8081)       │
    │                  │  │                   │
    │ - GET /cars      │  │ - GET /customers  │
    │ - GET /customer/ │──┤ - GET /customers/ │
    │   {name}         │  │   {name}/address  │
    └──────────────────┘  └───────────────────┘
            │                      │
            └──────────┬───────────┘
                       │
              ┌────────▼─────────┐
              │  Docker Network   │
              │ microservices-net │
              └───────────────────┘
```

---

## 🎓 Points clés à retenir

1. **Docker Compose simplifie l'orchestration** de plusieurs conteneurs
2. **Les services communiquent via leurs noms** dans le réseau Docker
3. **Les variables d'environnement** permettent de configurer sans modifier le code
4. **depends_on** contrôle l'ordre de démarrage
5. **Les ports sont mappés** entre la machine hôte et les conteneurs
6. **Un seul fichier YAML** remplace de multiples commandes Docker

---

## 🐛 Dépannage

### Les services ne démarrent pas
```bash
# Vérifier les logs
docker-compose logs

# Vérifier que les ports ne sont pas déjà utilisés
lsof -i :8080
lsof -i :8081
```

### Communication entre services impossible
- Vérifiez que les services sont sur le même réseau
- Utilisez le nom du service (pas `localhost`) pour les appels inter-services
- Vérifiez la variable `CUSTOMER_SERVICE_URL` dans l'application

### Reconstruire complètement
```bash
# Tout supprimer et reconstruire
docker-compose down -v
docker-compose build --no-cache
docker-compose up
```

---

## 📖 Ressources supplémentaires

- [Documentation officielle Docker Compose](https://docs.docker.com/compose/)
- [Référence du fichier docker-compose.yml](https://docs.docker.com/compose/compose-file/)
- [Docker Networking](https://docs.docker.com/network/)

---

## Arrêt du projet

```bash
# Arrêter les services
docker-compose down

# Arrêter et supprimer les volumes
docker-compose down -v
```
