package study.querydsl.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import study.querydsl.dto.MemberSearchCondition;
import study.querydsl.dto.MemberTeamDto;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: june
 * Date: 2020-03-17
 * Time: 22:05
 **/
public interface MemberRepositoryCustom {

    List<MemberTeamDto> search(MemberSearchCondition condition);

    Page<MemberTeamDto> searchPageSimple(MemberSearchCondition condition, Pageable pageable);

    // 카운트 쿼리를 별도의 쿼리로 사용
    Page<MemberTeamDto> searchPageComplex(MemberSearchCondition condition, Pageable pageable);

    // 카운트 쿼리 최적화
    Page<MemberTeamDto> searchPageComplexOptimization(MemberSearchCondition searchCondition, Pageable pageable);
}
