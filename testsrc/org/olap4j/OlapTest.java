/*
// This software is subject to the terms of the Common Public License
// Agreement, available at the following URL:
// http://www.opensource.org/licenses/cpl.html.
// Copyright (C) 2007-2007 Julian Hyde
// All Rights Reserved.
// You must accept the terms of that agreement to use this software.
*/
package org.olap4j;

import org.olap4j.metadata.*;
import org.olap4j.query.*;

import org.w3c.dom.*;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.dom.DOMSource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.io.StringWriter;

/**
 * Unit test illustrating sequence of calls to olap4j API from a graphical
 * client.
 *
 * @since  May 22, 2007
 * @author James Dixon
 * @version $Id$
 */
public class OlapTest {

    public OlapTest() {
        super();
    }

    public void testModel() {

        try {

            // define the connection information
            String schemaUri = "file:/open/mondrian/demo/FoodMart.xml";
            String schemaName = "FoodMart";
            String userName = "foodmartuser";
            String password = "foodmartpassword";
            String jdbc = "jdbc:mysql://localhost/foodmart?user=foodmartuser&password=foodmartpassword";

            // Create a connection object to the specific implementation of an olap4j source
            // This is the only provider-specific code
            Class.forName("mondrian.olap4j.MondrianOlap4jDriver");
            Connection jdbcConnection = DriverManager.getConnection(
                "jdbc:mondrian:Jdbc=" + jdbc
                    + ";User=" + userName
                    + ";Password=" + password
                    + ";Catalog=" + schemaUri);
            OlapConnection connection =
                ((OlapWrapper) jdbcConnection).unwrap(OlapConnection.class);

            // REVIEW: jhyde: Why do you want to name connections? We could add
            // a connect string property 'description', if that helps
//           connection.setName("First Connection");

            // The code from here on is generic olap4j stuff

            // Get a list of the schemas available from this connection and dump their names
            NamedList<Schema> schemas = connection.getCatalogs().get("LOCALDB").getSchemas();
            for (Schema schema : schemas) {
                System.out.println("schema name="+schema.getName());
            }

            if (schemas.size() == 0) {
                // No schemas were present
                return;
            }

            // Use the first schema
            Schema schema = schemas.get(0);
            System.out.println("using schema name=" + schema.getName());

            // Get a list of cube objects and dump their names
            NamedList<Cube> cubes = schema.getCubes();
            for (Cube cube : cubes) {
                System.out.println("cube name=" + cube.getName());
            }

            if (cubes.size() == 0) {
                // no cubes where present
                return;
            }

            // take the first cube
            Cube cube = cubes.get(0);
            System.out.println("using cube name="+cube.getName());

            // create an XML doc to represent the Cube and print it out
            // This XML would be used by remote clients to enable cube navigation
            System.out.println(Olap4jXml.xmlToString(Olap4jXml.cubeToDoc(cube)));

            // Get a list of dimension objects and dump their names, hierarchies, levels
            NamedList<Dimension> dimensions = cube.getDimensions();
            for (Dimension dimension : dimensions) {
                if (dimension.getDimensionType() == Dimension.Type.Measures) {
                    System.out.println("measures dimension name="+dimension.getName());
                } else {
                    System.out.println("dimension name="+dimension.getName());
                }
                listHierarchies(dimension);
            }

            // The code from this point on is for the Foodmart schema

            // Create a new query
            Query query = new Query("my query", cube);

            QueryDimension productQuery = query.getDimension("Product");

            QueryDimension storeQuery = query.getDimension("Store");
            QueryDimension timeQuery = query.getDimension("Time"); //$NON-NLS-1$

            listMembers(productQuery.getDimension().getHierarchies().get("Product").getLevels().get("Product Department"));

            listMembers(storeQuery.getDimension().getHierarchies().get("Store").getLevels().get("Store Country"));

            Selection selection = productQuery.createSelection("Product", "Product Family", "Drink", Selection.Operator.CHILDREN);

            // Create an XML doc to represent the resolved selection and print it out
            // This would be used by a client application to enable hierarchy navigation
            List<Member> members = productQuery.resolve(selection);
            System.out.println(Olap4jXml.xmlToString(Olap4jXml.selectionToDoc(selection, members)));

            // create some selections for Store
            Selection usa = storeQuery.createSelection("Store", "Store Country", "USA", Selection.Operator.CHILDREN);
            storeQuery.addSelection(usa);

            // create some selections for Product
            productQuery.clearSelections();
            Selection productSelection1 = productQuery.createSelection("Product", "Product Family", "Drink", Selection.Operator.CHILDREN);
            Selection productSelection2 = productQuery.createSelection("Product", "Product Family", "Food", Selection.Operator.CHILDREN);
            productQuery.addSelection(productSelection1);
            productQuery.addSelection(productSelection2);

            // create some selections for Time
            Selection year97 = timeQuery.createSelection("Time", "Year", "1997", Selection.Operator.CHILDREN);
            timeQuery.addSelection(year97);

            // place our dimensions on the axes
            query.getAxes().get(Axis.COLUMNS).appendDimension(productQuery);
            query.getAxes().get(Axis.ROWS).appendDimension(storeQuery);
            query.getAxes().get(Axis.ROWS).appendDimension(timeQuery);

            // Create an XML doc to represent the query and print it out
            // This XML would be used by a client application to persist a query
            System.out.println(Olap4jXml.xmlToString(Olap4jXml.queryToDoc(query)));

            query.validate();
            CellSet result = query.execute();
            System.out.println(result.toString());

            // Create an XML doc to represent the results and print it out
            // This XML would be used by a remote client application to get the results
            System.out.println(Olap4jXml.xmlToString(Olap4jXml.resultToDoc(result)));

            // for shits and giggles we'll swap the axes over
            query.swapAxes();
            System.out.println(Olap4jXml.xmlToString(Olap4jXml.queryToDoc(query)));

            query.validate();
            result = query.execute();
            System.out.println(result.toString());
            System.out.println(Olap4jXml.xmlToString(Olap4jXml.resultToDoc(result)));

        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(0);
        }

    }

    public static void listHierarchies(Dimension dimension) {
        // Get a list of hierarchy objects and dump their names
        for (Hierarchy hierarchy : dimension.getHierarchies()) {
            System.out.println("hierarchy name=" + hierarchy.getName());
            listLevels(hierarchy);
        }
    }

    public static void listLevels(Hierarchy hierarchy) {
        // Get a list of level objects and dump their names
        for (Level level : hierarchy.getLevels()) {
            System.out.println("level name=" + level.getName());
        }
    }

    public static void listMembers(Level level) {

        List<Member> members = level.getMembers();
        for (Member member : members) {
            System.out.println("member name=" + member.getName());
        }
    }

    public static void main(String args[]) {

        OlapTest olapTest = new OlapTest();

        olapTest.testModel();

    }

    static class Olap4jXml {

        public static Document newDocument() {
            DocumentBuilderFactory dbf;
            DocumentBuilder db;
            Document doc;

            try {
                // Check and open XML document
                dbf = DocumentBuilderFactory.newInstance();
                db = dbf.newDocumentBuilder();
                doc = db.newDocument();
                return doc;
            } catch (Throwable t) {
                // todo:
            }
            return null;
        }

        public static Document cubeToDoc(Cube cube) {
            Document doc = Olap4jXml.newDocument();
            Element root = doc.createElement("olap4j");
            doc.appendChild(root);
            cubeToXml(cube, true, doc, root);
            return doc;
        }

        public static Document selectionToDoc(Selection selection, List<Member> members) {
            Document doc = Olap4jXml.newDocument();
            Element root = doc.createElement("olap4j");
            doc.appendChild(root);
            selectionToXml(selection, members, doc, root);
            return doc;
        }

        public static Document queryToDoc(Query query) {
            Document doc = Olap4jXml.newDocument();
            Element root = doc.createElement("olap4j");
            doc.appendChild(root);
            queryToXml(query, doc, root);
            return doc;
        }

        public static Document resultToDoc(CellSet result) {
            Document doc = Olap4jXml.newDocument();
            Element root = doc.createElement("olap4j");
            doc.appendChild(root);
            resultsToXml(result, doc, root);
            return doc;
        }

        public static void selectionToXml(Selection selection, List<Member> members, Document doc, Element parent) {

            try {
                Element root = doc.createElement("olap4j-members");
                parent.appendChild(root);

                selectionToXml(selection, doc, root);

                Element membersNode = doc.createElement("members");
                root.appendChild(membersNode);

                if (members != null) {
                    for (Member member : members) {
                        memberToXml(member, doc, membersNode);
                    }
                }

            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        public static void queryToXml(Query query, Document doc, Element parent) {

            try {

                Element root = doc.createElement("query");
                parent.appendChild(root);

                cubeToXml(query.getCube(), false, doc, root);

                Element axes = doc.createElement("axes");
                root.appendChild(axes);

                axisToXml(query.getAxes().get(Axis.COLUMNS), doc, axes);
                axisToXml(query.getAxes().get(Axis.ROWS), doc, axes);
                axisToXml(query.getAxes().get(Axis.SLICER), doc, axes);

            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        public static void resultsToXml(CellSet result, Document doc, Element parent) {

            try {

                Element root = doc.createElement("result");
                parent.appendChild(root);

                Element dimensionsNode = doc.createElement("dimensions");
                root.appendChild(dimensionsNode);

                Element gridNode = doc.createElement("grid");
                root.appendChild(gridNode);

                for (CellSetAxis axis : result.getAxes()) {
                    for (Hierarchy hierarchy : axis.getAxisMetaData().getHierarchies()) {
                        Element dimensionNode = dimensionInfoToXml(hierarchy.getDimension(), doc);
                        dimensionsNode.appendChild(dimensionNode);
                    }
                }

                // TODO

            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        public static void axisToXml(QueryAxis axis, Document doc, Element parent) {

            try {
                Element root = doc.createElement("axis");
                parent.appendChild(root);
                Element dimensionsNode = doc.createElement("dimensions");
                root.appendChild(dimensionsNode);

                switch(axis.getLocation()) {
                case COLUMNS: addAttribute("location", "across", root); break;
                case ROWS: addAttribute("location", "down", root); break;
                case SLICER: addAttribute("location", "slicer", root); break;
                }

                List<QueryDimension> dimensions = axis.getDimensions();
                for (QueryDimension dimension : dimensions) {
                    dimensionSelectionsToXml(dimension, doc, dimensionsNode);
                }

            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        public static void memberToXml(
            Member member,
            Document doc,
            Element parent)
        {
            try {

                Element root = doc.createElement("member");
                parent.appendChild(root);

                addCDataNode("name", member.getName(), root);
                addCDataNode("unique-name", member.getUniqueName(), root);
                addCDataNode("description", member.getDescription(null), root);

            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        public static void selectionToXml(
            Selection selection,
            Document doc,
            Element parent)
        {
            try {

                Element root = doc.createElement("selection");
                parent.appendChild(root);

                addCDataNode("name", selection.getName(), root);
                addCDataNode("dimension-name", selection.getDimension().getName(), root);
                switch (selection.getOperator()) {
                case CHILDREN: addCDataNode("operation", "children", root); break;
                case SIBLINGS: addCDataNode("operation", "siblings", root); break;
                case MEMBER: addCDataNode("operation", "member", root); break;
                }

            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        public static void cubeToXml(
            Cube cube,
            boolean includeDimensions,
            Document doc,
            Element parent)
        {
            NamedList<Dimension> dimensions = cube.getDimensions();
            if (dimensions == null) {
                return;
            }

            try {

                Element root = doc.createElement("cube");
                parent.appendChild(root);

                addCDataNode("name", cube.getName(), root);

                if (includeDimensions) {
                    Element dimensionsNode = doc.createElement("dimensions");
                    root.appendChild(dimensionsNode);

                    for (Dimension dimension : dimensions) {
                        dimensionToXml(dimension, doc, dimensionsNode);
                    }
                }

            } catch (Throwable t) {
                t.printStackTrace();
            }
        }

        public static void dimensionToXml(
            Dimension dimension,
            Document doc,
            Element parent) throws OlapException
        {
            Element dimensionNode = dimensionInfoToXml(dimension, doc);
            parent.appendChild(dimensionNode);
            Element hierarchyNode;

            NamedList<Hierarchy> hierarchies  = dimension.getHierarchies();

            for (Hierarchy hierarchy : hierarchies) {
                hierarchyNode = hierarchyToXml(hierarchy, doc);
                dimensionNode.appendChild(hierarchyNode);
            }
        }

        public static Element dimensionInfoToXml(
            Dimension dimension,
            Document doc)
            throws OlapException
        {
            Element dimensionNode;
            Element nameNode;
            Attr attr;
            CDATASection cdata;

            dimensionNode = doc.createElement("dimension");
            nameNode = doc.createElement("name");
            cdata = doc.createCDATASection(dimension.getName());
            nameNode.appendChild(cdata);
            dimensionNode.appendChild(nameNode);
            attr = doc.createAttribute("isMeasure");
            boolean isMeasures =
                dimension.getDimensionType() == Dimension.Type.Measures;
            attr.setTextContent(Boolean.toString(isMeasures));
            dimensionNode.setAttribute("isMeasures", Boolean.toString(isMeasures));

            return dimensionNode;
        }

        public static void dimensionSelectionsToXml(
            QueryDimension dimension,
            Document doc,
            Element parent) throws OlapException
        {
            Element dimensionNode = dimensionInfoToXml(dimension.getDimension(), doc);
            parent.appendChild(dimensionNode);
            Element selectionsNode;

            selectionsNode = doc.createElement("selections");
            dimensionNode.appendChild(selectionsNode);

            List<Selection> selections = dimension.getSelections();

            for (Selection selection : selections) {
                selectionToXml(selection, doc, selectionsNode);
            }
        }

        public static Element hierarchyToXml(Hierarchy hierarchy, Document doc) {
            Element hierarchyNode;
            Element levelNode;
            Element nameNode;
            CDATASection cdata;

            hierarchyNode = doc.createElement("hierarchy");
            nameNode = doc.createElement("name");
            cdata = doc.createCDATASection(hierarchy.getName());
            nameNode.appendChild(cdata);
            hierarchyNode.appendChild(nameNode);
            Element defaultMember = doc.createElement("default-member");
            hierarchyNode.appendChild(defaultMember);
            memberToXml(hierarchy.getDefaultMember(), defaultMember);

            for (Level level : hierarchy.getLevels()) {
                levelNode = levelToXml(level, doc);
                hierarchyNode.appendChild(levelNode);
            }

            return hierarchyNode;
        }

        public static Element memberToXml(Member member, Element parent) {

            Document doc = parent.getOwnerDocument();
            Element memberNode = doc.createElement("member");

            addCDataNode("name", member.getName(), memberNode);
            addCDataNode("unique-name", member.getUniqueName(), memberNode);

            parent.appendChild(memberNode);

            return memberNode;
        }

        public static void addCDataNode(String name, String value, Element parent) {
            Document doc = parent.getOwnerDocument();
            Element node = doc.createElement(name);
            if (value != null) {
                CDATASection cdata = doc.createCDATASection(value);
                node.appendChild(cdata);
                parent.appendChild(node);
            }
        }

        public static void addNode(String name, String value, Element parent) {
            Document doc = parent.getOwnerDocument();
            Element node = doc.createElement(name);
            if (value != null) {
                node.setTextContent(value);
            }
            parent.appendChild(node);
        }

        public static void addAttribute(String name, String value, Element parent) {
            if (name != null && value != null) {
                parent.setAttribute(name, value);
            }
        }

        public static Element levelToXml(Level level, Document doc) {

            Element levelNode = doc.createElement("level");

            addCDataNode("name", level.getName(), levelNode);

            return levelNode;
        }

        public static String xmlToString(Node node) {
            try {
                Source source = new DOMSource(node);
                StringWriter stringWriter = new StringWriter();
                Result result = new StreamResult(stringWriter);
                TransformerFactory factory = TransformerFactory.newInstance();
                Transformer transformer = factory.newTransformer();
                transformer.transform(source, result);
                return stringWriter.getBuffer().toString();
            } catch (TransformerConfigurationException e) {
                e.printStackTrace();
            } catch (TransformerException e) {
                e.printStackTrace();
            }
            return null;
        }
    }
}

// End OlapTest.java