package com.cogent.loginservice.requestDTO;

import lombok.*;

import java.io.Serializable;

/**
 * @author smriti on 6/25/19
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDTO implements Serializable {
    private String username;
    private String emailAddress;
    private String roles;
    private String name;
    private String lastname;
}
