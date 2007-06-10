/*
// $Id: $
// This software is subject to the terms of the Common Public License
// Agreement, available at the following URL:
// http://www.opensource.org/licenses/cpl.html.
// Copyright (C) 2006-2006 Julian Hyde
// All Rights Reserved.
// You must accept the terms of that agreement to use this software.
*/
package org.olap4j;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

/**
 * Information about an OLAP database.
 *
 * <p>Methods are provided to query the metadata catalog of the database.
 * There is a method for each metadata class, and each method takes zero or more
 * parameters to qualify the instances should be returned, and returns a JDBC
 * {@link java.sql.ResultSet}.
 *
 * <p>For example, {@link #getCubes} returns the description of a cube.
 *
 * @author jhyde
 * @version $Id: $
 * @since Oct 12, 2006
 */
public interface OlapDatabaseMetaData extends DatabaseMetaData {

    /**
     * Retrieves a list of descriptions of an Action.
     *
     * <p>Specification as for XML/A MDSCHEMA_ACTIONS schema rowset.
     *
     * <p>todo: document parameters and result set columns
     */
    ResultSet getActions() throws OlapException;

    /**
     * Retrives a list of olap4j data sources that are available on the server.
     *
     * <p>Specification as for XML/A DISCOVER_DATASOURCES schema rowset.
     *
     * <p>todo: document parameters and result set columns
     */
    ResultSet getDatasources(
        String dataSourceName) throws OlapException;

    /**
     * Retrieves a list of information on supported literals, including data
     * types and values.
     *
     * <p>Specification as for XML/A DISCOVER_LITERALS schema rowset.
     *
     * <p>todo: document parameters and result set columns
     */
    ResultSet getLiterals() throws OlapException;

    /**
     * Retrieves a list of the standard and provider-specific properties
     * supported by an olap4j provider. Properties that are not supported by a
     * provider are not listed in the return result set.
     *
     * <p>Specification as for XML/A DISCOVER_PROPERTIES schema rowset.
     *
     * <p>Not to be confused with {@link #getProperties()}.
     *
     * <p>todo: document parameters and result set columns
     */
    ResultSet getDatabaseProperties(
        String dataSourceName) throws OlapException;

    /**
     * Retrieves a list of descriptions of member and cell Properties.
     *
     * <p>Specification as for XML/A MDSCHEMA_PROPERTIES schema rowset.
     *
     * <p>Not to be confused with {@link #getDatabaseProperties(String)}.
     *
     * <p>todo: document parameters and result set columns
     */
    ResultSet getProperties() throws OlapException;

    /**
     * Retrieves a comma-separated list of all of this database's MDX keywords.
     *
     * @return the list of this database's MDX keywords
     * @exception OlapException if a database access error occurs
     */
    String getMdxKeywords() throws OlapException;

    /**
     * Retrieves a description of a cube.
     *
     * @param catalog a catalog name; must match the catalog name as it
     *        is stored in the database; "" retrieves those without a catalog;
     *        <code>null</code> means that the catalog name should not be used to narrow
     *        the search
     * @param schemaPattern a schema name pattern; must match the schema name
     *        as it is stored in the database; "" retrieves those without a schema;
     *        <code>null</code> means that the schema name should not be used to narrow
     *        the search
     * @param cubeNamePattern a cube name pattern; must match the
     *        cube name as it is stored in the database
     * @return <code>ResultSet</code> - each row is a cube description
     */
    public ResultSet getCubes(
        String catalog,
        String schemaPattern,
        String cubeNamePattern) throws OlapException;

    /**
     * Retrieves a result set describing the shared and private dimensions
     * within a database.
     *
     * <p>Specification as for XML/A MDSCHEMA_DIMENSIONS schema rowset.
     *
     * <p>todo: document parameters and result set columns
     */
    ResultSet getDimensions() throws OlapException;

    /**
     * Retrieves a result set describing the functions available to client
     * applications connected to the database.
     *
     * <p>Specification as for XML/A MDSCHEMA_FUNCTIONS schema rowset.
     *
     * <p>todo: document parameters and result set columns
     */
    ResultSet getFunctions() throws OlapException;

    /**
     * Retrieves a result set describing each hierarchy within a particular
     * dimension.
     *
     * <p>Specification as for XML/A MDSCHEMA_HIERARCHIES schema rowset.
     *
     * <p>todo: document parameters and result set columns
     */
    ResultSet getHierarchies() throws OlapException;

    /**
     * Retrieves a result set describing each measure within a cube.
     *
     * <p>Specification as for XML/A MDSCHEMA_MEASURES schema rowset.
     *
     * <p>todo: document parameters and result set columns
     */
    ResultSet getMeasures() throws OlapException;

    /**
     * Retrieves a result set describing the members within a database.
     *
     * <p>Specification as for XML/A MDSCHEMA_MEMBERS schema rowset.
     *
     * <p>todo: document parameters and result set columns
     */
    ResultSet getMembers() throws OlapException;

    /**
     * Retrieves a result set describing each level within a particular
     * hierarchy.
     *
     * <p>Specification as for XML/A MDSCHEMA_LEVELS schema rowset.
     *
     * <p>todo: document parameters and result set columns
     */
    ResultSet getLevels() throws OlapException;

    /**
     * Retrieves a result set describing each calculated set.
     *
     * <p>Specification as for XML/A MDSCHEMA_SETS schema rowset.
     *
     * <p>todo: document parameters and result set columns
     */
    ResultSet getSets() throws OlapException;
}

// End OlapDatabaseMetaData.java
