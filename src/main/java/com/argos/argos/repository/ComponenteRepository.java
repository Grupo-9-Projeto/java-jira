package com.argos.argos.repository;

import com.argos.argos.model.Componente;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ComponenteRepository {

    private final JdbcTemplate jdbcTemplate;

    public ComponenteRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Componente buscarLimitesPorId(Long idComponente) {
        String sql = "SELECT id_componente, nome_identificador, limiar_aviso, limiar_critico " +
                "FROM componente WHERE id_componente = ?";

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
            return new Componente(
                    rs.getLong("id_componente"),
                    rs.getString("nome_identificador"),
                    rs.getDouble("limiar_aviso"),
                    rs.getDouble("limiar_critico")
            );
        }, idComponente);
    }
}
