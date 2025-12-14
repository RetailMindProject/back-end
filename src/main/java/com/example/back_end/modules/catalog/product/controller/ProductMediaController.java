package com.example.back_end.modules.catalog.product.controller;

import com.example.back_end.modules.catalog.product.dto.AddProductMediaDTO;
import com.example.back_end.modules.catalog.product.dto.ProductResponseDTO;
import com.example.back_end.modules.catalog.product.dto.UpdateProductMediaDTO;
import com.example.back_end.modules.catalog.product.service.ImageStorageService;
import com.example.back_end.modules.catalog.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProductMediaController {

    private final ImageStorageService imageStorage;
    private final ProductService productService;

    @PostMapping(value = "/{productId}/images/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponseDTO> uploadImage(@PathVariable Long productId,
                                                          @RequestParam("file") MultipartFile file,
                                                          @RequestParam(value = "isPrimary", required = false, defaultValue = "false") Boolean isPrimary,
                                                          @RequestParam(value = "sortOrder", required = false) Integer sortOrder,
                                                          @RequestParam(value = "title", required = false) String title,
                                                          @RequestParam(value = "altText", required = false) String altText) {

        String storedName = imageStorage.store(productId, file);
        String url = imageStorage.buildUrl(productId, storedName);

        AddProductMediaDTO dto = AddProductMediaDTO.builder()
                .url(url)
                .mimeType(Optional.ofNullable(file.getContentType()).orElse(null))
                .title(title)
                .altText(altText)
                .sortOrder(sortOrder)
                .isPrimary(isPrimary != null ? isPrimary : false)
                .build();

        return ResponseEntity.ok(productService.addImage(productId, dto));
    }

    @PutMapping(value = "/{productId}/images/{mediaId}/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ProductResponseDTO> updateImage(@PathVariable Long productId,
                                                           @PathVariable Long mediaId,
                                                           @RequestParam("file") MultipartFile file,
                                                           @RequestParam(value = "isPrimary", required = false) Boolean isPrimary,
                                                           @RequestParam(value = "sortOrder", required = false) Integer sortOrder,
                                                           @RequestParam(value = "title", required = false) String title,
                                                           @RequestParam(value = "altText", required = false) String altText) {

        String storedName = imageStorage.store(productId, file);
        String url = imageStorage.buildUrl(productId, storedName);

        UpdateProductMediaDTO dto = UpdateProductMediaDTO.builder()
                .url(url)
                .mimeType(Optional.ofNullable(file.getContentType()).orElse(null))
                .title(title)
                .altText(altText)
                .sortOrder(sortOrder)
                .isPrimary(isPrimary)
                .build();

        return ResponseEntity.ok(productService.updateImage(productId, mediaId, dto));
    }

    @GetMapping("/{productId}/images/{fileName:.+}")
    public ResponseEntity<Resource> serveImage(@PathVariable Long productId,
                                               @PathVariable String fileName) {
        try {
            Path path = imageStorage.getPath(productId, fileName);
            Resource resource = new UrlResource(path.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            String contentType = Files.probeContentType(path);
            MediaType mediaType = contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM;
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                    .contentType(mediaType)
                    .body(resource);
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @DeleteMapping("/{productId}/images/{mediaId}")
    public ResponseEntity<Void> deleteImage(@PathVariable Long productId,
                                             @PathVariable Long mediaId) {
        productService.removeImage(productId, mediaId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{productId}/images/{mediaId}/set-primary")
    public ResponseEntity<ProductResponseDTO> setPrimaryImage(@PathVariable Long productId,
                                                               @PathVariable Long mediaId) {
        return ResponseEntity.ok(productService.setPrimaryImage(productId, mediaId));
    }
}

