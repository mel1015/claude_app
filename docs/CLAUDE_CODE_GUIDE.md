# Claude Code 개발 필수 기능 가이드

> Claude Code (CLI · 데스크탑 앱 · 웹앱 · IDE 확장)의 개발에 직접 도움이 되는 핵심 기능 정리
>
> **사용 가능한 플랫폼**: 터미널 CLI | 데스크탑 앱 (Mac/Windows) | 웹앱 (claude.ai/code) | IDE 확장 (VS Code, JetBrains)

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
12. [oh-my-claudecode (OMC)](#12-oh-my-claudecode-omc--멀티-에이전트-오케스트레이션)
13. [토큰 절약 전략](#13-토큰-절약-전략)

---

## 1. 슬래시 명령어

### 컨텍스트 관리

| 명령어 | 설명 |
|--------|------|
| `/context` | 현재 컨텍스트 사용량 시각화 (색상 격자) |
| `/compact [focus]` | 대화 히스토리 압축 — 작업 목록은 유지됨 |
| `/clear` | 대화 히스토리 초기화. 별칭: `/reset`, `/new` |

#### /compact focus 상세

`focus` 인자를 주면 압축 요약의 핵심 주제를 지정할 수 있다. focus 없이 실행하면 Claude가 전체 대화를 균등하게 요약하지만, focus를 지정하면 **해당 영역 정보는 최대한 보존하고 나머지는 축약**한다.

```bash
/compact                              # 전체 대화 균등 압축
/compact focus on backend changes     # 백엔드 변경 내용 위주로 보존
/compact focus on signal feature      # 시그널 기능 관련 컨텍스트 우선 보존
/compact focus on the bug we found    # 발견한 버그 맥락 중심으로 보존
```

**언제 focus를 쓸까**:
- 긴 대화 중 특정 기능만 계속 이어서 작업할 때
- 다른 주제 논의가 많았지만 핵심 작업 컨텍스트는 잃고 싶지 않을 때
- 압축 후 남은 컨텍스트에서 특정 결정·코드 변경 이력이 필요할 때

> focus는 자연어로 자유롭게 작성 가능 — 명확할수록 보존 품질이 높아짐

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
| `/fast` | Fast 모드 토글 — **Opus 4.6 전용**, 더 빠른 출력 속도 (더 작은 모델로 다운그레이드 아님) |

**현재 Claude 모델 패밀리**:
| 모델 | ID | 용도 |
|------|----|------|
| Opus 4.7 | `claude-opus-4-7` | 복잡한 추론·아키텍처 설계 |
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
- 백엔드: cd backend && ./gradlew bootRun (Spring Boot 4.0.3 / Java 25)
- 프론트엔드: cd frontend && npm run dev

## 코딩 규칙
- Java: Temurin 25 사용
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

### 터미널 입력 편집 단축키 (readline)

| 단축키 | 동작 |
|--------|------|
| `Ctrl+W` | 커서 앞 단어 삭제 |
| `Ctrl+U` | 커서 앞 전체 삭제 (줄 처음까지) |
| `Ctrl+K` | 커서 뒤 전체 삭제 (줄 끝까지) |
| `Ctrl+A` | 줄 맨 앞으로 이동 |
| `Ctrl+E` | 줄 맨 끝으로 이동 |
| `Ctrl+B` | 한 글자 뒤로 |
| `Ctrl+F` | 한 글자 앞으로 |
| `Alt+B` | 한 단어 뒤로 |
| `Alt+F` | 한 단어 앞으로 |
| `Ctrl+H` | 백스페이스 (한 글자 삭제) |
| `Ctrl+D` | 커서 위치 글자 삭제 |

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

### .claude/settings.local.json 권한 설정

프로젝트 권한은 `.claude/settings.local.json`에 저장됨 (글로벌: `~/.claude/settings.json`).

현재 자동 허용 도구:

```
Bash(curl:*)           Bash(docker ps:*)      Bash(pkill:*)
Bash(gh pr:*)          Bash(npm run:*)         Bash(npm test:*)
Bash(npx vitest:*)     Bash(git add:*)         Bash(git checkout:*)
Bash(git diff:*)       Bash(git merge:*)       Bash(lsof -ti:*)
Bash(docker start:*)   Bash(docker exec:*)     Bash(open -a Docker)
WebSearch
```

> `/update-config` 스킬로 권한 추가 가능

### 활성 MCP 서버

| 서버 | 제공 기능 |
|------|----------|
| `ide` | `getDiagnostics` (TypeScript/Java 진단), `executeCode` (코드 실행) |

### 활성 스킬

| 스킬 | 명령어 | 설명 |
|------|--------|------|
| simplify | `/simplify` | 변경된 코드 품질 자동 개선 |
| loop | `/loop 5m <prompt>` | 정기 반복 작업 |
| claude-api | `/claude-api` | Claude API 연동 가이드 |
| notion | `/notion [제목] - [내용]` | Notion 페이지 생성 |
| keybindings-help | `/keybindings-help` | 키 바인딩 커스터마이징 |
| update-config | `/update-config` | settings.json 권한·훅 설정 |
| fewer-permission-prompts | `/fewer-permission-prompts` | 자동으로 allowedTools 최적화 |
| review | `/review` | 현재 브랜치 PR 코드 리뷰 |
| security-review | `/security-review` | 변경 사항 보안 취약점 검사 |

OMC 스킬은 섹션 12 참고.

### 프로젝트 규칙 (CLAUDE.md 핵심)

```
- Java: Temurin 25
- API 키: backend/.env 에만 저장 (GEMINI_API_KEY, SLACK_WEBHOOK_URL)
- 백엔드 재시작: 수정 후 자동 재시작 (확인 불필요)
- Timeframe.DAILY: 모든 DB 조회에 명시 필수
- MongoDB 프로퍼티: spring.mongodb.uri (Spring Boot 4 변경)
- compact 요약: 한글로 작성
- 시그널 자동 실행: DataCollectionCompletedEvent → SignalScheduler
- KR·US 수집 병렬 실행: CompletableFuture.allOf() (StartupDataCollector)
- 외부 API 보호: Resilience4j RateLimiter·CircuitBreaker·Retry 적용
```

---

> **문제 발생 시**: `/doctor` → 설치/설정 전체 검증
> **비용 과다 시**: `/cost` 확인 → `/compact` 압축 → `/clear` 초기화
> **방향 불확실 시**: `Shift+Tab` → Plan 모드에서 계획 먼저 수립

---

## 12. oh-my-claudecode (OMC) — 멀티 에이전트 오케스트레이션

이 프로젝트에는 OMC가 활성화되어 있음 (`.claude/settings.local.json` → `enabledPlugins`).

### 전문 에이전트 목록

`Agent` 도구의 `subagent_type`에 `oh-my-claudecode:<name>` 형식으로 사용.

| 에이전트 | 모델 | 용도 |
|---------|------|------|
| `explore` | haiku | 코드베이스 탐색, 파일 패턴 검색 |
| `executor` | sonnet | 멀티파일 구현 작업 |
| `debugger` | sonnet | 버그 원인 분석, 스택트레이스 추적 |
| `tracer` | sonnet | 근거 기반 원인 추적 (경쟁 가설 방식) |
| `verifier` | sonnet | 작업 완료 검증, 증거 수집 |
| `test-engineer` | sonnet | 테스트 전략, TDD, 통합/E2E 커버리지 |
| `qa-tester` | sonnet | tmux 기반 CLI 인터랙티브 테스트 |
| `security-reviewer` | sonnet | OWASP 취약점, 보안 감사 |
| `git-master` | sonnet | 커밋, 리베이스, 히스토리 관리 |
| `code-simplifier` | sonnet | 코드 단순화·가독성 개선 (기능 보존) |
| `designer` | sonnet | UI/UX 디자이너-개발자 (프론트엔드) |
| `writer` | haiku | README·API 문서·주석 작성 |
| `scientist` | sonnet | 데이터 분석·리서치 실행 |
| `document-specialist` | sonnet | 외부 문서·레퍼런스 조사 |
| `code-reviewer` | opus | 코드 리뷰, SOLID 원칙 검사 |
| `architect` | opus | 아키텍처 설계, 깊은 분석 (읽기 전용) |
| `analyst` | opus | 요구사항 분석, 사전 계획 컨설팅 |
| `critic` | opus | 작업 계획·코드 다각도 비평 |
| `planner` | opus | 인터뷰 기반 전략 계획 수립 |

### 워크플로우 스킬

```bash
/oh-my-claudecode:autopilot        # 아이디어 → 작동 코드 완전 자동 실행
/oh-my-claudecode:ralph            # 작업 완료까지 자체 루프 반복
/oh-my-claudecode:ultrawork        # 독립 태스크 병렬 고속 처리 (키워드: "ulw")
/oh-my-claudecode:plan             # 인터뷰 기반 전략 계획 수립
/oh-my-claudecode:ralplan          # 요청 가기 전 합의 계획 — autopilot/ralph 전 권장
/oh-my-claudecode:trace            # 버그/이슈 증거 기반 추적
/oh-my-claudecode:ai-slop-cleaner  # AI 생성 코드 품질 정리 (키워드: "deslop")
/oh-my-claudecode:ask              # Claude / Codex / Gemini 어드바이저 라우팅
/oh-my-claudecode:ccg              # Claude + Codex + Gemini 3모델 병렬 분석 후 합성
/oh-my-claudecode:deep-interview   # 모호한 요건 → 소크라테스식 인터뷰 후 자율 실행
/oh-my-claudecode:deep-dive        # trace → deep-interview 2단계 파이프라인
/oh-my-claudecode:sciomc           # 병렬 과학자 에이전트로 종합 분석
/oh-my-claudecode:ultraqa          # test → verify → fix 사이클 반복 (QA 자동화)
/oh-my-claudecode:wiki             # 세션 간 지속되는 프로젝트 지식베이스 관리
/oh-my-claudecode:verify           # 작업 완료 전 실제 동작 검증
```

키워드 트리거 (명령어 없이 대화 중 자동 감지):

| 키워드 | 동작 |
|--------|------|
| `"autopilot"` | autopilot 모드 진입 |
| `"ralph"` | ralph 루프 실행 |
| `"ulw"` | ultrawork 병렬 실행 |
| `"ralplan"` | 계획 합의 후 실행 |
| `"ccg"` | Claude+Codex+Gemini 3모델 분석 |
| `"deep interview"` | 소크라테스식 심층 인터뷰 |
| `"ultrathink"` | 깊은 추론 모드 |
| `"deepsearch"` | 코드베이스 심층 탐색 |
| `"deslop"` / `"anti-slop"` | AI 코드 슬럽 정리 |
| `"cancelomc"` | 현재 OMC 모드 취소 |

### `/ask` 어드바이저 라우팅 (신규)

실행 전 설계·조사가 필요할 때 단일 AI 대신 여러 모델에 동시에 물어보는 기능.

```bash
/oh-my-claudecode:ask              # 대화형 어드바이저 모드
omc ask codex "리팩토링 전략은?"   # Codex에게 직접 질문
omc ask gemini "이 API 패턴 맞나?" # Gemini에게 직접 질문
```

- 결과는 artifact로 캡처 → Claude가 종합 후 실행 계획 제안
- 코드 구현 전 설계 검토, 외부 API 조사, 기술 선택에 활용

### OMC 지식 도구 (Knowledge Tools)

세션을 넘어 지식을 축적하는 도구들 (MCP 통해 사용 가능):

| 도구 | 용도 |
|------|------|
| `wiki_add` / `wiki_query` | 프로젝트 위키 작성·검색 |
| `notepad_write_working` | 현재 작업 메모 (임시) |
| `notepad_write_priority` | 중요 메모 (영구 보존) |
| `notepad_read` | 메모 전체 읽기 |
| `project_memory_add_note` | 프로젝트 장기 메모리 추가 |
| `project_memory_read` | 프로젝트 메모리 조회 |
| `shared_memory_write` | 에이전트 간 공유 메모리 |

### LSP 코드 인텔리전스 도구

MCP를 통해 IDE 수준의 코드 분석 가능:

| 도구 | 용도 |
|------|------|
| `lsp_goto_definition` | 심볼 정의로 이동 |
| `lsp_find_references` | 참조 위치 전체 검색 |
| `lsp_hover` | 타입 정보·문서 조회 |
| `lsp_diagnostics` | 현재 파일 오류·경고 목록 |
| `lsp_rename` | 안전한 심볼 리네임 (전체 파일 반영) |
| `ast_grep_search` | AST 패턴으로 코드 검색 |
| `ast_grep_replace` | AST 패턴 기반 코드 변환 |

**이 프로젝트 활용 예시**:
```
"SignalEvaluator에서 rsi14 참조 위치 모두 찾아줘" → lsp_find_references
"StockDailyCache.timeframe 타입 확인해줘"        → lsp_hover
"timeframe 필드 있는 Java 파일 모두 찾아줘"      → ast_grep_search
```

### OMC 상태 저장 위치

```
.omc/project-memory.json   # 프로젝트 장기 메모리
.omc/plans/                # 수립된 계획 문서
.omc/state/                # 활성 에이전트 상태
```

> OMC 문제 발생 시: `/oh-my-claudecode:omc-doctor`

---

## 13. 토큰 절약 전략

Claude Code는 토큰 소모가 누적되므로 장기 작업일수록 비용 관리가 중요하다.

### 컨텍스트 관리

| 방법 | 효과 |
|------|------|
| `/compact` | 대화 히스토리 압축 (작업 목록 유지) — 가장 효과적 |
| `/context` | 현재 컨텍스트 사용률 시각화 → 80% 넘으면 compact 권장 |
| `/clear` | 컨텍스트 완전 초기화 (새 작업 시작 시) |
| `/cost` | 현재 세션 누적 토큰·비용 확인 |

**compact 타이밍**: 한 기능 완료 시마다 실행하면 누적 증가를 막을 수 있음.

```bash
/compact focus on signal feature   # 특정 영역 중심으로 압축 (나머지 날림)
```

### 모델 선택 전략

비용 차이가 크므로 작업 복잡도에 맞게 선택:

| 모델 | 비용 | 언제 쓸까 |
|------|------|----------|
| Haiku 4.5 | 최저 | 단순 검색·파일 읽기·포맷팅 |
| Sonnet 4.6 | 중간 | 일반 구현·리뷰 (기본 권장) |
| Opus 4.7 | 최고 | 아키텍처 설계·복잡한 디버깅만 |

```bash
/model   # 모델 전환 (← → 화살표로 effort 조정)
```

**OMC 에이전트는 모델을 자동 분배**: `explore` → haiku, `executor` → sonnet, `architect` → opus.  
직접 에이전트 호출 시 `model=haiku`로 명시하면 추가 절약 가능.

### 검색 효율화

파일 전체를 읽는 대신 정확히 필요한 부분만 조회:

```bash
# 비효율 (파일 전체 읽기)
> StockService.java 읽어줘

# 효율 (패턴 검색)
> StockService에서 getTopVolume 메서드 찾아줘   → Grep 사용
> backend/src 아래 *Service.java 파일 목록       → Glob 사용
> lsp_find_references로 특정 메서드 참조 위치 조회
```

### 서브에이전트 활용

긴 조사 작업은 서브에이전트에 위임하면 메인 컨텍스트 오염 없이 결과만 반환받음:

```
"explore 에이전트로 SignalEvaluator 관련 파일 모두 찾아줘"
→ 탐색 결과만 메인 컨텍스트에 요약 반환
```

### 세션 분리 원칙

| 상황 | 권장 행동 |
|------|----------|
| 완전히 다른 기능 작업 시 | `/clear` 후 새 세션 |
| 같은 기능인데 컨텍스트 80%+ | `/compact` |
| 장시간 디버깅 후 방향 전환 | `/compact focus on <새 주제>` |
| 비용 과다 경고 | `/cost` 확인 → Haiku 전환 고려 |

### Extended Thinking 주의

`/model` → effort `high`는 입력 토큰의 수배를 추가 소모:
- 복잡한 알고리즘·아키텍처 결정에만 사용
- 일반 구현 작업에는 `low` 또는 기본 유지
- 작업 전 `/cost`로 현재 소모량 확인 권장
