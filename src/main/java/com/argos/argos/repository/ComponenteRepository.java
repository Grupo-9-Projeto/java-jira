package com.argos.argos.repository;

import com.argos.argos.model.Componente;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ComponenteRepository {

    private final JdbcTemplate jdbcTemplate;

    public ComponenteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Componente> buscarPorMacETipo(String mac, String tipo) {
        String sql = """
                SELECT c.id_componente, c.nome_identificador, t.nome AS tipo, t.unidade_medida, c.capacidade,
                       c.limiar_atencao, c.limiar_alto, c.limiar_critico
                FROM componente c
                JOIN dispositivo d ON d.id_dispositivo = c.dispositivo_id
                JOIN tipo_componente t ON t.id_tipo = c.tipo_id
                WHERE d.endereco_mac = ?
                  AND t.nome = ?
                  AND c.ativo = 1
                """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new Componente(
                rs.getInt("id_componente"),
                rs.getString("nome_identificador"),
                rs.getString("tipo"),
                rs.getString("unidade_medida"),
                rs.getObject("capacidade", Double.class),
                rs.getObject("limiar_atencao", Double.class),
                rs.getObject("limiar_alto", Double.class),
                rs.getObject("limiar_critico", Double.class)
                // Double.class serve para devolver null caso a coluna for NULL
        ), mac, tipo);
    }
}