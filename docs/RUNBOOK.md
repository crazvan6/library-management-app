# Team runbook — opening PRs, reviews & merging

This repo was prepared with one branch per feature, **authored by the responsible member**.
Each member opens their own pull request and approves a teammate's, so the PR/review history
reflects everyone's contribution.

## 0. One-time setup (bogdiz & Georgian2003)

1. **Accept the GitHub collaborator invite** (email or https://github.com/notifications).
2. Authenticate the CLI as yourself:
   ```bash
   gh auth login        # choose GitHub.com → HTTPS → your account
   ```

## 1. Branches & ownership

| Branch | PR opened by | Reviewed by |
|--------|--------------|-------------|
| `feature/be-1-foundation` | crazvan6 | — (already merged ✅, PR #1) |
| `feature/be-2-security-catalog` | **bogdiz** | Georgian2003 |
| `feature/be-3-lending-tests` | **Georgian2003** | crazvan6 |
| `feature/fe-1-shell-auth` | crazvan6 (PR #3) | Georgian2003 |
| `feature/fe-2-catalog` | **bogdiz** | crazvan6 |
| `feature/fe-3-lending` | **Georgian2003** | bogdiz |
| `feature/deploy-and-docs` | crazvan6 | bogdiz |

## 2. Open the pull requests

**bogdiz:**
```bash
gh pr create --repo crazvan6/library-management-app --base dev --head feature/be-2-security-catalog \
  --title "BE-2: Spring Security (JWT) + catalog" --body "Security, auth, users, books & categories."
gh pr create --repo crazvan6/library-management-app --base dev --head feature/fe-2-catalog \
  --title "FE-2: Catalog UI" --body "Book search/pagination + book & category CRUD."
```

**Georgian2003:**
```bash
gh pr create --repo crazvan6/library-management-app --base dev --head feature/be-3-lending-tests \
  --title "BE-3: Lending, scheduling & tests" --body "Reservations, loans, fines, pagination, 75% coverage, ER diagram."
gh pr create --repo crazvan6/library-management-app --base dev --head feature/fe-3-lending \
  --title "FE-3: Lending UI & dashboards" --body "Reservations, loans, fines flows + role dashboards."
```

**crazvan6** (open the deploy/docs PR):
```bash
gh pr create --repo crazvan6/library-management-app --base dev --head feature/deploy-and-docs \
  --title "Deployment (Docker) + final README & runbook" --body "Dockerfiles, docker-compose, root README, runbook."
```

## 3. Approve teammates' PRs (`gh pr list` to get numbers)

```bash
gh pr review <PR#> --approve --body "Reviewed — LGTM"
```
- **Georgian2003** approves: BE-2, FE-1 (#3)
- **crazvan6** approves: BE-3, FE-2
- **bogdiz** approves: FE-3, deploy-and-docs

## 4. Merge — IN ORDER, with merge commits (never squash)

Squash merge would rewrite the shared commits and break the stacked branches. Always use `--merge`.

```bash
# backend stack
gh pr merge <BE-2#> --merge --delete-branch
gh pr merge <BE-3#> --merge --delete-branch
# frontend stack
gh pr merge <FE-1#> --merge --delete-branch   # PR #3
gh pr merge <FE-2#> --merge --delete-branch
gh pr merge <FE-3#> --merge --delete-branch
# deployment & docs
gh pr merge <deploy-and-docs#> --merge --delete-branch
```

## 5. Promote dev → main

```bash
gh pr create --repo crazvan6/library-management-app --base main --head dev \
  --title "Release: complete application" --body "Backend + frontend + deployment."
gh pr merge --merge   # on that PR
```

After this, `main` holds the complete app and `docker compose up` builds everything (see root README).
