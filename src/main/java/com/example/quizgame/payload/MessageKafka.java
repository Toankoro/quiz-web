package com.example.quizgame.payload;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Data
public class MessageKafka {
    private int id;
    private String firstName;
    private String lastName;

    @Override
    public String toString() {
        return "MessageKafka{" +
                "id=" + id +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                '}';
    }
}
