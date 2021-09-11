package org.jpwh.model.helloworld;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

/* 
    Каждый постоянный класс сущности должен иметь как минимум аннотацию @Entity.
    Hibernate сопоставляет этот класс с таблицей с именем MESSAGE.
 */
@Entity
public class Message {

    /* 
        Каждый постоянный класс сущности должен иметь атрибут идентификатора, помеченный @Id.
        Hibernate сопоставляет этот атрибут со столбцом с именем ID.
     */
    @Id
    /* 
        Кто-то должен генерировать значения идентификаторов;
        эта аннотация позволяет автоматическую генерацию идентификаторов.
     */
    @GeneratedValue
    private Long id;

    /* 
        Обычные атрибуты постоянного класса обычно реализуются с помощью приватных или защищенных полей
        и пар методов getter/setter.
        Hibernate сопоставляет этот атрибут со столбцом под названием TEXT.
     */
    private String text;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
