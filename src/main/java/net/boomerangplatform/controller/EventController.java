package net.boomerangplatform.controller;

import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.JsonNode;
import net.boomerangplatform.client.WorkflowClient;
import net.boomerangplatform.service.EventProcessor;

@RestController
@RequestMapping("/listener")
public class EventController {
  
  protected static final String TOKEN_HEADER = "Authorization";
      
  protected static final String TOKEN_PARAM = "access_token";

  @Autowired
  private EventProcessor eventProcessor;
  
  /*
   * Accepts the Webhook style. 
   * Note: Only partially conformant to the specification.
   * https://github.com/cloudevents/spec/blob/master/http-webhook.md
   */
  @Deprecated
  @PostMapping(value = "/webhook", consumes = "application/json; charset=utf-8")
  public ResponseEntity<HttpStatus> acceptWebhookEvent(HttpServletRequest request, @RequestBody JsonNode payload) {
    eventProcessor.routeEvent(getToken(request), request.getRequestURL().toString(), "webhook", payload.path("workflowId").asText(), payload);

    return ResponseEntity.ok(HttpStatus.OK);
  }
  
  @PostMapping(value = "/webhook/{workflowId}", consumes = "application/json; charset=utf-8")
  public ResponseEntity<HttpStatus> acceptWebhookEvent(HttpServletRequest request, @PathVariable String workflowId, @RequestBody JsonNode payload) {
    eventProcessor.routeEvent(getToken(request), request.getRequestURL().toString(), "webhook", workflowId, payload);
    return ResponseEntity.ok(HttpStatus.OK);
  }
  
  /*
   * Accepts the following style of Dockerhub payload
   * https://docs.docker.com/docker-hub/webhooks/
   */
  @PostMapping(value = "/dockerhub/{workflowId}", consumes = "application/json; charset=utf-8")
  public ResponseEntity<HttpStatus> acceptDockerhubEvent(HttpServletRequest request, @RequestHeader Map<String, Object> headers, @RequestParam String accessToken, @PathVariable String workflowId, @RequestBody JsonNode payload) {
    eventProcessor.routeEvent(getToken(request), request.getRequestURL().toString(), "dockerhub", workflowId, payload);
    return ResponseEntity.ok(HttpStatus.OK);
  }
  
  @PutMapping(value = "/event", consumes = "application/cloudevents+json; charset=utf-8")
  public ResponseEntity<HttpStatus> acceptEvent(HttpServletRequest request, @RequestHeader Map<String, Object> headers, @RequestBody JsonNode payload) {
    eventProcessor.routeCloudEvent(getToken(request), request.getRequestURL().toString(), headers, payload);
    return ResponseEntity.ok(HttpStatus.OK);
  }
  
  private String getToken(HttpServletRequest request) {
    if (request.getHeader(TOKEN_HEADER) != null && !request.getHeader(TOKEN_HEADER).isEmpty()) {
      return request.getHeader(TOKEN_HEADER).replace("Bearer ", "");
    } else if (request.getParameter(TOKEN_PARAM) != null && !request.getParameter(TOKEN_PARAM).isEmpty()) {
      return request.getParameter(TOKEN_PARAM);
    } else {
      return "";
    }
  }
}
