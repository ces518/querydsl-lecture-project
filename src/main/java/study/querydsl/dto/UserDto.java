package study.querydsl.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Created by IntelliJ IDEA.
 * User: june
 * Date: 2020-03-15
 * Time: 12:52
 **/
@Data
@NoArgsConstructor
public class UserDto {

    private String name;
    private int age;

    public UserDto(String name, int age) {
        this.name = name;
        this.age = age;
    }
}
