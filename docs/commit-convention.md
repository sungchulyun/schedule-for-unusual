# 커밋 컨벤션

이 저장소는 모노레포 구조로 운영한다.

- `schedule-api`: 백엔드 API
- `ScheduleApp`: 안드로이드 앱
- `docs`: 요구사항, 스펙, 설계 문서

커밋 메시지는 변경 의도를 빠르게 이해할 수 있도록 짧고 명확하게 작성한다.

## 기본 형식

기본 형식은 아래를 따른다.

```text
type(scope): summary
```

예시:

```text
feat(schedule-api): add monthly shift upsert API
fix(ScheduleApp): correct calendar cell badge overlap
docs(docs): add commit convention guide
refactor(schedule-api): split event validation logic
test(ScheduleApp): add calendar repository tests
chore(root): update gitignore for local docs
```

## 작성 규칙

- `summary`는 가능하면 영어 동사 원형으로 시작한다.
- 첫 글자는 소문자로 시작한다.
- 마침표는 붙이지 않는다.
- 한 커밋에는 하나의 목적만 담는다.
- 기능 추가와 리팩터링, 포맷 변경은 가능한 분리한다.
- 파일 이동이나 이름 변경만 있다면 그 의도가 드러나게 작성한다.

권장 길이:

- 제목은 50자 안팎으로 유지
- 길어질 경우 본문을 추가

## type 규칙

주로 아래 타입을 사용한다.

- `feat`: 사용자 기능 추가
- `fix`: 버그 수정
- `docs`: 문서 추가 또는 수정
- `refactor`: 동작 변화 없는 구조 개선
- `test`: 테스트 추가 또는 수정
- `chore`: 빌드, 설정, 의존성, 저장소 관리성 변경
- `style`: 동작 영향 없는 포맷팅, 정렬, 공백 수정

필요 시 아래 타입도 사용할 수 있다.

- `perf`: 성능 개선
- `build`: 빌드 설정 변경
- `ci`: CI 설정 변경

## scope 규칙

scope는 변경 위치를 빠르게 식별하기 위한 값이다.

권장 scope:

- `schedule-api`
- `ScheduleApp`
- `docs`
- `root`

예시:

- 루트 `.gitignore`, 저장소 설정 변경: `chore(root): ...`
- 안드로이드 UI 수정: `fix(ScheduleApp): ...`
- 백엔드 API 추가: `feat(schedule-api): ...`
- 문서 보강: `docs(docs): ...`

scope가 명확하지 않거나 여러 영역을 동시에 건드릴 때는 아래처럼 처리한다.

- 공통 변경이면 `root` 사용
- 정말 불필요하면 scope 생략 가능

예시:

```text
chore: align repository settings
```

## 본문이 필요한 경우

아래 상황에서는 커밋 본문 추가를 권장한다.

- 왜 이 변경이 필요한지 설명이 필요한 경우
- 호환성 영향이 있는 경우
- 데이터 구조나 API 계약이 바뀌는 경우
- 후속 작업이 필요한 경우

형식 예시:

```text
feat(schedule-api): add monthly shift upsert API

Support bulk overwrite for a target month.
Keep single-day upsert endpoint for manual edits.
Follow-up: add transaction boundary test.
```

## 브레이킹 체인지

호환성 깨짐이 있으면 본문에 반드시 명시한다.

예시:

```text
refactor(schedule-api): rename shift response fields

BREAKING CHANGE: clients must replace shiftCode with shiftType.
```

## 권장 사례

- 작은 커밋 여러 개로 나누되, 각 커밋이 단독으로 이해 가능해야 한다.
- 생성 파일과 소스 변경을 같이 넣을 때는 꼭 필요한 파일만 포함한다.
- 문서 커밋은 구현 커밋과 분리하면 추적이 쉬워진다.

## 피해야 할 예시

아래 같은 메시지는 피한다.

- `update`
- `fix bug`
- `changes`
- `작업중`
- `최종`

이유:

- 변경 목적이 드러나지 않는다.
- 나중에 히스토리 검색이 어렵다.
- 릴리즈 노트나 회고에 활용하기 어렵다.

## 추천 예시 모음

```text
feat(schedule-api): add event create endpoint
fix(schedule-api): reject invalid owner type
refactor(schedule-api): extract request context helper
feat(ScheduleApp): render partner events in month view
fix(ScheduleApp): keep selected month after refresh
test(schedule-api): add shift controller integration test
docs(docs): update backend API spec
chore(root): ignore backend db schema doc
```
