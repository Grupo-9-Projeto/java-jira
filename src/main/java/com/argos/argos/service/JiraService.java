package com.argos.argos.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class JiraService {

    @Value("${jira.url}") private String jiraUrl;
    @Value("${jira.email}") private String jiraEmail;
    @Value("${jira.token}") private String jiraToken;
    @Value("${jira.project-key}") private String projectKey;

    public boolean criarChamado(String nomeComponente, Double usoAtual, String unidade, String mac, String quandoColetado) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(jiraEmail, jiraToken));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        String unidadeTexto = (unidade == null) ? "" : unidade;

        String jsonJira = """
            {
                "fields": {
                   "project": { "key": "%s" },
                   "summary": "ALERTA: O componente %s atingiu %s %s",
                   "description": "Monitoramento detectou que o limite crítico foi ultrapassado. Dispositivo (MAC): %s. Coletado em: %s.",
                   "issuetype": { "name": "Tarefa" }
                }
            }
            """.formatted(projectKey, nomeComponente, usoAtual, unidadeTexto, mac, quandoColetado);

        HttpEntity<String> request = new HttpEntity<>(jsonJira, headers);
        String endpoint = jiraUrl + "/rest/api/2/issue";

        System.out.println("Tentando abrir chamado no Jira para o componente " + nomeComponente);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(endpoint, request, String.class);
            System.out.println("✅ Chamado criado com sucesso! Status: " + response.getStatusCode());
            return true;
        } catch (Exception e) {
            System.err.println("⚠️ FALHA DE CONEXÃO: Não foi possível abrir o chamado no Jira.");
            System.err.println("Motivo técnico: " + e.getMessage());
            System.err.println("O sistema continuará monitorando e tentará novamente no próximo ciclo.");
            return false;
        }
    }
}