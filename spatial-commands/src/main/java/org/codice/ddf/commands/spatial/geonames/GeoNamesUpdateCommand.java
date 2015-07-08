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
import org.apache.felix.gogo.commands.Option;
import org.apache.karaf.shell.console.OsgiCommandSupport;
import org.codice.ddf.spatial.geo.GeoIndexer;

@Command(scope = "geonames", name = "update",
        description = "Creates a local GeoNames index or adds to an existing one.")
public final class GeoNamesUpdateCommand extends OsgiCommandSupport {
    @Argument(index = 0, name = "file_location",
            description = "The absolute path to the GeoNames file whose contents you wish to " +
                "insert into the index.", required = true)
    private String inputLocation = null;

    @Option(name = "-c", aliases = "--create",
            description = "Create a new index or overwrite the existing one.")
    private boolean create;

    private GeoIndexer geoIndexer;

    public void setGeoIndexer(final GeoIndexer geoIndexer) {
        this.geoIndexer = geoIndexer;
    }

    @Override
    protected Object doExecute() {
        geoIndexer.updateIndex(inputLocation, create);
        return null;
    }
}
