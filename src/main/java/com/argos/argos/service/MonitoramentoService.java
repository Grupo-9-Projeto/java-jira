package com.argos.argos.service;

import com.argos.argos.model.Componente;
import com.argos.argos.repository.AlertaRepository;
import com.argos.argos.repository.ComponenteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MonitoramentoService {

    private final ComponenteRepository componenteRepository;
    private final AlertaRepository alertaRepository;
    private final JiraService jira;
    private final Map<String, Long> ultimoAlerta = new ConcurrentHashMap<>();
    private final Set<String> macsSemCadastro = ConcurrentHashMap.newKeySet();

    @Value("${monitoramento.cooldown-minutos}")
    private long cooldownMinutos;

    public MonitoramentoService(ComponenteRepository componenteRepository, AlertaRepository alertaRepository, JiraService jira) {
        this.componenteRepository = componenteRepository;
        this.alertaRepository = alertaRepository;
        this.jira = jira;
    }

    public void verificar(String mac, String tipo, Double uso, String quando) {
        if (uso == null) {
            return;
        }

        List<Componente> componentes = componenteRepository.buscarPorMacETipo(mac, tipo);

        if (componentes.isEmpty()) {
            if (macsSemCadastro.add(mac + "|" + tipo)) {
                System.out.println("Nenhum componente " + tipo + " cadastrado para o MAC " + mac + ". Leituras ignoradas.");
            }
            return;
        }

        for (Componente c : componentes) {
            String nivel = classificarNivel(uso, c);

            if (nivel == null) {
                System.out.println("Componente " + c.getNomeIdentificador() + " estável.");
                continue;
            }

            Double valorExibido = uso;
            String unidadeExibida = "%";

            if ("Rede".equals(tipo)) {
                if (c.getCapacidade() != null && c.getCapacidade() > 0) {
                    valorExibido = uso * 100 / c.getCapacidade();
                } else {
                    unidadeExibida = "Mbps";
                }
            }

            valorExibido = Math.round(valorExibido * 10) / 10.0;

            String chaveCooldown = c.getIdComponente() + ":" + nivel;
            Long agora = System.currentTimeMillis();
            Long ultimo = ultimoAlerta.get(chaveCooldown);

            if (ultimo != null && agora - ultimo < cooldownMinutos * 60_000) {
                System.out.println("Alerta (" + nivel + ") já registrado recentemente para "
                        + c.getNomeIdentificador() + ". Novo registro só depois de " + cooldownMinutos + " min.");
                continue;
            }

            String descricao = c.getNomeIdentificador() + " atingiu nível " + nivel
                    + " (" + valorExibido + " " + unidadeExibida + ")";

            if ("critico".equalsIgnoreCase(nivel)) {
                System.out.println("ALERTA CRÍTICO: " + c.getNomeIdentificador() + " passou do limite! Uso atual: "
                        + valorExibido + " " + unidadeExibida);

                String jiraKey = jira.criarChamado(c.getNomeIdentificador(), valorExibido, unidadeExibida, mac, quando);
                if (jiraKey != null) {
                    alertaRepository.salvarAlerta(
                            c.getIdComponente(),
                            "critico",
                            valorExibido,
                            unidadeExibida,
                            descricao,
                            quando,
                            jiraKey,
                            "Tarefas pendentes"
                    );
                    ultimoAlerta.put(chaveCooldown, agora);
                }
            } else {
                System.out.println("ALERTA (" + nivel.toUpperCase() + "): " + descricao);
                alertaRepository.salvarAlerta(
                        c.getIdComponente(),
                        nivel,
                        valorExibido,
                        unidadeExibida,
                        descricao,
                        quando,
                        null,
                        null
                );
                ultimoAlerta.put(chaveCooldown, agora);
            }
        }
    }

    private String classificarNivel(double uso, Componente c) {
        if (c.getLimiarCritico() != null && uso > c.getLimiarCritico()) {
            return "critico";
        }
        if (c.getLimiarAlto() != null && uso > c.getLimiarAlto()) {
            return "alto";
        }
        if (c.getLimiarAtencao() != null && uso > c.getLimiarAtencao()) {
            return "atencao";
        }
        return null;
    }
}