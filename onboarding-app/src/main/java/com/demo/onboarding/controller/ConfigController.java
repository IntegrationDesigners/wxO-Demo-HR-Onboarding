package com.demo.onboarding.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Exposes runtime configuration to the frontend so that environment-specific
 * values (e.g. wxO host URL, orchestration ID, agent ID) do not need to be
 * hard-coded in the static HTML.
 */
@RestController
@RequestMapping("/api/config")
@Tag(name = "Configuration", description = "Frontend runtime configuration")
public class ConfigController {

    @Value("${wxo.host-url}")
    private String wxoHostUrl;

    @Value("${wxo.orchestration-id}")
    private String wxoOrchestrationId;

    @Value("${wxo.agent-id}")
    private String wxoAgentId;

    @Value("${wxo.deployment-platform:}")
    private String wxoDeploymentPlatform;

    @Value("${wxo.crn:}")
    private String wxoCrn;

    @Value("${wxo.agent-environment-id:}")
    private String wxoAgentEnvironmentId;

    @GetMapping
    @Operation(summary = "Get frontend configuration", description = "Returns environment-specific configuration values used by the wxO chat widget.")
    public ResponseEntity<Map<String, String>> getConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("hostURL", wxoHostUrl);
        config.put("orchestrationID", wxoOrchestrationId);
        config.put("agentId", wxoAgentId);

        boolean hasDeploymentPlatform = wxoDeploymentPlatform != null && !wxoDeploymentPlatform.isBlank();
        String loaderURL = hasDeploymentPlatform
                ? wxoHostUrl + "/wxochat/wxoLoader.js?embed=true"
                : wxoHostUrl + "/wxoLoader.js?embed=true";
        config.put("loaderURL", loaderURL);

        if (hasDeploymentPlatform) {
            config.put("deploymentPlatform", wxoDeploymentPlatform);
        }
        if (wxoCrn != null && !wxoCrn.isBlank()) {
            config.put("crn", wxoCrn);
        }
        if (wxoAgentEnvironmentId != null && !wxoAgentEnvironmentId.isBlank()) {
            config.put("agentEnvironmentId", wxoAgentEnvironmentId);
        }
        return ResponseEntity.ok(config);
    }
}
