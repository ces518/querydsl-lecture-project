package study.querydsl.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/**
 * Created by IntelliJ IDEA.
 * User: june
 * Date: 2020-03-12
 * Time: 15:57
 **/
@Entity
@Getter @Setter
public class Hello {

    @Id @GeneratedValue
    private Long id;
}
