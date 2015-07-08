/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/

package org.codice.ddf.spatial.geo.indexer;

import static org.apache.lucene.index.IndexWriter.MaxFieldLength;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.codice.ddf.spatial.geo.GeoIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public class GeoNamesIndexer implements GeoIndexer {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeoNamesIndexer.class);

    private static final PrintStream CONSOLE = System.out;

    private String indexLocation;

    public void setIndexLocation(final String indexLocation) {
        this.indexLocation = indexLocation;
    }

    @Override
    public void updateIndex(final String fileLocation, final boolean create) {
        final String inputFileLocation = getInputFileLocation(fileLocation);

        if (inputFileLocation != null) {
            buildIndex(inputFileLocation, create);
        }
    }

    private String getInputFileLocation(final String fileLocation) {
        if (FilenameUtils.isExtension(fileLocation, "zip")) {
            // The GeoNames .zip files at http://download.geonames.org/export/dump each contain
            // a text file with the same name.
            final String baseName = FilenameUtils.getBaseName(fileLocation);
            final String textFileName = baseName + ".txt";
            try {
                unzipFile(fileLocation, textFileName);
                return FilenameUtils.getFullPath(fileLocation) + textFileName;
            } catch (ZipException e) {
                CONSOLE.printf("Couldn't unzip %s from %s\n", textFileName, fileLocation);
                return null;
            }
        } else if (FilenameUtils.isExtension(fileLocation, "txt")) {
            return fileLocation;
        }

        CONSOLE.println("Input must be a .txt or a .zip.");
        return null;
    }

    private void unzipFile(final String zipFileLocation, final String textFileName)
            throws ZipException {
        final ZipFile zipFile = new ZipFile(zipFileLocation);
        zipFile.extractFile(textFileName, FilenameUtils.getFullPath(zipFileLocation));
    }

    private void buildIndex(final String inputFilesLocation, final boolean create) {
        CONSOLE.println("Building index...");

        Directory directory;

        try {
            directory = FSDirectory.open(new File(indexLocation));
        } catch (IOException e) {
            CONSOLE.printf("Couldn't open the directory for the index, %s\n", indexLocation);
            LOGGER.error("Error opening the directory for the GeoNames index: {}",
                    indexLocation, e);
            return;
        }

        final StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_30);
        // Try-with-resources to ensure the IndexWriter always gets closed.
        try (final IndexWriter indexWriter = new IndexWriter(directory, analyzer, create,
                MaxFieldLength.LIMITED)) {
            try {
                indexDocumentsInFile(indexWriter, inputFilesLocation);
            } catch (ArrayIndexOutOfBoundsException | IOException e) {
                // Need to roll back here before the IndexWriter is closed at the end of the try
                // block.
                indexWriter.rollback();
                throw e;
            }
        } catch (ArrayIndexOutOfBoundsException | IOException e) {
            CONSOLE.println("Index update not successful.");
            LOGGER.error("Error updating the GeoNames index", e);
            return;
        }

        CONSOLE.println("Index update successful.");
    }

    private void indexDocumentsInFile(final IndexWriter indexWriter,
            final String inputTextFileLocation) throws IOException {
        CONSOLE.println("Indexing " + inputTextFileLocation);

        try (final BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(inputTextFileLocation)));
             final LineNumberReader lineNumberReader = new LineNumberReader(
                new InputStreamReader(new FileInputStream(inputTextFileLocation)))) {
            // Use the number of lines in the file to track the indexing progress.
            lineNumberReader.skip(Long.MAX_VALUE);
            final int lineCount = lineNumberReader.getLineNumber() + 1;

            int currentLine = 0;
            float progressPercentage = 0.0f;
            for (String line; (line = reader.readLine()) != null;) {
                // Passing a negative value to preserve empty fields.
                final String[] fields = line.split("\\t", -1);
                addDocument(indexWriter, fields);
                if (currentLine == (int) (progressPercentage * lineCount)) {
                    CONSOLE.printf("\r%.0f%%", progressPercentage * 100);
                    CONSOLE.flush();
                    progressPercentage += 0.05f;
                }
                ++currentLine;
            }
            CONSOLE.println("\rIndexed " + inputTextFileLocation);
        } catch (ArrayIndexOutOfBoundsException e) {
            CONSOLE.println(inputTextFileLocation + " does not follow the format specified here: " +
                            "http://download.geonames.org/export/dump/readme.txt");
            LOGGER.info("GeoNames text file is missing a field", e);
            throw e;
        } catch (FileNotFoundException e) {
            CONSOLE.println("Couldn't find " + inputTextFileLocation);
            throw e;
        } catch (IOException e) {
            CONSOLE.println("Couldn't add the contents of " + inputTextFileLocation +
                    " to the index");
            LOGGER.error("Error adding GeoNames text file to the index", e);
            throw e;
        }
    }

    private void addDocument(final IndexWriter indexWriter, final String[] fields)
            throws IOException {
        final Document document = new Document();
        document.add(new Field("id", fields[0], Field.Store.YES, Field.Index.NOT_ANALYZED));
        document.add(new Field("name", fields[1], Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field("ascii_name", fields[2], Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field(
                "alternate_names", fields[3], Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field("latitude", fields[4], Field.Store.YES, Field.Index.NOT_ANALYZED));
        document.add(new Field("longitude", fields[5], Field.Store.YES, Field.Index.NOT_ANALYZED));
        document.add(new Field("feature_class", fields[6], Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field("feature_code", fields[7], Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field("country_code", fields[8], Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field(
                "alternate_country_codes", fields[9], Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field("admin_1_code", fields[10], Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field("admin_2_code", fields[11], Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field("admin_3_code", fields[12], Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field("admin_4_code", fields[13], Field.Store.YES, Field.Index.ANALYZED));
        document.add(new Field(
                "population", fields[14], Field.Store.YES, Field.Index.NOT_ANALYZED));
        document.add(new Field("elevation", fields[15], Field.Store.YES, Field.Index.NOT_ANALYZED));
        document.add(new Field("dem", fields[16], Field.Store.YES, Field.Index.NOT_ANALYZED));
        document.add(new Field("time_zone", fields[17], Field.Store.YES, Field.Index.ANALYZED));
        try {
            indexWriter.addDocument(document);
        } catch (IOException e) {
            LOGGER.error("Error adding document to the index", e);
            throw e;
        }
    }
}
