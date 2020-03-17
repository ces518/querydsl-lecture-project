package study.querydsl.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import study.querydsl.entity.Member;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: june
 * Date: 2020-03-17
 * Time: 21:08
 **/
public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {
    // 메소드 명으로 쿼리생
    List<Member> findByUsername(String username);
}
