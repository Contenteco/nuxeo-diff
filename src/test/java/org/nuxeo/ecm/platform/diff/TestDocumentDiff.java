/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     ataillefer
 */
package org.nuxeo.ecm.platform.diff;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.Test;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.storage.sql.SQLRepositoryTestCase;
import org.nuxeo.ecm.platform.diff.model.DocumentDiff;
import org.nuxeo.ecm.platform.diff.model.PropertyDiff;
import org.nuxeo.ecm.platform.diff.model.SchemaDiff;
import org.nuxeo.ecm.platform.diff.model.impl.SimplePropertyDiff;
import org.nuxeo.ecm.platform.diff.service.DocumentDiffService;
import org.nuxeo.ecm.platform.xmlexport.DocumentXMLExporter;
import org.nuxeo.runtime.api.Framework;

/**
 * @author ataillefer
 */
public class TestDocumentDiff extends SQLRepositoryTestCase {

    private static final String NUXEO_PLATFORM_DIFF_BUNDLE = "org.nuxeo.platform.diff";

    private static final String NUXEO_PLATFORM_DIFF_TEST_BUNDLE = "org.nuxeo.platform.diff.test";

    private DocumentDiffService docDiffService;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        deployBundle(NUXEO_PLATFORM_DIFF_BUNDLE);
        deployBundle(NUXEO_PLATFORM_DIFF_TEST_BUNDLE);
        docDiffService = Framework.getService(DocumentDiffService.class);
        openSession();
    }

    @Override
    public void tearDown() throws Exception {
        closeSession();
        super.tearDown();
    }

    /**
     * Test doc diff.
     * 
     * @throws ClientException the client exception
     */
    @Test
    public void testDocDiff() throws ClientException {

        // Create two docs
        DocumentModel leftDoc = createLeftDoc();
        DocumentModel rightDoc = createRightDoc();
        session.save();

        // Create XML export temporary files
        createXMLExportTempFile(leftDoc);
        createXMLExportTempFile(rightDoc);

        // Do doc diff
        DocumentDiff docDiff = docDiffService.diff(session, leftDoc, rightDoc);
        assertEquals("Wrong schema count.", 3, docDiff.getSchemaCount());

        // ---------------------------
        // Check system elements
        // ---------------------------
        SchemaDiff schemaDiff = checkSchemaDiff(docDiff, "system", 2);

        // type
        checkSimpleFieldDiff(schemaDiff, "type", "SampleType",
                "OtherSampleType");
        // path
        checkSimpleFieldDiff(schemaDiff, "path", "leftDoc", "rightDoc");

        // ---------------------------
        // Check dublincore schema
        // ---------------------------
        schemaDiff = checkSchemaDiff(docDiff, "dublincore", 6);

        // title => different
        checkSimpleFieldDiff(schemaDiff, "title", "My first sample",
                "My second sample");
        // description => different
        checkSimpleFieldDiff(schemaDiff, "description", "description", null);
        // created => different
        checkSimpleFieldDiff(schemaDiff, "created", "2011-12-29T11:24:25Z",
                "2011-12-30T12:05:02Z");
        // creator => same
        checkIdenticalField(schemaDiff, "creator");
        // modified => different
        checkSimpleFieldDiff(schemaDiff, "created", "2011-12-29T11:24:25Z",
                "2011-12-30T12:05:02Z");
        // lastContributor => same once trimmed
        checkIdenticalField(schemaDiff, "lastContributor");
        // contributors => different (update) / same / different (add)

        // subjects => same / different (remove)

        // ---------------------------
        // Check simpletypes schema
        // ---------------------------
        schemaDiff = checkSchemaDiff(docDiff, "simpletypes", 4);

        // string => different
        checkSimpleFieldDiff(schemaDiff, "string", "a string property",
                "a different string property");
        // textarea => same
        checkIdenticalField(schemaDiff, "textarea");
        // boolean => different
        checkSimpleFieldDiff(schemaDiff, "boolean",
                String.valueOf(Boolean.TRUE), null);
        // integer => same
        checkIdenticalField(schemaDiff, "integer");
        // date => same
        checkIdenticalField(schemaDiff, "date");
        // htmlText => different
        checkSimpleFieldDiff(
                schemaDiff,
                "htmlText",
                "&lt;p&gt;html text with &lt;strong&gt;&lt;span style=\"text-decoration: underline;\"&gt;styles&lt;/span&gt;&lt;/strong&gt;&lt;/p&gt;\n&lt;ul&gt;\n&lt;li&gt;and&lt;/li&gt;\n&lt;li&gt;nice&lt;/li&gt;\n&lt;li&gt;bullets&lt;/li&gt;\n&lt;/ul&gt;\n&lt;p&gt;&amp;nbsp;&lt;/p&gt;",
                "&lt;p&gt;html  text modified with &lt;span style=\"text-decoration: underline;\"&gt;styles&lt;/span&gt;&lt;/p&gt;\n&lt;ul&gt;\n&lt;li&gt;and&lt;/li&gt;\n&lt;li&gt;nice&lt;/li&gt;\n&lt;li&gt;bullets&lt;/li&gt;\n&lt;/ul&gt;\n&lt;p&gt;&amp;nbsp;&lt;/p&gt;");
        // multivalued => different

    }

    /**
     * Creates the left doc.
     * 
     * @return the document model
     * @throws ClientException the client exception
     */
    protected final DocumentModel createLeftDoc() throws ClientException {

        DocumentModel doc = session.createDocumentModel("/", "leftDoc",
                "SampleType");

        // -----------------------
        // dublincore
        // -----------------------
        doc.setPropertyValue("dc:title", "My first sample");
        doc.setPropertyValue("dc:description", "description");
        doc.setPropertyValue("dc:created", "2011-12-29T11:24:25Z");
        doc.setPropertyValue("dc:creator", "Administrator");
        doc.setPropertyValue("dc:modified", "2011-12-29T11:24:25Z");
        doc.setPropertyValue("dc:lastContributor", "Administrator");
        doc.setPropertyValue("dc:contributors", new String[] { "Administrator",
                "joe" });
        doc.setPropertyValue("dc:subjects", new String[] { "Art",
                "Architecture" });

        // -----------------------
        // simpletypes
        // -----------------------
        doc.setPropertyValue("st:string", "a string property");
        doc.setPropertyValue("st:textarea", "a textarea property");
        doc.setPropertyValue("st:boolean", true);
        doc.setPropertyValue("st:integer", 10);
        doc.setPropertyValue("st:date", "2011-12-28T23:00:00Z");
        doc.setPropertyValue(
                "st:htmlText",
                "&lt;p&gt;html text with &lt;strong&gt;&lt;span style=\"text-decoration: underline;\"&gt;styles&lt;/span&gt;&lt;/strong&gt;&lt;/p&gt;\n&lt;ul&gt;\n&lt;li&gt;and&lt;/li&gt;\n&lt;li&gt;nice&lt;/li&gt;\n&lt;li&gt;bullets&lt;/li&gt;\n&lt;/ul&gt;\n&lt;p&gt;&amp;nbsp;&lt;/p&gt;");
        doc.setPropertyValue("st:multivalued", new String[] { "monday",
                "tuesday", "wednesday", "thursday" });

        return session.createDocument(doc);
    }

    /**
     * Creates the right doc.
     * 
     * @return the document model
     * @throws ClientException the client exception
     */
    protected final DocumentModel createRightDoc() throws ClientException {

        DocumentModel doc = session.createDocumentModel("/", "rightDoc",
                "OtherSampleType");

        // -----------------------
        // dublincore
        // -----------------------
        // different
        doc.setPropertyValue("dc:title", "My second sample");
        // no description => different
        // different
        doc.setPropertyValue("dc:created", "2011-12-30T12:05:02Z");
        // same
        doc.setPropertyValue("dc:creator", "Administrator");
        // different
        doc.setPropertyValue("dc:modified", "2011-12-30T12:05:02Z");
        // same once trimmed
        doc.setPropertyValue("dc:lastContributor", " Administrator ");
        // different (update) / same / different (add)
        doc.setPropertyValue("dc:contributors", new String[] {
                "anotherAdministrator", "joe", "jack" });
        // same / different (remove)
        doc.setPropertyValue("dc:subjects", new String[] { "Art" });

        // -----------------------
        // simpletypes
        // -----------------------
        // different
        doc.setPropertyValue("st:string", "a different string property");
        // same
        doc.setPropertyValue("st:textarea", "a textarea property");
        // no boolean => different
        // same
        doc.setPropertyValue("st:integer", 10);
        // same
        doc.setPropertyValue("st:date", "2011-12-28T23:00:00Z");
        // different
        doc.setPropertyValue(
                "st:htmlText",
                "&lt;p&gt;html  text modified with &lt;span style=\"text-decoration: underline;\"&gt;styles&lt;/span&gt;&lt;/p&gt;\n&lt;ul&gt;\n&lt;li&gt;and&lt;/li&gt;\n&lt;li&gt;nice&lt;/li&gt;\n&lt;li&gt;bullets&lt;/li&gt;\n&lt;/ul&gt;\n&lt;p&gt;&amp;nbsp;&lt;/p&gt;");
        // no multivalued => different

        return session.createDocument(doc);
    }

    /**
     * Checks a schema diff.
     * 
     * @param docDiff the doc diff
     * @param schema the schema
     * @param expectedFieldCount the expected field count
     * @return the schema diff
     */
    protected final SchemaDiff checkSchemaDiff(DocumentDiff docDiff,
            String schema, int expectedFieldCount) {

        SchemaDiff schemaDiff = docDiff.getSchemaDiff(schema);
        assertNotNull("Schema diff should not be null", schemaDiff);
        assertEquals("Wrong field count.", expectedFieldCount,
                schemaDiff.getFieldCount());

        return schemaDiff;
    }

    /**
     * Check identical field.
     * 
     * @param schemaDiff the schema diff
     * @param field the field
     */
    protected final void checkIdenticalField(SchemaDiff schemaDiff, String field) {

        PropertyDiff fieldDiff = schemaDiff.getFieldDiff(field);
        assertNull("Field diff should be null", fieldDiff);
    }

    /**
     * Checks a field diff.
     * 
     * @param schemaDiff the schema diff
     * @param field the field
     * @param expectedLeftValue the expected left value
     * @param expectedRightValue the expected right value
     * @return the property diff
     */
    protected final PropertyDiff checkSimpleFieldDiff(SchemaDiff schemaDiff,
            String field, String expectedLeftValue, String expectedRightValue) {

        PropertyDiff fieldDiff = schemaDiff.getFieldDiff(field);
        assertNotNull("Field diff should not be null", fieldDiff);
        assertTrue("Wrong PropertyDiff implementation.",
                fieldDiff instanceof SimplePropertyDiff);

        assertEquals("Wrong left value.", expectedLeftValue,
                ((SimplePropertyDiff) fieldDiff).getLeftValue());
        assertEquals("Wrong right value.", expectedRightValue,
                ((SimplePropertyDiff) fieldDiff).getRightValue());

        return fieldDiff;
    }

    /**
     * Creates an XML export temp file.
     * 
     * @param doc the doc
     * @throws ClientException the client exception
     */
    protected final void createXMLExportTempFile(DocumentModel doc)
            throws ClientException {

        DocumentXMLExporter docXMLExporter = docDiffService.getDocumentXMLExporter();
        byte[] xmlExportByteArray = docXMLExporter.exportXMLAsByteArray(doc,
                session);

        File tempDir = new File("target/classes");
        File tempFile;
        OutputStream fos = null;
        try {
            tempFile = File.createTempFile("export_" + doc.getName() + "_",
                    ".xml", tempDir);
            fos = new FileOutputStream(tempFile);
            fos.write(xmlExportByteArray);
        } catch (IOException ioe) {
            throw ClientException.wrap(ioe);
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException ioe) {
                    throw ClientException.wrap(ioe);
                }
            }
        }
    }

}