package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.*;

/**
 * Created by IntelliJ IDEA.
 * User: june
 * Date: 2020-03-13
 * Time: 15:54
 **/
@SpringBootTest
@Transactional
public class QuerydslBasicTest {

    @Autowired
    EntityManager em;

//    멀티스레드에 문제없도록 설계되어 있음.
//    EntityManager가 스프링에서 스레드 세이프하게 동작한다.
    JPAQueryFactory queryFactory;

    /**
     * @BeforeEach 각 테스트 실행전에 실행되는 로직
     * 테스트 전처리 작업시 사용한다.
     */
    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);

        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }


    @Test
    public void startJPQL() {
        // member1 조회
        // 런타임에 오류 체크
        Member findMember = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {
//        JPAQueryFactory queryFactory = new JPAQueryFactory(em);
//        QMember m = new QMember("m"); // QMember를 구분하는 이름 (Alias) 크게 중요하진 않음
        QMember m = member;

        // JDBC의 PrepareStatement로 자동으로 파라메터 바인딩을 해준다.
        // > SQL Injection도 방지됨.
        // 컴파일 시점에 오류 체크
        Member findMember = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        Member findMember = queryFactory.
                selectFrom(member)
                .where(member.username.eq("member1")
                        .and(member.age.eq(10)))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
    }

    @Test
    public void searchAndParam() {
        Member findMember = queryFactory.
                selectFrom(member)
                .where(
                        member.username.eq("member1"),
                        member.age.eq(10) // and 조건의 경우 파라메터를 ,로 구분하여 넘길경우 and 조건이 적용
                        // null이 들어가게 되면 조건절에서 무시된다.
                        // 동적쿼리 작성시 매우 깔끔하게 적용됨
                )
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
        assertThat(findMember.getAge()).isEqualTo(10);
    }

    @Test
    public void resultFetch() {
        // Member의 목록을 리스트로 조회
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        // 단건 조회
        Member fetchOne = queryFactory
                .selectFrom(QMember.member)
                .fetchOne();

        // fetchFirst
        Member fetchFirst = queryFactory
                .selectFrom(QMember.member)
                //.limit(1).fetchOne() 과 동일
                .fetchFirst();

        // fetchResults()
        // 쿼리가 2번 실행된다.
        QueryResults<Member> results = queryFactory
                .selectFrom(QMember.member)
                .fetchResults();
        long totalCount = results.getTotal();// totalCount
        List<Member> content = results.getResults(); // 실제 데이터
        long limit = results.getLimit(); // limit
        long offset = results.getOffset(); // offset

        // 실무에서는 실제 데이터를 가져오는 쿼리와, totalCount쿼리가 다른 케이스가 있음
        // 따라서 성능 문제가 발생할 수 있음
        // 정말 실시간 데이터가 중요한 페이징 API의 경우에는 fetchResults()를 사용해선 안된다.


        // fetchCount()
        // select절을 count로 변경해서 날림
        // JPQL에서 엔티티를 직접 지정하면 식별자로 변경되어 실행된다.
        // 실제 SQL에서는 member_id 로 지정됨
        long fetchCount = queryFactory
                .selectFrom(member)
                .fetchCount();

    }
}







