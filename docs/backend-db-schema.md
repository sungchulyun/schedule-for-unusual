# ScheduleApp 백엔드 DB 설계서

## 1. 문서 개요

이 문서는 [backend-requirements.md](/C:/Users/admin/Desktop/schedule/schedule-api/docs/backend-requirements.md)와 [api-spec.md](/C:/Users/admin/Desktop/schedule/schedule-api/docs/api-spec.md)를 기준으로 `ScheduleApp` 백엔드의 MySQL 데이터베이스 구조를 정의한다.

CRUD 구현 직후 인증/인가를 바로 도입할 계획이므로, 본 문서는 처음부터 `users.id`를 중심으로 한 설계를 기본안으로 사용한다.

## 2. 설계 원칙

### 2.1 저장소 원칙

- 주 저장소는 `MySQL`을 사용한다.
- 일정, 근무 스케줄, 그룹, 사용자, 초대 정보는 관계형 모델로 관리한다.

### 2.2 식별 원칙

- 주요 테이블의 PK는 문자열 ID를 사용한다.
- 권장 prefix:
  - `usr_...`
  - `grp_...`
  - `evt_...`
  - `sft_...`
  - `inv_...`

### 2.3 그룹 원칙

- 일정과 근무 스케줄은 모두 `group_id` 기준으로 분리한다.
- 사용자 한 명은 동시에 하나의 그룹에만 속할 수 있다.
- 하나의 그룹은 최대 2명의 사용자로만 구성된다.

### 2.4 소유/표시 원칙

- DB에는 절대값만 저장한다.
- API 표시용 `ownerType = ME | US | PARTNER`는 DB 저장값이 아니라 응답 시 계산한다.
- DB 저장 기준:
  - `subject_type = PERSONAL | SHARED`
  - `owner_user_id`

계산 규칙:

- `subject_type = SHARED` 이면 응답 `ownerType = US`
- `subject_type = PERSONAL` 이고 `owner_user_id = currentUserId` 이면 응답 `ownerType = ME`
- `subject_type = PERSONAL` 이고 `owner_user_id != currentUserId` 이면 응답 `ownerType = PARTNER`

### 2.5 삭제 원칙

- MVP부터 `deleted_at` 기반 소프트 삭제를 적용한다.
- 기본 조회는 항상 `deleted_at is null` 조건을 사용한다.

## 3. 범위 구분

### 3.1 조기 구현 대상 테이블

- `users`
- `couple_groups`
- `couple_group_members`
- `events`
- `shift_schedules`

### 3.2 후속 확장 테이블

- `invites`
- `refresh_tokens`
- `audit_logs`

## 4. ERD 개요

```text
users 1 --- 1 couple_group_members N --- 1 couple_groups
couple_groups 1 --- N events
couple_groups 1 --- N shift_schedules
couple_groups 1 --- N invites

users 1 --- N events(created_by_user_id)
users 1 --- N events(updated_by_user_id)
users 1 --- N events(owner_user_id)
users 1 --- N shift_schedules(created_by_user_id)
users 1 --- N shift_schedules(updated_by_user_id)
```

설명:

- `couple_group_members.user_id`는 unique여야 한다.
- 따라서 한 사용자는 하나의 그룹에만 소속될 수 있다.

## 5. 테이블 상세

## 5.1 `users`

### 컬럼 정의

| 컬럼명 | 타입 | NULL | 제약 | 설명 |
| --- | --- | --- | --- | --- |
| `id` | varchar(40) | N | PK | 사용자 ID |
| `email` | varchar(255) | N | UK | 이메일 |
| `password_hash` | varchar(255) | N |  | 비밀번호 해시 |
| `nickname` | varchar(50) | Y |  | 닉네임 |
| `status` | varchar(20) | N | default `'ACTIVE'` | 사용자 상태 |
| `created_at` | datetime(3) | N |  | 생성 시각 |
| `updated_at` | datetime(3) | N |  | 수정 시각 |

### 인덱스

| 인덱스명 | 컬럼 | 목적 |
| --- | --- | --- |
| `uk_users_email` | `(email)` | 로그인/중복 방지 |

## 5.2 `couple_groups`

### 컬럼 정의

| 컬럼명 | 타입 | NULL | 제약 | 설명 |
| --- | --- | --- | --- | --- |
| `id` | varchar(40) | N | PK | 그룹 ID |
| `name` | varchar(100) | Y |  | 그룹 표시명 |
| `status` | varchar(20) | N | default `'ACTIVE'` | 그룹 상태 |
| `created_at` | datetime(3) | N |  | 생성 시각 |
| `updated_at` | datetime(3) | N |  | 수정 시각 |

## 5.3 `couple_group_members`

### 컬럼 정의

| 컬럼명 | 타입 | NULL | 제약 | 설명 |
| --- | --- | --- | --- | --- |
| `id` | bigint | N | PK, AI | 멤버 관계 ID |
| `group_id` | varchar(40) | N | FK | 그룹 ID |
| `user_id` | varchar(40) | N | FK, UK | 사용자 ID |
| `role` | varchar(20) | N |  | 그룹 역할 |
| `partner_status` | varchar(20) | N | default `'PENDING'` | 연결 상태 |
| `created_at` | datetime(3) | N |  | 생성 시각 |

### 역할값 예시

- `OWNER`
- `PARTNER`

### 상태값 예시

- `PENDING`
- `CONNECTED`
- `REMOVED`

### 제약 조건

- `(group_id, user_id)` unique
- `user_id` unique
- 그룹당 활성 멤버 최대 2명

### 인덱스

| 인덱스명 | 컬럼 | 목적 |
| --- | --- | --- |
| `uk_group_members_group_user` | `(group_id, user_id)` | 중복 가입 방지 |
| `uk_group_members_user` | `(user_id)` | 사용자 단일 그룹 참여 보장 |
| `idx_group_members_group_status` | `(group_id, partner_status)` | 그룹 멤버 조회 |

### 비고

- `user_id` unique로 "사용자 1명은 하나의 그룹에만 참여"는 DB에서 강제할 수 있다.
- "그룹당 최대 2명"은 애플리케이션 트랜잭션과 검증 쿼리로 보장하는 것이 현실적이다.

## 5.4 `events`

### 컬럼 정의

| 컬럼명 | 타입 | NULL | 제약 | 설명 |
| --- | --- | --- | --- | --- |
| `id` | varchar(40) | N | PK | 일정 ID |
| `group_id` | varchar(40) | N | FK | 소속 그룹 ID |
| `title` | varchar(100) | N |  | 일정 제목 |
| `start_date` | date | N |  | 시작일 |
| `end_date` | date | N |  | 종료일 |
| `subject_type` | varchar(20) | N |  | `PERSONAL`, `SHARED` |
| `owner_user_id` | varchar(40) | Y | FK | 개인 일정 소유 사용자 |
| `note` | text | Y |  | 메모 |
| `created_by_user_id` | varchar(40) | N | FK | 생성 사용자 ID |
| `updated_by_user_id` | varchar(40) | Y | FK | 최종 수정 사용자 ID |
| `created_at` | datetime(3) | N |  | 생성 시각 |
| `updated_at` | datetime(3) | N |  | 수정 시각 |
| `deleted_at` | datetime(3) | Y |  | 삭제 시각 |

### 제약 조건

- `start_date <= end_date`
- `subject_type in ('PERSONAL', 'SHARED')`
- `subject_type = 'PERSONAL'`이면 `owner_user_id`는 not null
- `subject_type = 'SHARED'`이면 `owner_user_id`는 null 허용

### 인덱스

| 인덱스명 | 컬럼 | 목적 |
| --- | --- | --- |
| `idx_events_group_deleted_start_end` | `(group_id, deleted_at, start_date, end_date)` | 월간/날짜 조회 |
| `idx_events_group_subject_owner` | `(group_id, subject_type, owner_user_id)` | 주체 계산 보조 |
| `idx_events_created_by_user_id` | `(created_by_user_id)` | 감사/이력 |

### 비고

- `ownerType`은 저장하지 않는다.
- `owner_user_id`는 개인 일정의 실제 소유자다.

## 5.5 `shift_schedules`

### 컬럼 정의

| 컬럼명 | 타입 | NULL | 제약 | 설명 |
| --- | --- | --- | --- | --- |
| `id` | varchar(40) | N | PK | 스케줄 ID |
| `group_id` | varchar(40) | N | FK | 소속 그룹 ID |
| `date` | date | N |  | 근무 날짜 |
| `shift_type` | varchar(20) | N |  | 근무 유형 |
| `created_by_user_id` | varchar(40) | N | FK | 생성 사용자 ID |
| `updated_by_user_id` | varchar(40) | Y | FK | 최종 수정 사용자 ID |
| `created_at` | datetime(3) | N |  | 생성 시각 |
| `updated_at` | datetime(3) | N |  | 수정 시각 |
| `deleted_at` | datetime(3) | Y |  | 삭제 시각 |

### 제약 조건

- `shift_type in ('DAY', 'NIGHT', 'MID', 'EVENING', 'OFF', 'VACATION')`
- `(group_id, date)` unique

### 인덱스

| 인덱스명 | 컬럼 | 목적 |
| --- | --- | --- |
| `uk_shift_schedules_group_date` | `(group_id, date)` | 날짜 중복 방지 |
| `idx_shift_schedules_group_deleted_date` | `(group_id, deleted_at, date)` | 월간/날짜 조회 |

## 5.6 `invites`

초기 파트너 연결은 초대 기반 흐름을 우선 적용한다.

### 컬럼 정의

| 컬럼명 | 타입 | NULL | 제약 | 설명 |
| --- | --- | --- | --- | --- |
| `id` | varchar(40) | N | PK | 초대 ID |
| `group_id` | varchar(40) | N | FK | 대상 그룹 |
| `code` | varchar(100) | N | UK | 초대 코드 |
| `status` | varchar(20) | N | default `'PENDING'` | 초대 상태 |
| `expires_at` | datetime(3) | N |  | 만료 시각 |
| `created_by_user_id` | varchar(40) | N | FK | 생성 사용자 ID |
| `accepted_by_user_id` | varchar(40) | Y | FK | 수락 사용자 ID |
| `accepted_at` | datetime(3) | Y |  | 수락 시각 |
| `created_at` | datetime(3) | N |  | 생성 시각 |

## 5.7 `refresh_tokens`

| 컬럼명 | 타입 | NULL | 제약 | 설명 |
| --- | --- | --- | --- | --- |
| `id` | bigint | N | PK, AI | 토큰 ID |
| `user_id` | varchar(40) | N | FK | 사용자 ID |
| `token_hash` | varchar(255) | N | UK | 토큰 해시 |
| `expires_at` | datetime(3) | N |  | 만료 시각 |
| `revoked_at` | datetime(3) | Y |  | 폐기 시각 |
| `created_at` | datetime(3) | N |  | 생성 시각 |

## 5.8 `audit_logs`

| 컬럼명 | 타입 | NULL | 제약 | 설명 |
| --- | --- | --- | --- | --- |
| `id` | bigint | N | PK, AI | 로그 ID |
| `actor_user_id` | varchar(40) | Y | FK | 작업 수행 사용자 |
| `group_id` | varchar(40) | Y | FK | 관련 그룹 |
| `target_type` | varchar(30) | N |  | 대상 타입 |
| `target_id` | varchar(40) | Y |  | 대상 ID |
| `action` | varchar(30) | N |  | 액션 |
| `payload` | json | Y |  | 변경 요약 |
| `created_at` | datetime(3) | N |  | 생성 시각 |

## 6. 테이블 생성 우선순위

### 6.1 CRUD + 인증 연계 기준

1. `users`
2. `couple_groups`
3. `couple_group_members`
4. `events`
5. `shift_schedules`

### 6.2 후속 확장

1. `invites`
2. `refresh_tokens`
3. `audit_logs`

## 7. 권장 DDL 예시

```sql
create table users (
    id varchar(40) not null,
    email varchar(255) not null,
    password_hash varchar(255) not null,
    nickname varchar(50) null,
    status varchar(20) not null default 'ACTIVE',
    created_at datetime(3) not null,
    updated_at datetime(3) not null,
    primary key (id),
    constraint uk_users_email unique (email)
);

create table couple_groups (
    id varchar(40) not null,
    name varchar(100) null,
    status varchar(20) not null default 'ACTIVE',
    created_at datetime(3) not null,
    updated_at datetime(3) not null,
    primary key (id)
);

create table couple_group_members (
    id bigint not null auto_increment,
    group_id varchar(40) not null,
    user_id varchar(40) not null,
    role varchar(20) not null,
    partner_status varchar(20) not null default 'PENDING',
    created_at datetime(3) not null,
    primary key (id),
    constraint fk_group_members_group foreign key (group_id) references couple_groups(id),
    constraint fk_group_members_user foreign key (user_id) references users(id),
    constraint uk_group_members_group_user unique (group_id, user_id),
    constraint uk_group_members_user unique (user_id)
);

create table events (
    id varchar(40) not null,
    group_id varchar(40) not null,
    title varchar(100) not null,
    start_date date not null,
    end_date date not null,
    subject_type varchar(20) not null,
    owner_user_id varchar(40) null,
    note text null,
    created_by_user_id varchar(40) not null,
    updated_by_user_id varchar(40) null,
    created_at datetime(3) not null,
    updated_at datetime(3) not null,
    deleted_at datetime(3) null,
    primary key (id),
    constraint fk_events_group foreign key (group_id) references couple_groups(id),
    constraint fk_events_owner_user foreign key (owner_user_id) references users(id),
    constraint fk_events_created_by_user foreign key (created_by_user_id) references users(id),
    constraint fk_events_updated_by_user foreign key (updated_by_user_id) references users(id),
    constraint chk_events_date_range check (start_date <= end_date),
    constraint chk_events_subject_type check (subject_type in ('PERSONAL', 'SHARED'))
);

create index idx_events_group_deleted_start_end
    on events (group_id, deleted_at, start_date, end_date);

create index idx_events_group_subject_owner
    on events (group_id, subject_type, owner_user_id);

create table shift_schedules (
    id varchar(40) not null,
    group_id varchar(40) not null,
    date date not null,
    shift_type varchar(20) not null,
    created_by_user_id varchar(40) not null,
    updated_by_user_id varchar(40) null,
    created_at datetime(3) not null,
    updated_at datetime(3) not null,
    deleted_at datetime(3) null,
    primary key (id),
    constraint fk_shifts_group foreign key (group_id) references couple_groups(id),
    constraint fk_shifts_created_by_user foreign key (created_by_user_id) references users(id),
    constraint fk_shifts_updated_by_user foreign key (updated_by_user_id) references users(id),
    constraint chk_shift_type check (shift_type in ('DAY', 'NIGHT', 'MID', 'EVENING', 'OFF', 'VACATION')),
    constraint uk_shift_schedules_group_date unique (group_id, date)
);

create index idx_shift_schedules_group_deleted_date
    on shift_schedules (group_id, deleted_at, date);
```

## 8. 조회 패턴 설계 메모

### 8.1 월간 일정 조회

```sql
where group_id = ?
  and deleted_at is null
  and start_date <= :month_end
  and end_date >= :month_start
```

### 8.2 날짜 상세 일정 조회

```sql
where group_id = ?
  and deleted_at is null
  and start_date <= :target_date
  and end_date >= :target_date
```

### 8.3 월간 근무 스케줄 조회

```sql
where group_id = ?
  and deleted_at is null
  and date between :month_start and :month_end
```

### 8.4 ownerType 계산

```text
if subject_type == SHARED -> US
if subject_type == PERSONAL and owner_user_id == currentUserId -> ME
if subject_type == PERSONAL and owner_user_id != currentUserId -> PARTNER
```

## 9. 무결성 및 애플리케이션 책임

DB에서 강제 가능한 규칙:

- 사용자 이메일 유니크
- 사용자 한 명당 단일 그룹 참여
- 일정 날짜 역전 방지
- 일정 `subject_type` enum 제한
- 근무 스케줄 타입 enum 제한
- 같은 그룹 같은 날짜 스케줄 유니크

애플리케이션에서 함께 보장해야 하는 규칙:

- 그룹당 최대 2명 제한
- 이미 파트너가 연결된 그룹에 추가 등록 금지
- 이미 다른 그룹에 속한 사용자는 새 그룹에 등록 금지
- `ownerType` 계산
- 같은 그룹 구성원 2명의 공동 편집 권한

## 10. 확정 사항

1. CRUD 이후 인증/인가를 바로 도입하므로 `users`를 조기 구현 대상으로 둔다.
2. 작성자/수정자는 임시 actor key가 아니라 `users.id` FK 기반으로 저장한다.
3. 일정 소유 정보는 `owner_user_id`와 `subject_type`으로 저장한다.
4. `ownerType = ME | US | PARTNER`는 응답 계산값으로만 사용한다.
5. 삭제는 `deleted_at` 기반 소프트 삭제를 적용한다.
6. 파트너 연결은 초대 기반 흐름을 먼저 구현한다.

## 11. 남은 결정 사항

1. `ME`/`PARTNER` 표시를 서버에서만 계산할지, 클라이언트와 공통 규칙으로 문서화할지
2. 그룹 생성 시 기본 표시명 정책
3. Firebase Authentication을 초기 인증 단계부터 같이 사용할지
