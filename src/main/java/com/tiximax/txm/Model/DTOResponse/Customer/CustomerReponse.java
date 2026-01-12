package com.tiximax.txm.Model.DTOResponse.Customer;

import com.tiximax.txm.Entity.Customer;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class CustomerReponse extends Customer {
    String token;
}