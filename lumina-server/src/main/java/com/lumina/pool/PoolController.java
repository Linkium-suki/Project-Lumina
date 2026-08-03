package com.lumina.pool;

import com.lumina.common.ApiResponse;
import com.lumina.common.web.CurrentUser;
import com.lumina.key.KeyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/pool")
@RequiredArgsConstructor
public class PoolController {

    private final PoolService poolService;

    @PostMapping("/donate/{keyId}")
    public ApiResponse<KeyResponse> donate(@PathVariable Long keyId) {
        return ApiResponse.ok(poolService.donate(CurrentUser.userId(), keyId));
    }

    @PostMapping("/withdraw/{keyId}")
    public ApiResponse<Void> withdraw(@PathVariable Long keyId) {
        poolService.withdraw(CurrentUser.userId(), keyId);
        return ApiResponse.ok();
    }

    @PostMapping("/join")
    public ApiResponse<Void> join() {
        poolService.join(CurrentUser.userId());
        return ApiResponse.ok();
    }

    @PostMapping("/leave")
    public ApiResponse<Void> leave() {
        poolService.leave(CurrentUser.userId());
        return ApiResponse.ok();
    }

    @GetMapping("/status")
    public ApiResponse<PoolStatusResponse> status() {
        return ApiResponse.ok(poolService.status(CurrentUser.userId()));
    }
}
