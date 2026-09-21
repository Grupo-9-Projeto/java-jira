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
    public void verificarComponentes() {
        System.out.println("Iniciando varredura de hardware...");

        Long idComponenteMonitorado = 1L;
        Double usoAtual = 92.0;

        Componente limiteBanco = repository.buscarLimitesPorId(idComponenteMonitorado);

        if (usoAtual > limiteBanco.getLimiarCritico()) {
            System.out.println("ALERTA CRÍTICO: " + limiteBanco.getNomeIdentificador() + " passou do limite! Uso atual: " + usoAtual);

            jira.criarChamado(limiteBanco.getNomeIdentificador(), usoAtual);
        } else {
            System.out.println("Componente " + limiteBanco.getNomeIdentificador() + " estável.");
        }
    }
}
