package gift;

import io.restassured.http.ContentType;
import gift.option.Option;
import gift.product.Product;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

class ProductAcceptanceTest extends AcceptanceTestFixture {

    // --- GET /api/products ---

    @Test
    void 상품_목록_페이징_조회_성공() {
        // given
        Long categoryId = createCategory("전자기기");
        Long productId1 = createProduct("상품A", 1000, "http://img.test/a.png", categoryId);
        Long productId2 = createProduct("상품B", 2000, "http://img.test/b.png", categoryId);

        // when
        var response = given()
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/products");

        // then
        response.then()
            .statusCode(200)
            .body("content.size()", is(2))
            .body("content.id", hasItems(productId1.intValue(), productId2.intValue()))
            .body("content.name", hasItems("상품A", "상품B"))
            .body("totalElements", is(2));
    }

    @Test
    void 상품_목록_카테고리_정보_포함() {
        // given
        Long categoryId = createCategory("전자기기");
        createProduct("상품A", 1000, "http://img.test/a.png", categoryId);

        // when
        var response = given()
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/products");

        // then
        response.then()
            .statusCode(200)
            .body("content[0].categoryId", equalTo(categoryId.intValue()));
    }

    @Test
    void 상품_목록_빈_페이지_조회() {
        // given
        // 데이터 없음

        // when
        var response = given()
            .param("page", 0)
            .param("size", 10)
        .when()
            .get("/api/products");

        // then
        response.then()
            .statusCode(200)
            .body("content.size()", is(0))
            .body("totalElements", is(0));
    }

    // --- GET /api/products/{id} ---

    @Test
    void 상품_상세_조회_성공() {
        // given
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("노트북", 1500000, "http://img.test/laptop.png", categoryId);

        // when
        var response = given()
        .when()
            .get("/api/products/" + productId);

        // then
        response.then()
            .statusCode(200)
            .body("id", equalTo(productId.intValue()))
            .body("name", equalTo("노트북"))
            .body("price", equalTo(1500000))
            .body("imageUrl", equalTo("http://img.test/laptop.png"))
            .body("categoryId", equalTo(categoryId.intValue()));
    }

    @Test
    void 상품_상세_조회_실패_존재하지_않는_ID() {
        // given
        // 존재하지 않는 ID

        // when
        var response = given()
        .when()
            .get("/api/products/" + nonExistingId());

        // then
        response.then()
            .statusCode(404);
    }

    // --- POST /api/products ---

    @Test
    void 상품_생성_성공() {
        // given
        Long categoryId = createCategory("전자기기");
        var request = Map.of(
            "name", "노트북",
            "price", 1500000,
            "imageUrl", "http://img.test/laptop.png",
            "categoryId", categoryId
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/products");

        // then
        response.then()
            .statusCode(201)
            .body("id", greaterThan(0))
            .body("name", equalTo("노트북"))
            .body("price", equalTo(1500000))
            .body("imageUrl", equalTo("http://img.test/laptop.png"))
            .body("categoryId", equalTo(categoryId.intValue()));

        assertThat(productRepository.count()).isEqualTo(1);
    }

    @Test
    void 상품_생성_실패_이름_누락() {
        // given
        Long categoryId = createCategory("전자기기");
        var request = Map.of(
            "price", 1000,
            "imageUrl", "http://img.test/1.png",
            "categoryId", categoryId
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/products");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 상품_생성_실패_가격_0이하() {
        // given
        Long categoryId = createCategory("전자기기");
        var request = Map.of(
            "name", "상품",
            "price", 0,
            "imageUrl", "http://img.test/1.png",
            "categoryId", categoryId
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/products");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 상품_생성_실패_카테고리ID_누락() {
        // given
        var request = Map.of(
            "name", "상품",
            "price", 1000,
            "imageUrl", "http://img.test/1.png"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/products");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 상품_생성_실패_존재하지_않는_카테고리() {
        // given
        var request = Map.of(
            "name", "상품",
            "price", 1000,
            "imageUrl", "http://img.test/1.png",
            "categoryId", 999
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/products");

        // then
        response.then()
            .statusCode(404);
    }

    @Test
    void 상품_생성_실패_이름_15자_초과() {
        // given
        Long categoryId = createCategory("전자기기");
        var request = Map.of(
            "name", "가나다라마바사아자차카타파하일이",
            "price", 1000,
            "imageUrl", "http://img.test/1.png",
            "categoryId", categoryId
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/products");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 상품_생성_실패_카카오_포함() {
        // given
        Long categoryId = createCategory("전자기기");
        var request = Map.of(
            "name", "카카오선물",
            "price", 1000,
            "imageUrl", "http://img.test/1.png",
            "categoryId", categoryId
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/products");

        // then
        response.then()
            .statusCode(400);
    }

    // --- PUT /api/products/{id} ---

    @Test
    void 상품_수정_성공() {
        // given
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("수정전", 1000, "http://img.test/old.png", categoryId);
        var request = Map.of(
            "name", "수정후",
            "price", 2000,
            "imageUrl", "http://img.test/new.png",
            "categoryId", categoryId
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .put("/api/products/" + productId);

        // then
        response.then()
            .statusCode(200)
            .body("id", equalTo(productId.intValue()))
            .body("name", equalTo("수정후"))
            .body("price", equalTo(2000))
            .body("imageUrl", equalTo("http://img.test/new.png"));
    }

    @Test
    void 상품_수정_실패_존재하지_않는_ID() {
        // given
        Long categoryId = createCategory("전자기기");
        var request = Map.of(
            "name", "수정후",
            "price", 2000,
            "imageUrl", "http://img.test/new.png",
            "categoryId", categoryId
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .put("/api/products/" + nonExistingId());

        // then
        response.then()
            .statusCode(404);
    }

    // --- DELETE /api/products/{id} ---

    @Test
    void 상품_삭제_성공_옵션도_함께_삭제() {
        // given
        Long categoryId = createCategory("전자기기");
        Long productId = createProduct("삭제대상", 1000, "http://img.test/del.png", categoryId);

        // Option을 Repository로 생성 (Product 엔티티 필요)
        Product product = productRepository.findById(productId).orElseThrow();
        Option option = optionRepository.save(new Option(product, "기본옵션", 10));
        Long optionId = option.getId();

        // when
        var response = given()
        .when()
            .delete("/api/products/" + productId);

        // then
        response.then()
            .statusCode(204);

        assertThat(productRepository.findById(productId)).isEmpty();
        assertThat(optionRepository.findById(optionId)).isEmpty();
    }
}
