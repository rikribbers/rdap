/**
 * Copyright 2014 DNS Belgium vzw
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package be.dnsbelgium.rdap;

import be.dnsbelgium.rdap.jackson.CustomObjectMapper;
import be.dnsbelgium.rdap.service.*;
import be.dnsbelgium.rdap.service.impl.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

@Configuration
@ComponentScan(basePackages = "be.dnsbelgium")
public class WebConfig extends WebMvcConfigurationSupport {

  private static final Logger logger = LoggerFactory.getLogger(WebConfig.class);

  @Bean
  public DomainService getDomainService() {
    return new DefaultDomainService();
  }

  @Bean
  public NameserverService getNameserverService() {
    return new DefaultNameserverService();
  }

  @Bean
  public EntityService getEntityService() {
    return new DefaultEntityService();
  }

  @Bean
  public IPService getIPService() {
    return new DefaultIPService();
  }

  @Bean
  public AutNumService getAutNumService() {
    return new DefaultAutNumService();
  }

  @Bean
  public HelpService getHelpService() {
    return new DefaultHelpService();
  }

  @Override
  public final void configureMessageConverters(final List<HttpMessageConverter<?>> converters) {
    converters.add(converter());
  }

  @Override
  protected void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
    super.configureContentNegotiation(configurer);
    configurer.favorPathExtension(false);
  }

  @Bean
  MappingJackson2HttpMessageConverter converter() {
    MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
    converter.setObjectMapper(getObjectMapper());
    return converter;
  }

  @Bean
  public ObjectMapper getObjectMapper() {
    return new CustomObjectMapper();
  }

  @Bean
  public RequestMappingHandlerMapping requestMappingHandlerMapping() {
    RequestMappingHandlerMapping handlerMapping = super.requestMappingHandlerMapping();
    handlerMapping.setUseSuffixPatternMatch(false);
    handlerMapping.setUseTrailingSlashMatch(false);
    return handlerMapping;
  }

  @Bean(name = "applicationProperties")
  public Properties getProperties() {
    Properties p = new Properties();
    InputStream is = getClass().getResourceAsStream("/application.properties");
    try {
      p.load(is);
    } catch (IOException e) {
      logger.debug("Error loading application.properties", e);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          logger.debug("Error closing inputstream", e);
        }
      }
    }
    return p;
  }
}
