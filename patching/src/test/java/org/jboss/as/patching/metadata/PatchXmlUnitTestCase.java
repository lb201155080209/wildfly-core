/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.as.patching.metadata;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Scanner;
import javax.xml.stream.XMLStreamException;

import org.junit.Test;
import org.xnio.IoUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Emanuel Muckenhuber
 */
public class PatchXmlUnitTestCase {

    @Test
    public void testParseCP() throws Exception {

        final InputStream is = getResource("patch-01-CP.xml");
        final Patch patch = PatchXml.parse(is).resolvePatch(null, null);
        // Cumulative Patch
        assertNotNull(patch);
        assertNotNull(patch.getPatchId());
        assertNotNull(patch.getDescription());
        final Identity identity = patch.getIdentity();
        assertNotNull(identity);
        assertEquals(Patch.PatchType.CUMULATIVE, identity.getPatchType());
        assertNotNull(identity.forType(Patch.PatchType.CUMULATIVE, Identity.IdentityUpgrade.class).getResultingVersion());
        assertNotNull(identity.getVersion());
    }

    @Test
    public void testParseOneOff() throws Exception {

        final InputStream is = getResource("patch-02-ONE-OFF.xml");
        final Patch patch = PatchXml.parse(is).resolvePatch(null, null);
        // One-off Patch
        assertNotNull(patch);
        assertNotNull(patch.getPatchId());
        assertNotNull(patch.getDescription());
        final Identity identity = patch.getIdentity();
        assertNotNull(identity);
        assertNotNull(patch.getIdentity().getVersion());
    }

    @Test
    public void testParseDuplicateElementPatchId() throws Exception {

        final InputStream is = getResource("patch-02-ONE-OFF.xml");
        final StringBuilder buf = new StringBuilder();
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(is));
            String line = reader.readLine();
            final String newLine = System.getProperty("line.separator");
            while(line != null) {
                buf.append(line).append(newLine);
                line = reader.readLine();
            }
        } finally {
            IoUtils.safeClose(reader);
        }

        // duplicate an element
        int elementStart = buf.indexOf("<element ");
        if(elementStart < 0) {
            fail("failed to locate <element> tag in " + buf.toString());
        }
        int elementEnd = buf.indexOf("</element>", elementStart);
        if(elementEnd < 0) {
            fail("failed to locate </element> tag starting from position " + elementStart + " in " + buf.toString());
        }
        buf.insert(elementEnd + "</element>".length(), buf.substring(elementStart, elementEnd + "</element>".length()));

        try {
            PatchXml.parse(new StringReader(buf.toString())).resolvePatch(null, null);
            fail("duplicate element patch-id error expected");
        } catch(XMLStreamException e) {
            assertTrue(e.getMessage().contains("WFLYPAT0038"));
        }
    }

    @Test
    public void testMarshallCP() throws Exception {
        doMarshall("patch-11-CP.xml");
    }

    @Test
    public void testMarshallOneOff() throws Exception {
        doMarshall("patch-21-ONE-OFF.xml");
    }

    private void doMarshall(String fileName) throws Exception {
        final String original = toString(fileName);

        try (final InputStream is = getResource(fileName)) {
            final Patch patch = PatchXml.parse(is).resolvePatch(null, null);
            final StringWriter writer = new StringWriter();
            PatchXml.marshal(writer, patch);
            final String marshalled = writer.toString();
            XMLUtils.compareXml(original, marshalled, false);
        }
    }

    static InputStream getResource(String name) throws IOException {
        final URL resource = PatchXmlUnitTestCase.class.getClassLoader().getResource(name);
        assertNotNull(name, resource);
        return resource.openStream();
    }

    private String toString(String fileName) throws Exception {
        try (final InputStream is = getResource(fileName)){
            assertNotNull(is);
            is.mark(0);
            String out = new Scanner(is).useDelimiter("\\A").next();
            is.reset();
            return out;
        } catch (Exception e) {
            return "";
        }
    }
}
