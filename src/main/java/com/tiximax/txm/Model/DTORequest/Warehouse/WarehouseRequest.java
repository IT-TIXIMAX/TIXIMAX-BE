package com.tiximax.txm.Model.DTORequest.Warehouse;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter

public class WarehouseRequest {

        private Double length;

        private Double width;

        private Double height;

        private Double weight;

        private String image;

        private String imageCheck;
}