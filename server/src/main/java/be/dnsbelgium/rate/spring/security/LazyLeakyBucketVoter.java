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
package be.dnsbelgium.rate.spring.security;

import be.dnsbelgium.rate.LazyLeakyBucket;
import be.dnsbelgium.rdap.spring.security.RDAPErrorException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.pool.KeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDecisionVoter;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collection;

public class LazyLeakyBucketVoter<T extends LazyLeakyBucketKey> implements AccessDecisionVoter<Object> {

  private static final Logger LOGGER = LoggerFactory.getLogger(LazyLeakyBucketVoter.class);
  public static final int TOO_MANY_REQUESTS_HTTP_CODE = 429;

  private final KeyedObjectPool<T, LazyLeakyBucket> objectPool;

  private final int defaultAmount;

  public static final String PREFIX = "LEAKY_BUCKET_";

  public static final String SEPARATOR = "#";

  private final LazyLeakyBucketKeyFactory<T> keyFactory;

  public LazyLeakyBucketVoter(KeyedObjectPool<T, LazyLeakyBucket> objectPool, LazyLeakyBucketKeyFactory<T> keyFactory, int defaultAmount) {
    this.objectPool = objectPool;
    this.keyFactory = keyFactory;
    this.defaultAmount = defaultAmount;
  }

  @Override
  public boolean supports(ConfigAttribute attribute) {
    return attribute.getAttribute() != null && attribute.getAttribute().startsWith(PREFIX);
  }

  @Override
  public boolean supports(Class<?> clazz) {
    return true;
  }

  @Override
  public int vote(Authentication authentication, Object object, Collection<ConfigAttribute> attributes) {
    int amount = this.defaultAmount;
    if (attributes != null) {
      for (ConfigAttribute attribute : attributes) {
        if (attribute.getAttribute() != null && attribute.getAttribute().startsWith(PREFIX + SEPARATOR)) {
          String amountAttributeValue = StringUtils.splitByWholeSeparatorPreserveAllTokens(attribute.getAttribute(), SEPARATOR)[1];
          try {
            // should be minimum zero
            amount = Math.max(0, Integer.parseInt(amountAttributeValue));
          } catch (NumberFormatException nfe) {
            LOGGER.debug(String.format("%s is NaN. Defaulting to %s for amount.", amountAttributeValue, defaultAmount), nfe);
          }
        }
      }
    }
    int result = ACCESS_DENIED;
    LazyLeakyBucket bucket = null;
    T key = keyFactory.create(SecurityContextHolder.getContext());
    try {
      bucket = objectPool.borrowObject(key);
      if (bucket.add(amount)) {
        result = ACCESS_GRANTED;
      }
    } catch (IllegalArgumentException iae) {
      // this should never occur since amount is minimum zero (Math.max)
      LOGGER.error(String.format("Illegal amount of tokens added to bucket: %s", amount),iae);
      throw iae;
    } catch (Exception e) {
      LOGGER.debug("Error borrowing object. Should never occur", e);
    } finally {
      if (bucket != null) {
        try {
          objectPool.returnObject(key, bucket);
        } catch (Exception e) {
          LOGGER.debug("an error occurred while returning object to pool", e);
        }
      }
    }
    if (result == ACCESS_DENIED) {
      throw new RDAPErrorException(TOO_MANY_REQUESTS_HTTP_CODE, "Too many requests");
    }
    return result;
  }

}