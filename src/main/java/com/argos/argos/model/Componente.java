package com.argos.argos.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Componente {
    private Long idComponente;
    private String nomeIdentificador;
    private Double limiarAviso;
    private Double limiarCritico;
}
