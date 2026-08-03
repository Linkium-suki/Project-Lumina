package com.lumina.key;

import com.lumina.common.ApiResponse;
import com.lumina.common.web.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/keys")
@RequiredArgsConstructor
public class KeyController {

    private final KeyService keyService;

    @PostMapping
    public ApiResponse<KeyResponse> addKey(@Valid @RequestBody KeyRequest request) {
        return ApiResponse.ok(keyService.addKey(CurrentUser.userId(), request));
    }

    @GetMapping
    public ApiResponse<List<KeyResponse>> listKeys() {
        return ApiResponse.ok(keyService.listKeys(CurrentUser.userId()));
    }

    @DeleteMapping("/{keyId}")
    public ApiResponse<Void> deleteKey(@PathVariable Long keyId) {
        keyService.deleteKey(CurrentUser.userId(), keyId);
        return ApiResponse.ok();
    }
}
