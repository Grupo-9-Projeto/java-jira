package com.argos.argos.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Repository
public class AlertaRepository {

    private static final DateTimeFormatter FORMATO_CSV = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final JdbcTemplate jdbcTemplate;

    public AlertaRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean possuiChamadoEmAberto(Integer fkComponente) {
        String sql = """
                SELECT COUNT(*)
                FROM alerta
                WHERE fkComponente = ?
                  AND jira_key IS NOT NULL
                  AND LOWER(TRIM(status_jira)) IN ('a fazer', 'em andamento')
                """;

        Integer total = jdbcTemplate.queryForObject(sql, Integer.class, fkComponente);
        return total != null && total > 0;
    }

    public void salvarAlerta(Integer fkComponente, String nivelAlerta, Double valorRegistrado, String unidadeMedida, String descricao, String quandoColetado, String jiraKey, String statusJira) {
        Timestamp registradoEm = parseTimestamp(quandoColetado);

        String sql = """
                INSERT IGNORE INTO alerta (
                    registrado_em, nivel_alerta, valor_registrado, unidade_medida,
                    descricao, fkComponente, jira_key, status_jira
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        jdbcTemplate.update(
                sql,
                registradoEm,
                nivelAlerta,
                valorRegistrado,
                unidadeMedida,
                descricao,
                fkComponente,
                jiraKey,
                statusJira
        );
    }

    public Integer atualizarStatusPorJiraKey(String jiraKey, String statusJira) {
        String sql = "UPDATE alerta SET status_jira = ? WHERE jira_key = ?";
        return jdbcTemplate.update(sql, statusJira, jiraKey);
    }

    private Timestamp parseTimestamp(String quandoColetado) {
        if (quandoColetado == null || quandoColetado.isBlank()) {
            return new Timestamp(System.currentTimeMillis());
        }
        try {
            return Timestamp.valueOf(LocalDateTime.parse(quandoColetado.trim(), FORMATO_CSV));
        } catch (Exception e) {
            return new Timestamp(System.currentTimeMillis());
        }
    }
}