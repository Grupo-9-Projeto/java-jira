package com.argos.argos.service;

import com.argos.argos.client.S3Provider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.InputStream;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


@Service
public class LeitorCsvService {

    private final Integer TOTAL_COLUNAS = 13;
    private final Integer COL_CPU = 0;
    private final Integer COL_RAM = 1;
    private final Integer COL_DISCO = 2;
    private final Integer COL_QUANDO = 3;
    private final Integer COL_REDE_RECEBIDA = 4;
    private final Integer COL_REDE_ENVIADA = 5;
    private final Integer COL_MAC = 7;

    private final S3Client s3Client = new S3Provider().getS3Client();
    private final MonitoramentoService monitoramentoService;

    // arquivos que já foram lidos (senão ele leria tudo de novo a cada 10 segundos)
    private Set<String> arquivosProcessados = ConcurrentHashMap.newKeySet();

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.prefix}")
    private String prefix;

    private Double numero(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double maior(Double a, Double b) {
        if (a == null) return b;
        if (b == null) return a;
        return Math.max(a, b);
    }



    public LeitorCsvService(MonitoramentoService monitoramentoService) {
        this.monitoramentoService = monitoramentoService;
    }

    @Scheduled(fixedDelayString = "${leitor.intervalo-ms:10000}")
    public void lerCsvsNoS3() {
        ListObjectsRequest listObjects = ListObjectsRequest.builder()
                .bucket(bucket)
                .prefix(prefix)
                .build();

        List<S3Object> objects = s3Client.listObjects(listObjects).contents();

        for (S3Object object : objects) {
            String chave = object.key();

            if (!chave.endsWith(".csv") || arquivosProcessados.contains(chave)) {
                continue;
            }

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(chave)
                    .build();

            InputStream objectContent = s3Client.getObject(getObjectRequest, ResponseTransformer.toInputStream());
            Scanner leitor = new Scanner(objectContent);

            if (leitor.hasNextLine()) {
                leitor.nextLine();
            }

            while (leitor.hasNextLine()) {
                String linha = leitor.nextLine();
                if (linha.isBlank()) {
                    continue;
                }

                String[] c = linha.split(",", -1);

                if (c.length < TOTAL_COLUNAS) {
                    System.out.println("Linha ignorada em " + chave + ": esperava " + TOTAL_COLUNAS + " colunas, veio " + c.length + ". Cadastre CPU, RAM, Disco e Rede para o dispositivo.");
                    continue;
                }

                String mac = c[COL_MAC].trim();
                String quando = c[COL_QUANDO].trim();
                Double rede = maior(numero(c[COL_REDE_RECEBIDA]), numero(c[COL_REDE_ENVIADA]));

                monitoramentoService.verificar(mac, "CPU", numero(c[COL_CPU]), quando);
                monitoramentoService.verificar(mac, "RAM", numero(c[COL_RAM]), quando);
                monitoramentoService.verificar(mac, "Disco", numero(c[COL_DISCO]), quando);
                monitoramentoService.verificar(mac, "Rede", rede, quando);
            }

            leitor.close();
            arquivosProcessados.add(chave);

            }
        }
    }
