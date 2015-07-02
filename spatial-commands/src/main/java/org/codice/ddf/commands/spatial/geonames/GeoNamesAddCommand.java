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
 */

package org.codice.ddf.commands.spatial.geonames;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

@Command(scope = "geonames", name = "add",
        description = "Adds to the local GeoNames index, if it exists.")
public final class GeoNamesAddCommand extends OsgiCommandSupport {
    @Argument(index = 0, name = "file_location",
            description = "Location of the GeoNames .txt file or directory containing GeoNames " +
                    ".txt files.\n" +
                    "If it's a directory, the contents of all the .txt files inside will be " +
                    "added.\n" +
                    "The files to add should be obtained from " +
                    "http://download.geonames.org/export/dump and have the format described " +
                    "there.", required = true)
    private String fileLocation = null;

    @Override
    protected Object doExecute() {
        final String inputFilesLocation = GeoNamesCreateCommand
                .getInputFilesLocation(fileLocation);

        if (inputFilesLocation != null) {
            GeoNamesCreateCommand.buildIndex(inputFilesLocation, false);
        }

        return null;
    }
}
