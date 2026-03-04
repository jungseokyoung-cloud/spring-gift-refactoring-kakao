---
name: service-layer-extractor
description: >
  컨트롤러의 비즈니스 로직을 Service 계층으로 추출한다.
  도메인명을 인자로 받거나, 미지정 시 의존 순서대로 하나씩 처리한다.
  구조 변경만 수행하며, 동작 변경·주석 수정·테스트 수정은 하지 않는다.
  한 번 호출에 한 도메인만 처리한다.
argument-hint: "[도메인명, e.g. Category, OrderController]"
allowed-tools: Read, Write, Edit, Grep, Glob, Bash(./gradlew *), Task
---

# service-layer-extractor

컨트롤러의 비즈니스 로직을 Service 계층으로 추출한다.

## 역할
- 컨트롤러가 직접 호출하는 Repository 로직을 Service로 이동한다.
- 구조 변경만 수행한다. 외부에서 관찰 가능한 동작은 일절 바꾸지 않는다.
- 한 번 호출에 한 도메인만 처리한다.

## 입력 계약
- 사용자가 도메인명(예: `Category`) 또는 컨트롤러명(예: `OrderController`)을 지정하면 해당 도메인을 처리한다.
- 대상이 없으면 아래 처리 순서에서 아직 Service가 없는 첫 번째 도메인을 처리한다.
- `Controller` 접미사가 있으면 떼고 도메인명으로 사용한다.

## 처리 순서 (의존 관계 순)
1. Category
2. Member
3. Product
4. Option
5. Wish
6. Order
7. KakaoAuth

앞선 도메인의 Service가 아직 없으면, 그 도메인부터 처리하도록 사용자에게 안내한다.

## 도메인별 컨트롤러 매핑

| 도메인 | 컨트롤러 | 서비스 |
|--------|----------|--------|
| Category | `CategoryController` | `CategoryService` |
| Member | `MemberController`, `AdminMemberController` | `MemberService` |
| Product | `ProductController`, `AdminProductController` | `ProductService` |
| Option | `OptionController` | `OptionService` |
| Wish | `WishController` | `WishService` |
| Order | `OrderController` | `OrderService` |
| KakaoAuth | `KakaoAuthController` | `KakaoAuthService` |

REST + Admin 컨트롤러가 같은 도메인이면 **하나의 Service**를 공유한다.

## 실행 절차

### Step 1: 대상 파일 읽기
대상 컨트롤러 + 관련 파일(Entity, Repository, DTO, Validator 등)을 모두 읽는다.
```
Glob: src/main/java/gift/{domain}/**/*.java
```

### Step 2: 비즈니스 로직 분류
컨트롤러 코드를 한 줄씩 분석하여 아래 표에 따라 분류한다.

#### 컨트롤러에 남기는 것 (웹 계층 관심사)
- `@Valid`, `@RequestBody`, `@RequestParam` 등 요청 파싱
- `ResponseEntity` 빌드, HTTP 상태 코드 결정
- `@ExceptionHandler` 메서드
- 인증 체크 (`authenticationResolver.extractMember`) + 401 반환
- Admin 폼 검증 (`@RequestParam` 바인딩) + 뷰 이름 반환 + 리다이렉트
- `populateXxxForm` 같은 Model 조작 private 메서드

#### 서비스로 이동하는 것 (비즈니스 로직)
- Repository 호출 (findAll, findById, save, delete 등)
- 비즈니스 검증 (중복체크, 존재체크, 소유권 검사 등)
- 도메인 로직 조합 (주문 플로우: 재고 차감 → 포인트 차감 → 저장 등)
- 외부 연동 (카카오 메시지 전송 등)
- Validator 호출 (REST 컨트롤러에서 `XxxValidator.validate()` 호출)

### Step 3: Service 클래스 생성
대상 도메인 패키지에 `{Domain}Service.java`를 생성한다.

#### 서비스 클래스 템플릿
```java
package gift.{domain};

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class {Domain}Service {
    private final {Domain}Repository {domain}Repository;
    // 필요한 다른 Repository, Client 등

    public {Domain}Service({Domain}Repository {domain}Repository) {
        this.{domain}Repository = {domain}Repository;
    }

    // 읽기 메서드 — 클래스 레벨 @Transactional(readOnly = true) 적용
    // 쓰기 메서드 — @Transactional 개별 부착
}
```

#### 서비스 설계 규칙
- `@Service` + 생성자 주입 (final 필드)
- 클래스 레벨 `@Transactional(readOnly = true)`, 쓰기 메서드에 `@Transactional`
- **Entity를 반환**한다. `ResponseEntity`를 반환하지 않는다.
- 존재하지 않는 엔티티는 `orElseThrow()`로 예외를 던진다 (컨트롤러의 `orElse(null) + if null → notFound` 패턴을 대체)
- 컨트롤러에서 서비스의 예외를 catch하여 적절한 HTTP 응답으로 변환한다

#### `orElse(null)` → `orElseThrow()` 변환 패턴

**변환 전 (컨트롤러):**
```java
Product product = productRepository.findById(id).orElse(null);
if (product == null) {
    return ResponseEntity.notFound().build();
}
```

**변환 후 (서비스):**
```java
public Product getProduct(Long id) {
    return productRepository.findById(id)
        .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다. id=" + id));
}
```

**변환 후 (컨트롤러):**
```java
try {
    Product product = productService.getProduct(id);
    return ResponseEntity.ok(ProductResponse.from(product));
} catch (NoSuchElementException e) {
    return ResponseEntity.notFound().build();
}
```

### Step 4: 컨트롤러 리팩토링
컨트롤러의 Repository 의존을 Service 의존으로 교체한다.

#### 리팩토링 규칙
- Repository 필드 → Service 필드로 교체
- Repository 직접 호출 → Service 메서드 호출로 변경
- `ResponseEntity` 빌드, HTTP 상태 코드, `@ExceptionHandler`는 컨트롤러에 유지
- 인증 체크 (`authenticationResolver.extractMember` + 401)는 컨트롤러에 유지
- Admin 컨트롤러의 폼 검증, Model 조작, 뷰 반환은 컨트롤러에 유지
- `populateXxxForm` private 메서드는 컨트롤러에 유지

#### Admin 컨트롤러 특수 처리
Admin 컨트롤러(`@Controller`)에서 Validator 호출 후 에러가 있으면 뷰를 반환하는 패턴은 **컨트롤러에 유지**한다. Service는 Validator 호출 결과를 알 필요가 없다.
```java
// AdminProductController — 이 부분은 컨트롤러에 유지
List<String> errors = ProductNameValidator.validate(name, true);
if (!errors.isEmpty()) {
    populateNewForm(model, errors, name, price, imageUrl, categoryId);
    return "product/new";
}
// 이 이후의 Repository 호출만 Service로 이동
```

### Step 5: 테스트 실행
```bash
./gradlew test
```
- 모든 테스트가 통과해야 한다.
- 실패 시 **Service 또는 Controller 코드를 수정**한다. 테스트 코드는 수정하지 않는다.
- 실패 원인이 명확하지 않으면 사용자에게 보고한다.

### Step 6: 자기 검증 체크리스트
모든 테스트 통과 후, 아래 항목을 하나씩 확인한다.

- [ ] Service 클래스에 `@Service` 어노테이션이 있는가
- [ ] Service 클래스에 `@Transactional(readOnly = true)` 클래스 레벨 어노테이션이 있는가
- [ ] 쓰기 메서드(save, delete, update)에 `@Transactional`이 붙어 있는가
- [ ] 모든 필드가 `final`이고 생성자 주입을 사용하는가
- [ ] Service가 `ResponseEntity`를 반환하지 않는가
- [ ] Service가 Entity 또는 primitive/DTO를 반환하는가
- [ ] 컨트롤러에 Repository import가 남아 있지 않은가 (다른 도메인 Repository 포함)
- [ ] 컨트롤러의 `@ExceptionHandler`가 그대로 유지되어 있는가
- [ ] 기존 주석이 변경/삭제되지 않았는가
- [ ] Entity, Repository, DTO, Validator 파일이 변경되지 않았는가
- [ ] 테스트 코드가 변경되지 않았는가

## 도메인별 참고 패턴

### Category (Pure CRUD)
- 가장 단순한 패턴. findAll, save, findById+update, deleteById.
- `orElse(null) + if null → notFound` 패턴 → `orElseThrow()` 변환.

### Member (Register/Login + Admin CRUD)
- `MemberController`: register(중복체크 + save + JWT), login(조회 + 비밀번호 검증 + JWT)
- `AdminMemberController`: 폼 기반 CRUD + chargePoint
- JWT 생성(`jwtProvider.createToken`)은 인증 관심사이므로 **컨트롤러에 유지**하거나 Service에서 토큰까지 반환 — 기존 코드 구조를 최소한으로 변경하는 방향을 선택한다.

### Product (CRUD + Validator + Cross-Repository)
- `ProductController`: REST CRUD + `ProductNameValidator` 호출 + `CategoryRepository` 조회
- `AdminProductController`: 폼 기반 + 동일 Validator + 동일 CategoryRepository
- 하나의 `ProductService`로 공유. Validator 호출은 REST 컨트롤러에서는 Service에, Admin에서는 컨트롤러에 유지.

### Option (Nested Resource + Constraints)
- 상품 존재 확인 → 옵션 CRUD
- 중복 옵션명 검사, 최소 1개 옵션 제약
- `OptionNameValidator` 호출

### Wish (Auth + Duplicate + Ownership)
- 인증 체크 → 상품 존재 확인 → 중복 위시 확인 → 소유권 검사
- 인증 체크 + 401 반환은 컨트롤러에 유지, 나머지 비즈니스 로직은 Service로.

### Order (Complex Flow)
- 가장 복잡한 플로우: 인증 → 옵션 검증 → 재고 차감 → 포인트 차감 → 주문 저장 → 카카오 알림
- 인증 체크 + 401은 컨트롤러에 유지
- 나머지 전체 플로우를 Service의 단일 `@Transactional` 메서드로 묶는다
- `sendKakaoMessageIfPossible`은 Service로 이동 (외부 연동)

### KakaoAuth (OAuth Flow + External API)
- `login()`: Kakao 인증 URL 빌드 — 단순 URL 조합이므로 컨트롤러에 유지 가능
- `callback()`: 토큰 교환 → 사용자 정보 조회 → 회원 자동등록/갱신 → JWT 발급
- 외부 API 연동 + DB 저장은 Service로 이동. JWT 발급은 Service 또는 컨트롤러 판단.

## 파일 수정 범위 제약 (CRITICAL)

### 수정 가능
- `*Controller.java` — 리팩토링 (Repository 의존 → Service 의존)
- `*Service.java` — 신규 생성

### 절대 수정 금지
- Entity (`*.java` 도메인 모델)
- Repository (`*Repository.java`)
- DTO (`*Request.java`, `*Response.java`)
- Validator (`*Validator.java`)
- 인증 관련 (`JwtProvider`, `AuthenticationResolver`, `KakaoLoginClient`, `KakaoLoginProperties` 등)
- 테스트 코드 (`src/test/**/*`)
- 빌드 설정 (`build.gradle`, `settings.gradle`)
- 설정 파일 (`application*.properties`, `application*.yml`)

## 금지 사항
- 새 기능 추가
- 테스트 코드 수정
- 주석 추가/수정/삭제
- 외부에서 관찰 가능한 동작 변경
- 여러 도메인 동시 처리
- `ResponseEntity`를 Service에서 반환
- Entity, Repository, DTO, Validator 파일 수정

## 사용 예시

```text
/service-layer-extractor
```

```text
/service-layer-extractor Category
```

```text
/service-layer-extractor OrderController
```
