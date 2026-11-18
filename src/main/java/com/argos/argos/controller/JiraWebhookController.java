package com.argos.argos.controller;

import com.argos.argos.repository.AlertaRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/webhooks")
public class JiraWebhookController {

    private final AlertaRepository alertaRepository;

    public JiraWebhookController(AlertaRepository alertaRepository) {
        this.alertaRepository = alertaRepository;
    }

    @PostMapping("/jira")
    public ResponseEntity<Void> receberEventoJira(@RequestBody Map<String, Object> payload) {
        Object issueObj = payload.get("issue");
        if (!(issueObj instanceof Map<?,?> issue)) {
            return ResponseEntity.badRequest().build();
        }

        Object keyObj = issue.get("key");
        Object fieldsObj = issue.get("fields");
        if (keyObj == null || !(fieldsObj instanceof Map<?,?> fields)) {
            return ResponseEntity.badRequest().build();
        }

        Object statusObj = fields.get("status");
        if (!(statusObj instanceof Map<?, ?> status)) {
            return ResponseEntity.ok().build();
        }

        String jiraKey = keyObj.toString();
        Object statusNameObj = status.get("name");
        if (statusNameObj != null) {
            String novoStatus = statusNameObj.toString();
            Integer atualizados = alertaRepository.atualizarStatusPorJiraKey(jiraKey, novoStatus);
            System.out.println("Webhook Jira [" + jiraKey + "] -> status '" + novoStatus + "' (" + atualizados + " alerta(s) atualizado(s))");
        }

        return ResponseEntity.ok().build();
    }
}
