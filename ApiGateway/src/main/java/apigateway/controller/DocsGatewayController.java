package apigateway.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class DocsGatewayController {

  @GetMapping("/")
  public String root() {
    return "redirect:/swagger-ui.html";
  }

  @GetMapping("/asyncapi")
  public String asyncApiUi() {
    return "redirect:/asyncapi-ui.html";
  }

  @ResponseBody
  @GetMapping("/asyncapi.yaml")
  public ResponseEntity<Resource> asyncApiYaml() {
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("application/yaml"))
        .body(new ClassPathResource("static/asyncapi.yaml"));
  }
}
