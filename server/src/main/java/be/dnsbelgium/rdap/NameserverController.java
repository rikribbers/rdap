package be.dnsbelgium.rdap;

import be.dnsbelgium.core.DomainName;
import be.dnsbelgium.core.LabelException;
import be.dnsbelgium.rdap.core.Nameserver;
import be.dnsbelgium.rdap.core.RDAPError;
import be.dnsbelgium.rdap.service.NameserverService;
import com.ibm.icu.text.IDNA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping(value = "nameserver")
public final class NameserverController {

  private final static Logger logger = LoggerFactory.getLogger(NameserverController.class);

  private final String baseRedirectURL;

  private final int redirectThreshold;

  @Autowired
  private NameserverService nameserverService;

  @Autowired
  public NameserverController(
          @Value("#{applicationProperties['baseRedirectURL']}") String baseRedirectURL,
          @Value("#{applicationProperties['redirectThreshold']}") int redirectThreshold) {
    this.baseRedirectURL = baseRedirectURL;
    this.redirectThreshold = redirectThreshold;
  }

  @RequestMapping(value = "/{nameserverName}", method = RequestMethod.GET, produces = Controllers.CONTENT_TYPE)
  @ResponseBody
  public Nameserver get(@PathVariable("nameserverName") final String nameserverName) throws RDAPError {
    logger.debug("Query for nameserver {}", nameserverName);
    final DomainName domainName;
    try {
      domainName = DomainName.of(nameserverName);
      Nameserver nameserver = nameserverService.getNameserver(domainName);
      if (nameserver == null) {
        logger.debug("Query result for {} is null. Throwing NameserverNotFound Error");
        throw RDAPError.nameserverNotFound(domainName);
      }
      return nameserver;
    } catch (LabelException.IDNParseException e) {
      List<String> description = new ArrayList<String>(e.getErrors().size());
      for (IDNA.Error error : e.getErrors()) {
        description.add(error.name());
      }
      throw new RDAPError(400, "Invalid nameserver name", description, e);
    } catch (RDAPError e) {
      throw e;
    } catch (Exception e) {
      logger.error("Some errors not handled", e);
      throw new RDAPError(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Internal server error");
    }
  }

  @ExceptionHandler(value = RDAPError.NotAuthoritative.class)
  @ResponseBody
  protected RDAPError handleResourceNotFoundException(RDAPError.NotAuthoritative error, HttpServletResponse response) throws UnsupportedEncodingException {
    response.setStatus(error.getErrorCode());
    String location = baseRedirectURL + "/nameserver/" + URLEncoder.encode(error.getDomainName(), "UTF-8");
    response.addHeader(Controllers.LOCATION_HEADER, location);
    return error;
  }

  public int getRedirectThreshold() {
    return redirectThreshold;
  }
}
