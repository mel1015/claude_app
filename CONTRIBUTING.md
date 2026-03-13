# Contributing Guide

## Git 브랜치 전략

### 브랜치 구조

```
main          ← 항상 배포 가능한 안정 브랜치 (production)
develop       ← 통합 개발 브랜치 (staging)
│
├── feat/...  ← 기능 개발
├── fix/...   ← 버그 수정
├── chore/... ← 설정·의존성·CI 등 비기능 변경
└── hotfix/.. ← main에서 직접 분기하는 긴급 수정
```

### 브랜치 명명 규칙

| 유형 | 패턴 | 예시 |
|------|------|------|
| 기능 | `feat/<영역>/<내용>` | `feat/signal/ai-analyze` |
| 버그 | `fix/<영역>/<내용>` | `fix/stock/pagination-reset` |
| 설정 | `chore/<내용>` | `chore/update-dependencies` |
| 긴급 | `hotfix/<내용>` | `hotfix/api-key-leak` |

**영역 구분**: `stock` / `signal` / `news` / `dashboard` / `favorites` / `infra`

### 워크플로우

```bash
# 1. 항상 develop에서 분기
git checkout develop
git pull origin develop
git checkout -b feat/signal/new-condition

# 2. 작업 → 커밋
git add <files>
git commit -m "feat(signal): AND/OR 중첩 조건 UI 추가"

# 3. develop으로 PR → squash merge
git push origin feat/signal/new-condition

# 4. 릴리즈 준비가 되면 develop → main merge
git checkout main
git merge develop --no-ff -m "release: v1.1.0"
git tag v1.1.0
```

### 커밋 메시지 규칙

```
<type>(<scope>): <내용>

type : feat | fix | chore | docs | refactor | test | hotfix | perf
scope: signal | stock | news | dashboard | favorites | backend | frontend | infra
```

### 브랜치 보호 규칙 (GitHub 권장 설정)

| 브랜치 | 직접 push | PR 필요 |
|--------|-----------|---------|
| `main` | 금지 | 필수 |
| `develop` | 허용 | 권장 |
| `feat/*` | 자유 | - |
