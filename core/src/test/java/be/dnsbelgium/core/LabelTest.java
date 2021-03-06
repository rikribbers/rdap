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
package be.dnsbelgium.core;

import be.dnsbelgium.junit.Assert;
import com.ibm.icu.text.UnicodeSet;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LabelTest {

  @Test
  public void testLabelInstances() {
    Label label = Label.of("example");
    assertTrue(label instanceof Label.ASCIILabel.LDHLabel.NonReservedLDHLabel);
    label = Label.of("");
    assertTrue(label instanceof Label.RootLabel);
    label = Label.of("xn--bcher-kva");
    assertTrue(label instanceof Label.ASCIILabel.LDHLabel.ReservedLDHLabel.ALabel);
    label = Label.of("xn--bcher-aaa");
    assertTrue(label instanceof Label.ASCIILabel.LDHLabel.ReservedLDHLabel.FakeALabel);
    label = Label.of("_tcp");
    assertTrue(label instanceof Label.ASCIILabel.NONLDHLabel);
    label = Label.of("-example");
    assertTrue(label instanceof Label.ASCIILabel.NONLDHLabel);
    label = Label.of("example-");
    assertTrue(label instanceof Label.ASCIILabel.NONLDHLabel);
  }

  @Test
  public void testToUnicode() {
    Label label = Label.of("xn--bcher-kva");
    assertEquals("b\u00FCcher", label.toUnicode().getStringValue());
  }

  @Test
  public void testToLDH() {
    Label label = Label.of("b\u00FCcher");
    assertEquals("xn--bcher-kva", label.toLDH().getStringValue());
    // test combining characters in Unicode
    label = Label.of("Bu\u0308cher");
    assertEquals("xn--bcher-kva", label.toLDH().getStringValue());
    assertEquals("b\u00FCcher", label.getStringValue());
  }

  @Test
  public void testControlCharacters() {
    for (final String s : new UnicodeSet(0, 31)) {
      Assert.assertThrows(new Assert.Closure() {
        @Override
        public void execute() throws Throwable {
          Label.of(s);
        }
      }, LabelException.IDNParseException.class, "expects LabelException");
    }
  }

}