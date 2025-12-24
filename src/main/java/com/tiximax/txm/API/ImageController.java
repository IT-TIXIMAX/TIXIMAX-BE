package com.tiximax.txm.API;

import com.tiximax.txm.Service.ImageStorageService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@CrossOrigin
@RequestMapping("/images")
@SecurityRequirement(name = "bearerAuth")

public class ImageController {

    private final ImageStorageService imageStorageService;

    public ImageController(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @PostMapping("/upload-image")
    public ResponseEntity<String> uploadImage(@RequestParam("image") MultipartFile file) {
        try {
            String imageUrl = imageStorageService.uploadImage(file);
            return ResponseEntity.ok(imageUrl);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Lỗi upload: " + e.getMessage());
        }
    }

    @PostMapping("/upload-file")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String fileUrl = imageStorageService.uploadFile(file);
            return ResponseEntity.ok(fileUrl);
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Lỗi upload: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete-image")
    public ResponseEntity<String> deleteImage(@RequestParam("filePath") String filePath) {
        try {
            boolean isDeleted = imageStorageService.deleteImage(filePath);
            return ResponseEntity.ok("Xóa ảnh " + (isDeleted ? "thành công!" : "thất bại!"));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Lỗi xóa: " + e.getMessage());
        }
    }

    @DeleteMapping("/delete-file")
    public ResponseEntity<String> deleteFile(@RequestParam("filePath") String filePath) {
        try {
            boolean isDeleted = imageStorageService.deleteFile(filePath);
            return ResponseEntity.ok("Xóa file " + (isDeleted ? "thành công!" : "thất bại!"));
        } catch (IOException e) {
            return ResponseEntity.badRequest().body("Lỗi xóa: " + e.getMessage());
        }
    }

}