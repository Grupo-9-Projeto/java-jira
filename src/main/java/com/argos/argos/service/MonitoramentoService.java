package com.argos.argos.service;

import com.argos.argos.model.Componente;
import com.argos.argos.repository.ComponenteRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MonitoramentoService {

    private final ComponenteRepository repository;
    private final JiraService jira;

    public MonitoramentoService(ComponenteRepository repository, JiraService jira) {
        this.repository = repository;
        this.jira = jira;
    }

    @Scheduled(fixedRate = 10000)
    public void verificarComponentes(Integer idComponente, Double uso) {
        System.out.println("Iniciando varredura de hardware...");

        Componente limiteBanco = repository.buscarLimitesPorId(idComponente);

        if (uso > limiteBanco.getLimiarCritico()) {
            System.out.println("ALERTA CRÍTICO: " + limiteBanco.getNomeIdentificador() + " passou do limite! Uso atual: " + uso);

            jira.criarChamado(limiteBanco.getNomeIdentificador(), uso);
        } else {
            System.out.println("Componente " + limiteBanco.getNomeIdentificador() + " estável.");
        }
    }
}
