package dev.noby.history.controller;

import dev.noby.history.dto.PackageHistoryResponse;
import dev.noby.history.service.PackageHistoryService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/v1/packages")
public class PackageHistoryController {

    private final PackageHistoryService historyService;

    public PackageHistoryController(PackageHistoryService historyService) {
        this.historyService = historyService;
    }

    @GetMapping("/{packageId}/history")
    public List<PackageHistoryResponse> getHistory(@PathVariable Long packageId) {
        if (packageId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "packageId debe ser positivo");
        }
        return historyService.getHistory(packageId);
    }
}
