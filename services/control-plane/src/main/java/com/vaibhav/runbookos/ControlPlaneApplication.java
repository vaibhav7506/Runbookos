package com.vaibhav.runbookos;

import com.vaibhav.runbookos.config.RunbookOsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RunbookOsProperties.class)
public class ControlPlaneApplication {

  public static void main(String[] args) {
    SpringApplication.run(ControlPlaneApplication.class, args);
  }
}
