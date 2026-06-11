package com.bank.bian.investmentportfolioanalysis.api;

import com.bank.bian.investmentportfolioanalysis.model.ControlRecord;
import com.bank.bian.investmentportfolioanalysis.service.ControlRecordStore;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Map;

/**
 * BIAN semantic API for the "Investment Portfolio Analysis" service domain.
 *
 * Endpoints follow the BIAN action-term style:
 *   GET  /v1/service-domain                          → who am I (SD metadata)
 *   POST /v1/portfolio-analysis-analysis/initiate                    → Initiate a control record
 *   GET  /v1/portfolio-analysis-analysis                             → Retrieve (list)
 *   GET  /v1/portfolio-analysis-analysis/{crId}/retrieve             → Retrieve (single)
 *   PUT  /v1/portfolio-analysis-analysis/{crId}/update               → Update
 *   PUT  /v1/portfolio-analysis-analysis/{crId}/control              → Control (suspend|resume|terminate)
 */
@RestController
@RequestMapping("/v1")
public class ServiceDomainController {

    private final ControlRecordStore store;

    public ServiceDomainController(ControlRecordStore store) {
        this.store = store;
    }

    @GetMapping("/service-domain")
    public Map<String, String> serviceDomain() {
        return Map.of(
                "serviceDomain", "Investment Portfolio Analysis",
                "businessArea", "Operations and Execution",
                "businessDomain", "Securities",
                "functionalPattern", "Analyze",
                "assetType", "Portfolio Analysis",
                "controlRecord", "Portfolio Analysis Analysis",
                "version", "0.1.0",
                "phase", "1-shallow"
        );
    }

    @PostMapping("/portfolio-analysis-analysis/initiate")
    @CircuitBreaker(name = "serviceDomain")
    public ResponseEntity<ControlRecord> initiate(@RequestBody(required = false) Map<String, Object> properties) {
        return ResponseEntity.status(HttpStatus.CREATED).body(store.initiate(properties));
    }

    @GetMapping("/portfolio-analysis-analysis")
    public Collection<ControlRecord> list() {
        return store.list();
    }

    @GetMapping("/portfolio-analysis-analysis/{crId}/retrieve")
    public ResponseEntity<ControlRecord> retrieve(@PathVariable String crId) {
        return store.retrieve(crId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/portfolio-analysis-analysis/{crId}/update")
    public ResponseEntity<ControlRecord> update(@PathVariable String crId,
                                                @RequestBody Map<String, Object> properties) {
        return store.update(crId, properties)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PutMapping("/portfolio-analysis-analysis/{crId}/control")
    public ResponseEntity<?> control(@PathVariable String crId,
                                     @RequestBody Map<String, String> body) {
        try {
            return store.control(crId, body.get("action"))
                    .<ResponseEntity<?>>map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
