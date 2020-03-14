package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.*;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
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

    /**
     * TeamA에 소속된 모든 회원을 찾아라.
     */
    @Test
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
//                .leftJoin(member.team, team)
//                .rightJoin(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        // result 결과중 username 프로퍼티의 값을 검증
        // > 조회된 회원의 이름은 member1, member2 인지 검증
        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");
    }


    /**
     * 회원이름이 팀이름과 같은 회원을 조회
     */
    @Test
    public void theta_join() {
        // 세타조인
        // 연관관계가 없어도 조인을 하도록 제공
        // 외부 조인이 불가능하다.
        // > 추후 추가된 조인 on 을 사용하면 외부 조인이 가능하다.
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();


    }

    /**
     * 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: select m, t from Member m left join m.team t on t.name = 'teamA'
     * @throws Exception
     */
    @Test
    public void join_on_filtering() throws Exception {
        // select 절에 여러 타입이 존재하기 때문에 Tuple타입으로 반환됨.
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team).on(team.name.eq("teamA"))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
            /*
            teamA 소속은 정상적인 조인이 되고, teamB 소속은 조인에서 제외되고, leftJoin이기 때문에 멤버는 모두 조회
            tuple = [Member(id=3, username=member1, age=10), Team(id=1, name=teamA)]
            tuple = [Member(id=4, username=member2, age=20), Team(id=1, name=teamA)]
            tuple = [Member(id=5, username=member3, age=30), null]
            tuple = [Member(id=6, username=member4, age=40), null]
            * */
        }
    }


    /**
     * 연관관계가 없는 엔티티를 외부 조인
     * 회원이름이 팀이름과 같은 회원을 조회
     */
    @Test
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
//                .leftJoin(member.team, team) 기존의 조인 문법 (연관관계로 조인을 하면 ID값으로 조인을 함)
                .leftJoin(team).on(member.username.eq(team.name))
                // 연관관계가 없는 조인 (일명 막 조인, ID값이 아닌 ON 절로만 조인)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
            /*
            tuple = [Member(id=3, username=member1, age=10), null]
            tuple = [Member(id=4, username=member2, age=20), null]
            tuple = [Member(id=5, username=member3, age=30), null]
            tuple = [Member(id=6, username=member4, age=40), null]
            tuple = [Member(id=7, username=teamA, age=0), Team(id=1, name=teamA)]
            tuple = [Member(id=8, username=teamB, age=0), Team(id=2, name=teamB)]
            */
        }
    }


    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() {
        em.flush();
        em.clear();

        // 멤버와 팀의 관계는 지연로딩 LAZY이다.
        // 조회시 멤버만 조회되고, 팀은 조회되지 않음
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        // 로딩된 엔티티인지 를 판단
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치조인 미적용").isFalse();
    }

    @Test
    public void fetchJoin() {
        em.flush();
        em.clear();

        // 멤버 조회시 연관된 엔티티인 팀을 같이 가져온다.
        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin() // fetchJoin
                .where(member.username.eq("member1"))
                .fetchOne();

        // 로딩된 엔티티인지 를 판단
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("페치조인 적용").isTrue();
    }

    /**
     * 나이가 가장 많은 회원을 조회
     */
    @Test
    public void subQuery() {
        // Alias가 중복되면 안되는경우 별도로 생성
        QMember memberSub = new QMember("memberSub");
        List<Member> members = queryFactory.selectFrom(member)
                .where(
                        member.age.eq(
                                JPAExpressions // JPAExpressions를 사용하여 서브쿼리 사용
                                        .select(memberSub.age.max())
                                        .from(memberSub)
                        )
                )
                .fetch();

        // 결과 목록에서 age값 검증
        assertThat(members)
                .extracting("age")
                .containsExactly(40);
    }

    /**
     * 나이가 평균 이상인 회원
     */
    @Test
    public void subQueryGoe() {
        QMember memberSub = new QMember("memberSub");
        List<Member> members = queryFactory.selectFrom(member)
                .where(
                        member.age.goe(
                                JPAExpressions // JPAExpressions를 사용하여 서브쿼리 사용
                                        .select(memberSub.age.avg())
                                        .from(memberSub)
                        )
                )
                .fetch();

        // 결과 목록에서 age값 검증
        assertThat(members)
                .extracting("age")
                .containsExactly(30, 40);
    }

    /**
     * 10살보다 많은 회원 조회
     */
    @Test
    public void subQueryIn() {
        QMember memberSub = new QMember("memberSub");
        List<Member> members = queryFactory.selectFrom(member)
                .where(
                        member.age.in(
                                JPAExpressions // JPAExpressions를 사용하여 서브쿼리 사용
                                        .select(memberSub.age)
                                        .from(memberSub)
                                        .where(memberSub.age.gt(10))
                        )
                )
                .fetch();

        // 결과 목록에서 age값 검증
        assertThat(members)
                .extracting("age")
                .containsExactly(20, 30, 40);
    }
    
    @Test
    public void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> members = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub))
                .from(member)
                .fetch();

        for (Tuple tuple : members) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 간단한 조건
     */
    @Test
    public void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("10살")
                        .when(20).then("20살")
                        .otherwise("기타"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
            /*
                s = 10살
                s = 20살
                s = 기타
                s = 기타
             */
        }
    }

    /**
     * 복잡한 조건
     */
    @Test
    public void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20")
                        .when(member.age.between(21, 30)).then("21~30")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String s : result) {
            System.out.println("s = " + s);
            /*
                s = 0~20
                s = 0~20
                s = 21~30
                s = 기타
             */
        }
    }
    
    @Test
    public void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
            /*
                tuple = [member1, A]
                tuple = [member2, A]
                tuple = [member3, A]
                tuple = [member4, A]
             */
        }
    }

    @Test
    public void concat() {
        // concat()을 사용할때 타입이 맞지 않으면 에러가 발생함.
        // stringValue() 메소드를 이용하여 해결이 가능하다.
        // 문자가 아닌 다른 타입을 처리할때 사용한다.
        // enum Type 같은경우 유용하게 사용된다.
        // member1_10
        List<String> result = queryFactory
                .select(member.username.concat("-").concat(member.age.stringValue()))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
            /*
                s = member1-10
                s = member2-20
                s = member3-30
                s = member4-40
             */
        }
    }
}







