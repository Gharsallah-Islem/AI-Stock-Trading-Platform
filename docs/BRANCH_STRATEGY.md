# 🌿 Branch Strategy & Git Workflow

This document describes the branching strategy and Git workflow for the AI Stock Trading Platform project.

## 📋 Table of Contents

- [Branch Types](#branch-types)
- [Branch Naming Conventions](#branch-naming-conventions)
- [Workflow](#workflow)
- [Release Process](#release-process)
- [Hotfix Process](#hotfix-process)
- [Best Practices](#best-practices)

## 🌳 Branch Types

We follow **Git Flow** branching model with the following branch types:

### Main Branches

#### `main` (Production)
- **Purpose**: Production-ready code only
- **Protected**: Yes
- **Merge from**: `release/*` and `hotfix/*` branches only
- **Deployment**: Automatically deploys to production
- **Tagging**: All merges create a version tag (e.g., `v1.0.0`)

#### `develop` (Integration)
- **Purpose**: Integration branch for ongoing development
- **Protected**: Yes
- **Merge from**: `feature/*`, `bugfix/*`, and `hotfix/*` branches
- **Deployment**: Automatically deploys to staging environment
- **Base for**: New feature and bugfix branches

### Supporting Branches

#### `feature/*` (New Features)
- **Purpose**: Develop new features
- **Branch from**: `develop`
- **Merge to**: `develop`
- **Naming**: `feature/description-of-feature`
- **Lifespan**: Until feature is complete
- **Example**: `feature/add-sentiment-analysis`, `feature/portfolio-optimization`

#### `bugfix/*` (Bug Fixes)
- **Purpose**: Fix non-critical bugs in development
- **Branch from**: `develop`
- **Merge to**: `develop`
- **Naming**: `bugfix/description-of-fix`
- **Lifespan**: Until bug is fixed
- **Example**: `bugfix/fix-login-validation`, `bugfix/resolve-chart-loading`

#### `hotfix/*` (Critical Fixes)
- **Purpose**: Fix critical bugs in production
- **Branch from**: `main`
- **Merge to**: `main` AND `develop`
- **Naming**: `hotfix/description-of-fix`
- **Lifespan**: Until critical bug is fixed
- **Example**: `hotfix/fix-security-vulnerability`, `hotfix/resolve-payment-crash`

#### `release/*` (Release Preparation)
- **Purpose**: Prepare for production release
- **Branch from**: `develop`
- **Merge to**: `main` AND `develop`
- **Naming**: `release/vX.Y.Z`
- **Lifespan**: Until release is deployed
- **Example**: `release/v1.1.0`, `release/v2.0.0`

## 📝 Branch Naming Conventions

### Format
```
<type>/<scope>-<short-description>
```

### Types
- `feature/` - New features
- `bugfix/` - Bug fixes
- `hotfix/` - Critical production fixes
- `release/` - Release preparation
- `docs/` - Documentation updates
- `refactor/` - Code refactoring
- `test/` - Test additions or updates
- `chore/` - Maintenance tasks

### Scope (Optional)
- `frontend` - Angular frontend changes
- `backend` - Spring Boot backend changes
- `ai` - AI/ML Python code changes
- `infra` - Infrastructure/DevOps changes
- `db` - Database changes

### Examples
```
feature/frontend-real-time-updates
bugfix/backend-prediction-timeout
hotfix/security-jwt-validation
release/v1.2.0
docs/api-documentation
refactor/ai-model-optimization
test/frontend-component-coverage
chore/update-dependencies
```

## 🔄 Workflow

### 1. Starting a New Feature

```bash
# Ensure you're on the latest develop
git checkout develop
git pull origin develop

# Create feature branch
git checkout -b feature/your-feature-name

# Work on your feature
git add .
git commit -m "feat: add your feature description"

# Push to remote
git push origin feature/your-feature-name

# Create Pull Request to develop
```

### 2. Working on a Bug Fix

```bash
# Ensure you're on the latest develop
git checkout develop
git pull origin develop

# Create bugfix branch
git checkout -b bugfix/fix-description

# Work on your fix
git add .
git commit -m "fix: resolve issue description"

# Push to remote
git push origin bugfix/fix-description

# Create Pull Request to develop
```

### 3. Creating a Release

```bash
# Branch from develop
git checkout develop
git pull origin develop
git checkout -b release/v1.1.0

# Update version numbers
# - Frontend: package.json
# - Backend: pom.xml
# - Update CHANGELOG.md

git commit -am "chore: bump version to 1.1.0"

# Push release branch
git push origin release/v1.1.0

# Create PR to main (after testing)
# After merge to main, also merge back to develop
```

### 4. Handling Hotfixes

```bash
# Branch from main
git checkout main
git pull origin main
git checkout -b hotfix/critical-fix

# Make the fix
git add .
git commit -m "fix: critical issue description"

# Push to remote
git push origin hotfix/critical-fix

# Create PR to main
# After merge, also merge to develop
```

## 🚀 Release Process

### Step 1: Create Release Branch
```bash
git checkout develop
git pull origin develop
git checkout -b release/v1.1.0
```

### Step 2: Update Version Numbers
```bash
# Frontend (package.json)
"version": "1.1.0"

# Backend (pom.xml)
<version>1.1.0</version>

# Update CHANGELOG.md with release notes
```

### Step 3: Testing
- Run all automated tests
- Perform manual testing
- Security audit
- Performance testing

### Step 4: Merge to Main
```bash
# Create PR from release/v1.1.0 to main
# After approval and merge:

git checkout main
git pull origin main
git tag -a v1.1.0 -m "Release version 1.1.0"
git push origin v1.1.0
```

### Step 5: Merge Back to Develop
```bash
git checkout develop
git pull origin develop
git merge release/v1.1.0
git push origin develop
```

### Step 6: Delete Release Branch
```bash
git branch -d release/v1.1.0
git push origin --delete release/v1.1.0
```

## 🔥 Hotfix Process

### Step 1: Create Hotfix Branch
```bash
git checkout main
git pull origin main
git checkout -b hotfix/fix-critical-bug
```

### Step 2: Make the Fix
```bash
git add .
git commit -m "fix: resolve critical bug"
```

### Step 3: Update Version (Patch)
```bash
# Bump patch version (e.g., 1.1.0 -> 1.1.1)
# Update CHANGELOG.md
```

### Step 4: Merge to Main
```bash
# Create PR to main
# After merge:
git checkout main
git pull origin main
git tag -a v1.1.1 -m "Hotfix version 1.1.1"
git push origin v1.1.1
```

### Step 5: Merge to Develop
```bash
git checkout develop
git pull origin develop
git merge hotfix/fix-critical-bug
git push origin develop
```

## 💡 Best Practices

### Commits

1. **Commit Often**: Small, logical commits are better than large ones
2. **Write Good Messages**: Follow [Conventional Commits](https://www.conventionalcommits.org/)
3. **Test Before Commit**: Ensure code compiles and tests pass

### Pull Requests

1. **Keep PRs Small**: Easier to review and less likely to have conflicts
2. **Update Branch**: Rebase with target branch before creating PR
3. **Describe Changes**: Use the PR template
4. **Request Reviews**: Tag relevant reviewers
5. **Address Feedback**: Respond to all review comments

### Merging

1. **Squash Commits**: For feature branches (keeps history clean)
2. **Merge Commits**: For release and hotfix branches (preserves history)
3. **Delete Branches**: After successful merge
4. **Update Local**: Pull latest changes after merge

### Code Review

1. **Review Promptly**: Don't let PRs sit for too long
2. **Be Constructive**: Provide helpful feedback
3. **Test Locally**: Pull and test the changes
4. **Check All Aspects**: Code quality, tests, documentation

## 🔒 Branch Protection Rules

### `main` Branch
- ✅ Require pull request reviews (2 approvals)
- ✅ Require status checks to pass
- ✅ Require branches to be up to date
- ✅ Require linear history
- ✅ Restrict who can push
- ✅ Require signed commits

### `develop` Branch
- ✅ Require pull request reviews (1 approval)
- ✅ Require status checks to pass
- ✅ Require branches to be up to date
- ❌ Allow force pushes (for maintainers only)

## 📊 Visual Workflow

```
main        ──●─────────────●─────────●──────────●─────>
              │             │         │          │
              │      release/v1.1.0   │     hotfix/
              │             ╱         │          │
develop   ────●───●───●────●───●──────●──────────●───●─>
              │   │   │            │                 │
              │   │   │            │                 │
         feature/ │   bugfix/      │            feature/
              │   │   │            │                 │
              ●───●   ●────●       ●─────●          ●───●
```

## 🎯 Quick Reference

| Action | Command |
|--------|---------|
| Create feature | `git checkout -b feature/name` |
| Create bugfix | `git checkout -b bugfix/name` |
| Create hotfix | `git checkout -b hotfix/name` |
| Create release | `git checkout -b release/vX.Y.Z` |
| Update from develop | `git pull origin develop` |
| Rebase with develop | `git rebase develop` |
| Push branch | `git push origin branch-name` |
| Delete local branch | `git branch -d branch-name` |
| Delete remote branch | `git push origin --delete branch-name` |
| Create tag | `git tag -a vX.Y.Z -m "message"` |
| Push tag | `git push origin vX.Y.Z` |

---

**Remember**: Good branching strategy leads to clean history, easier collaboration, and fewer conflicts!
