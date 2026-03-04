package gift;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import gift.category.CategoryRepository;
import gift.member.Member;
import gift.member.MemberRepository;
import gift.option.OptionRepository;
import gift.order.OrderRepository;
import gift.product.ProductRepository;
import gift.wish.WishRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import java.util.Map;

import static io.restassured.RestAssured.given;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AcceptanceTestFixture {

    @LocalServerPort
    int port;

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    CategoryRepository categoryRepository;

    @Autowired
    ProductRepository productRepository;

    @Autowired
    OptionRepository optionRepository;

    @Autowired
    WishRepository wishRepository;

    @Autowired
    OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        // FK 역순으로 삭제
        orderRepository.deleteAll();
        wishRepository.deleteAll();
        optionRepository.deleteAll();
        productRepository.deleteAll();
        categoryRepository.deleteAll();
        memberRepository.deleteAll();
    }

    Long createCategory(String name) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", name, "color", "#000000", "imageUrl", "http://img.test/cat.png"))
        .when()
            .post("/api/categories")
        .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");
    }

    Long createProduct(String name, int price, String imageUrl, Long categoryId) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", name, "price", price, "imageUrl", imageUrl, "categoryId", categoryId))
        .when()
            .post("/api/products")
        .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");
    }

    Long createOption(Long productId, String name, int quantity) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("name", name, "quantity", quantity))
        .when()
            .post("/api/products/" + productId + "/options")
        .then()
            .statusCode(201)
            .extract().jsonPath().getLong("id");
    }

    String registerAndGetToken(String email, String password) {
        return given()
            .contentType(ContentType.JSON)
            .body(Map.of("email", email, "password", password))
        .when()
            .post("/api/members/register")
        .then()
            .statusCode(201)
            .extract().jsonPath().getString("token");
    }

    Long nonExistingId() {
        return -1L;
    }

    // 포인트 조회/충전 API가 없어 주문 테스트에서만 예외적으로 Repository를 사용한다.
    void chargeMemberPoints(String email, int amount) {
        Member member = memberRepository.findByEmail(email).orElseThrow();
        member.chargePoint(amount);
        memberRepository.save(member);
    }

    int getMemberPoint(String email) {
        return memberRepository.findByEmail(email).orElseThrow().getPoint();
    }
}
