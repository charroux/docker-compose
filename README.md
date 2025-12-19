# Projet Microservices avec Docker Compose

## Description
Projet pédagogique illustrant l'architecture microservices avec deux services REST et une base de données :
- **CustomerService** : Gestion des clients (port 8081)
- **RentalService** : Gestion des locations de voitures (port 8080)
- **MySQL** : Base de données pour stocker les voitures (port 3307)

Ce projet démontre la communication inter-services, la persistance des données et l'orchestration avec Docker Compose.

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
│   │   └── main/
│   │       ├── java/com/rental/
│   │       │   ├── controller/    # Contrôleurs REST
│   │       │   ├── model/         # Entités JPA (Car)
│   │       │   └── repository/    # Repositories Spring Data JPA
│   │       └── resources/
│   │           └── application.properties  # Configuration JPA/MySQL
│   ├── build.gradle
│   ├── Dockerfile
│   └── ...
└── docker-compose.yml      # Configuration d'orchestration Docker + MySQL
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
┌──────────────────────────────────────────────────────────────┐
│              microservices-network (bridge)                   │
│                                                                │
│  ┌─────────────────┐      ┌─────────────────┐               │
│  │ customer-service│◄─────┤ rental-service  │               │
│  │   port 8081     │      │   port 8080     │               │
│  └────────┬────────┘      └────────┬────────┘               │
│           │                         │                         │
│           │                         │                         │
│           │               ┌─────────▼─────────┐              │
│           │               │   rental-mysql    │              │
│           │               │   port 3306       │              │
│           │               │  (MySQL 8.0)      │              │
│           │               └─────────┬─────────┘              │
└───────────┼─────────────────────────┼────────────────────────┘
            │                         │
         (8081)                    (8080)
            │                         │
       ┌────▼─────────────────────────▼────┐
       │   Votre machine (localhost)        │
       │   MySQL accessible sur port 3307   │
       └────────────────────────────────────┘
```
       └─────────────────────────────────────┘
```

#### 4. Dépendances entre services

```yaml
    depends_on:
      rental-mysql:
        condition: service_healthy
      customer-service:
        condition: service_started
```

**Ordre de démarrage et health checks** :
- `depends_on` garantit que les dépendances démarrent **avant** le service
- `condition: service_healthy` : attend que MySQL soit **réellement prêt** (pas juste démarré)
- `condition: service_started` : attend simplement que le conteneur soit démarré
- Important car `rental-service` a besoin d'une connexion MySQL fonctionnelle

**Pourquoi un health check pour MySQL ?**

Sans health check, Docker démarre MySQL mais le service peut ne pas être prêt à accepter des connexions. Le `rental-service` démarrerait trop tôt et crasherait en tentant de se connecter à MySQL. Le health check vérifie régulièrement que MySQL répond aux requêtes avant d'autoriser le démarrage du service dépendant.

```yaml
healthcheck:
  test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-proot_password"]
  interval: 10s      # Vérifie toutes les 10 secondes
  timeout: 5s        # Timeout après 5 secondes
  retries: 5         # 5 tentatives avant d'échouer
  start_period: 30s  # Période de grâce au démarrage
```

#### 5. Volumes persistants

```yaml
volumes:
  rental-mysql-data:
    driver: local
```

```yaml
services:
  rental-mysql:
    volumes:
      - rental-mysql-data:/var/lib/mysql
```

**Pourquoi des volumes ?**
- Par défaut, les données dans un conteneur sont **éphémères** (perdues à l'arrêt)
- Un **volume Docker** persiste les données en dehors du conteneur
- `/var/lib/mysql` est le répertoire où MySQL stocke ses bases de données
- Même si vous supprimez le conteneur avec `docker-compose down`, les données restent
- Pour tout supprimer y compris les volumes : `docker-compose down -v`

**Types de montage :**
```yaml
# Volume nommé (géré par Docker) - RECOMMANDÉ pour les données
volumes:
  - rental-mysql-data:/var/lib/mysql

# Bind mount (dossier local) - utile pour le développement
volumes:
  - ./mysql-data:/var/lib/mysql

# Volume anonyme (créé automatiquement)
volumes:
  - /var/lib/mysql
```

#### 6. Politique de redémarrage

```yaml
    restart: unless-stopped
```

**Options de redémarrage :**
- `no` : ne jamais redémarrer (défaut)
- `always` : toujours redémarrer si le conteneur s'arrête
- `on-failure` : redémarrer uniquement en cas d'erreur
- `unless-stopped` : redémarrer sauf si vous l'arrêtez manuellement

---

## 🗄️ Intégration de la base de données MySQL

### Pourquoi une base de données ?

Dans la version initiale, `RentalService` stockait les voitures **en mémoire** (dans une `ArrayList`). Cela pose plusieurs problèmes :
- Les données sont **perdues** à chaque redémarrage du service
- Impossible de **partager** les données entre plusieurs instances
- Pas de **persistance** des modifications

Avec MySQL, les données sont stockées de manière **persistante** dans une base de données relationnelle.

### Architecture de la persistance

```
┌─────────────────────────────────────────────┐
│          RentalService (Spring Boot)         │
│                                               │
│  ┌────────────────┐      ┌────────────────┐│
│  │ RentalController│─────►│  CarRepository ││
│  │  (REST API)    │      │   (JPA)        ││
│  └────────────────┘      └────────┬───────┘│
│                                     │        │
└─────────────────────────────────────┼────────┘
                                      │
                          Spring Data JPA / Hibernate
                                      │
                          ┌───────────▼──────────┐
                          │   MySQL Database     │
                          │   - Table: cars      │
                          │   - Colonnes:        │
                          │     * plateNumber    │
                          │     * brand          │
                          │     * price          │
                          └──────────────────────┘
```

### Configuration dans docker-compose.yml

#### 1. Service MySQL

```yaml
rental-mysql:
  image: mysql:8.0                    # Image officielle MySQL version 8.0
  container_name: rental-mysql
  environment:
    MYSQL_DATABASE: rentaldb          # Nom de la base à créer
    MYSQL_USER: rental_user           # Utilisateur applicatif
    MYSQL_PASSWORD: rental_password   # Mot de passe de l'utilisateur
    MYSQL_ROOT_PASSWORD: root_password # Mot de passe root
  ports:
    - "3307:3306"                     # Port 3306 du conteneur → 3307 sur l'hôte
  volumes:
    - rental-mysql-data:/var/lib/mysql # Volume pour la persistance
  networks:
    - microservices-network
  healthcheck:
    test: ["CMD", "mysqladmin", "ping", "-h", "localhost", "-u", "root", "-proot_password"]
    interval: 10s
    timeout: 5s
    retries: 5
    start_period: 30s
  restart: unless-stopped
```

**Points importants :**
- **Port mapping** : `3307:3306`
  - Port 3306 est le port MySQL par défaut
  - Mappé sur 3307 sur la machine hôte pour éviter les conflits si vous avez déjà MySQL installé localement
- **Variables d'environnement** :
  - `MYSQL_DATABASE` : crée automatiquement la base `rentaldb` au premier démarrage
  - `MYSQL_USER` et `MYSQL_PASSWORD` : identifiants pour l'application
  - `MYSQL_ROOT_PASSWORD` : mot de passe administrateur

⚠️ **En production**, ne mettez JAMAIS les mots de passe en dur ! Utilisez des secrets Docker ou des variables d'environnement externes.

#### 2. Configuration du RentalService

```yaml
rental-service:
  environment:
    - SPRING_DATASOURCE_URL=jdbc:mysql://rental-mysql:3306/rentaldb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
    - SPRING_DATASOURCE_USERNAME=rental_user
    - SPRING_DATASOURCE_PASSWORD=rental_password
    - SPRING_JPA_HIBERNATE_DDL_AUTO=update
  depends_on:
    rental-mysql:
      condition: service_healthy
```

**URL de connexion décomposée :**
```
jdbc:mysql://rental-mysql:3306/rentaldb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
│    │      │               │    │       └── Paramètres de connexion
│    │      │               │    └────────── Nom de la base de données
│    │      │               └─────────────── Port MySQL
│    │      └─────────────────────────────── Nom du service (DNS Docker)
│    └────────────────────────────────────── Protocole MySQL
└─────────────────────────────────────────── Préfixe JDBC
```

**Paramètres URL :**
- `useSSL=false` : désactive SSL (acceptable en développement sur réseau Docker interne)
- `allowPublicKeyRetrieval=true` : nécessaire pour MySQL 8.0+
- `serverTimezone=UTC` : définit le fuseau horaire

**DDL Auto modes :**
```yaml
SPRING_JPA_HIBERNATE_DDL_AUTO=update  # Met à jour le schéma automatiquement
```
- `none` : ne fait rien (production)
- `validate` : valide le schéma sans le modifier
- `update` : met à jour le schéma si nécessaire (développement)
- `create` : recrée le schéma à chaque démarrage (perte de données !)
- `create-drop` : recrée au démarrage, supprime à l'arrêt

### Côté application Spring Boot

#### 1. Dépendances Gradle (`build.gradle`)

```gradle
dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'  // JPA/Hibernate
    runtimeOnly 'com.mysql:mysql-connector-j'                              // Driver MySQL
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}
```

**Rôle de chaque dépendance :**
- `spring-boot-starter-data-jpa` : fournit JPA, Hibernate et Spring Data
- `mysql-connector-j` : driver JDBC pour communiquer avec MySQL

#### 2. Configuration (`application.properties`)

```properties
# Configuration de la connexion MySQL
spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:mysql://localhost:3307/rentaldb?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:rental_user}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:rental_password}
spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver

# Configuration JPA/Hibernate
spring.jpa.hibernate.ddl-auto=${SPRING_JPA_HIBERNATE_DDL_AUTO:update}
spring.jpa.show-sql=true                                    # Affiche les requêtes SQL
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQLDialect
spring.jpa.properties.hibernate.format_sql=true             # Formate les requêtes SQL
```

**Valeurs par défaut :**
- Le pattern `${VAR:default}` utilise la variable d'environnement `VAR` ou la valeur par défaut
- En local : utilise `localhost:3307`
- En Docker : Docker Compose injecte les bonnes variables d'environnement

#### 3. Entité JPA (`Car.java`)

```java
@Entity                           // Indique que c'est une entité JPA
@Table(name = "cars")             // Nom de la table en base
public class Car {
    
    @Id                           // Clé primaire
    private String plateNumber;   // Champ utilisé comme ID
    
    private String brand;         // Colonne "brand"
    private double price;         // Colonne "price"
    
    // Constructeurs, getters, setters...
}
```

**Annotations JPA :**
- `@Entity` : déclare la classe comme entité persistante
- `@Table(name = "cars")` : nomme la table (sinon utilise le nom de la classe)
- `@Id` : désigne le champ comme clé primaire
- `@GeneratedValue` : génération automatique de l'ID (non utilisé ici car plateNumber est fourni)

**Mapping automatique :**
- `plateNumber` → colonne `plate_number` (ou `plateNumber` selon la config)
- `brand` → colonne `brand`
- `price` → colonne `price`

Si vous voulez personnaliser :
```java
@Column(name = "plate_number", nullable = false, unique = true)
private String plateNumber;
```

#### 4. Repository (`CarRepository.java`)

```java
@Repository
public interface CarRepository extends JpaRepository<Car, String> {
    // Spring Data JPA génère automatiquement l'implémentation !
}
```

**Méthodes automatiques disponibles :**
```java
carRepository.findAll()           // SELECT * FROM cars
carRepository.findById("AA-123")  // SELECT * FROM cars WHERE plate_number = 'AA-123'
carRepository.save(car)           // INSERT ou UPDATE
carRepository.deleteById("AA-123") // DELETE FROM cars WHERE plate_number = 'AA-123'
carRepository.count()             // SELECT COUNT(*) FROM cars
```

**Requêtes personnalisées :**
```java
public interface CarRepository extends JpaRepository<Car, String> {
    List<Car> findByBrand(String brand);                    // WHERE brand = ?
    List<Car> findByPriceLessThan(double price);            // WHERE price < ?
    List<Car> findByBrandAndPriceLessThan(String brand, double price); // WHERE brand = ? AND price < ?
    
    @Query("SELECT c FROM Car c WHERE c.price BETWEEN :min AND :max")
    List<Car> findByPriceRange(@Param("min") double min, @Param("max") double max);
}
```

#### 5. Controller avec Repository (`RentalController.java`)

```java
@RestController
public class RentalController {

    private final CarRepository carRepository;

    // Injection par constructeur (recommandé)
    public RentalController(CarRepository carRepository) {
        this.carRepository = carRepository;
    }

    // Initialisation au démarrage
    @PostConstruct
    public void initDatabase() {
        if (carRepository.count() == 0) {
            logger.info("Initializing database with cars...");
            carRepository.save(new Car("AA-123-BB", "Renault", 45.0));
            carRepository.save(new Car("CC-456-DD", "Peugeot", 50.0));
            // ...
        }
    }

    @GetMapping("/cars")
    public List<Car> getCars() {
        return carRepository.findAll();  // Récupère depuis la base
    }
}
```

**`@PostConstruct` :**
- Méthode appelée automatiquement après l'initialisation du bean
- Idéal pour peupler la base avec des données de test
- Vérifie `count() == 0` pour ne pas dupliquer les données

### Flux de requête complet

Quand vous appelez `GET http://localhost:8080/cars` :

```
1. Client HTTP
   ↓
2. RentalController.getCars()
   ↓
3. carRepository.findAll()
   ↓
4. Spring Data JPA génère : SELECT * FROM cars
   ↓
5. Hibernate exécute la requête
   ↓
6. Driver MySQL envoie via JDBC : jdbc:mysql://rental-mysql:3306/rentaldb
   ↓
7. Réseau Docker achemine vers le conteneur rental-mysql
   ↓
8. MySQL exécute la requête et retourne les lignes
   ↓
9. Hibernate mappe les résultats vers List<Car>
   ↓
10. Spring sérialise en JSON
    ↓
11. Réponse HTTP au client
```

### Persistance des données

**Test de persistance :**
```bash
# Démarrer les services
docker-compose up -d

# Vérifier les voitures
curl http://localhost:8080/cars

# Arrêter les conteneurs
docker-compose down

# Redémarrer
docker-compose up -d

# Les voitures sont toujours là !
curl http://localhost:8080/cars
```

Les données survivent car le volume `rental-mysql-data` persiste même après `docker-compose down`.

**Pour tout réinitialiser :**
```bash
docker-compose down -v  # Le -v supprime les volumes
```

### Connexion directe à MySQL

Pour inspecter la base de données :

```bash
# Depuis votre machine (port 3307)
mysql -h 127.0.0.1 -P 3307 -u rental_user -p
# Mot de passe: rental_password

# Depuis le conteneur
docker-compose exec rental-mysql mysql -u rental_user -p

# Une fois connecté
USE rentaldb;
SHOW TABLES;
SELECT * FROM cars;
DESC cars;
```

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
# Toutes les voitures (récupérées depuis MySQL)
curl http://localhost:8080/cars

# Communication inter-services : RentalService appelle CustomerService
curl http://localhost:8080/customer/Jean%20Dupont
```

**MySQL (port 3307) :**
```bash
# Connexion à la base de données
mysql -h 127.0.0.1 -P 3307 -u rental_user -p
# Mot de passe: rental_password

# Requêtes SQL
USE rentaldb;
SELECT * FROM cars;
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
   ping rental-mysql
   curl http://customer-service:8081/customers
   ```

### Exercice 2 : Vérifier la persistance des données
1. Démarrez les services : `docker-compose up -d`
2. Vérifiez les voitures : `curl http://localhost:8080/cars`
3. Arrêtez les services : `docker-compose down`
4. Redémarrez : `docker-compose up -d`
5. Les voitures sont toujours là ! Le volume a persisté les données.
6. Pour tout supprimer : `docker-compose down -v`

### Exercice 3 : Explorer la base de données
1. Connectez-vous à MySQL :
   ```bash
   docker-compose exec rental-mysql mysql -u rental_user -p
   # Mot de passe: rental_password
   ```
2. Explorez la base :
   ```sql
   USE rentaldb;
   SHOW TABLES;
   DESCRIBE cars;
   SELECT * FROM cars;
   SELECT brand, COUNT(*) FROM cars GROUP BY brand;
   ```
3. Ajoutez une voiture manuellement :
   ```sql
   INSERT INTO cars (plate_number, brand, price) VALUES ('KK-999-LL', 'Tesla', 120.0);
   ```
4. Vérifiez via l'API : `curl http://localhost:8080/cars`
┬────────┘  └───────────────────┘
              │
              │ JDBC
              │
    ┌─────────▼──────────┐
    │   MySQL Database   │
    │   (port 3306)      │
    │                    │
    │ - Database: rentaldb │
    │ - Table: cars      │
    │ - Volume: persisté │
    └────────────────────┘
              │
    ┌─────────▼──────────┐
    │  Docker Network    │
    │ microservices-net  │
    └─c `docker-compose up` (sans -d pour voir les logs)
2. Observez que MySQL démarre d'abord et devient "healthy"
3. Puis rental-service démarre et se connecte à MySQL
4. Vérifiez le statut : `docker-compose ps`

### Exercice 6 : Modifier une variable d'environnement
1. Dans `docker-compose.yml`, changez `CUSTOMER_SERVICE_URL`
2. Redémarrez : `docker-compose up --build`
3. Observez l'impact sur la communication

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
``` avec health checks** garantit que les dépendances sont prêtes
5. **Les ports sont mappés** entre la machine hôte et les conteneurs
6. **Les volumes Docker** assurent la persistance des données
7. **Spring Data JPA** simplifie l'accès aux données avec des repositories
8. **Hibernate** gère automatiquement le mapping objet-relationnel
9--

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
