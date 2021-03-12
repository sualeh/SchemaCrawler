/*
========================================================================
SchemaCrawler
http://www.schemacrawler.com
Copyright (c) 2000-2021, Sualeh Fatehi <sualeh@hotmail.com>.
All rights reserved.
------------------------------------------------------------------------

SchemaCrawler is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

SchemaCrawler and the accompanying materials are made available under
the terms of the Eclipse Public License v1.0, GNU General Public License
v3 or GNU Lesser General Public License v3.

You may elect to redistribute this code under any of these licenses.

The Eclipse Public License is available at:
http://www.eclipse.org/legal/epl-v10.html

The GNU General Public License v3 and the GNU Lesser General Public
License v3 are available at:
http://www.gnu.org/licenses/

========================================================================
*/

package schemacrawler.loader.attributes;

import static schemacrawler.loader.attributes.model.CatalogAttributesUtility.readCatalogAttributes;
import static us.fatehi.utility.Utility.isBlank;

import java.util.Map.Entry;
import java.util.Optional;
import java.util.logging.Level;

import schemacrawler.SchemaCrawlerLogger;
import schemacrawler.crawl.WeakAssociationBuilder;
import schemacrawler.crawl.WeakAssociationBuilder.WeakAssociationColumn;
import schemacrawler.loader.attributes.model.CatalogAttributes;
import schemacrawler.loader.attributes.model.ColumnAttributes;
import schemacrawler.loader.attributes.model.TableAttributes;
import schemacrawler.loader.attributes.model.WeakAssociationAttributes;
import schemacrawler.schema.Catalog;
import schemacrawler.schema.Table;
import schemacrawler.schema.WeakAssociation;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.tools.catalogloader.BaseCatalogLoader;
import schemacrawler.tools.executable.CommandDescription;
import schemacrawler.tools.executable.commandline.PluginCommand;
import schemacrawler.tools.options.Config;
import us.fatehi.utility.StopWatch;
import us.fatehi.utility.ioresource.InputResource;
import us.fatehi.utility.ioresource.InputResourceUtility;
import us.fatehi.utility.string.StringFormat;

public class AttributesCatalogLoader extends BaseCatalogLoader {

  private static final SchemaCrawlerLogger LOGGER =
      SchemaCrawlerLogger.getLogger(AttributesCatalogLoader.class.getName());

  private static final String OPTION_ATTRIBUTES_FILE = "attributes-file";

  public AttributesCatalogLoader() {
    super(
        new CommandDescription(
            "attributesloader", "Loader for catalog attributes, such as remarks or tags"),
        2);
  }

  @Override
  public PluginCommand getCommandLineCommand() {
    final CommandDescription commandDescription = getCommandDescription();
    final PluginCommand pluginCommand =
        PluginCommand.newCatalogLoaderCommand(
            commandDescription.getName(), commandDescription.getDescription());
    pluginCommand.addOption(
        OPTION_ATTRIBUTES_FILE,
        String.class,
        "Path to a YAML file with table and column attributes to add to the schema");
    return pluginCommand;
  }

  @Override
  public void loadCatalog() throws SchemaCrawlerException {
    if (!isLoaded()) {
      return;
    }

    LOGGER.log(Level.INFO, "Retrieving catalog attributes");
    final StopWatch stopWatch = new StopWatch("loadTableRowCounts");
    try {
      final Catalog catalog = getCatalog();
      final Config config = getAdditionalConfiguration();
      stopWatch.time(
          "retrieveCatalogAttributes",
          () -> {
            final String catalogAttributesFile = config.getObject(OPTION_ATTRIBUTES_FILE, null);
            if (isBlank(catalogAttributesFile)) {
              return null;
            }
            final InputResource inputResource =
                InputResourceUtility.createInputResource(catalogAttributesFile)
                    .orElseThrow(
                        () ->
                            new SchemaCrawlerException(
                                "Cannot locate catalog attributes file, " + catalogAttributesFile));
            final CatalogAttributes catalogAttributes = readCatalogAttributes(inputResource);
            loadRemarks(catalog, catalogAttributes);
            loadWeakAssociations(catalog, catalogAttributes);

            return null;
          });

      LOGGER.log(Level.INFO, stopWatch.stringify());
    } catch (final Exception e) {
      throw new SchemaCrawlerException("Exception loading catalog attributes", e);
    }
  }

  private void loadRemarks(final Catalog catalog, final CatalogAttributes catalogAttributes) {
    for (final TableAttributes tableAttributes : catalogAttributes.getTables()) {
      final Optional<Table> lookupTable =
          catalog.lookupTable(tableAttributes.getSchema(), tableAttributes.getName());
      final Table table;
      if (lookupTable.isPresent()) {
        table = lookupTable.get();
      } else {
        LOGGER.log(Level.CONFIG, new StringFormat("%s not found", tableAttributes));
        continue;
      }

      if (tableAttributes.hasRemarks()) {
        table.setRemarks(tableAttributes.getRemarks());
      }

      for (final ColumnAttributes columnAttributes : tableAttributes) {
        if (columnAttributes.hasRemarks()) {
          table
              .lookupColumn(columnAttributes.getName())
              .ifPresent(column -> column.setRemarks(columnAttributes.getRemarks()));
        }
      }
    }
  }

  private void loadWeakAssociations(
      final Catalog catalog, final CatalogAttributes catalogAttributes) {
    for (final WeakAssociationAttributes weakAssociationAttributes :
        catalogAttributes.getWeakAssociations()) {

      final TableAttributes pkTableAttributes = weakAssociationAttributes.getReferencedTable();
      final TableAttributes fkTableAttributes = weakAssociationAttributes.getReferencingTable();

      final WeakAssociationBuilder weakAssociationBuilder = WeakAssociationBuilder.builder(catalog);

      for (final Entry<String, String> entry :
          weakAssociationAttributes.getColumnReferences().entrySet()) {
        final String pkColumnName = entry.getKey();
        final String fkColumnName = entry.getValue();

        final WeakAssociationColumn pkColumn =
            new WeakAssociationColumn(
                pkTableAttributes.getSchema(), pkTableAttributes.getName(), pkColumnName);
        final WeakAssociationColumn fkColumn =
            new WeakAssociationColumn(
                fkTableAttributes.getSchema(), fkTableAttributes.getName(), fkColumnName);

        weakAssociationBuilder.addColumnReference(pkColumn, fkColumn);
      }

      final WeakAssociation weakAssociation =
          weakAssociationBuilder.build(weakAssociationAttributes.getName());
      if (weakAssociation == null) {
        continue;
      }

      weakAssociation.setRemarks(weakAssociationAttributes.getRemarks());
      for (final Entry<String, String> attribute :
          weakAssociationAttributes.getAttributes().entrySet()) {
        weakAssociation.setAttribute(attribute.getKey(), attribute.getValue());
      }
    }
  }
}