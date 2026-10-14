package com.argos.argos.model;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Componente {
    private Integer idComponente;
    private String nomeIdentificador;
    private String tipo;
    private String unidade;
    private Double capacidade;
    private Double limiarAtencao;
    private Double limiarAlto;
    private Double limiarCritico;
}
