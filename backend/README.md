# Application Backend - Gestion d'Hôtels

Application Java utilisant Maven et le framework personnalisé avec connexion PostgreSQL.

## Prérequis

- Java 17+
- Maven 3.6+
- PostgreSQL 12+
- Apache Tomcat 10+ (ou serveur compatible Jakarta EE)

## Configuration de la Base de Données

1. Créer la base de données PostgreSQL:
```sql
CREATE DATABASE hotel_db;
```

2. Exécuter le script SQL depuis le dossier `database/schema.sql`

3. Configurer les variables d'environnement (ou créer un fichier `.env`):
```bash
export DB_URL=jdbc:postgresql://localhost:5432/hotel_db
export DB_USER=postgres
export DB_PASSWORD=postgres
```

## Installation

1. D'abord, compiler et installer le framework dans le repository Maven local:
```bash
cd ../framework
mvn clean install
```

2. Compiler le projet backend:
```bash
cd ../backend
mvn clean package
```

Le fichier WAR sera généré dans `target/back-office.war`

## Déploiement

### Option 1: Tomcat
1. Copier `target/back-office.war` dans le dossier `webapps` de Tomcat
2. Démarrer Tomcat
3. L'application sera accessible sur `http://localhost:8080/back-office/`

### Option 2: Maven Tomcat Plugin
Ajouter dans le `pom.xml` et exécuter:
```bash
mvn tomcat7:run
```

## Structure du Projet

```
backend/
├── src/main/java/com/example/
│   ├── controller/          # Contrôleurs avec annotations du framework
│   │   ├── HomeController.java
│   │   ├── HotelController.java
│   │   └── ReservationController.java
│   ├── dao/                 # Data Access Objects
│   │   ├── HotelDAO.java
│   │   └── ReservationDAO.java
│   ├── database/            # Connexion DB
│   │   └── DatabaseConnection.java
│   └── model/               # Entités
│       ├── Hotel.java
│       └── Reservation.java
├── src/main/webapp/
│   ├── WEB-INF/
│   │   └── web.xml          # Configuration servlet
│   └── *.jsp                # Vues JSP
└── pom.xml
```

## Endpoints API

### Hôtels
- `GET /hotels` - Liste tous les hôtels (JSON)
- `GET /hotel?id=X` - Récupère un hôtel par ID (JSON)
- `GET /hotels/view` - Vue HTML des hôtels
- `POST /hotels` - Crée un nouvel hôtel
  - Params: `nom`, `adresse`
- `POST /hotel/update` - Met à jour un hôtel
  - Params: `id`, `nom`, `adresse`
- `POST /hotel/delete` - Supprime un hôtel
  - Params: `id`

### Réservations
- `GET /reservations` - Liste toutes les réservations (JSON)
- `GET /reservation?id=X` - Récupère une réservation par ID (JSON)
- `GET /reservations/hotel?id=X` - Réservations d'un hôtel (JSON)
- `GET /reservations/view` - Vue HTML des réservations
- `POST /reservations` - Crée une nouvelle réservation
  - Params: `clientId`, `nombrePassager`, `dateHeureArrivee` (format: yyyy-MM-dd'T'HH:mm), `idHotel`
- `POST /reservation/update` - Met à jour une réservation
  - Params: `id`, `clientId`, `nombrePassager`, `dateHeureArrivee`, `idHotel`
- `POST /reservation/delete` - Supprime une réservation
  - Params: `id`

### Pages
- `GET /` - Page d'accueil
- `GET /test` - Page de test du framework

## Exemples d'utilisation

### Créer un hôtel (curl)
```bash
curl -X POST http://localhost:8080/back-office/hotels \
  -d "nom=Grand Hotel&adresse=123 Rue Principale"
```

### Créer une réservation (curl)
```bash
curl -X POST http://localhost:8080/back-office/reservations \
  -d "clientId=C001&nombrePassager=2&dateHeureArrivee=2026-03-15T14:00&idHotel=1"
```

### Récupérer tous les hôtels (JSON)
```bash
curl http://localhost:8080/back-office/hotels
```

## Annotations du Framework Utilisées

- `@Controller` - Marque une classe comme contrôleur
- `@GetMapping("/path")` - Mappe une méthode sur une requête GET
- `@PostMapping("/path")` - Mappe une méthode sur une requête POST
- `@Param("name")` - Injecte un paramètre de requête
- `@Json` - Retourne automatiquement du JSON

## Développement

Pour recompiler rapidement pendant le développement:
```bash
mvn clean compile war:war
```

## Notes

- Le framework scanne automatiquement le package `com.example.controller` pour trouver les contrôleurs
- Les paramètres de méthode sont automatiquement convertis par le framework
- Les exceptions SQL sont propagées - considérez ajouter un gestionnaire d'erreurs global
- Pour la production, utilisez un pool de connexions (HikariCP, etc.)
