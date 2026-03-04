# Validation Checklist

검증 서브에이전트가 생성된 테스트 코드를 판정할 때 사용하는 체크리스트.
각 항목의 severity가 `MUST`인 경우 위반 시 `pass=false`, `SHOULD`인 경우에도 위반 시 `pass=false`.

---

## STR — 클래스 구조 (6)

| ID | Severity | 항목 |
|----|----------|------|
| STR-1 | MUST | `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` 어노테이션이 테스트 클래스에 존재한다 |
| STR-2 | MUST | `@LocalServerPort int port;` 필드가 선언되어 있다 |
| STR-3 | MUST | `@BeforeEach` 메서드에서 `RestAssured.port = port;`를 설정한다 |
| STR-4 | MUST | `@BeforeEach` 메서드에서 FK 역순으로 `deleteAll()`을 호출하여 테스트 격리를 보장한다 |
| STR-5 | MUST | 패키지 선언이 `package gift;`이다 |
| STR-6 | SHOULD | 테스트 클래스명이 `XxxAcceptanceTest` 패턴을 따른다 (예: `CategoryAcceptanceTest`) |

## NAM — 명명 규칙 (2)

| ID | Severity | 항목 |
|----|----------|------|
| NAM-1 | MUST | 테스트 메서드명이 한글이며 `대상_상황_기대결과` 형식을 따른다 |
| NAM-2 | SHOULD | 메서드명에 영어가 섞이지 않는다 (예외: HTTP 메서드명, 상태코드 등 기술 용어) |

## GWT — given-when-then 구조 (2)

| ID | Severity | 항목 |
|----|----------|------|
| GWT-1 | MUST | 모든 `@Test` 메서드에 `// given`, `// when`, `// then` 주석이 존재한다 |
| GWT-2 | SHOULD | given → when → then 순서가 지켜진다 (역순이나 뒤섞임 없음) |

## VAL — 검증 계층 (3)

| ID | Severity | 항목 |
|----|----------|------|
| VAL-1 | MUST | 모든 테스트가 Layer 1 (HTTP 상태 코드) 검증을 포함한다 — `.statusCode(...)` |
| VAL-2 | SHOULD | 응답 본문이 있는 API는 Layer 2 (응답 필드/값) 검증을 포함한다 — `.body(...)` |
| VAL-3 | SHOULD | 상태 변경 API(POST/PUT/PATCH/DELETE)는 Layer 3 (DB 상태 변화) 검증을 고려한다 |

## DAT — 데이터 생성 전략 (4)

| ID | Severity | 항목 |
|----|----------|------|
| DAT-1 | MUST | API가 있는 엔티티(Category, Product 등)는 API 호출(Black-box)로 생성한다 |
| DAT-2 | MUST | API가 없는 엔티티(Member, Option 등)는 Repository(White-box)로 생성한다 |
| DAT-3 | SHOULD | 데이터 생성 로직이 헬퍼 메서드로 추출되어 있다 |
| DAT-4 | MUST | Option 생성 시 `productRepository.findById()`로 Product 엔티티를 조회하여 전달한다 |

## RA — RestAssured 패턴 (3)

| ID | Severity | 항목 |
|----|----------|------|
| RA-1 | MUST | `given().contentType(ContentType.JSON).body(request).when().post(...)` 패턴을 따른다 |
| RA-2 | MUST | 응답 검증은 `.then().statusCode(...)` 체이닝으로 수행한다 |
| RA-3 | SHOULD | 헬퍼 메서드에서 `.extract().jsonPath().getLong("id")` 패턴으로 ID를 추출한다 |

## PATH — 경로 제약 (2)

| ID | Severity | 항목 |
|----|----------|------|
| PATH-1 | MUST | 테스트 파일이 `src/test/` 경로 하위에만 생성/수정되었다 |
| PATH-2 | MUST | `src/main/` 하위 파일이 수정되지 않았다 |

## BAN — 금지 사항 (3)

| ID | Severity | 항목 |
|----|----------|------|
| BAN-1 | MUST | `@Transactional`이 테스트 클래스/메서드에 사용되지 않았다 |
| BAN-2 | MUST | 프로덕션 로직을 테스트 코드에 복제하지 않았다 |
| BAN-3 | SHOULD | 한 번에 하나의 API에 대한 테스트만 작성했다 (여러 API 동시 작성 금지) |

## FAIL — 실패 대응 (2)

| ID | Severity | 항목 |
|----|----------|------|
| FAIL-1 | MUST | 프로덕션 버그 발견 시 프로덕션 코드를 수정하지 않고, 테스트 코드에 기록만 한다 |
| FAIL-2 | SHOULD | 프로덕션 버그로 인한 테스트는 `@Disabled("BUG: ...")` 또는 현재 동작 기준으로 통과시키고 주석을 남긴다 |

## IMP — 임포트 (4)

| ID | Severity | 항목 |
|----|----------|------|
| IMP-1 | MUST | `import static io.restassured.RestAssured.given;`이 존재한다 |
| IMP-2 | MUST | `import static org.hamcrest.Matchers.*;`이 존재한다 |
| IMP-3 | SHOULD | `import static org.assertj.core.api.Assertions.assertThat;`이 존재한다 (Layer 3 검증 시) |
| IMP-4 | MUST | `import io.restassured.http.ContentType;`이 존재한다 |

## BUILD — 빌드 (1)

| ID | Severity | 항목 |
|----|----------|------|
| BUILD-1 | MUST | `./gradlew test`가 성공한다 (테스트 작성 후 최종 빌드 통과) |
