package com.argos.argos.service;

import com.argos.argos.client.S3Provider;
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


@Service
public class LeitorCsvService {

    private final MonitoramentoService monitoramentoService;

    public LeitorCsvService(MonitoramentoService monitoramentoService) {
        this.monitoramentoService = monitoramentoService;
    }

    @Scheduled(fixedRate = 10000)
    public void lerCsvsNoS3() {
        S3Client s3Client = new S3Provider().getS3Client();

        ListObjectsRequest listObjects = ListObjectsRequest.builder()
                .bucket("nome-do-bucket")
                .prefix("client/")
                .build();

        List<S3Object> objects = s3Client.listObjects(listObjects).contents();
        for (S3Object object : objects) {
            if (object.key().endsWith(".csv")) {
                GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                        .bucket("nome-do-bucket")
                        .key(object.key())
                        .build();

                InputStream objectContent = s3Client.getObject(getObjectRequest, ResponseTransformer.toInputStream());

                Scanner leitor = new Scanner(objectContent);

                while (leitor.hasNextLine()) {
                    String linha = leitor.nextLine();

                    try {
                        String[] colunas = linha.split(",");
                        Integer idComponente = Integer.parseInt(colunas[0].trim());
                        Double uso = Double.parseDouble(colunas[1].trim());
                        monitoramentoService.verificarComponentes(idComponente, uso);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
                leitor.close();
            }
        }
    }
}
