package gift;

import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

class CategoryAcceptanceTest extends AcceptanceTestFixture {

    // --- GET /api/categories ---

    @Test
    void 카테고리_목록_조회_성공() {
        // given
        Long id1 = createCategory("전자기기");
        Long id2 = createCategory("식품");

        // when
        var response = given()
        .when()
            .get("/api/categories");

        // then
        response.then()
            .statusCode(200)
            .body("size()", is(2))
            .body("id", hasItems(id1.intValue(), id2.intValue()))
            .body("name", hasItems("전자기기", "식품"));
    }

    @Test
    void 카테고리_목록_조회_빈_목록() {
        // given
        // 데이터 없음

        // when
        var response = given()
        .when()
            .get("/api/categories");

        // then
        response.then()
            .statusCode(200)
            .body("size()", is(0));
    }

    // --- POST /api/categories ---

    @Test
    void 카테고리_생성_성공() {
        // given
        var request = Map.of(
            "name", "전자기기",
            "color", "#FF0000",
            "imageUrl", "http://img.test/electronics.png"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/categories");

        // then
        response.then()
            .statusCode(201)
            .body("id", greaterThan(0))
            .body("name", equalTo("전자기기"))
            .body("color", equalTo("#FF0000"))
            .body("imageUrl", equalTo("http://img.test/electronics.png"));

        assertThat(categoryRepository.count()).isEqualTo(1);
    }

    @Test
    void 카테고리_생성_실패_이름_누락() {
        // given
        var request = Map.of(
            "color", "#FF0000",
            "imageUrl", "http://img.test/1.png"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/categories");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 카테고리_생성_실패_색상_누락() {
        // given
        var request = Map.of(
            "name", "전자기기",
            "imageUrl", "http://img.test/1.png"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/categories");

        // then
        response.then()
            .statusCode(400);
    }

    @Test
    void 카테고리_생성_실패_이미지URL_누락() {
        // given
        var request = Map.of(
            "name", "전자기기",
            "color", "#FF0000"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .post("/api/categories");

        // then
        response.then()
            .statusCode(400);
    }

    // --- PUT /api/categories/{id} ---

    @Test
    void 카테고리_수정_성공() {
        // given
        Long id = createCategory("수정전");
        var request = Map.of(
            "name", "수정후",
            "color", "#00FF00",
            "imageUrl", "http://img.test/updated.png"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .put("/api/categories/" + id);

        // then
        response.then()
            .statusCode(200)
            .body("id", equalTo(id.intValue()))
            .body("name", equalTo("수정후"))
            .body("color", equalTo("#00FF00"))
            .body("imageUrl", equalTo("http://img.test/updated.png"));
    }

    @Test
    void 카테고리_수정_실패_존재하지_않는_ID() {
        // given
        var request = Map.of(
            "name", "수정후",
            "color", "#00FF00",
            "imageUrl", "http://img.test/updated.png"
        );

        // when
        var response = given()
            .contentType(ContentType.JSON)
            .body(request)
        .when()
            .put("/api/categories/" + nonExistingId());

        // then
        response.then()
            .statusCode(404);
    }

    // --- DELETE /api/categories/{id} ---

    @Test
    void 카테고리_삭제_성공() {
        // given
        Long id = createCategory("삭제대상");

        // when
        var response = given()
        .when()
            .delete("/api/categories/" + id);

        // then
        response.then()
            .statusCode(204);

        assertThat(categoryRepository.findById(id)).isEmpty();
    }
}
