package study.querydsl;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Hello;
import study.querydsl.entity.QHello;

import javax.persistence.EntityManager;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class QuerydslApplicationTests {

    @Autowired EntityManager em;

    @Test
    void contextLoads() {
        Hello hello = new Hello();
        em.persist(hello);

        // 최신버전에서는 JPAQueryFactory를 사용할것을 권장
        JPAQueryFactory query = new JPAQueryFactory(em);
        QHello qHello = new QHello("h");

        // Querydsl 사용시 쿼리와 관련된 것은 QType을 사용
        Hello result = query
                .selectFrom(qHello)
                .fetchOne();

        // 조회한 엔티티가 동일한 엔티티인지 검증
        assertThat(result).isEqualTo(hello);
        assertThat(result.getId()).isEqualTo(hello.getId());
    }

}
