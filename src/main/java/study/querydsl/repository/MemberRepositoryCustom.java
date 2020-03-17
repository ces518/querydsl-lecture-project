package study.querydsl.repository;

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
}
