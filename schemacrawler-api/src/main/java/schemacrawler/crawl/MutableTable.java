/*
 *
 * SchemaCrawler
 * http://sourceforge.net/projects/schemacrawler
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

package schemacrawler.crawl;


import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import schemacrawler.schema.Column;
import schemacrawler.schema.ForeignKey;
import schemacrawler.schema.ForeignKeyColumnReference;
import schemacrawler.schema.Index;
import schemacrawler.schema.IndexColumn;
import schemacrawler.schema.NamedObject;
import schemacrawler.schema.Privilege;
import schemacrawler.schema.Schema;
import schemacrawler.schema.Table;
import schemacrawler.schema.TableConstraint;
import schemacrawler.schema.TableReference;
import schemacrawler.schema.TableRelationshipType;
import schemacrawler.schema.TableType;
import schemacrawler.schema.Trigger;
import schemacrawler.utility.NamedObjectSort;

/**
 * Represents a table in the database.
 *
 * @author Sualeh Fatehi
 */
class MutableTable
  extends AbstractDatabaseObject
  implements Table
{

  private enum TableAssociationType
  {

    all,
    exported,
    imported;
  }

  private static final long serialVersionUID = 3257290248802284852L;

  private TableType tableType = TableType.UNKNOWN; // Default value
  private MutablePrimaryKey primaryKey;
  private final NamedObjectList<MutableColumn> columns = new NamedObjectList<>();
  private final NamedObjectList<MutableForeignKey> foreignKeys = new NamedObjectList<>();
  private final NamedObjectList<MutableIndex> indices = new NamedObjectList<>();
  private final NamedObjectList<MutableTableConstraint> constraints = new NamedObjectList<>();
  private final NamedObjectList<MutableTrigger> triggers = new NamedObjectList<>();
  private final NamedObjectList<MutablePrivilege<Table>> privileges = new NamedObjectList<>();
  private int sortIndex;
  private final StringBuilder definition;

  MutableTable(final Schema schema, final String name)
  {
    super(schema, name);
    definition = new StringBuilder();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(final NamedObject obj)
  {
    if (obj == null)
    {
      return -1;
    }

    int comparison = 0;

    if (comparison == 0 && obj instanceof MutableTable)
    {
      comparison = sortIndex - ((MutableTable) obj).sortIndex;
    }
    if (comparison == 0)
    {
      comparison = super.compareTo(obj);
    }

    return comparison;
  }

  /**
   * {@inheritDoc}
   *
   * @see schemacrawler.schema.Table#getColumn(java.lang.String)
   */
  @Override
  public MutableColumn getColumn(final String name)
  {
    return columns.lookup(this, name);
  }

  /**
   * {@inheritDoc}
   *
   * @see Table#getColumns()
   */
  @Override
  public List<Column> getColumns()
  {
    return new ArrayList<Column>(columns.values());
  }

  /**
   * {@inheritDoc}
   *
   * @see schemacrawler.schema.View#getDefinition()
   */
  @Override
  public String getDefinition()
  {
    return definition.toString();
  }

  /**
   * {@inheritDoc}
   *
   * @see schemacrawler.schema.Table#getExportedForeignKeys()
   */
  @Override
  public Collection<ForeignKey> getExportedForeignKeys()
  {
    return getForeignKeys(TableAssociationType.exported);
  }

  /**
   * {@inheritDoc}
   *
   * @see schemacrawler.schema.Table#getForeignKey(java.lang.String)
   */
  @Override
  public MutableForeignKey getForeignKey(final String name)
  {
    return foreignKeys.lookup(this, name);
  }

  /**
   * {@inheritDoc}
   *
   * @see schemacrawler.schema.Table#getForeignKeys()
   */
  @Override
  public Collection<ForeignKey> getForeignKeys()
  {
    return getForeignKeys(TableAssociationType.all);
  }

  @Override
  public Collection<ForeignKey> getImportedForeignKeys()
  {
    return getForeignKeys(TableAssociationType.imported);
  }

  /**
   * {@inheritDoc}
   *
   * @see schemacrawler.schema.Table#getIndex(java.lang.String)
   */
  @Override
  public MutableIndex getIndex(final String name)
  {
    return indices.lookup(this, name);
  }

  /**
   * {@inheritDoc}
   *
   * @see Table#getIndices()
   */
  @Override
  public Collection<Index> getIndices()
  {
    return new ArrayList<Index>(indices.values());
  }

  /**
   * {@inheritDoc}
   *
   * @see Table#getPrimaryKey()
   */
  @Override
  public MutablePrimaryKey getPrimaryKey()
  {
    return primaryKey;
  }

  /**
   * {@inheritDoc}
   *
   * @see schemacrawler.schema.Table#getPrivilege(java.lang.String)
   */
  @Override
  public MutablePrivilege<Table> getPrivilege(final String name)
  {
    return privileges.lookup(this, name);
  }

  /**
   * {@inheritDoc}
   *
   * @see Table#getPrivileges()
   */
  @Override
  public Collection<Privilege<Table>> getPrivileges()
  {
    return new ArrayList<Privilege<Table>>(privileges.values());
  }

  /**
   * {@inheritDoc}
   *
   * @see schemacrawler.schema.Table#getRelatedTables(schemacrawler.schema.TableRelationshipType)
   */
  @Override
  public Collection<TableReference> getRelatedTables(final TableRelationshipType tableRelationshipType)
  {
    final Set<TableReference> relatedTables = new HashSet<>();
    if (tableRelationshipType != null
        && tableRelationshipType != TableRelationshipType.none)
    {
      final List<MutableForeignKey> foreignKeysList = new ArrayList<>(foreignKeys
        .values());
      for (final ForeignKey foreignKey: foreignKeysList)
      {
        for (final ForeignKeyColumnReference columnReference: foreignKey
          .getColumnReferences())
        {
          final Table parentTable = columnReference.getPrimaryKeyColumn()
            .getParent();
          final Table childTable = columnReference.getForeignKeyColumn()
            .getParent();
          switch (tableRelationshipType)
          {
            case parent:
              if (equals(childTable))
              {
                relatedTables.add(parentTable);
              }
              break;
            case child:
              if (equals(parentTable))
              {
                relatedTables.add(childTable);
              }
              break;
            default:
              break;
          }
        }
      }
    }

    final List<TableReference> relatedTablesList = new ArrayList<>(relatedTables);
    Collections.sort(relatedTablesList, NamedObjectSort.alphabetical);
    return relatedTablesList;
  }

  /**
   * {@inheritDoc}
   *
   * @see Table#getTableConstraints()
   */
  @Override
  public Collection<TableConstraint> getTableConstraints()
  {
    return new ArrayList<TableConstraint>(constraints.values());
  }

  /**
   * {@inheritDoc}
   *
   * @see Table#getTableType()
   */
  @Override
  public TableType getTableType()
  {
    return tableType;
  }

  /**
   * {@inheritDoc}
   *
   * @see schemacrawler.schema.Table#getTrigger(java.lang.String)
   */
  @Override
  public MutableTrigger getTrigger(final String name)
  {
    return lookupTrigger(name);
  }

  /**
   * {@inheritDoc}
   *
   * @see Table#getTriggers()
   */
  @Override
  public Collection<Trigger> getTriggers()
  {
    return new ArrayList<Trigger>(triggers.values());
  }

  /**
   * {@inheritDoc}
   *
   * @see schemacrawler.schema.TypedObject#getType()
   */
  @Override
  public final TableType getType()
  {
    return getTableType();
  }

  @Override
  public boolean hasDefinition()
  {
    return definition.length() > 0;
  }

  void addColumn(final MutableColumn column)
  {
    columns.add(column);
  }

  void addForeignKey(final MutableForeignKey foreignKey)
  {
    foreignKeys.add(foreignKey);
  }

  void addIndex(final MutableIndex index)
  {
    indices.add(index);
  }

  void addPrivilege(final MutablePrivilege<Table> privilege)
  {
    privileges.add(privilege);
  }

  void addTableConstraint(final MutableTableConstraint tableConstraint)
  {
    constraints.add(tableConstraint);
  }

  void addTrigger(final MutableTrigger trigger)
  {
    triggers.add(trigger);
  }

  void appendDefinition(final String definition)
  {
    if (definition != null)
    {
      this.definition.append(definition);
    }
  }

  int getSortIndex()
  {
    return sortIndex;
  }

  /**
   * Looks up a trigger by name.
   *
   * @param triggerName
   *        Trigger name
   * @return Trigger, if found, or null
   */
  MutableTrigger lookupTrigger(final String triggerName)
  {
    return triggers.lookup(this, triggerName);
  }

  void removeForeignKey(final String fullName)
  {
    foreignKeys.remove(fullName);
  }

  void replacePrimaryKey()
  {
    if (primaryKey == null)
    {
      return;
    }

    final String primaryKeyName = primaryKey.getName();
    final MutableIndex index = indices.lookup(this, primaryKeyName);
    if (index != null)
    {
      boolean indexHasPkColumns = false;
      final List<IndexColumn> pkColumns = primaryKey.getColumns();
      final List<IndexColumn> indexColumns = index.getColumns();
      if (pkColumns.size() == indexColumns.size())
      {
        for (int i = 0; i < indexColumns.size(); i++)
        {
          if (!pkColumns.get(i).equals(indexColumns.get(i)))
          {
            break;
          }
        }
        indexHasPkColumns = true;
      }
      if (indexHasPkColumns)
      {
        indices.remove(index);
        setPrimaryKey(new MutablePrimaryKey(index));
      }
    }
  }

  void setPrimaryKey(final MutablePrimaryKey primaryKey)
  {
    this.primaryKey = primaryKey;
  }

  void setSortIndex(final int sortIndex)
  {
    this.sortIndex = sortIndex;
  }

  void setTableType(final TableType tableType)
  {
    if (tableType == null)
    {
      this.tableType = TableType.UNKNOWN;
    }
    else
    {
      this.tableType = tableType;
    }
  }

  private Collection<ForeignKey> getForeignKeys(final TableAssociationType tableAssociationType)
  {
    final List<ForeignKey> foreignKeysList = new ArrayList<ForeignKey>(foreignKeys
      .values());
    if (tableAssociationType != null
        && tableAssociationType != TableAssociationType.all)
    {
      for (final Iterator<ForeignKey> iterator = foreignKeysList.iterator(); iterator
        .hasNext();)
      {
        final ForeignKey mutableForeignKey = iterator.next();
        boolean isExportedKey = false;
        boolean isImportedKey = false;
        for (final ForeignKeyColumnReference columnReference: mutableForeignKey
          .getColumnReferences())
        {
          if (columnReference.getPrimaryKeyColumn().getParent().equals(this))
          {
            isExportedKey = true;
          }
          if (columnReference.getForeignKeyColumn().getParent().equals(this))
          {
            isImportedKey = true;
          }
        }
        switch (tableAssociationType)
        {
          case exported:
            if (!isExportedKey)
            {
              iterator.remove();
            }
            break;
          case imported:
            if (!isImportedKey)
            {
              iterator.remove();
            }
            break;
          default:
            break;
        }
      }
    }
    return foreignKeysList;
  }

}
