package study.querydsl.dto;

import lombok.Data;

/**
 * Created by IntelliJ IDEA.
 * User: june
 * Date: 2020-03-17
 * Time: 18:21
 **/
@Data
public class MemberSearchCondition {
    // 회원명, 팀명, 나이(Goe, Loe)

    private String username;
    private String teamName;
    private Integer ageGoe;
    private Integer ageLoe;
}
