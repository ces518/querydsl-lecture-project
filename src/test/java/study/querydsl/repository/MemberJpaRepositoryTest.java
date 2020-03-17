package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;

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
}