package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired EntityManager em;
    @Autowired MemberJpaRepository memberJpaRepository;

    @Test
    public void basicTest() throws Exception {
        Member member1 = new Member("member1", 10);
        memberJpaRepository.save(member1);
        Optional<Member> memberOptional = memberJpaRepository.findById(member1.getId());
        Member findMember = memberOptional.get();

        assertThat(findMember).isEqualTo(member1);

        List<Member> result1 = memberJpaRepository.findAll();
        assertThat(result1).containsExactly(member1);

        List<Member> result2 = memberJpaRepository.findByUsername("member1");
        assertThat(result2).containsExactly(member1);
    }

    @Test
    public void basicQuerydslTest() throws Exception {
        Member member1 = new Member("member1", 10);
        memberJpaRepository.save(member1);
        Optional<Member> memberOptional = memberJpaRepository.findById(member1.getId());
        Member findMember = memberOptional.get();

        assertThat(findMember).isEqualTo(member1);

        List<Member> result1 = memberJpaRepository.findAll_Querydsl();
        assertThat(result1).containsExactly(member1);

        List<Member> result2 = memberJpaRepository.findByUsername_Querydsl("member1");
        assertThat(result2).containsExactly(member1);
    }

    @Test
    public void searchTestBuilder() throws Exception {
        // given
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

        // 가급적이면 이런 동적쿼리는 페이징 쿼리가 있는것이 좋다.
        // 조건절이 없다면 모든 데이터를 끌어오게 된다.
        // 데이터가 3만건이 존재한다면 3만건을 끌어오게 되어 문제가 발생함!
        MemberSearchCondition searchCondition = new MemberSearchCondition();
        searchCondition.setAgeGoe(35);
        searchCondition.setAgeLoe(40);
        searchCondition.setTeamName("teamB");

        // when
        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(searchCondition);

        // then
        assertThat(result)
                .extracting("username")
                .containsExactly("member4");
    }

    @Test
    public void searchTestWhere() throws Exception {
        // given
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

        MemberSearchCondition searchCondition = new MemberSearchCondition();
        searchCondition.setAgeGoe(35);
        searchCondition.setAgeLoe(40);
        searchCondition.setTeamName("teamB");

        // when
        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(searchCondition);

        // then
        assertThat(result)
                .extracting("username")
                .containsExactly("member4");
    }
}