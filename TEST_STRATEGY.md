# 테스트 전략 문서 (TEST_STRATEGY.md)

## 1. 검증할 행위 목록

### 1.1 선택 기준

API E2E 테스트 대상 행위를 선택할 때 다음 기준을 적용한다:

| 우선순위 | 기준 | 설명 |
|----------|------|------|
| P0 | 핵심 성공 경로 | 사용자가 정상적으로 기능을 사용하는 시나리오 |
| P1 | 필수값 검증 | 요청 데이터 누락/오류 시 적절한 에러 반환 |
| P2 | 도메인 규칙 위반 | 비즈니스 로직 위반 시 적절한 에러 반환 |
| P3 | 엣지 케이스 | 경계값, 빈 데이터 등 특수 상황 |

### 1.2 대상 API 전체 목록 (19개)

| # | 도메인 | HTTP | 경로 | 인증 | 컨트롤러 |
|---|--------|------|------|------|----------|
| 1 | Member | POST | /api/members/register | X | MemberController |
| 2 | Member | POST | /api/members/login | X | MemberController |
| 3 | Category | GET | /api/categories | X | CategoryController |
| 4 | Category | POST | /api/categories | X | CategoryController |
| 5 | Category | PUT | /api/categories/{id} | X | CategoryController |
| 6 | Category | DELETE | /api/categories/{id} | X | CategoryController |
| 7 | Product | GET | /api/products | X | ProductController |
| 8 | Product | GET | /api/products/{id} | X | ProductController |
| 9 | Product | POST | /api/products | X | ProductController |
| 10 | Product | PUT | /api/products/{id} | X | ProductController |
| 11 | Product | DELETE | /api/products/{id} | X | ProductController |
| 12 | Option | GET | /api/products/{productId}/options | X | OptionController |
| 13 | Option | POST | /api/products/{productId}/options | X | OptionController |
| 14 | Option | DELETE | /api/products/{productId}/options/{optionId} | X | OptionController |
| 15 | Wish | GET | /api/wishes | O (JWT) | WishController |
| 16 | Wish | POST | /api/wishes | O (JWT) | WishController |
| 17 | Wish | DELETE | /api/wishes/{id} | O (JWT) | WishController |
| 18 | Order | GET | /api/orders | O (JWT) | OrderController |
| 19 | Order | POST | /api/orders | O (JWT) | OrderController |

### 1.3 API별 검증 행위 (6개 도메인, 19 API, 64 시나리오)

#### Member (2 API, 7 시나리오)

**POST /api/members/register**

| 우선순위 | 행위 | 근거 (소스코드) |
|----------|------|----------------|
| P0 | 유효한 이메일/비밀번호로 회원 가입 성공 (201 + JWT) | 핵심 기능 |
| P1 | email 누락 시 400 | @NotBlank @Email 검증 |
| P1 | 잘못된 이메일 형식 시 400 | @Email 검증 |
| P2 | 이미 등록된 이메일로 가입 시 400 | `existsByEmail` 중복 체크 |

**POST /api/members/login**

| 우선순위 | 행위 | 근거 |
|----------|------|------|
| P0 | 유효한 이메일/비밀번호로 로그인 성공 (200 + JWT) | 핵심 기능 |
| P2 | 존재하지 않는 이메일로 로그인 시 400 | `findByEmail` orElseThrow |
| P2 | 잘못된 비밀번호로 로그인 시 400 | password.equals 검증 |

#### Category (4 API, 11 시나리오)

**GET /api/categories**

| 우선순위 | 행위 | 근거 |
|----------|------|------|
| P0 | 카테고리 목록 조회 성공 (200) | 핵심 기능 |
| P3 | 빈 목록 조회 시 빈 배열 반환 (200) | 초기 상태 |

**POST /api/categories**

| 우선순위 | 행위 | 근거 |
|----------|------|------|
| P0 | 유효한 데이터로 카테고리 생성 (201) | 핵심 기능 |
| P1 | name 누락/빈 문자열 시 400 | @NotBlank 검증 |
| P1 | color 누락 시 400 | @NotBlank 검증 |
| P1 | imageUrl 누락 시 400 | @NotBlank 검증 |

**PUT /api/categories/{id}**

| 우선순위 | 행위 | 근거 |
|----------|------|------|
| P0 | 카테고리 수정 성공 (200) | 핵심 기능 |
| P2 | 존재하지 않는 id 수정 시 404 | `findById` orElse null |

**DELETE /api/categories/{id}**

| 우선순위 | 행위 | 근거 |
|----------|------|------|
| P0 | 카테고리 삭제 성공 (204) | 핵심 기능 |

#### Product (5 API, 16 시나리오)

**GET /api/products**

| 우선순위 | 행위 | 근거 |
|----------|------|------|
| P0 | 상품 페이징 목록 조회 성공 (200) | 핵심 기능, Pageable |
| P0 | 상품에 카테고리 정보 포함 확인 | 연관 데이터 반환 |
| P3 | 빈 목록 조회 시 빈 페이지 반환 (200) | 초기 상태 |

**GET /api/products/{id}**

| 우선순위 | 행위 | 근거 |
|----------|------|------|
| P0 | 상품 상세 조회 성공 (200) | 핵심 기능 |
| P2 | 존재하지 않는 id 조회 시 404 | `findById` orElse null |

**POST /api/products**

| 우선순위 | 행위 | 근거 |
|----------|------|------|
| P0 | 유효한 데이터로 상품 생성 (201) | 핵심 기능 |
| P1 | name 누락 시 400 | @NotBlank 검증 |
| P1 | price 0 이하 시 400 | @Positive 검증 |
| P1 | categoryId 누락 시 400 | @NotNull 검증 |
| P2 | 존재하지 않는 categoryId 시 404 | `findById` orElse null |
| P2 | name 15자 초과 시 400 | ProductNameValidator |
| P2 | name에 "카카오" 포함 시 400 | ProductNameValidator (allowKakao=false) |

**PUT /api/products/{id}**

| 우선순위 | 행위 | 근거 |
|----------|------|------|
| P0 | 상품 수정 성공 (200) | 핵심 기능 |
| P2 | 존재하지 않는 id 수정 시 404 | `findById` orElse null |

**DELETE /api/products/{id}**

| 우선순위 | 행위 | 근거 |
|----------|------|------|
| P0 | 상품 삭제 성공 (204) | 핵심 기능, CascadeType.ALL로 옵션도 삭제 |

#### Option (3 API, 11 시나리오)

**GET /api/products/{productId}/options**

| 우선순위 | 행위 | 근거 |
|----------|------|------|
| P0 | 특정 상품의 옵션 목록 조회 (200) | 핵심 기능 |
| P2 | 존재하지 않는 productId 시 404 | `findById` orElse null |

**POST /api/products/{productId}/options**

| 우선순위 | 행위 | 근거 |
|----------|------|------|
| P0 | 유효한 데이터로 옵션 추가 (201) | 핵심 기능 |
| P1 | name 누락 시 400 | @NotBlank 검증 |
| P1 | quantity 0 이하 시 400 | @Min(1) 검증 |
| P2 | 존재하지 않는 productId 시 404 | `findById` orElse null |
| P2 | 중복 옵션명 시 400 | `existsByProductIdAndName` 검증 |
| P2 | name 50자 초과 시 400 | OptionNameValidator |

**DELETE /api/products/{productId}/options/{optionId}**

| 우선순위 | 행위 | 근거 |
|----------|------|------|
| P0 | 옵션 삭제 성공 (204) | 핵심 기능 |
| P2 | 마지막 1개 옵션 삭제 시도 시 400 | `options.size() <= 1` 검증 |
| P2 | 존재하지 않는 optionId 시 404 | `findById` orElse null |

#### Wish (3 API, 10 시나리오)

**GET /api/wishes**

| 우선순위 | 행위 | 근거 |
|----------|------|------|
| P0 | 위시리스트 페이징 조회 성공 (200) | 핵심 기능 |
| P1 | Authorization 헤더 누락 시 400 | `@RequestHeader` required=true (기본값) → MissingRequestHeaderException |
| P3 | 빈 위시리스트 조회 시 빈 페이지 (200) | 초기 상태 |

**POST /api/wishes**

| 우선순위 | 행위 | 근거 |
|----------|------|------|
| P0 | 위시리스트에 상품 추가 성공 (201) | 핵심 기능 |
| P1 | Authorization 헤더 누락 시 400 | `@RequestHeader` required=true (기본값) → MissingRequestHeaderException |
| P2 | 존재하지 않는 productId 시 404 | `findById` orElse null |
| P3 | 이미 추가된 상품 재추가 시 기존 항목 반환 (200) | `findByMemberIdAndProductId` 중복 체크 |

**DELETE /api/wishes/{id}**

| 우선순위 | 행위 | 근거 |
|----------|------|------|
| P0 | 위시 항목 삭제 성공 (204) | 핵심 기능 |
| P1 | Authorization 헤더 누락 시 400 | `@RequestHeader` required=true (기본값) → MissingRequestHeaderException |
| P2 | 다른 사용자의 위시 삭제 시 403 | `memberId.equals` 권한 검증 |

#### Order (2 API, 9 시나리오)

**GET /api/orders**

| 우선순위 | 행위 | 근거 |
|----------|------|------|
| P0 | 주문 목록 페이징 조회 성공 (200) | 핵심 기능 |
| P1 | Authorization 헤더 누락 시 400 | `@RequestHeader` required=true (기본값) → MissingRequestHeaderException |

**POST /api/orders**

| 우선순위 | 행위 | 근거 |
|----------|------|------|
| P0 | 유효한 주문 생성 성공 (201, 재고 차감 + 포인트 차감) | 핵심 기능 |
| P1 | Authorization 헤더 누락 시 400 | `@RequestHeader` required=true (기본값) → MissingRequestHeaderException |
| P1 | optionId 누락 시 400 | @NotNull 검증 |
| P1 | quantity 0 이하 시 400 | @Min(1) 검증 |
| P2 | 존재하지 않는 optionId 시 404 | `findById` orElse null |
| P2 | 재고 부족 시 주문 실패 + 상태 무결성 검증 | `subtractQuantity` 예외 시 재고/포인트 변동 없음 + 주문 미생성 |
| P2 | 포인트 부족 시 주문 실패 + 원자성(롤백) 검증 | 재고 차감 후 `deductPoint` 예외가 나도 재고/포인트 변동 없음 + 주문 미생성 |

### 1.4 시나리오 요약

| 도메인 | API 수 | 시나리오 수 |
|--------|--------|------------|
| Member | 2 | 7 |
| Category | 4 | 11 |
| Product | 5 | 16 |
| Option | 3 | 11 |
| Wish | 3 | 10 |
| Order | 2 | 9 |
| **합계** | **19** | **64** |
