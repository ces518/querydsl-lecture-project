package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.*;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.*;
import static study.querydsl.entity.QTeam.team;

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

    /**
     * 회원 정렬 순서
     * 1. 나이 내림차순
     * 2. 이름 오림차순
     * 단 2에서 회원 이름이 없으면 마지막에 출력 (nulls last)
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> members = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(),
                        member.username.asc().nullsLast())
                .fetch();
        Member member5 = members.get(0);
        Member member6 = members.get(1);
        Member memberNull = members.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging() {
        // 데이터베이스 방언에 따라 다르게 실행된다.
        List<Member> members = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(members.size()).isEqualTo(2);
    }

    @Test
    public void paging2() {
        // totalCount가 필요할 경우 fetchResults() 사용
        // 실무에서는 count쿼리와 페이징 쿼리가 다를경우가 있음
        // 그런경우에는 fetchResults() 보다는 count쿼리를 분리할것
        QueryResults<Member> results = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();
        long total = results.getTotal();
        List<Member> members = results.getResults();

        assertThat(total).isEqualTo(4);
        assertThat(results.getLimit()).isEqualTo(2);
        assertThat(results.getOffset()).isEqualTo(1);
        assertThat(members.size()).isEqualTo(2);
    }

    @Test
    public void aggregation() {
        // 집합 함수를 사용하면 Tuple 타입으로 나온다. * 여러개의 타입이 있을때 꺼내올 수 있음.
        // Querydsl의 Tuple 타입이다.
        // 실무에서는 Tuple보다는 DTO를 많이 사용한다.
        List<Tuple> result = queryFactory
                .select(
                        member.count(), // count
                        member.age.sum(), // sum
                        member.age.max(), // max
                        member.age.min() // min
                )
                .from(member)
                .fetch();

        // Tuple에서 값을 꺼내올때는 select절에서 사용한 표현식을 그대로 사용하면 된다.
        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                // select 절에 사용하는것은 QType이어야 한다.
                .select(team.name, member.age.avg()) // 팀의 이름과 팀의 평균연령
                .from(member)
                .join(member.team, team) // member.team과 팀을 조인
                .groupBy(team.name) // 팀 이름으로 그룹핑
//                .having() having 기능도 제공
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }
}







