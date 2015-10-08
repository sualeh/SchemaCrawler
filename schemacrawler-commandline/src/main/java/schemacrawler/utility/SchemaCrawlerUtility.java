/*
 *
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2015, Sualeh Fatehi.
 *
 * This library is free software; you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License as published by the Free Software Foundation;
 * either version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * library; if not, write to the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA 02111-1307, USA.
 *
 */

package schemacrawler.utility;


import static sf.util.DatabaseUtility.checkConnection;

import java.sql.Connection;
import java.sql.ResultSet;

import static java.util.Objects.requireNonNull;

import schemacrawler.crawl.SchemaCrawler;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.ResultsColumns;
import schemacrawler.schemacrawler.DatabaseSpecificOverrideOptions;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.tools.databaseconnector.DatabaseConnector;
import schemacrawler.tools.databaseconnector.DatabaseConnectorRegistry;

/**
 * SchemaCrawler utility methods.
 *
 * @author sfatehi
 */
public final class SchemaCrawlerUtility
{

  public static Catalog
    getCatalog(final Connection connection,
               final SchemaCrawlerOptions schemaCrawlerOptions)
                 throws SchemaCrawlerException
  {
    checkConnection(connection);

    final DatabaseConnectorRegistry registry = new DatabaseConnectorRegistry();
    final DatabaseConnector dbConnector = registry
      .lookupDatabaseSystemIdentifier(connection);

    checkConnection(connection);
    requireNonNull(dbConnector,
                   "No database specific override options provided");

    final DatabaseSpecificOverrideOptions dbSpecificOverrideOptions = dbConnector
      .getDatabaseSpecificOverrideOptionsBuilder().toOptions();
    checkConnection(connection);
    requireNonNull(dbSpecificOverrideOptions,
                   "No database specific override options provided");

    final SchemaCrawler schemaCrawler = new SchemaCrawler(connection,
                                                          dbSpecificOverrideOptions);
    final Catalog catalog = schemaCrawler.crawl(schemaCrawlerOptions);

    return catalog;
  }

  public static ResultsColumns getResultColumns(final ResultSet resultSet)
  {
    return SchemaCrawler.getResultColumns(resultSet);
  }

  private SchemaCrawlerUtility()
  {
    // Prevent instantiation
  }

}
