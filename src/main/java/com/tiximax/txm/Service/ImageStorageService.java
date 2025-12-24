package com.tiximax.txm.Service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

@Service
public class ImageStorageService {

    private final RestTemplate restTemplate;
    private final String supabaseUrl;
    private final String supabaseKey;
    private final String bucketName;
    private final String bucketFileName;

    public ImageStorageService(RestTemplate restTemplate,
                               @Value("${supabase.url}") String supabaseUrl,
                               @Value("${supabase.key}") String supabaseKey,
                               @Value("${supabase.bucket}") String bucketName,
                               @Value("${supabase.bucketFile}") String bucketFileName) {
        this.restTemplate = restTemplate;
        this.supabaseUrl = supabaseUrl;
        this.supabaseKey = supabaseKey;
        this.bucketName = bucketName;
        this.bucketFileName = bucketFileName;
    }

    public String uploadImage(MultipartFile file) throws IOException {

        String customFileName = "tiximax-" + System.currentTimeMillis() +
                (file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")
                        ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."))
                        : ".png");
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + customFileName;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(file.getContentType() != null ? file.getContentType() : "image/png"));
        headers.set("Authorization", "Bearer " + supabaseKey);

        HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + customFileName;
        } else {
            throw new IOException("Upload thất bại: " + response.getStatusCode() + " - " + response.getBody());
        }
    }

    public boolean deleteImage(String filePath) throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            return true;
        }

        String fileName = filePath.contains(bucketName)
                ? filePath.substring(filePath.indexOf(bucketName) + bucketName.length() + 1)
                : filePath;
        String deleteUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + fileName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                deleteUrl,
                org.springframework.http.HttpMethod.DELETE,
                entity,
                String.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            return true;
        } else {
            throw new IOException("Xóa ảnh thất bại: " + response.getStatusCode() + " - " + response.getBody());
        }
    }

    public String uploadFile(MultipartFile file) throws IOException {

        String customFileName = "tiximax-" + System.currentTimeMillis() +
                (file.getOriginalFilename() != null && file.getOriginalFilename().contains(".")
                        ? file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."))
                        :".xlsx");

        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucketFileName + "/" + customFileName;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.valueOf(file.getContentType() != null ? file.getContentType() : "file/xlsx")
    );
        headers.set("Authorization", "Bearer " + supabaseKey);

        HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);
        ResponseEntity<String> response = restTemplate.postForEntity(uploadUrl, entity, String.class);

        if (response.getStatusCode().is2xxSuccessful()) {
            return supabaseUrl + "/storage/v1/object/public/" + bucketFileName + "/" + customFileName;
        } else {
            throw new IOException("Upload thất bại: " + response.getStatusCode() + " - " + response.getBody());
        }
    }

    public boolean deleteFile(String filePath) throws IOException {
        if (filePath == null || filePath.isEmpty()) {
            return true;
        }

        String fileName = filePath.contains(bucketFileName)
                ? filePath.substring(filePath.indexOf(bucketFileName) + bucketFileName.length() + 1)
                : filePath;
        String deleteUrl = supabaseUrl + "/storage/v1/object/" + bucketFileName + "/" + fileName;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseKey);

        HttpEntity<Void> entity = new HttpEntity<>(headers);
        ResponseEntity<String> response = restTemplate.exchange(
                deleteUrl,
                org.springframework.http.HttpMethod.DELETE,
                entity,
                String.class
        );

        if (response.getStatusCode().is2xxSuccessful()) {
            return true;
        } else {
            throw new IOException("Xóa file thất bại: " + response.getStatusCode() + " - " + response.getBody());
        }
    }

}