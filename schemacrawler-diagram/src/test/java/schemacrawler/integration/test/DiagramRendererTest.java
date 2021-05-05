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

package schemacrawler.integration.test;

import static java.nio.file.Files.createDirectories;
import static org.apache.commons.io.FileUtils.deleteDirectory;
import static org.hamcrest.MatcherAssert.assertThat;
import static schemacrawler.test.utility.ExecutableTestUtility.hasSameContentAndTypeAs;
import static schemacrawler.test.utility.FileHasContent.classpathResource;
import static schemacrawler.test.utility.FileHasContent.outputOf;
import static schemacrawler.test.utility.TestUtility.clean;
import static schemacrawler.tools.command.text.diagram.options.DiagramOptionsBuilder.builder;

import java.nio.file.Path;
import java.sql.Connection;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import schemacrawler.inclusionrule.RegularExpressionExclusionRule;
import schemacrawler.schema.Catalog;
import schemacrawler.schemacrawler.LimitOptionsBuilder;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder;
import schemacrawler.schemacrawler.SchemaRetrievalOptions;
import schemacrawler.schemacrawler.SchemaRetrievalOptionsBuilder;
import schemacrawler.test.utility.DatabaseTestUtility;
import schemacrawler.test.utility.TestContext;
import schemacrawler.test.utility.TestContextParameterResolver;
import schemacrawler.test.utility.TestDatabaseConnectionParameterResolver;
import schemacrawler.tools.command.text.diagram.DiagramRenderer;
import schemacrawler.tools.command.text.diagram.options.DiagramOptions;
import schemacrawler.tools.command.text.diagram.options.DiagramOptionsBuilder;
import schemacrawler.tools.command.text.diagram.options.DiagramOutputFormat;
import schemacrawler.tools.command.text.embeddeddiagram.EmbeddedDiagramRenderer;
import schemacrawler.tools.command.text.schema.options.SchemaTextDetailType;
import schemacrawler.tools.executable.SchemaCrawlerCommand;
import schemacrawler.tools.options.Config;
import schemacrawler.tools.options.OutputOptionsBuilder;
import schemacrawler.tools.utility.SchemaCrawlerUtility;
import us.fatehi.utility.IOUtility;

@ExtendWith(TestDatabaseConnectionParameterResolver.class)
@ExtendWith(TestContextParameterResolver.class)
public class DiagramRendererTest {

  private static final String DIAGRAM_OUTPUT = "diagram_renderer_output/";
  private static Path directory;

  public static Path commandExecution(
      final Connection connection,
      final SchemaCrawlerCommand<DiagramOptions> scCommand,
      final DiagramOutputFormat outputFormat)
      throws Exception {
    final Path tempFilePath = IOUtility.createTempFilePath("test", "");
    final OutputOptionsBuilder outputOptionsBuilder =
        OutputOptionsBuilder.builder()
            .withOutputFormatValue(outputFormat.getFormat())
            .withOutputFile(tempFilePath);

    scCommand.setOutputOptions(outputOptionsBuilder.toOptions());
    scCommand.setConnection(connection);

    final SchemaRetrievalOptions schemaRetrievalOptions =
        SchemaCrawlerUtility.matchSchemaRetrievalOptions(connection);
    scCommand.setIdentifiers(schemaRetrievalOptions.getIdentifiers());

    // Initialize, and check if the command is available
    scCommand.initialize();
    scCommand.checkAvailability();

    scCommand.execute();

    return tempFilePath;
  }

  @BeforeAll
  public static void removeOutputDir() throws Exception {
    clean(DIAGRAM_OUTPUT);
  }

  @BeforeAll
  public static void setupDirectory(final TestContext testContext) throws Exception {
    directory =
        testContext.resolveTargetFromRootPath(
            "test-output-diagrams/" + DiagramOutputTest.class.getSimpleName());
    deleteDirectory(directory.toFile());
    createDirectories(directory);
  }

  private static void commandDiagram(
      final SchemaCrawlerCommand<DiagramOptions> scCommand,
      final Connection connection,
      final Catalog catalog,
      final DiagramOptions diagramOptions,
      final DiagramOutputFormat diagramOutputFormat,
      final String testMethodName)
      throws Exception {

    scCommand.setCommandOptions(diagramOptions);
    scCommand.setSchemaCrawlerOptions(SchemaCrawlerOptionsBuilder.newSchemaCrawlerOptions());

    scCommand.setCatalog(catalog);

    // Check output file
    final String referenceFileName = testMethodName;
    assertThat(
        outputOf(commandExecution(connection, scCommand, diagramOutputFormat)),
        hasSameContentAndTypeAs(
            classpathResource(
                DIAGRAM_OUTPUT + referenceFileName + "." + diagramOutputFormat.getFormat()),
            diagramOutputFormat));
  }

  private static Catalog getCatalog(final Connection connection) throws SchemaCrawlerException {
    SchemaCrawlerOptions schemaCrawlerOptions =
        DatabaseTestUtility.schemaCrawlerOptionsWithMaximumSchemaInfoLevel;
    final LimitOptionsBuilder limitOptionsBuilder =
        LimitOptionsBuilder.builder()
            .fromOptions(schemaCrawlerOptions.getLimitOptions())
            .includeSchemas(new RegularExpressionExclusionRule(".*\\.SYSTEM_LOBS|.*\\.FOR_LINT"));
    schemaCrawlerOptions = schemaCrawlerOptions.withLimitOptions(limitOptionsBuilder.toOptions());

    SchemaRetrievalOptions schemaRetrievalOptions =
        SchemaCrawlerUtility.matchSchemaRetrievalOptions(connection);
    schemaRetrievalOptions =
        SchemaRetrievalOptionsBuilder.builder(schemaRetrievalOptions).toOptions();

    final Catalog catalog =
        SchemaCrawlerUtility.getCatalog(
            connection, schemaRetrievalOptions, schemaCrawlerOptions, new Config());
    return catalog;
  }

  @Test
  public void diagramRenderer_graphviz(final TestContext testContext, final Connection connection)
      throws Exception {

    final DiagramOptionsBuilder diagramOptionsBuilder = builder();
    final DiagramOptions diagramOptions = diagramOptionsBuilder.toOptions();

    final Catalog catalog = getCatalog(connection);

    commandDiagram(
        new DiagramRenderer(SchemaTextDetailType.details.name()),
        connection,
        catalog,
        diagramOptions,
        DiagramOutputFormat.canon,
        testContext.testMethodName());
  }

  @Test
  public void embeddedDiagramRenderer_graphviz(
      final TestContext testContext, final Connection connection) throws Exception {

    final DiagramOptionsBuilder diagramOptionsBuilder = builder();
    final DiagramOptions diagramOptions = diagramOptionsBuilder.toOptions();

    final Catalog catalog = getCatalog(connection);

    commandDiagram(
        new EmbeddedDiagramRenderer(SchemaTextDetailType.details.name()),
        connection,
        catalog,
        diagramOptions,
        DiagramOutputFormat.htmlx,
        testContext.testMethodName());
  }
}
