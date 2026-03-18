# Git Workflow - Back Office

## Architecture des branches

```
┌─────────────────────────────────┐
│            main                 │
│      Code stable PROD           │
└─────────────────────────────────┘
              ↑
    cherry-pick (commits validés)
              │
┌─────────────────────────────────┐
│           staging               │
│   Environnement de TEST/RECETTE │
│      Base de données dédiée     │
└─────────────────────────────────┘
        ↑             ↑
   Merge Request  Merge Request
        │             │
┌───────────────┐ ┌───────────────┐
│ feature/dev-A │ │  fix/bug-xxx  │
└───────────────┘ └───────────────┘
```

---

## Étape 1 : Créer la branche staging (une seule fois)

```bash
# Se positionner sur main et le mettre à jour
git checkout main
git pull origin main

# Créer staging depuis main
git checkout -b staging
git push -u origin staging
```

---

## Étape 2 : Intégrer le travail existant dans staging

```bash
# Se positionner sur staging
git checkout staging

# Merger la branche de développement actuelle
git merge origin/sprint2_crud-vehicules-token

# Pousser les changements
git push origin staging
```

---

## Étape 3 : Développer une nouvelle feature

```bash
# Toujours partir de staging à jour
git checkout staging
git pull origin staging

# Créer une nouvelle branche feature
git checkout -b feature/nom-de-la-feature

# Développer...
# ...

# Commiter les changements
git add .
git commit -m "feat: description de la feature"

# Pousser la branche
git push -u origin feature/nom-de-la-feature
```

**→ Créer une Merge Request vers `staging` sur GitLab/GitHub**

---

## Étape 4 : Corriger un bug trouvé sur staging

> ⚠️ **IMPORTANT** : On corrige TOUJOURS depuis `staging`, jamais depuis `main`

```bash
# Partir de staging à jour
git checkout staging
git pull origin staging

# Créer une branche de fix
git checkout -b fix/nom-du-bug

# Corriger le bug...
# ...

# Commiter
git add .
git commit -m "fix: description du bug corrigé"

# Pousser
git push -u origin fix/nom-du-bug
```

**→ Créer une Merge Request vers `staging` sur GitLab/GitHub**

---

## Étape 5 : Passer en production (après validation sur staging)

```bash
# Se positionner sur main
git checkout main
git pull origin main

# Cherry-pick UNIQUEMENT les commits validés depuis staging
git cherry-pick <hash-du-commit-1>
git cherry-pick <hash-du-commit-2>

# Pousser vers production
git push origin main
```

### Trouver les hash des commits à cherry-pick

```bash
# Voir les commits de staging
git log staging --oneline

# Copier les hash des commits validés
```

---

## Résumé des règles

| Règle | Description |
|-------|-------------|
| ✅ Branches feature/fix | Toujours créées depuis `staging` |
| ✅ Merge Request | Toujours vers `staging` |
| ✅ Production | Uniquement via cherry-pick de commits validés |
| ❌ Ne jamais | Créer une branche directement depuis `main` |
| ❌ Ne jamais | Merger directement vers `main` |

---

## Commandes utiles

```bash
# Voir toutes les branches
git branch -a

# Voir l'historique des commits
git log --oneline -10

# Voir les différences avec staging
git diff staging

# Annuler le dernier commit (non poussé)
git reset --soft HEAD~1

# Mettre de côté des modifications
git stash
git stash pop
```

---

## Convention de nommage des branches

- `feature/nom-feature` : Nouvelle fonctionnalité
- `fix/nom-bug` : Correction de bug
- `refactor/description` : Refactoring de code
- `docs/description` : Documentation

## Convention de commits

- `feat:` : Nouvelle fonctionnalité
- `fix:` : Correction de bug
- `refactor:` : Refactoring
- `docs:` : Documentation
- `style:` : Formatage, pas de changement de code
- `test:` : Ajout de tests
