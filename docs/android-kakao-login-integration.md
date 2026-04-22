# ScheduleApp Android 카카오 로그인 연동 문서

## 1. 문서 목적

이 문서는 현재 `schedule-api` 백엔드 구현을 기준으로 Android 앱에서 `카카오 로그인만으로 회원가입 및 로그인`을 처리하기 위한 최신 연동 방식을 정리한다.

본 문서는 아래 목적을 가진다.

- Android 앱과 백엔드의 역할을 명확히 분리한다.
- 카카오 Android SDK 직접 로그인 방식을 기준 아키텍처로 정리한다.
- 백엔드에서 이미 구현된 모바일 로그인 API와 브라우저 fallback 흐름을 구분한다.
- 앱/백엔드 협업 시 필요한 작업 항목과 운영 메모를 정리한다.

## 2. 현재 기준 아키텍처

현재 프로젝트의 기본 로그인 방식은 `카카오 Android SDK 직접 로그인 -> 백엔드 JWT 발급` 구조다.

핵심 흐름은 아래와 같다.

1. Android 앱이 카카오 Android SDK로 로그인한다.
2. 카카오톡 앱이 있으면 앱 기반 인증을 사용하고, 없으면 카카오 계정 로그인으로 진행한다.
3. 로그인 성공 시 Android 앱이 카카오 access token을 받는다.
4. 앱은 `POST /api/v1/auth/kakao/mobile`로 access token을 백엔드에 전달한다.
5. 백엔드는 카카오 사용자 정보 API로 access token을 검증하고 사용자 프로필을 조회한다.
6. 백엔드는 기존 회원이면 로그인, 신규면 회원가입 처리 후 서비스용 JWT를 발급한다.
7. 앱은 응답의 access token, refresh token을 저장하고 이후 모든 API를 백엔드 JWT로 호출한다.

핵심 원칙은 아래와 같다.

- 카카오 인증 책임은 카카오 SDK가 가진다.
- 서비스 인증 책임은 백엔드 JWT가 가진다.
- Android 앱은 카카오 토큰을 장기 세션 토큰으로 사용하지 않는다.
- 회원가입과 로그인 구분은 백엔드가 처리한다.

## 3. 현재 백엔드 구현 상태

현재 백엔드에서 구현된 인증 경로는 아래 두 가지다.

### 3.1 기본 경로: 카카오 SDK 직접 로그인

구현 완료 항목:

- `POST /api/v1/auth/kakao/mobile`
- 카카오 access token 기반 사용자 정보 조회
- 기존 회원 로그인 / 신규 회원가입 처리
- 자체 JWT access token / refresh token 발급
- 통합 테스트 추가

이 경로가 Android 앱에서 우선 사용해야 하는 기본 경로다.

### 3.2 보조 경로: 브라우저 OAuth2 fallback

기존 브라우저 기반 흐름도 유지 중이다.

- `GET /api/v1/auth/kakao/login`
- `GET /api/v1/auth/kakao/callback`
- `POST /api/v1/auth/mobile/exchange`

이 흐름은 아래 용도로 유지한다.

- 브라우저 fallback
- 테스트
- 필요 시 특정 환경에서의 우회 로그인

현재 상태 메모:

- 브라우저 fallback은 유지되지만, Android 앱 기본 구현 대상은 아니다.
- `state`와 `loginCode` 저장소는 현재 메모리 기반이다.
- 단일 서버 개발 환경에서는 충분하지만, 다중 인스턴스 운영 시 Redis 같은 외부 저장소가 더 적절하다.

## 4. Android 앱 권장 연동 방식

Android 앱에서는 아래 순서로 구현하는 것을 권장한다.

1. 로그인 버튼은 `카카오로 시작하기` 하나만 노출한다.
2. 버튼 클릭 시 카카오 Android SDK 로그인 시도를 한다.
3. 로그인 성공 시 SDK에서 받은 카카오 access token을 추출한다.
4. 앱은 백엔드 `POST /api/v1/auth/kakao/mobile` API를 호출한다.
5. 응답으로 받은 `user + tokens + isNewUser`를 저장한다.
6. 이후 모든 API 호출은 백엔드 JWT로 처리한다.

즉, 앱에서는 더 이상 `/api/v1/auth/kakao/login`을 브라우저로 열 필요가 없다.

## 5. Android 구현 가이드

### 5.1 Android 앱 책임

- 카카오 SDK 의존성 추가
- AndroidManifest 설정 추가
- 카카오 앱 키 설정
- 로그인 버튼 노출
- 카카오 SDK 로그인 처리
- 백엔드 `/api/v1/auth/kakao/mobile` 호출
- 백엔드 JWT 저장
- refresh 재발급 처리
- 로그아웃 처리

### 5.2 백엔드 책임

- 카카오 access token 검증
- 카카오 사용자 정보 조회
- 회원가입/로그인 판별
- JWT 발급 및 재발급
- refresh token 저장 및 revoke 처리
- 브라우저 fallback 경로 유지

### 5.3 Android SDK 로그인 예시

예시 개념 코드는 아래와 같다.

```kotlin
UserApiClient.instance.loginWithKakaoTalk(context) { token, error ->
    if (error != null) {
        // 카카오톡 로그인 실패 시 계정 로그인 fallback 고려
        return@loginWithKakaoTalk
    }

    val kakaoAccessToken = token?.accessToken ?: return@loginWithKakaoTalk

    // 백엔드로 access token 전달
    authApi.kakaoMobileLogin(
        KakaoMobileLoginRequest(accessToken = kakaoAccessToken)
    )
}
```

카카오톡 앱 로그인이 불가능한 경우에는 카카오 계정 로그인으로 fallback 하면 된다.

### 5.4 백엔드 호출 예시

```json
POST /api/v1/auth/kakao/mobile
Content-Type: application/json

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
      "id": "usr_01",
      "oauthProvider": "KAKAO",
      "oauthProviderUserId": "kakao-123",
      "nickname": "홍길동",
      "profileImageUrl": "https://image.example/profile.png",
      "groupId": "grp_01",
      "createdAt": "2026-04-22T01:00:00Z",
      "updatedAt": "2026-04-22T01:00:00Z"
    },
    "tokens": {
      "accessToken": "...",
      "refreshToken": "...",
      "tokenType": "Bearer",
      "expiresIn": 3600,
      "refreshTokenExpiresIn": 1209600
    },
    "isNewUser": true
  }
}
```

### 5.5 토큰 저장 원칙

앱은 아래 원칙을 따른다.

- access token과 refresh token은 일반 SharedPreferences가 아니라 `EncryptedSharedPreferences` 또는 이에 준하는 안전 저장소에 저장한다.
- access token 만료 시 `/api/v1/auth/refresh`를 호출한다.
- 로그아웃 시 `/api/v1/auth/logout`을 호출한 뒤 로컬 토큰을 삭제한다.
- 카카오 access token 자체를 장기 세션 저장소로 사용하지 않는다.

## 6. 브라우저 fallback 흐름

현재 구현에는 브라우저 기반 fallback도 남아 있다.

흐름은 아래와 같다.

1. 앱 또는 브라우저가 `GET /api/v1/auth/kakao/login?appRedirectUri=scheduleapp://auth/callback` 호출
2. 백엔드가 카카오 인증 URL로 redirect
3. 카카오가 `/api/v1/auth/kakao/callback`으로 `code`와 `state` 전달
4. 백엔드가 카카오 사용자 인증 처리 후 앱 딥링크로 `loginCode` 전달
5. 앱이 `POST /api/v1/auth/mobile/exchange`로 최종 JWT 교환

이 경로는 기본 경로가 아니라 fallback 또는 테스트용 경로로 간주한다.

## 7. 협업 체크리스트

### 7.1 백엔드 완료 항목

- [x] `POST /api/v1/auth/kakao/mobile` 추가
- [x] 카카오 access token 기반 사용자 조회 구현
- [x] 기존 회원 로그인 / 신규 회원가입 처리 공통화
- [x] JWT 발급 연동
- [x] 브라우저 fallback 유지
- [x] 인증 통합 테스트 추가

### 7.2 Android 작업 항목

- [ ] 카카오 Android SDK 의존성 추가
- [ ] AndroidManifest 카카오 설정 추가
- [ ] 카카오 앱 키 설정 추가
- [ ] `카카오로 시작하기` 버튼 구현
- [ ] SDK 로그인 성공 후 `/api/v1/auth/kakao/mobile` 호출 구현
- [ ] 백엔드 JWT 저장 구현
- [ ] access token 첨부 인터셉터 구현
- [ ] refresh 재발급 처리 구현
- [ ] 로그아웃 처리 구현

### 7.3 공동 합의 필요 항목

- [x] Android 기본 로그인 방식은 SDK 직접 로그인으로 확정
- [ ] 개발/운영 API 도메인 확정
- [ ] 카카오 개발자 콘솔 앱 키 및 redirect URI 환경별 정리
- [ ] 로그인 실패 시 사용자 메시지 정책
- [ ] 브라우저 fallback 유지 범위 결정

## 8. 운영 메모

현재 구현에서 주의할 점은 아래와 같다.

- 서버는 현재 카카오 access token을 카카오 사용자 정보 API 호출로 검증한다.
- 브라우저 fallback의 `state`와 `loginCode`는 메모리 기반 저장소를 사용한다.
- 다중 인스턴스 운영에서는 Redis 같은 외부 저장소로 전환하는 것이 적절하다.
- Android 앱은 가능한 한 SDK 직접 로그인 경로만 사용하도록 구현하는 것이 UX 측면에서 낫다.

## 9. 최종 권장안

현재 프로젝트 기준 최종 권장안은 아래와 같다.

- Android 앱 로그인 수단은 `카카오 로그인` 하나만 제공한다.
- Android 앱은 카카오 SDK 직접 로그인을 사용한다.
- 백엔드는 `POST /api/v1/auth/kakao/mobile`로 카카오 access token을 받아 사용자 검증 후 JWT를 발급한다.
- 이후 앱은 백엔드 JWT 기반으로 모든 인증을 처리한다.
- 기존 브라우저 OAuth2 흐름은 fallback으로만 유지한다.

이 방식이면 아래 요구사항을 만족할 수 있다.

- 앱 안에서 자연스러운 로그인 UX 제공
- 카카오 로그인만으로 가입/로그인 진행
- 앱과 백엔드 책임 분리 명확화
- 기존 JWT 인증 구조 재사용
