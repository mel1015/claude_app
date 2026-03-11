# Claude Code 사용 가이드

> 이 프로젝트에 설정된 스킬 및 Claude Code 주요 명령어 정리

---

## 목차

1. [개발 생산성 명령어 (핵심)](#1-개발-생산성-명령어-핵심)
2. [코드 검토 & 품질 관리](#2-코드-검토--품질-관리)
3. [컨텍스트 & 메모리 관리](#3-컨텍스트--메모리-관리)
4. [Git & 세션 작업](#4-git--세션-작업)
5. [빌드 & 실행 관리](#5-빌드--실행-관리)
6. [설정 & 환경](#6-설정--환경)
7. [분석 & 통계](#7-분석--통계)
8. [도움말 & 정보](#8-도움말--정보)
9. [실전 팁 & 베스트 프랙티스](#실전-팁--베스트-프랙티스)

---

## 1. 개발 생산성 명령어 (핵심)

### `/simplify` ⭐ (이 프로젝트에 설정됨)

최근 변경 파일들을 검토하고 코드 재사용성·품질·효율성 문제를 자동 수정

| 항목 | 내용 |
|------|------|
| 언제 사용 | 기능 구현 또는 버그 수정 후 |
| 실행 방식 | 코드 재사용·품질·효율성 에이전트 병렬 실행 |
| 팁 | `/simplify focus on memory efficiency` 처럼 초점 지정 가능 |

### `/batch <instruction>` ⭐ (이 프로젝트에 설정됨)

코드베이스 전체에 걸친 대규모 변경을 자동 계획 → 분해 → 병렬 실행

```
/batch migrate src/ from Solid to React
/batch update all API endpoints to use the new authentication
```

**작동 방식**: 코드베이스 연구 → 작업을 5~30개 독립 단위로 분해 → 워크트리 에이전트 병렬 실행 → 테스트 + PR 자동 생성

### `/debug [description]` ⭐ (이 프로젝트에 설정됨)

현재 Claude Code 세션의 디버그 로그를 읽고 문제 자동 진단

```
/debug Redis 연결 에러가 반복될 때
```

---

## 2. 코드 검토 & 품질 관리

### `/review [PR]`

| 사용법 | 설명 |
|--------|------|
| `/review` | 열린 PR 목록 표시 |
| `/review 123` | PR #123 검토 |
| `/review <URL>` | 직접 URL 지정 |

검토 항목: 코드 품질·정확성·보안 취약점·인젝션 공격·인증 문제·데이터 노출

> 실행 요구사항: `gh` CLI 설치 필요

### `/security-review` (이 프로젝트에 설정됨)

현재 브랜치의 펜딩 변경사항에서 보안 취약점 분석. PR 생성 전 또는 민감한 코드 변경 후 사용.

### `/pr-comments [PR]` (이 프로젝트에 설정됨)

GitHub PR의 모든 댓글을 세션에 로드

| 사용법 | 설명 |
|--------|------|
| `/pr-comments` | 현재 브랜치 PR 자동 감지 |
| `/pr-comments 123` | PR #123 |
| `/pr-comments <URL>` | 직접 URL |

---

## 3. 컨텍스트 & 메모리 관리

### `/context`

현재 컨텍스트 사용량을 색상 격자로 시각화. 큰 작업 시작 전 확인 권장.

### `/compact [instructions]`

대화 히스토리를 압축하여 컨텍스트 확보

```
/compact focus on API changes
```

- 작업 목록이 유지되어 진행 상황 보존
- 압축 요약이 자동으로 **한글** 생성 (이 프로젝트 설정)

### `/memory`

프로젝트 메모리 파일 직접 편집 및 자동 메모리 활성화/비활성화

- 프로젝트별: `~/.claude/projects/[project]/memory/MEMORY.md`
- 수동 문서: `CLAUDE.md`

### `/clear`

대화 히스토리 삭제 + 컨텍스트 해제 (Git 히스토리는 유지). 별칭: `/reset`, `/new`

---

## 4. Git & 세션 작업

### `/fork [name]`

현재 지점에서 새로운 대화 분기 생성. 여러 방안을 동시 탐색하거나 리스크 있는 변경 실험 시 사용.

### `/rewind`

코드 및 대화를 이전 지점으로 복원 또는 요약. 별칭: `/checkpoint`

### `/resume [session]`

이전 세션 재개. 별칭: `/continue`

| 사용법 | 설명 |
|--------|------|
| `/resume` | 세션 선택기 표시 |
| `/resume my-project-setup` | 이름으로 복구 |

### `/rename [name]`

현재 세션에 의미 있는 이름 지정

```
/rename implement-auth-flow
/rename           # 자동 이름 생성
```

### `/export [filename]`

세션을 마크다운으로 저장/복사

```
/export           # 대화형 저장/복사 선택
/export session.md
```

---

## 5. 빌드 & 실행 관리

### `/loop [interval] <prompt>` (이 프로젝트에 설정됨)

프롬프트를 정기적으로 반복 실행

```
/loop 5m check if the deploy finished
/loop 30s npm run test
```

### `/tasks`

현재 실행 중인 백그라운드 태스크 조회 및 관리. 단축키: `Ctrl+T`

---

## 6. 설정 & 환경

### `/config`

권한·모델·명령어 등 모든 설정 관리. 별칭: `/settings`

### `/model [model]`

즉시 모델 전환 (현재 응답 대기 없이). 좌/우 화살표로 노력도 조정.

### `/keybindings` (이 프로젝트에 설정됨)

`~/.claude/keybindings.json` 생성/편집. Ctrl+S, 다중 키 조합(chord) 설정 등.

### `/statusline` (이 프로젝트에 설정됨)

터미널 프롬프트 옆에 컨텍스트·비용·토큰 수 실시간 표시.

### `/terminal-setup`

Shift+Enter 등 터미널별 맞춤 키 설정. VS Code·Alacritty·Warp·Zed 대상.
> iTerm2·WezTerm·Ghostty·Kitty는 기본 지원

---

## 7. 분석 & 통계

### `/cost`

현재 세션의 토큰 비용 조회. 큰 작업 전후로 비교하여 효율성 측정.

### `/insights`

최근 Claude Code 세션 패턴 분석 (프로젝트별 상호작용·마찰점·개선 영역)

### `/stats`

일일 사용량·세션 히스토리·연속 사용일·모델 선호도 시각화

---

## 8. 도움말 & 정보

### `/help`

현재 환경에서 사용 가능한 모든 명령어 표시

### `/skills`

프로젝트에 로드된 모든 스킬 확인

**이 프로젝트에 설정된 스킬**:
- `/keybindings-help` — 키 바인딩 커스터마이징
- `/debug` — 세션 자동 진단
- `/simplify` — 코드 품질 자동 개선
- `/batch` — 대규모 병렬 변경
- `/claude-api` — Claude API 연동 지원
- `/pr-comments` — PR 댓글 로드
- `/statusline` — 터미널 상태 라인 설정
- `/review` — PR 자동 검토
- `/security-review` — 보안 자동 스캔
- `/loop` — 정기 작업 스케줄
- `/insights` — 세션 분석 리포트

### `/doctor`

Claude Code 설치·설정·도구 전체 검증. 문제 발생 시 첫 디버깅 단계.

---

## 실전 팁 & 베스트 프랙티스

### 컨텍스트 효율적 관리

```
큰 작업 시작 전  →  /context 확인
30% 초과 시      →  /compact 으로 압축
새 작업 시작 시  →  /clear
중요 규칙은      →  CLAUDE.md 에 명시
```

### 개발 워크플로우

```
/init (프로젝트 첫 시작)
    ↓
코드 변경
    ↓
/simplify (선택)
    ↓
/security-review (보안 민감 코드)
    ↓
/review (PR 생성 전)
    ↓
git commit & PR
```

### 대규모 변경

```
/batch 로 계획 검토
    → 각 워크트리에서 병렬 구현
    → 각자 PR 자동 생성
    → PR 리뷰 통합
```

### 문제 해결

```
예상치 못한 동작 발생
    ↓
/debug [증상 설명]
    ↓
/rewind (필요 시 되돌리기)
    ↓
다시 시도
```

### 장시간 세션

```
1. /tasks 로 진행 상황 추적 (Ctrl+T)
2. 수시로 /cost 로 비용 확인
3. 컨텍스트 과부하 시 /compact + /fork 로 새 세션 분기
```

---

### 이 프로젝트 특화 설정

| 설정 | 내용 |
|------|------|
| 언어 | 한글 응답·압축 요약 자동 한글 생성 |
| 허용 도구 | `curl`, `docker ps`, `pkill` 자동 실행 허용 |
| 민감 정보 | API 키·비밀번호는 반드시 `backend/.env` 분리 |
| MCP 서버 | `ide` (진단·코드 실행) |
