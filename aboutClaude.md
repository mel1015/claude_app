# Claude Code 개발 필수 기능 가이드

> Claude Code CLI의 개발에 직접 도움이 되는 핵심 기능 정리

---

## 목차

1. [슬래시 명령어](#1-슬래시-명령어)
2. [Plan Mode — 코드 작성 전 계획 수립](#2-plan-mode--코드-작성-전-계획-수립)
3. [메모리 시스템 — 컨텍스트 영속성](#3-메모리-시스템--컨텍스트-영속성)
4. [Hooks — 워크플로우 자동화](#4-hooks--워크플로우-자동화)
5. [MCP 서버 — 외부 도구 연동](#5-mcp-서버--외부-도구-연동)
6. [권한 제어 — allowedTools](#6-권한-제어--allowedtools)
7. [멀티모달 입력](#7-멀티모달-입력)
8. [Extended Thinking](#8-extended-thinking)
9. [Background Tasks & Worktree](#9-background-tasks--worktree)
10. [키보드 단축키](#10-키보드-단축키)
11. [이 프로젝트 설정 요약](#11-이-프로젝트-설정-요약)

---

## 1. 슬래시 명령어

### 컨텍스트 관리

| 명령어 | 설명 |
|--------|------|
| `/context` | 현재 컨텍스트 사용량 시각화 (색상 격자) |
| `/compact [focus]` | 대화 히스토리 압축 — 작업 목록은 유지됨 |
| `/clear` | 대화 히스토리 초기화. 별칭: `/reset`, `/new` |

```bash
/compact focus on backend changes   # 특정 영역 중심으로 압축
```

### 세션 관리

| 명령어 | 설명 |
|--------|------|
| `/resume [name]` | 이전 세션 재개. 별칭: `/continue` |
| `/rename [name]` | 현재 세션 이름 지정 (빈칸 시 자동 생성) |

### 모델 & 비용

| 명령어 | 설명 |
|--------|------|
| `/model [id]` | 모델 전환. 좌/우 화살표로 effort 조정 |
| `/cost` | 현재 세션 토큰 비용 조회 |
| `/fast` | Fast 모드 토글 (같은 모델, 출력 속도만 향상) |

**현재 Claude 모델 패밀리**:
| 모델 | ID | 용도 |
|------|----|------|
| Opus 4.6 | `claude-opus-4-6` | 복잡한 추론·아키텍처 설계 |
| Sonnet 4.6 | `claude-sonnet-4-6` | 균형 (기본 권장) |
| Haiku 4.5 | `claude-haiku-4-5-20251001` | 단순 작업·빠른 응답 |

### 메모리 & 설정

| 명령어 | 설명 |
|--------|------|
| `/memory` | 메모리 파일 편집 및 자동 메모리 on/off |
| `/config` | 권한·모델·명령어 전체 설정. 별칭: `/settings` |
| `/mcp` | 연결된 MCP 서버 목록 확인 |

### 환경 & 디버깅

| 명령어 | 설명 |
|--------|------|
| `/doctor` | 설치·설정·도구 전체 검증 |
| `/terminal-setup` | Shift+Enter 등 터미널별 키 설정 |
| `/init` | 프로젝트에 CLAUDE.md 초안 생성 |
| `/bug` | Claude Code 버그 리포트 제출 |
| `/login` / `/logout` | Anthropic 계정 인증 관리 |
| `/vim` | Vim 모드 토글 |

### Tasks

| 명령어 | 설명 |
|--------|------|
| `/tasks` | 백그라운드 태스크 조회. 단축키: `Ctrl+T` |

---

## 2. Plan Mode — 코드 작성 전 계획 수립

코드를 한 줄도 변경하지 않고 구현 전략을 먼저 검토하는 모드.

```
Shift+Tab   → Plan 모드 토글
```

**사용 흐름**:
```
1. Shift+Tab → Plan 모드 진입
2. Claude가 구현 계획 초안 작성 (파일 수정 없음)
3. 사용자가 계획 검토 · 수정 · 보완
4. 승인 → 일반 모드로 돌아와 실제 코드 변경
```

**언제 사용하면 좋은가**:
- 여러 파일에 걸친 복잡한 기능 구현 전
- 아키텍처 변경처럼 되돌리기 어려운 작업 전
- Claude의 접근 방식이 맞는지 먼저 확인하고 싶을 때

> 잘못된 방향으로 코드가 대량 작성되는 것을 방지하는 가장 효과적인 방법

---

## 3. 메모리 시스템 — 컨텍스트 영속성

Claude Code는 두 가지 방식으로 프로젝트 컨텍스트를 유지한다.

### CLAUDE.md — 수동 규칙 문서

프로젝트 루트의 `CLAUDE.md`는 Claude가 **항상 자동으로 읽는** 규칙 파일.

```markdown
# CLAUDE.md 예시

## 실행 명령
- 백엔드: cd backend && ./gradlew bootRun
- 프론트엔드: cd frontend && npm run dev

## 코딩 규칙
- Java: Temurin 17 사용 (21 아님)
- API 키는 backend/.env에만 저장

## 주의사항
- gradlew APP_HOME 버그 수정 완료 — 건드리지 말 것
```

> `/init` 명령어로 현재 프로젝트를 분석해 CLAUDE.md 초안 자동 생성 가능

### Auto Memory — 자동 학습 메모리

Claude가 대화 중 유용한 정보를 자동으로 파일에 저장하고 다음 대화에서 불러옴.

저장 위치: `~/.claude/projects/[project-path]/memory/`

| 타입 | 저장 내용 | 예시 |
|------|----------|------|
| `user` | 사용자 역할·선호·도메인 지식 | "사용자는 백엔드 전문, React 처음" |
| `feedback` | 수정 요청·가이드라인 | "mock 금지, 실 DB 사용" |
| `project` | 진행 작업·목표·의사결정 | "auth 미들웨어 컴플라이언스 이슈로 교체 중" |
| `reference` | 외부 시스템 포인터 | "버그 트래킹: Linear INGEST 프로젝트" |

```bash
/memory   # 메모리 파일 직접 편집
```

**기억시키기**: "이것 기억해줘" → 즉시 메모리 파일에 저장됨
**잊게 하기**: "그거 잊어줘" → 관련 메모리 파일 삭제됨

---

## 4. Hooks — 워크플로우 자동화

Claude Code 이벤트에 반응하는 셸 커맨드를 등록하여 반복 작업 자동화.

### 설정 위치

```bash
~/.claude/settings.json          # 글로벌 (모든 프로젝트)
.claude/settings.json            # 프로젝트별
```

### 이벤트 타입

| 이벤트 | 발생 시점 | 주요 활용 |
|--------|----------|----------|
| `UserPromptSubmit` | 사용자가 메시지 전송 직전 | 입력 전처리, 로깅 |
| `PreToolUse` | 도구 실행 직전 | 위험한 명령 차단 |
| `PostToolUse` | 도구 실행 직후 | 린트·포맷 자동 실행 |
| `Notification` | Claude 알림 발생 시 | 시스템 알림 연동 |
| `Stop` | Claude 응답 완료 시 | 완료 알림, 후처리 |

### 실전 예시

```json
{
  "hooks": {
    "PostToolUse": [
      {
        "matcher": "Edit|Write",
        "hooks": [
          {
            "type": "command",
            "command": "cd frontend && npm run lint --fix 2>/dev/null; exit 0"
          }
        ]
      }
    ],
    "Stop": [
      {
        "hooks": [
          {
            "type": "command",
            "command": "osascript -e 'display notification \"Claude 완료\" with title \"Claude Code\"'"
          }
        ]
      }
    ],
    "PreToolUse": [
      {
        "matcher": "Bash",
        "hooks": [
          {
            "type": "command",
            "command": "echo '[Claude] 명령 실행: ' >> ~/.claude/bash-audit.log"
          }
        ]
      }
    ]
  }
}
```

### Hook의 exit code 활용

- **exit 0**: 정상 처리, Claude 계속 진행
- **exit 2**: Claude 실행 차단 (stdout 메시지가 Claude에게 전달됨)

```bash
# PreToolUse에서 위험한 명령 차단 예시
#!/bin/bash
if echo "$CLAUDE_TOOL_INPUT" | grep -q "DROP TABLE"; then
  echo "위험한 쿼리 감지 — 실행 차단"
  exit 2
fi
```

---

## 5. MCP 서버 — 외부 도구 연동

Model Context Protocol로 외부 서비스를 Claude에 직접 연결. Claude가 DB 조회, API 호출, 파일 시스템 접근 등을 직접 수행할 수 있음.

### 설정

```json
// .claude/mcp.json (프로젝트별)
{
  "mcpServers": {
    "mongodb": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-mongodb"],
      "env": {
        "MONGODB_URI": "mongodb://localhost:27017/stockreport"
      }
    },
    "filesystem": {
      "command": "npx",
      "args": ["-y", "@modelcontextprotocol/server-filesystem", "/Users/sks/Desktop/git"]
    }
  }
}
```

### 확인

```bash
/mcp   # 연결된 MCP 서버 목록 및 상태
```

### 주요 공식 MCP 서버

| 서버 | 패키지 | 기능 |
|------|--------|------|
| MongoDB | `@modelcontextprotocol/server-mongodb` | DB 직접 쿼리 |
| Filesystem | `@modelcontextprotocol/server-filesystem` | 파일 읽기/쓰기 |
| GitHub | `@modelcontextprotocol/server-github` | PR·이슈 관리 |
| Slack | `@modelcontextprotocol/server-slack` | 메시지 전송 |
| PostgreSQL | `@modelcontextprotocol/server-postgres` | SQL 실행 |

**이 프로젝트**: `ide` MCP 서버 연결됨 → `getDiagnostics`, `executeCode` 사용 가능

---

## 6. 권한 제어 — allowedTools

매번 확인 요청 없이 자동 실행할 도구를 미리 허용.

### 설정 위치

```json
// .claude/settings.json
{
  "allowedTools": [
    "Bash(git *)",
    "Bash(npm run *)",
    "Bash(./gradlew *)",
    "Bash(docker ps)",
    "Bash(lsof *)",
    "Bash(pkill *)",
    "Bash(curl *)"
  ]
}
```

### 패턴 문법

| 패턴 | 의미 |
|------|------|
| `"Bash(git *)"` | git으로 시작하는 모든 명령 허용 |
| `"Bash(npm run test)"` | 정확히 `npm run test`만 허용 |
| `"Edit"` | Edit 도구 전체 허용 |
| `"Bash(*)"` | 모든 bash 명령 허용 (주의) |

### /config 에서 대화형 설정

```bash
/config   # 권한 설정 UI 진입
```

---

## 7. 멀티모달 입력

### 이미지/스크린샷

Claude Code는 이미지를 직접 읽고 분석할 수 있음.

```bash
# 터미널에서 이미지 경로 붙여넣기
> /tmp/screenshot.png 이 에러 화면 분석해줘

# 또는 드래그 앤 드롭 (터미널이 지원하는 경우)
```

**활용 예시**:
- 브라우저 스크린샷 → UI 버그 재현 요청
- 에러 화면 캡처 → 원인 분석
- 디자인 목업 → 컴포넌트 구현 요청
- 다이어그램 → 코드 구조 설명

### PDF

```bash
> /path/to/api-spec.pdf 이 스펙대로 API 구현해줘
```

---

## 8. Extended Thinking

Claude가 답변 전에 내부적으로 깊이 사고하는 모드. 복잡한 문제일수록 효과적.

### 활성화 방법

```
/model → 모델 선택 후 → 오른쪽 화살표(→)로 effort 높이기
```

effort 레벨: `low` → `medium` → `high`

### 언제 사용하면 좋은가

| 상황 | 효과 |
|------|------|
| 알고리즘 설계 | 엣지케이스를 스스로 탐색 |
| 아키텍처 결정 | 트레이드오프를 깊이 분석 |
| 복잡한 버그 추적 | 원인을 단계별로 추론 |
| 보안 취약점 검토 | 공격 시나리오를 스스로 시뮬레이션 |

> 토큰 소모가 크게 증가하므로 `/cost`로 비용 확인 권장

---

## 9. Background Tasks & Worktree

### Background Tasks

장시간 걸리는 작업(빌드·테스트 등)을 백그라운드로 실행하고 다른 작업 병행.

```
Ctrl+T   → 백그라운드 태스크 목록 조회 및 관리
```

Claude가 내부적으로 독립 작업을 병렬 에이전트로 자동 분배하기도 함 (파일 검색 등).

### Worktree 격리 모드

에이전트가 임시 git worktree에서 작업 → 메인 브랜치에 영향 없이 실험 가능.

```
변경 없으면 워크트리 자동 삭제
변경 있으면 워크트리 경로 + 브랜치 반환
```

---

## 10. 키보드 단축키

| 단축키 | 동작 |
|--------|------|
| `Shift+Tab` | Plan 모드 토글 |
| `Ctrl+C` | 현재 응답 중단 |
| `Ctrl+T` | 백그라운드 태스크 조회 |
| `Ctrl+R` | 히스토리 검색 (터미널 기본) |
| `↑ / ↓` | 이전/다음 입력 히스토리 |
| `← / →` | `/model` 선택 시 effort 조정 |
| `Shift+Enter` | 줄바꿈 (터미널 설정 필요) |

### Shift+Enter 설정 (터미널별)

```bash
/terminal-setup   # VS Code·Alacritty·Warp·Zed 자동 설정
```

> iTerm2·WezTerm·Ghostty·Kitty는 기본 지원

### 커스텀 키 바인딩

`~/.claude/keybindings.json` 직접 편집:

```json
{
  "bindings": [
    {
      "keys": ["ctrl+s"],
      "action": "submit"
    },
    {
      "keys": ["ctrl+k", "ctrl+r"],
      "action": "run_command",
      "command": "/compact"
    }
  ]
}
```

---

## 11. 이 프로젝트 설정 요약

### .claude/settings.json 주요 설정

```json
{
  "allowedTools": [
    "Bash(git *)", "Bash(npm run *)", "Bash(./gradlew *)",
    "Bash(docker ps)", "Bash(lsof *)", "Bash(pkill *)", "Bash(curl *)"
  ]
}
```

### 활성 MCP 서버

| 서버 | 제공 기능 |
|------|----------|
| `ide` | `getDiagnostics` — TypeScript/Java 진단 |
| `ide` | `executeCode` — 코드 실행 |

### 활성 스킬 (사용자 정의 슬래시 명령어)

| 스킬 | 명령어 | 설명 |
|------|--------|------|
| simplify | `/simplify` | 변경된 코드 품질 자동 개선 |
| loop | `/loop 5m <prompt>` | 정기 반복 작업 |
| claude-api | `/claude-api` | Claude API 연동 가이드 |
| notion | `/notion [제목] - [내용]` | Notion 페이지 생성 |
| keybindings-help | `/keybindings-help` | 키 바인딩 커스터마이징 |

### 프로젝트 규칙 (CLAUDE.md 핵심)

```
- Java: Temurin 17 (21 아님)
- API 키: backend/.env 에만 저장
- 백엔드 재시작: 수정 후 자동 재시작 (확인 불필요)
- Timeframe.DAILY: 모든 DB 조회에 명시 필수
- compact 요약: 한글로 작성
```

---

> **문제 발생 시**: `/doctor` → 설치/설정 전체 검증
> **비용 과다 시**: `/cost` 확인 → `/compact` 압축 → `/clear` 초기화
> **방향 불확실 시**: `Shift+Tab` → Plan 모드에서 계획 먼저 수립
