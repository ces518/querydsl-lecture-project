package study.querydsl.repository;

import com.querydsl.core.QueryResults;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.dto.QMemberTeamDto;
import study.querydsl.entity.Member;

import javax.persistence.EntityManager;
import java.util.List;

import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

/**
 * Created by IntelliJ IDEA.
 * User: june
 * Date: 2020-03-17
 * Time: 22:06
 **/
// +Impl 이라는 접미사를 사용해주어야 한다.
public class MemberRepositoryImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(EntityManager em) {
        this.queryFactory = new JPAQueryFactory(em);
    }

    @Override
    public List<MemberTeamDto> search(MemberSearchCondition searchCondition) {
        return queryFactory
                .select(new QMemberTeamDto(member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(searchCondition.getUsername()),
                        teamNameEq(searchCondition.getTeamName()),
                        ageGoe(searchCondition.getAgeGoe()),
                        ageLoe(searchCondition.getAgeLoe())
                )
                .fetch();
    }

    @Override
    public Page<MemberTeamDto> searchPageSimple(MemberSearchCondition searchCondition, Pageable pageable) {
        QueryResults<MemberTeamDto> results = queryFactory
                .select(new QMemberTeamDto(member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(searchCondition.getUsername()),
                        teamNameEq(searchCondition.getTeamName()),
                        ageGoe(searchCondition.getAgeGoe()),
                        ageLoe(searchCondition.getAgeLoe())
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetchResults();

        // fetchResults() 를 사용하면, 컨텐츠 쿼리와 카운트 쿼리가 나간다.
        // orderBy는 카운트쿼리에서 제거됨
        // -> 단점: 카운트 쿼리 최적화를 못한다.
        List<MemberTeamDto> content = results.getResults();
        long totalCount = results.getTotal();

        return new PageImpl<>(content, pageable, totalCount);
    }

    /**
     * 쿼리 자체를 분리하는 방법
     * @param searchCondition
     * @param pageable
     * @return
     */
    @Override
    public Page<MemberTeamDto> searchPageComplex(MemberSearchCondition searchCondition, Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
                .select(new QMemberTeamDto(member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(searchCondition.getUsername()),
                        teamNameEq(searchCondition.getTeamName()),
                        ageGoe(searchCondition.getAgeGoe()),
                        ageLoe(searchCondition.getAgeLoe())
                )
                .fetch();

        // count 쿼리에서 성능최적화를 할 수있다.
        // 조인수를 줄이는 등..
        long totalCount = queryFactory
                .select(member)
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(searchCondition.getUsername()),
                        teamNameEq(searchCondition.getTeamName()),
                        ageGoe(searchCondition.getAgeGoe()),
                        ageLoe(searchCondition.getAgeLoe())
                )
                .fetchCount();

        return new PageImpl<>(content, pageable, totalCount);
    }

    /**
     * Count 쿼리 최적화
     * @param searchCondition
     * @param pageable
     * @return
     */
    @Override
    public Page<MemberTeamDto> searchPageComplexOptimization(MemberSearchCondition searchCondition, Pageable pageable) {
        List<MemberTeamDto> content = queryFactory
                .select(new QMemberTeamDto(member.id.as("memberId"),
                        member.username,
                        member.age,
                        team.id.as("teamId"),
                        team.name))
                .from(member)
                .leftJoin(member.team, team)
                .where(
                        usernameEq(searchCondition.getUsername()),
                        teamNameEq(searchCondition.getTeamName()),
                        ageGoe(searchCondition.getAgeGoe()),
                        ageLoe(searchCondition.getAgeLoe())
                )
                .fetch();

        JPAQuery<Member> countQuery = queryFactory
                .select(member)
                .from(member)
                .where(
                        usernameEq(searchCondition.getUsername()),
                        teamNameEq(searchCondition.getTeamName()),
                        ageGoe(searchCondition.getAgeGoe()),
                        ageLoe(searchCondition.getAgeLoe())
                );

        return PageableExecutionUtils.getPage(content, pageable,  countQuery::fetchCount);
    }

    private BooleanExpression usernameEq(String username) {
        return StringUtils.isEmpty(username) ? null : member.username.eq(username);
    }

    private BooleanExpression teamNameEq(String teamName) {
        return StringUtils.isEmpty(teamName) ? null : team.name.eq(teamName);
    }

    private BooleanExpression ageGoe(Integer ageGoe) {
        return ageGoe == null ? null : member.age.goe(ageGoe);
    }

    private BooleanExpression ageLoe(Integer ageLoe) {
        return ageLoe == null ? null : member.age.loe(ageLoe);
    }
}
