/*
 *
 * SchemaCrawler
 * http://www.schemacrawler.com
 * Copyright (c) 2000-2016, Sualeh Fatehi.
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

package schemacrawler.tools.options;


/**
 * Enumeration for text format type.
 */
public enum TextOutputFormat
  implements OutputFormat
{

 text("Plain text format"),
 html("HyperText Markup Language (HTML) format"),
 csv("Comma-separated values (CSV) format"),
 tsv("Tab-separated values (TSV) format"),
 json("JavaScript Object Notation (JSON) format"),;

  public static TextOutputFormat valueOfFromString(final String format)
  {
    TextOutputFormat outputFormat;
    try
    {
      outputFormat = TextOutputFormat.valueOf(format);
    }
    catch (final IllegalArgumentException | NullPointerException e)
    {
      outputFormat = text;
    }
    return outputFormat;
  }

  public static boolean isTextOutputFormat(final String format)
  {
    try
    {
      TextOutputFormat.valueOf(format);
      return true;
    }
    catch (final IllegalArgumentException | NullPointerException e)
    {
      return false;
    }
  }

  private final String description;

  private TextOutputFormat(final String description)
  {
    this.description = description;
  }

  @Override
  public String getDescription()
  {
    return description;
  }

  @Override
  public String getFormat()
  {
    return name();
  }

  @Override
  public String toString()
  {
    return String.format("%s - %s", getFormat(), description);
  }

}
