# ScheduleApp API 명세서

## 1. 문서 개요

이 문서는 [backend-requirements.md](/C:/Users/admin/Desktop/schedule/schedule-api/docs/backend-requirements.md)와 [requirements.md](/C:/Users/admin/Desktop/schedule/schedule-api/docs/requirements.md)를 기준으로 `ScheduleApp` 백엔드 API를 구체화한 명세서다.

초기 구현은 일정/근무 스케줄 CRUD와 캘린더 조회를 우선한다. 다만 CRUD 이후 인증/인가를 바로 도입할 계획이므로, 본 문서는 처음부터 `users.id` 기반 데이터 모델을 전제로 작성한다.

- 포함 범위
  - 일정 CRUD
  - 근무 스케줄 CRUD
  - 월별 근무 스케줄 일괄 저장
  - 월간 캘린더 통합 조회
  - 날짜 상세 조회
- 후속 범위
  - 로그인/토큰
  - 그룹/파트너 연결
  - 정식 권한 검증

## 2. 기본 원칙

### 2.1 Base URL

- 개발 기준: `/api/v1`

### 2.2 콘텐츠 타입

- 요청: `Content-Type: application/json`
- 응답: `Content-Type: application/json;charset=UTF-8`

### 2.3 식별 원칙

- 모든 일정과 근무 스케줄은 `groupId` 기준으로 분리한다.
- 인증 도입 이후 실제 권한 판단은 `currentUserId`와 그룹 멤버십으로 처리한다.
- 사용자 한 명은 동시에 하나의 그룹에만 속할 수 있다.
- 하나의 그룹은 최대 2명의 사용자로만 구성된다.

초기 연결 전환기에는 아래 헤더를 보조적으로 쓸 수 있다.

```http
X-Group-Id: grp_01J8ZP3TQ4X
```

### 2.4 날짜 규칙

- 날짜: `YYYY-MM-DD`
- 일시: ISO-8601 UTC datetime 문자열

### 2.5 공통 응답 형식

성공:

```json
{
  "success": true,
  "data": {}
}
```

실패:

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "month must be between 1 and 12"
  }
}
```

### 2.6 HTTP 상태 코드 원칙

- `200 OK`: 조회, 수정, 삭제 성공
- `201 Created`: 생성 성공
- `400 Bad Request`: 요청 형식 오류, 유효성 오류
- `404 Not Found`: 대상 리소스 없음
- `409 Conflict`: 중복 또는 상태 충돌
- `500 Internal Server Error`: 서버 내부 오류

## 3. 공통 도메인 타입

### 3.1 EventOwnerType

API 응답에서만 사용하는 계산 값이다.

- `ME`
- `US`
- `PARTNER`

계산 규칙:

- `subjectType = SHARED` 이면 `ownerType = US`
- `subjectType = PERSONAL` 이고 `ownerUserId = currentUserId` 이면 `ownerType = ME`
- `subjectType = PERSONAL` 이고 `ownerUserId != currentUserId` 이면 `ownerType = PARTNER`

### 3.2 EventSubjectType

DB 저장 기준의 절대값이다.

- `PERSONAL`
- `SHARED`

### 3.3 ShiftType

- `DAY`
- `NIGHT`
- `MID`
- `EVENING`
- `OFF`
- `VACATION`

## 4. 공통 스키마

### 4.1 Event

```json
{
  "id": "evt_01J8ZR0W6K8",
  "groupId": "grp_01J8ZP3TQ4X",
  "title": "야간 근무 후 병원",
  "startDate": "2026-04-18",
  "endDate": "2026-04-18",
  "subjectType": "PERSONAL",
  "ownerUserId": "usr_01J8ZQ11ABC",
  "ownerType": "ME",
  "note": "오후 3시 예약",
  "createdByUserId": "usr_01J8ZQ11ABC",
  "updatedByUserId": "usr_01J8ZQ11ABC",
  "createdAt": "2026-04-18T01:10:22Z",
  "updatedAt": "2026-04-18T01:10:22Z",
  "deletedAt": null
}
```

설명:

- `subjectType`, `ownerUserId`는 저장 기준의 절대값이다.
- `ownerType`은 현재 로그인 사용자 기준으로 계산된 응답 값이다.
- `createdByUserId`, `updatedByUserId`는 모두 `users.id`를 사용한다.

### 4.2 ShiftSchedule

```json
{
  "id": "sft_01J8ZR7J9CB",
  "groupId": "grp_01J8ZP3TQ4X",
  "date": "2026-04-18",
  "shiftType": "DAY",
  "createdByUserId": "usr_01J8ZQ11ABC",
  "updatedByUserId": "usr_01J8ZQ11ABC",
  "createdAt": "2026-04-18T01:20:30Z",
  "updatedAt": "2026-04-18T01:20:30Z",
  "deletedAt": null
}
```

### 4.3 CalendarDaySummary

```json
{
  "date": "2026-04-18",
  "shift": {
    "id": "sft_01J8ZR7J9CB",
    "shiftType": "DAY"
  },
  "events": [
    {
      "id": "evt_01J8ZR0W6K8",
      "title": "야간 근무 후 병원",
      "subjectType": "PERSONAL",
      "ownerUserId": "usr_01J8ZQ11ABC",
      "ownerType": "ME",
      "startDate": "2026-04-18",
      "endDate": "2026-04-18",
      "isMultiDay": false
    }
  ]
}
```

## 5. 공통 에러 코드

| 코드 | 설명 |
| --- | --- |
| `VALIDATION_ERROR` | 요청 값 유효성 검증 실패 |
| `GROUP_NOT_FOUND` | 존재하지 않는 그룹 |
| `GROUP_ACCESS_DENIED` | 그룹 접근 불가 |
| `GROUP_PARTNER_ALREADY_EXISTS` | 이미 파트너가 연결된 그룹 |
| `GROUP_MEMBER_LIMIT_EXCEEDED` | 그룹 인원 제한 초과 |
| `USER_ALREADY_IN_GROUP` | 사용자가 이미 다른 그룹에 속해 있음 |
| `EVENT_NOT_FOUND` | 존재하지 않는 일정 |
| `EVENT_INVALID_DATE_RANGE` | 시작일이 종료일보다 늦음 |
| `SHIFT_NOT_FOUND` | 존재하지 않는 근무 스케줄 |
| `SHIFT_INVALID_TYPE` | 지원하지 않는 근무 타입 |
| `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |

## 6. 헤더 규약

### 6.1 인증 도입 이후

- `Authorization: Bearer <access-token>`

### 6.2 전환기 보조 헤더

| 헤더 | 필수 여부 | 설명 |
| --- | --- | --- |
| `X-Group-Id` | 선택 | 초기 전환기용 그룹 식별 보조 헤더 |

## 7. 일정 API

권한 원칙:

- 같은 그룹에 속한 두 사용자는 그룹 내 모든 일정을 생성/수정/삭제할 수 있다.
- `ownerType`은 권한 판단값이 아니다.
- 권한 판단은 `currentUserId`, `groupId`, 그룹 멤버십으로 처리한다.

## 7.1 월간 일정 조회

- `GET /api/v1/events?year=2026&month=4`

Query Parameters:

| 이름 | 타입 | 필수 | 설명 |
| --- | --- | --- | --- |
| `year` | number | 필수 | 조회 연도 |
| `month` | number | 필수 | 조회 월 |
| `ownerTypes` | string | 선택 | 응답 계산값 기준 필터. 예: `ME,US` |

응답 예시:

```json
{
  "success": true,
  "data": {
    "year": 2026,
    "month": 4,
    "items": [
      {
        "id": "evt_01",
        "groupId": "grp_01",
        "title": "데이트",
        "startDate": "2026-04-18",
        "endDate": "2026-04-18",
        "subjectType": "SHARED",
        "ownerUserId": null,
        "ownerType": "US",
        "note": "저녁 7시",
        "createdByUserId": "usr_me",
        "updatedByUserId": "usr_me",
        "createdAt": "2026-04-01T10:00:00Z",
        "updatedAt": "2026-04-01T10:00:00Z",
        "deletedAt": null
      },
      {
        "id": "evt_02",
        "groupId": "grp_01",
        "title": "상대 병원 일정",
        "startDate": "2026-04-20",
        "endDate": "2026-04-20",
        "subjectType": "PERSONAL",
        "ownerUserId": "usr_partner",
        "ownerType": "PARTNER",
        "note": null,
        "createdByUserId": "usr_partner",
        "updatedByUserId": "usr_partner",
        "createdAt": "2026-04-02T10:00:00Z",
        "updatedAt": "2026-04-02T10:00:00Z",
        "deletedAt": null
      }
    ]
  }
}
```

## 7.2 날짜별 일정 조회

- `GET /api/v1/events/date/{date}`

응답 예시:

```json
{
  "success": true,
  "data": {
    "date": "2026-04-18",
    "items": [
      {
        "id": "evt_01",
        "groupId": "grp_01",
        "title": "데이트",
        "startDate": "2026-04-18",
        "endDate": "2026-04-18",
        "subjectType": "SHARED",
        "ownerUserId": null,
        "ownerType": "US",
        "note": "저녁 7시",
        "createdByUserId": "usr_me",
        "updatedByUserId": "usr_me",
        "createdAt": "2026-04-01T10:00:00Z",
        "updatedAt": "2026-04-01T10:00:00Z",
        "deletedAt": null
      }
    ]
  }
}
```

## 7.3 일정 생성

- `POST /api/v1/events`

요청 본문:

```json
{
  "title": "병원 방문",
  "startDate": "2026-04-18",
  "endDate": "2026-04-18",
  "subjectType": "PERSONAL",
  "ownerUserId": "usr_me",
  "note": "정형외과"
}
```

요청 규칙:

- `title`은 필수다.
- `startDate`, `endDate`는 필수다.
- `subjectType`은 `PERSONAL`, `SHARED` 중 하나여야 한다.
- `subjectType = PERSONAL`이면 `ownerUserId`는 필수다.
- `subjectType = SHARED`이면 `ownerUserId`는 nullable 허용이다.

응답 예시:

```json
{
  "success": true,
  "data": {
    "id": "evt_01",
    "groupId": "grp_01",
    "title": "병원 방문",
    "startDate": "2026-04-18",
    "endDate": "2026-04-18",
    "subjectType": "PERSONAL",
    "ownerUserId": "usr_me",
    "ownerType": "ME",
    "note": "정형외과",
    "createdByUserId": "usr_me",
    "updatedByUserId": "usr_me",
    "createdAt": "2026-04-18T05:10:00Z",
    "updatedAt": "2026-04-18T05:10:00Z",
    "deletedAt": null
  }
}
```

## 7.4 일정 수정

- `PATCH /api/v1/events/{eventId}`

요청 본문 예시:

```json
{
  "title": "병원 방문 후 약국",
  "note": "처방전 수령"
}
```

응답 예시:

```json
{
  "success": true,
  "data": {
    "id": "evt_01",
    "groupId": "grp_01",
    "title": "병원 방문 후 약국",
    "startDate": "2026-04-18",
    "endDate": "2026-04-18",
    "subjectType": "PERSONAL",
    "ownerUserId": "usr_me",
    "ownerType": "ME",
    "note": "처방전 수령",
    "createdByUserId": "usr_me",
    "updatedByUserId": "usr_partner",
    "createdAt": "2026-04-18T05:10:00Z",
    "updatedAt": "2026-04-18T05:30:00Z",
    "deletedAt": null
  }
}
```

## 7.5 일정 삭제

- `DELETE /api/v1/events/{eventId}`

설명:

- `deletedAt` 기반 소프트 삭제를 적용한다.

응답 예시:

```json
{
  "success": true,
  "data": {
    "id": "evt_01",
    "deleted": true,
    "deletedAt": "2026-04-18T05:40:00Z"
  }
}
```

## 8. 근무 스케줄 API

권한 원칙:

- 같은 그룹에 속한 두 사용자는 그룹 내 모든 근무 스케줄을 생성/수정/삭제할 수 있다.

## 8.1 월간 근무 스케줄 조회

- `GET /api/v1/shifts?year=2026&month=4`

응답 예시:

```json
{
  "success": true,
  "data": {
    "year": 2026,
    "month": 4,
    "items": [
      {
        "id": "sft_01",
        "groupId": "grp_01",
        "date": "2026-04-18",
        "shiftType": "DAY",
        "createdByUserId": "usr_me",
        "updatedByUserId": "usr_me",
        "createdAt": "2026-04-01T00:00:00Z",
        "updatedAt": "2026-04-01T00:00:00Z",
        "deletedAt": null
      }
    ]
  }
}
```

## 8.2 날짜별 근무 스케줄 조회

- `GET /api/v1/shifts/date/{date}`

응답 예시:

```json
{
  "success": true,
  "data": {
    "date": "2026-04-18",
    "item": {
      "id": "sft_01",
      "groupId": "grp_01",
      "date": "2026-04-18",
      "shiftType": "DAY",
      "createdByUserId": "usr_me",
      "updatedByUserId": "usr_me",
      "createdAt": "2026-04-01T00:00:00Z",
      "updatedAt": "2026-04-01T00:00:00Z",
      "deletedAt": null
    }
  }
}
```

## 8.3 날짜별 근무 스케줄 저장 또는 수정

- `PUT /api/v1/shifts/{date}`

요청 본문:

```json
{
  "shiftType": "DAY"
}
```

응답 예시:

```json
{
  "success": true,
  "data": {
    "id": "sft_01",
    "groupId": "grp_01",
    "date": "2026-04-18",
    "shiftType": "DAY",
    "createdByUserId": "usr_me",
    "updatedByUserId": "usr_me",
    "createdAt": "2026-04-18T02:00:00Z",
    "updatedAt": "2026-04-18T02:00:00Z",
    "deletedAt": null
  }
}
```

## 8.4 월별 근무 스케줄 일괄 저장

- `PUT /api/v1/shifts/monthly?year=2026&month=4`

요청 본문:

```json
{
  "items": [
    {
      "date": "2026-04-01",
      "shiftType": "DAY"
    },
    {
      "date": "2026-04-02",
      "shiftType": "NIGHT"
    }
  ]
}
```

응답 예시:

```json
{
  "success": true,
  "data": {
    "year": 2026,
    "month": 4,
    "replacedCount": 30,
    "items": [
      {
        "id": "sft_01",
        "groupId": "grp_01",
        "date": "2026-04-01",
        "shiftType": "DAY",
        "createdByUserId": "usr_me",
        "updatedByUserId": "usr_me",
        "createdAt": "2026-04-18T03:00:00Z",
        "updatedAt": "2026-04-18T03:00:00Z",
        "deletedAt": null
      }
    ]
  }
}
```

## 8.5 날짜별 근무 스케줄 삭제

- `DELETE /api/v1/shifts/{date}`

응답 예시:

```json
{
  "success": true,
  "data": {
    "date": "2026-04-18",
    "deleted": true,
    "deletedAt": "2026-04-18T05:40:00Z"
  }
}
```

## 9. 캘린더 통합 조회 API

## 9.1 월간 캘린더 조회

- `GET /api/v1/calendar/month?year=2026&month=4`

응답 예시:

```json
{
  "success": true,
  "data": {
    "year": 2026,
    "month": 4,
    "filters": {
      "ownerTypes": ["ME", "US", "PARTNER"],
      "includeShifts": true
    },
    "meta": {
      "groupId": "grp_01",
      "currentUserId": "usr_me",
      "members": [
        {
          "userId": "usr_me",
          "role": "OWNER"
        },
        {
          "userId": "usr_partner",
          "role": "PARTNER"
        }
      ],
      "shiftTypes": ["DAY", "NIGHT", "MID", "EVENING", "OFF", "VACATION"]
    },
    "events": [
      {
        "id": "evt_01",
        "groupId": "grp_01",
        "title": "데이트",
        "startDate": "2026-04-18",
        "endDate": "2026-04-18",
        "subjectType": "SHARED",
        "ownerUserId": null,
        "ownerType": "US",
        "note": "저녁 7시",
        "createdByUserId": "usr_me",
        "updatedByUserId": "usr_me",
        "createdAt": "2026-04-01T10:00:00Z",
        "updatedAt": "2026-04-01T10:00:00Z",
        "deletedAt": null
      }
    ],
    "shifts": [
      {
        "id": "sft_01",
        "groupId": "grp_01",
        "date": "2026-04-18",
        "shiftType": "DAY",
        "createdByUserId": "usr_me",
        "updatedByUserId": "usr_me",
        "createdAt": "2026-04-01T00:00:00Z",
        "updatedAt": "2026-04-01T00:00:00Z",
        "deletedAt": null
      }
    ],
    "days": [
      {
        "date": "2026-04-18",
        "shift": {
          "id": "sft_01",
          "shiftType": "DAY"
        },
        "events": [
          {
            "id": "evt_01",
            "title": "데이트",
            "subjectType": "SHARED",
            "ownerUserId": null,
            "ownerType": "US",
            "startDate": "2026-04-18",
            "endDate": "2026-04-18",
            "isMultiDay": false
          }
        ]
      }
    ]
  }
}
```

## 9.2 날짜 상세 조회

- `GET /api/v1/calendar/date/{date}`

응답 예시:

```json
{
  "success": true,
  "data": {
    "date": "2026-04-18",
    "meta": {
      "groupId": "grp_01",
      "currentUserId": "usr_me"
    },
    "shift": {
      "id": "sft_01",
      "groupId": "grp_01",
      "date": "2026-04-18",
      "shiftType": "DAY",
      "createdByUserId": "usr_me",
      "updatedByUserId": "usr_me",
      "createdAt": "2026-04-01T00:00:00Z",
      "updatedAt": "2026-04-01T00:00:00Z",
      "deletedAt": null
    },
    "events": [
      {
        "id": "evt_01",
        "groupId": "grp_01",
        "title": "데이트",
        "startDate": "2026-04-18",
        "endDate": "2026-04-18",
        "subjectType": "SHARED",
        "ownerUserId": null,
        "ownerType": "US",
        "note": "저녁 7시",
        "createdByUserId": "usr_me",
        "updatedByUserId": "usr_me",
        "createdAt": "2026-04-01T10:00:00Z",
        "updatedAt": "2026-04-01T10:00:00Z",
        "deletedAt": null
      },
      {
        "id": "evt_02",
        "groupId": "grp_01",
        "title": "상대 병원 일정",
        "startDate": "2026-04-18",
        "endDate": "2026-04-18",
        "subjectType": "PERSONAL",
        "ownerUserId": "usr_partner",
        "ownerType": "PARTNER",
        "note": null,
        "createdByUserId": "usr_partner",
        "updatedByUserId": "usr_partner",
        "createdAt": "2026-04-02T10:00:00Z",
        "updatedAt": "2026-04-02T10:00:00Z",
        "deletedAt": null
      }
    ]
  }
}
```

## 10. 유효성 검증 규칙

### 10.1 일정

- `title`은 공백 제외 1자 이상이어야 한다.
- `startDate <= endDate`를 만족해야 한다.
- `subjectType`은 `PERSONAL`, `SHARED` 중 하나여야 한다.
- `subjectType = PERSONAL`이면 `ownerUserId`는 필수다.
- `subjectType = SHARED`이면 `ownerUserId`는 nullable 허용이다.
- `ownerType`은 저장값이 아니라 응답 계산값이다.

### 10.2 근무 스케줄

- `shiftType`은 정의된 enum 중 하나여야 한다.
- 같은 `groupId`, 같은 `date`에는 하나의 스케줄만 허용한다.

### 10.3 그룹/파트너

- 사용자 한 명은 동시에 하나의 그룹에만 속할 수 없다.
- 하나의 그룹은 최대 2명으로 제한한다.
- 이미 다른 그룹에 속한 사용자는 파트너로 등록할 수 없다.

## 11. 후속 확장 API

### 11.1 인증

- `POST /api/v1/auth/signup`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`

### 11.2 그룹/파트너/초대

- `POST /api/v1/groups`
- `GET /api/v1/groups/me`
- `POST /api/v1/groups/partner`
- `POST /api/v1/groups/invites`
- `POST /api/v1/groups/invites/accept`

#### 11.2.1 파트너 추가

- `POST /api/v1/groups/partner`

설명:

- 현재 사용자가 속한 그룹에 다른 사용자 1명을 파트너로 등록한다.
- 이미 다른 그룹에 속한 사용자는 등록할 수 없다.
- 그룹 정원이 2명인 경우 추가 등록은 실패해야 한다.
- 초기 구현은 초대 코드 또는 초대 링크 수락 흐름을 우선 사용한다.

요청 본문 예시:

```json
{
  "inviteCode": "CP-9K3LQ2"
}
```

응답 예시:

```json
{
  "success": true,
  "data": {
    "groupId": "grp_01",
    "members": [
      {
        "userId": "usr_me",
        "role": "OWNER",
        "partnerStatus": "CONNECTED"
      },
      {
        "userId": "usr_partner",
        "role": "PARTNER",
        "partnerStatus": "CONNECTED"
      }
    ],
    "permissions": {
      "canReadAllEvents": true,
      "canEditAllEvents": true,
      "canEditAllShifts": true
    }
  }
}
```

## 12. 구현 메모

- 일정과 근무 스케줄의 작성자/수정자는 모두 `users.id` 기준으로 저장하는 것이 좋다.
- `ownerType`은 저장값으로 두지 말고, `subjectType`, `ownerUserId`, `currentUserId`로 응답 시 계산하는 것이 좋다.
- 일정과 근무 스케줄 조회는 `deleted_at is null` 조건을 기본으로 사용해야 한다.
- 권한 판단은 `currentUserId`와 그룹 멤버십으로 처리하고, `ME`/`PARTNER` 표시값과 분리해야 한다.

## 13. MVP 우선 구현 목록

1. `users`, `couple_groups`, `events`, `shift_schedules` 구조 확정
2. `POST /events`, `PATCH /events/{eventId}`, `DELETE /events/{eventId}`
3. `PUT /shifts/{date}`, `DELETE /shifts/{date}`, `PUT /shifts/monthly`
4. `GET /calendar/month`, `GET /calendar/date/{date}`
5. 이후 인증/그룹/초대 API 연결
