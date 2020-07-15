package schemacrawler.test;


import static java.io.File.createTempFile;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.sql.Connection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import schemacrawler.inclusionrule.RegularExpressionInclusionRule;
import schemacrawler.schemacrawler.LimitOptionsBuilder;
import schemacrawler.schemacrawler.LoadOptionsBuilder;
import schemacrawler.schemacrawler.SchemaCrawlerException;
import schemacrawler.schemacrawler.SchemaCrawlerOptions;
import schemacrawler.schemacrawler.SchemaCrawlerOptionsBuilder;
import schemacrawler.schemacrawler.SchemaInfoLevelBuilder;
import schemacrawler.test.utility.TestDatabaseConnectionParameterResolver;
import schemacrawler.tools.executable.SchemaCrawlerExecutable;
import schemacrawler.tools.options.OutputOptions;
import schemacrawler.tools.options.OutputOptionsBuilder;

@ExtendWith(TestDatabaseConnectionParameterResolver.class)
public class Issue342Test
{

  @Test
  public void unsupportedOutputFormat(final Connection connection)
    throws Exception
  {
    // Create the options
    final LimitOptionsBuilder limitOptionsBuilder = LimitOptionsBuilder
      .builder()
      .includeSchemas(new RegularExpressionInclusionRule("PUBLIC.BOOKS"));
    final LoadOptionsBuilder loadOptionsBuilder = LoadOptionsBuilder.builder()
      // Set what details are required in the schema - this affects the
      // time taken to crawl the schema
      .withSchemaInfoLevel(SchemaInfoLevelBuilder.standard());
    final SchemaCrawlerOptionsBuilder optionsBuilder =
      SchemaCrawlerOptionsBuilder
        .builder()
        .withLimitOptionsBuilder(limitOptionsBuilder)
        .withLoadOptionsBuilder(loadOptionsBuilder);
    final SchemaCrawlerOptions options = optionsBuilder.toOptions();

    final Path outputFile = createTempFile("schemacrawler", ".dat").toPath();
    final OutputOptions outputOptions =
      OutputOptionsBuilder.newOutputOptions("json", outputFile);
    final String command = "schema";

    final SchemaCrawlerExecutable executable =
      new SchemaCrawlerExecutable(command);
    executable.setSchemaCrawlerOptions(options);
    executable.setOutputOptions(outputOptions);
    executable.setConnection(connection);

    final SchemaCrawlerException exception =
      assertThrows(SchemaCrawlerException.class, () -> executable.execute());
    assertThat(exception.getMessage(),
               is("Output format <json> not supported for command <schema>"));
  }

}
