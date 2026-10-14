package com.argos.argos.service;

import com.argos.argos.model.Componente;
import com.argos.argos.repository.ComponenteRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MonitoramentoService {

    private final ComponenteRepository repository;
    private final JiraService jira;
    private final Map<Integer, Long> ultimoChamado = new ConcurrentHashMap<>();

    @Value("${monitoramento.cooldown-minutos}")
    private long cooldownMinutos;

    public MonitoramentoService(ComponenteRepository repository, JiraService jira) {
        this.repository = repository;
        this.jira = jira;
    }

    public void verificar(String mac, String tipo, Double uso, String quando) {
        if (uso == null) {
            return;
        }

        for (Componente c : repository.buscarPorMacETipo(mac, tipo)) {

            if (c.getLimiarCritico() == null || uso <= c.getLimiarCritico()) {
                System.out.println("Componente " + c.getNomeIdentificador() + " estável.");
                continue;
            }

            double valorExibido = uso;
            String unidadeExibida = "%";
            if (tipo.equals("Rede")) {
                if (c.getCapacidade() != null && c.getCapacidade() > 0) {
                    valorExibido = uso * 100 / c.getCapacidade();
                } else {
                    unidadeExibida = "Mbps";
                }
            }
            valorExibido = Math.round(valorExibido * 10) / 10.0;

            System.out.println("ALERTA CRÍTICO: " + c.getNomeIdentificador() + " passou do limite! Uso atual: "
                    + valorExibido + " " + unidadeExibida);

            Long agora = System.currentTimeMillis();
            Long ultimo = ultimoChamado.get(c.getIdComponente());

            if (ultimo != null && agora - ultimo < cooldownMinutos * 60_000) {
                System.out.println("Chamado já aberto recentemente para " + c.getNomeIdentificador()
                        + ". Novo chamado só depois de " + cooldownMinutos + " min.");
                continue;
            }

            if (jira.criarChamado(c.getNomeIdentificador(), valorExibido, unidadeExibida, mac, quando)) {
                ultimoChamado.put(c.getIdComponente(), agora);
            }
        }
    }
}