# ScheduleApp API 명세서

## 1. 문서 개요

이 문서는 [backend-requirements.md](C:\Users\admin\Desktop\schedule\docs\backend-requirements.md)와 [requirements.md](C:\Users\admin\Desktop\schedule\docs\requirements.md)를 기준으로 `ScheduleApp` 백엔드 API를 구체화한 명세서다.

초기 구현은 `Spring Security + Kakao OAuth2 + JWT` 기반 인증과 일정/근무 스케줄 CRUD, 캘린더 조회를 함께 포함한다. 본 문서는 처음부터 `users.id` 기반 데이터 모델을 전제로 작성한다.

- 포함 범위
  - 카카오 OAuth2 회원가입/로그인
  - JWT 액세스 토큰/리프레시 토큰
  - 일정 CRUD
  - 근무 스케줄 CRUD
  - 월별 근무 스케줄 일괄 저장
  - 월간 캘린더 통합 조회
  - 날짜 상세 조회
  - FCM 등록 토큰 관리 및 푸시 알림 발송
- 후속 범위
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
- `302 Found`: OAuth2 로그인 리다이렉트
- `400 Bad Request`: 요청 형식 오류, 유효성 오류
- `401 Unauthorized`: 인증 실패, 토큰 오류, 만료
- `403 Forbidden`: 인가 실패
- `404 Not Found`: 대상 리소스 없음
- `409 Conflict`: 중복 또는 상태 충돌
- `500 Internal Server Error`: 서버 내부 오류

## 3. 인증 및 보안

### 3.1 인증 방식

- 인증 프레임워크: `Spring Security`
- 소셜 로그인: `Kakao OAuth2`
- API 인증 토큰: `JWT`

동작 원칙:

1. Android 앱은 `POST /api/v1/auth/kakao/mobile`로 카카오 SDK에서 획득한 access token을 백엔드에 전달한다.
2. 서버는 카카오 사용자 정보 API로 access token을 검증하고 사용자 프로필을 조회한다.
3. 서버는 기존 회원이면 로그인, 신규면 회원가입 처리한 뒤 서비스용 JWT를 발급한다.
4. Android 앱은 응답으로 받은 access token과 refresh token을 안전 저장소에 저장한다.
5. 보호된 API는 `Authorization: Bearer <access-token>`로 호출한다.
6. 기존 `/api/v1/auth/kakao/login -> /kakao/callback -> /mobile/exchange` 흐름은 브라우저 fallback 또는 테스트용으로 유지한다.

### 3.2 보호 대상 API

아래 API는 인증이 필요하다.

- `/api/v1/users/me`
- `/api/v1/users/me/settings`
- `/api/v1/events/**`
- `/api/v1/shifts/**`
- `/api/v1/calendar/**`
- `/api/v1/groups/**`
- `/api/v1/notifications/**`

인증 없이 허용되는 API:

- `/api/v1/auth/kakao/login`
- `/api/v1/auth/kakao/callback`
- `/api/v1/auth/kakao/mobile`
- `/api/v1/auth/mobile/exchange`
- `/api/v1/auth/refresh`

## 4. 공통 도메인 타입

### 4.1 EventOwnerType

API 응답에서만 사용하는 계산 값이다.

- `ME`
- `US`
- `PARTNER`

계산 규칙:

- `subjectType = SHARED` 이면 `ownerType = US`
- `subjectType = PERSONAL` 이고 `ownerUserId = currentUserId` 이면 `ownerType = ME`
- `subjectType = PERSONAL` 이고 `ownerUserId != currentUserId` 이면 `ownerType = PARTNER`

### 4.2 EventSubjectType

DB 저장 기준의 절대값이다.

- `PERSONAL`
- `SHARED`

### 4.3 ShiftType

- `DAY`
- `NIGHT`
- `MID`
- `EVENING`
- `OFF`
- `VACATION`

## 5. 공통 스키마

### 5.1 AuthTokenPair

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "refreshTokenExpiresIn": 1209600
}
```

### 5.2 UserProfile

```json
{
  "id": "usr_01J8ZQ11ABC",
  "oauthProvider": "KAKAO",
  "oauthProviderUserId": "3848383992",
  "nickname": "성철",
  "profileImageUrl": "https://k.kakaocdn.net/...",
  "groupId": "grp_01J8ZP3TQ4X",
  "defaultShiftOwnerType": "ME",
  "createdAt": "2026-04-18T01:00:00Z",
  "updatedAt": "2026-04-18T01:00:00Z"
}
```

### 5.3 Event

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

### 5.4 ShiftSchedule

```json
{
  "id": "sft_01J8ZR7J9CB",
  "groupId": "grp_01J8ZP3TQ4X",
  "date": "2026-04-18",
  "ownerUserId": "usr_01J8ZQ11ABC",
  "ownerType": "ME",
  "shiftType": "DAY",
  "createdByUserId": "usr_01J8ZQ11ABC",
  "updatedByUserId": "usr_01J8ZQ11ABC",
  "createdAt": "2026-04-18T01:20:30Z",
  "updatedAt": "2026-04-18T01:20:30Z",
  "deletedAt": null
}
```

### 5.5 CalendarDaySummary

```json
{
  "date": "2026-04-18",
  "shift": {
    "id": "sft_01J8ZR7J9CB",
    "ownerUserId": "usr_01J8ZQ11ABC",
    "ownerType": "ME",
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

## 6. 공통 에러 코드

| 코드 | 설명 |
| --- | --- |
| `VALIDATION_ERROR` | 요청 값 유효성 검증 실패 |
| `AUTH_UNAUTHORIZED` | 인증 정보 없음 또는 인증 실패 |
| `AUTH_INVALID_TOKEN` | JWT 서명 오류 또는 형식 오류 |
| `AUTH_TOKEN_EXPIRED` | 액세스 토큰 또는 리프레시 토큰 만료 |
| `AUTH_REFRESH_TOKEN_REVOKED` | 무효화된 리프레시 토큰 |
| `AUTH_KAKAO_LOGIN_FAILED` | 카카오 OAuth2 로그인 실패 |
| `GROUP_NOT_FOUND` | 존재하지 않는 그룹 |
| `GROUP_ACCESS_DENIED` | 그룹 접근 불가 |
| `GROUP_PARTNER_ALREADY_EXISTS` | 이미 파트너가 연결된 그룹 |
| `GROUP_MEMBER_LIMIT_EXCEEDED` | 그룹 인원 제한 초과 |
| `GROUP_INVITE_NOT_FOUND` | 존재하지 않거나 더 이상 사용할 수 없는 초대 |
| `GROUP_INVITE_EXPIRED` | 만료된 초대 |
| `GROUP_SELF_INVITE_NOT_ALLOWED` | 초대 생성자가 본인 초대를 수락함 |
| `USER_ALREADY_IN_GROUP` | 사용자가 이미 다른 그룹에 속해 있음 |
| `EVENT_NOT_FOUND` | 존재하지 않는 일정 |
| `EVENT_INVALID_DATE_RANGE` | 시작일이 종료일보다 늦음 |
| `SHIFT_NOT_FOUND` | 존재하지 않는 근무 스케줄 |
| `SHIFT_INVALID_TYPE` | 지원하지 않는 근무 타입 |
| `INTERNAL_SERVER_ERROR` | 서버 내부 오류 |

## 7. 헤더 규약

### 7.1 인증 헤더

- `Authorization: Bearer <access-token>`

### 7.2 전환기 보조 헤더

| 헤더 | 필수 여부 | 설명 |
| --- | --- | --- |
| `X-Group-Id` | 선택 | 초기 전환기용 그룹 식별 보조 헤더 |

## 8. 인증 API

## 8.1 카카오 모바일 로그인

- `POST /api/v1/auth/kakao/mobile`

설명:

- Android 앱은 카카오 Android SDK 로그인 성공 후 받은 `accessToken`을 이 API로 전달한다.
- 서버는 카카오 사용자 정보 API로 토큰을 검증하고, 서비스용 JWT를 발급한다.
- Android 앱 기준 기본 로그인 경로는 이 API다.

요청 본문:

```json
{
  "accessToken": "kakao-access-token-from-sdk"
}
```

응답 예시:

```json
{
  "success": true,
  "data": {
    "user": {
      "id": "usr_01J8ZQ11ABC",
      "oauthProvider": "KAKAO",
      "oauthProviderUserId": "3848383992",
      "nickname": "성철",
      "profileImageUrl": "https://k.kakaocdn.net/...",
      "groupId": "grp_01J8ZP3TQ4X",
      "defaultShiftOwnerType": "ME",
      "createdAt": "2026-04-18T01:00:00Z",
      "updatedAt": "2026-04-18T01:00:00Z"
    },
    "tokens": {
      "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
      "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
      "tokenType": "Bearer",
      "expiresIn": 3600,
      "refreshTokenExpiresIn": 1209600
    },
    "isNewUser": true
  }
}
```

## 8.2 카카오 로그인 시작 브라우저 fallback

- `GET /api/v1/auth/kakao/login?appRedirectUri={appRedirectUri}`

설명:

- 기존 브라우저 기반 OAuth2 로그인 시작 엔드포인트다.
- Android SDK 직접 로그인으로 전환했더라도 fallback 또는 테스트 용도로 유지한다.
- 서버는 1회용 `state`를 생성하고 `state -> appRedirectUri`를 5분 TTL 메모리 저장소에 연결한다.

## 8.3 카카오 로그인 콜백 브라우저 fallback

- `GET /api/v1/auth/kakao/callback?code={authorizationCode}&state={state}`

설명:

- 브라우저 기반 fallback 흐름에서만 사용한다.
- 성공 시 앱 딥링크로 `loginCode`를 전달한다.
- 실패 시 앱 딥링크로 `errorCode`, `error`, `errorDescription`을 전달한다.
- `state`가 없으면 브라우저/테스트용 JSON 응답을 그대로 반환한다.

성공 응답 헤더 예시:

```http
HTTP/1.1 302 Found
Location: scheduleapp://auth/callback?loginCode=9fd0a0d4-f1d2-4d77-9d28-8d1bc7e4cf35&isNewUser=true
```

실패 응답 헤더 예시:

```http
HTTP/1.1 302 Found
Location: scheduleapp://auth/callback?errorCode=AUTH_KAKAO_LOGIN_FAILED&error=access_denied&errorDescription=user%20cancelled
```

## 8.4 모바일 로그인 코드 교환 브라우저 fallback

- `POST /api/v1/auth/mobile/exchange`

요청 본문:

```json
{
  "loginCode": "9fd0a0d4-f1d2-4d77-9d28-8d1bc7e4cf35"
}
```

설명:

- 브라우저 fallback 로그인에서만 사용한다.
- `loginCode`는 1회용이며, 교환 후 즉시 폐기된다.
- 만료 시간은 현재 5분이다.

## 8.5 토큰 재발급

- `POST /api/v1/auth/refresh`

요청 본문:

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

## 8.6 로그아웃

- `POST /api/v1/auth/logout`

요청 본문:

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

## 8.7 내 정보 조회

- `GET /api/v1/users/me`

응답의 `defaultShiftOwnerType`은 캘린더 조회에서 `shiftOwnerType`을 생략했을 때 사용할 기본 근무 스케줄 표시 대상이다.

## 8.8 내 표시 설정 수정

- `PATCH /api/v1/users/me/settings`

요청 본문:

```json
{
  "defaultShiftOwnerType": "PARTNER"
}
```

설명:

- 상근 등으로 본인 근무 스케줄을 기록할 필요가 없는 사용자는 `defaultShiftOwnerType`을 `PARTNER`로 저장할 수 있다.
- 설정값은 `ME` 또는 `PARTNER`만 허용한다.
- 설정 저장 후 캘린더 통합 조회에서 `shiftOwnerType`을 생략하면 저장된 기본값이 적용된다.

응답 예시:

```json
{
  "success": true,
  "data": {
    "defaultShiftOwnerType": "PARTNER"
  }
}
```

## 9. 일정 API

권한 원칙:

- 같은 그룹에 속한 두 사용자는 그룹 내 모든 일정을 생성/수정/삭제할 수 있다.
- `ownerType`은 권한 판단값이 아니다.
- 권한 판단은 `currentUserId`, `groupId`, 그룹 멤버십으로 처리한다.

## 9.1 월간 일정 조회

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

## 9.2 날짜별 일정 조회

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

## 9.3 일정 생성

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

## 9.4 일정 수정

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

## 9.5 일정 삭제

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

## 10. 근무 스케줄 API

권한 원칙:

- 같은 그룹에 속한 두 사용자는 각자의 근무 스케줄을 별도로 생성/수정/삭제할 수 있다.
- 근무 스케줄 저장/수정/삭제 API는 기본적으로 현재 로그인 사용자의 스케줄에만 적용한다.
- 월간 캘린더 조회는 `shiftOwnerType` 선택값에 따라 나 또는 상대방 중 한 명의 근무 스케줄만 일정과 함께 반환한다.
- 한 사용자가 상근 등으로 근무 스케줄을 기록할 필요가 없는 경우, 사용자는 기본 근무 스케줄 표시 대상을 `PARTNER`로 설정할 수 있다.
- 기본 표시 대상 설정이 `PARTNER`인 사용자는 캘린더 조회 시 `shiftOwnerType`을 생략해도 상대방 근무 스케줄이 기본으로 반환되어야 한다.

## 10.1 월간 근무 스케줄 조회

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
        "ownerUserId": "usr_me",
        "ownerType": "ME",
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

## 10.2 날짜별 근무 스케줄 조회

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
      "ownerUserId": "usr_me",
      "ownerType": "ME",
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

## 10.3 날짜별 근무 스케줄 저장 또는 수정

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
    "ownerUserId": "usr_me",
    "ownerType": "ME",
    "shiftType": "DAY",
    "createdByUserId": "usr_me",
    "updatedByUserId": "usr_me",
    "createdAt": "2026-04-18T02:00:00Z",
    "updatedAt": "2026-04-18T02:00:00Z",
    "deletedAt": null
  }
}
```

## 10.4 월별 근무 스케줄 일괄 저장

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
        "ownerUserId": "usr_me",
        "ownerType": "ME",
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

## 10.5 날짜별 근무 스케줄 삭제

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

## 11. 캘린더 통합 조회 API

## 11.1 월간 캘린더 조회

- `GET /api/v1/calendar/month?year=2026&month=4&shiftOwnerType=ME`
- `shiftOwnerType`은 `ME` 또는 `PARTNER`를 허용한다.
- `shiftOwnerType`을 생략하면 현재 사용자의 기본 근무 스케줄 표시 대상 설정을 따른다.
- 기본 설정이 없으면 `ME`로 처리한다.
- 명시적으로 전달된 `shiftOwnerType`은 저장된 기본 설정보다 우선한다.

응답 예시:

```json
{
  "success": true,
  "data": {
    "year": 2026,
    "month": 4,
    "filters": {
      "ownerTypes": ["ME", "US", "PARTNER"],
      "includeShifts": true,
      "shiftOwnerType": "ME"
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
        "ownerUserId": "usr_me",
        "ownerType": "ME",
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
          "ownerUserId": "usr_me",
          "ownerType": "ME",
          "shiftType": "DAY"
        },
        "shifts": [
          {
            "id": "sft_01",
            "ownerUserId": "usr_me",
            "ownerType": "ME",
            "shiftType": "DAY"
          }
        ],
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

## 11.2 날짜 상세 조회

- `GET /api/v1/calendar/date/{date}?shiftOwnerType=ME`
- `shiftOwnerType`은 `ME` 또는 `PARTNER`를 허용한다.
- `shiftOwnerType`을 생략하면 현재 사용자의 기본 근무 스케줄 표시 대상 설정을 따른다.
- 기본 설정이 없으면 `ME`로 처리한다.
- 명시적으로 전달된 `shiftOwnerType`은 저장된 기본 설정보다 우선한다.

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
      "ownerUserId": "usr_me",
      "ownerType": "ME",
      "shiftType": "DAY",
      "createdByUserId": "usr_me",
      "updatedByUserId": "usr_me",
      "createdAt": "2026-04-01T00:00:00Z",
      "updatedAt": "2026-04-01T00:00:00Z",
      "deletedAt": null
    },
    "shifts": [
      {
        "id": "sft_01",
        "groupId": "grp_01",
        "date": "2026-04-18",
        "ownerUserId": "usr_me",
        "ownerType": "ME",
        "shiftType": "DAY",
        "createdByUserId": "usr_me",
        "updatedByUserId": "usr_me",
        "createdAt": "2026-04-01T00:00:00Z",
        "updatedAt": "2026-04-01T00:00:00Z",
        "deletedAt": null
      }
    ],
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

## 12. 알림 API

## 12.1 FCM 등록 토큰 저장

- `POST /api/v1/notifications/fcm-token`

설명:

- 로그인한 사용자의 FCM 등록 토큰을 서버에 저장한다.
- 같은 토큰이 이미 있으면 현재 사용자와 그룹 기준으로 소유 정보를 갱신한다.
- 저장된 토큰은 일정 변경 알림과 일일 요약 알림 발송 대상 식별에 사용한다.

요청 본문:

```json
{
  "token": "fcm-registration-token",
  "platform": "ANDROID"
}
```

응답 예시:

```json
{
  "success": true,
  "data": {
    "registered": true
  }
}
```

검증:

- `token`은 필수이며 공백일 수 없다.
- `token` 최대 길이는 512자다.
- `platform` 최대 길이는 30자다.

## 12.2 FCM 등록 토큰 삭제

- `DELETE /api/v1/notifications/fcm-token`

설명:

- 로그인한 사용자의 FCM 등록 토큰을 삭제한다.
- 요청 사용자가 소유한 토큰만 삭제한다.
- 존재하지 않는 토큰이거나 다른 사용자의 토큰이면 성공 응답만 반환하고 삭제하지 않는다.

요청 본문:

```json
{
  "token": "fcm-registration-token"
}
```

응답 예시:

```json
{
  "success": true,
  "data": null
}
```

## 12.3 서버 발송 알림

현재 백엔드는 아래 알림을 발송한다. FCM은 `app.fcm.enabled=true`이고 Firebase 서비스 계정 경로가 설정된 경우에만 실제 전송된다. 운영 기본 경로는 `classpath:firebase/schedule-for-unusual-fbe03-firebase-adminsdk-fbsvc-f4d15576a0.json`이며, 환경변수 `APP_FCM_CREDENTIALS_PATH`로 덮어쓸 수 있다.

| 알림 | 발송 시점 | 대상 | data |
| --- | --- | --- | --- |
| 일정 변경 | 일정 생성/수정/삭제 트랜잭션 커밋 후 | 같은 그룹의 행위자 제외 사용자 | `type=SCHEDULE_CHANGED`, `changeType=CREATED|UPDATED|DELETED` |
| 오늘 일정 요약 | 매일 `app.fcm.daily-summary-cron` | 그룹 내 각 사용자 | `type=DAILY_SCHEDULE_SUMMARY`, `date=YYYY-MM-DD` |

## 13. 유효성 검증 규칙

### 13.1 일정

- `title`은 공백 제외 1자 이상이어야 한다.
- `startDate <= endDate`를 만족해야 한다.
- `subjectType`은 `PERSONAL`, `SHARED` 중 하나여야 한다.
- `subjectType = PERSONAL`이면 `ownerUserId`는 필수다.
- `subjectType = SHARED`이면 `ownerUserId`는 nullable 허용이다.
- `ownerType`은 저장값이 아니라 응답 계산값이다.

### 13.2 근무 스케줄

- `shiftType`은 정의된 enum 중 하나여야 한다.
- 같은 `groupId`, 같은 `ownerUserId`, 같은 `date`에는 하나의 스케줄만 허용한다.
- 기본 근무 스케줄 표시 대상 설정값은 `ME` 또는 `PARTNER`만 허용한다.

### 13.3 그룹/파트너

- 사용자 한 명은 동시에 하나의 그룹에만 속할 수 없다.
- 하나의 그룹은 최대 2명으로 제한한다.
- 이미 다른 그룹에 속한 사용자는 파트너로 등록할 수 없다.
- 파트너 초대의 기본 전달 방식은 카카오톡 친구 메시지 API 직접 발송이 아니라 `카카오톡 공유 기반 초대 링크`다.
- 초대 링크는 만료 시간과 상태를 가져야 하며, 수락 시 1회성으로 처리되어야 한다.
- 비로그인 사용자는 초대 링크 진입 후 가입 또는 로그인 완료 뒤 같은 초대로 복귀할 수 있어야 한다.

### 12.4 인증

- 카카오 OAuth2 콜백의 `code`는 1회성으로 처리해야 한다.
- 액세스 토큰이 만료되면 보호 API는 `401 Unauthorized`를 반환해야 한다.
- 무효화된 리프레시 토큰으로 재발급할 수 없어야 한다.
- 로그아웃 후 동일 리프레시 토큰 재사용은 실패해야 한다.

## 14. 그룹/파트너 API

### 13.1 그룹 조회

- `GET /api/v1/groups/me`

설명:

- 현재 로그인 사용자가 속한 그룹과 멤버 정보를 조회한다.
- 파트너 연결 상태 확인용 기본 API다.

### 13.2 초대 생성

- `POST /api/v1/groups/invites`

설명:

- 현재 로그인 사용자가 자신의 그룹에 대한 파트너 초대를 생성한다.
- Android 앱에서는 메인 캘린더 상단 배너형 CTA를 통해 이 API를 호출한다.
- 생성 결과에는 카카오톡 공유에 사용할 초대 링크와 초대 토큰 메타데이터가 포함될 수 있다.
- 실제 전달은 서버의 친구 메시지 직접 발송이 아니라 Android 앱의 `카카오톡 공유` 기능으로 수행한다.

요청 본문 예시:

```json
{
  "channel": "KAKAO_TALK_SHARE"
}
```

응답 예시:

```json
{
  "success": true,
  "data": {
    "inviteId": "inv_01JATK123ABC",
    "groupId": "grp_01",
    "inviteToken": "itk_01JATKXYZ987",
    "shareUrl": "https://app.example.com/invites/itk_01JATKXYZ987",
    "deepLink": "scheduleapp://invite/accept?inviteToken=itk_01JATKXYZ987",
    "status": "PENDING",
    "expiresAt": "2026-04-25T12:00:00Z"
  }
}
```

요청 규칙:

- 앱은 `파트너 미연결 상태`에서만 상단 배너형 CTA를 노출하는 것을 기본 UX로 사용한다.
- 이미 파트너가 연결된 그룹이면 실패해야 한다.
- 이미 유효한 `PENDING` 초대가 있으면 새 토큰을 만들지 않고 기존 초대 정보를 그대로 반환한다.
- 만료된 `PENDING` 초대가 있으면 `EXPIRED`로 정리한 뒤 새 초대를 생성한다.
- 초대 링크는 만료 시간을 가져야 한다.

### 13.3 초대 조회

- `GET /api/v1/groups/invites/{inviteToken}`

설명:

- 초대 링크 진입 후 초대 상세 정보를 조회한다.
- 비로그인 사용자는 링크 유효성 정도만 확인하고, 로그인 후 수락 가능한 상태로 이어질 수 있어야 한다.

응답 예시:

```json
{
  "success": true,
  "data": {
    "inviteId": "inv_01JATK123ABC",
    "groupId": "grp_01",
    "inviter": {
      "userId": "usr_me",
      "nickname": "성철"
    },
    "status": "PENDING",
    "requiresAuth": true,
    "expiresAt": "2026-04-25T12:00:00Z"
  }
}
```

### 13.4 초대 수락

- `POST /api/v1/groups/invites/accept`

설명:

- 로그인된 사용자가 초대 토큰을 기준으로 파트너 연결을 수락한다.
- 성공 시 같은 그룹으로 연결되고, 이후 그룹 내 일정 공동 편집과 서로의 근무 스케줄 조회가 가능해진다.
- 성공 응답에는 최신 `groupId`가 포함된 새 인증 토큰을 포함한다. 클라이언트는 기존 세션 토큰을 즉시 교체해야 한다.

요청 본문 예시:

```json
{
  "inviteToken": "itk_01JATKXYZ987"
}
```

응답 예시:

```json
{
  "success": true,
  "data": {
    "groupId": "grp_01",
    "inviteId": "inv_01JATK123ABC",
    "accepted": true,
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
      "canReadAllShifts": true,
      "canEditOwnShifts": true
    },
    "tokens": {
      "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
      "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
      "tokenType": "Bearer",
      "expiresIn": 3600,
      "refreshTokenExpiresIn": 1209600
    }
  }
}
```

요청 규칙:

- 로그인 사용자만 수락할 수 있어야 한다.
- 초대 생성자는 본인이 만든 초대를 수락할 수 없어야 한다.
- 만료, 취소, 사용 완료 초대는 실패해야 한다.
- 이미 다른 그룹에 속한 사용자는 실패해야 한다.
- 이미 파트너가 연결된 그룹이면 실패해야 한다.
- 수락 성공 후 기존 access token의 `groupId`는 더 이상 사용하지 말고, 응답의 `tokens.accessToken`으로 교체해야 한다.

### 13.5 파트너 직접 추가

- `POST /api/v1/groups/partner`

설명:

- 현재 단계에서는 별도 직접 연결 API보다 초대 생성 및 수락 흐름을 우선 사용한다.
- 이 엔드포인트가 유지되더라도 내부적으로는 유효한 초대 수락 결과를 확정하는 용도로 한정하는 것이 바람직하다.

## 15. 구현 메모

- 일정과 근무 스케줄의 작성자/수정자는 모두 `users.id` 기준으로 저장하는 것이 좋다.
- `ownerType`은 저장값으로 두지 말고, `subjectType`, `ownerUserId`, `currentUserId`로 응답 시 계산하는 것이 좋다.
- 일정과 근무 스케줄 조회는 `deleted_at is null` 조건을 기본으로 사용해야 한다.
- 권한 판단은 `currentUserId`와 그룹 멤버십으로 처리하고, `ME`/`PARTNER` 표시값과 분리해야 한다.
- 근무 스케줄 조회 대상은 명시된 `shiftOwnerType`을 우선하고, 없으면 사용자의 `defaultShiftOwnerType`을 따른다.
- Spring Security OAuth2 로그인 성공 처리와 JWT 발급을 같은 인증 서비스에서 연결하면 단순하다.
- 리프레시 토큰은 서버에서 무효화 가능해야 하므로 DB 저장 전략이 적합하다.

## 16. MVP 우선 구현 목록

1. `users`, `refresh_tokens`, `events`, `shift_schedules`, `group_invites` 구조 확정
2. `GET /auth/kakao/login`, `GET /auth/kakao/callback`, `POST /auth/refresh`, `POST /auth/logout`
3. `GET /users/me`, `PATCH /users/me/settings`
4. `POST /events`, `PATCH /events/{eventId}`, `DELETE /events/{eventId}`
5. `PUT /shifts/{date}`, `DELETE /shifts/{date}`, `PUT /shifts/monthly`
6. `GET /calendar/month`, `GET /calendar/date/{date}`
7. 그룹/초대 API 연결





