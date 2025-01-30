/*
 * Copyright [2016-2030] [George Papadakis (gpapadis@yahoo.gr)]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.scify.jedai.datawriter;

import com.esotericsoftware.minlog.Log;
import gnu.trove.iterator.TIntIterator;
import org.apache.jena.update.UpdateExecutionFactory;
import org.apache.jena.update.UpdateFactory;
import org.apache.jena.update.UpdateProcessor;
import org.apache.jena.update.UpdateRequest;
import org.rdfhdt.hdt.enums.RDFNotation;
import org.rdfhdt.hdt.exceptions.ParserException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.options.HDTSpecification;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.EquivalenceCluster;
import org.scify.jedai.utilities.DBUtils;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

public class PrintStatsToFile {

    private final List<EntityProfile> profilesD1;
    private final List<EntityProfile> profilesD2;
    private final EquivalenceCluster[] entityClusters;

    private String dbpassword;
    private String dbtable;
    private String dbuser;
    private boolean ssl;
    private String endpointURL;
    private String endpointGraph;

    public PrintStatsToFile(List<EntityProfile> profilesD1, List<EntityProfile> profilesD2, EquivalenceCluster[] entityClusters) {
        this.profilesD1 = profilesD1;
        this.profilesD2 = profilesD2;
        this.entityClusters = entityClusters;
    }

    public void setPassword(String password) {
        this.dbpassword = password;
    }

    public void setTable(String table) {
        this.dbtable = table;
    }

    public void setUser(String user) {
        this.dbuser = user;
    }

    public void setSSL(boolean ssl) {
        this.ssl = ssl;
    }

    public void setEndpointURL(String endpointURL) {
        this.endpointURL = endpointURL;
    }

    public void setEndpointGraph(String endpointGraph) {
        this.endpointGraph = endpointGraph;
    }

    public void printToCSV(String filename) throws FileNotFoundException {
        final PrintWriter pw = new PrintWriter(new File(filename));
        final StringBuilder sb = new StringBuilder();

        sb.append("cluster_id,dataset,entity_url\n");
        int counter = 0;
        for (EquivalenceCluster eqc : entityClusters) {
            if (eqc.getEntityIdsD1().isEmpty()) {
                continue;
            }
            counter++;
            for (TIntIterator iterator = eqc.getEntityIdsD1().iterator(); iterator.hasNext(); ) {
                sb.append(counter).append(",").append(1).append(",")
                        .append(profilesD1.get(iterator.next()).getEntityUrl()).append("\n");
            }
            if (eqc.getEntityIdsD2().isEmpty()) {
                continue;
            }
            if (profilesD2 == null) {
                Log.error("The entity profiles of Dataset 2 are missing!");
                continue;
            }
            for (TIntIterator iterator = eqc.getEntityIdsD2().iterator(); iterator.hasNext(); ) {
                sb.append(counter).append(",").append(2).append(",")
                        .append(profilesD2.get(iterator.next()).getEntityUrl()).append("\n");
            }

        }
        pw.write(sb.toString());
        pw.close();
    }

    public void printToRDFXML(String filename) throws FileNotFoundException {
        final PrintWriter printWriter = new PrintWriter(new File(filename));

        printWriter.println("<?xml version=\"1.0\"?>");
        printWriter.println();
        printWriter.println("<rdf:RDF");
        printWriter.println("xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
        printWriter.println("xmlns:obj=\"https://www.w3schools.com/rdf/\">");

        int counter = 0;
        for (EquivalenceCluster eqc : entityClusters) {
            if (eqc.getEntityIdsD1().isEmpty()) {
                continue;
            }
            counter++;
            for (TIntIterator iterator = eqc.getEntityIdsD1().iterator(); iterator.hasNext(); ) {
                printWriter.println();

                printWriter.println("<rdf:Description rdf:about=\"" + counter + "\">");

                printWriter.print("<obj:" + "cluster_id" + ">");
                printWriter.print(counter + "");
                printWriter.println("</obj:" + "cluster_id>");

                printWriter.print("<obj:" + "dataset" + ">");
                printWriter.print("1");
                printWriter.println("</obj:" + "dataset>");

                printWriter.print("<obj:" + "entity_url" + ">");
                printWriter.print(profilesD1.get(iterator.next()).getEntityUrl().replace("&", "") + "");
                printWriter.println("</obj:" + "entity_url" + ">");

                printWriter.println("</rdf:Description>");

            }
            if (eqc.getEntityIdsD2().isEmpty()) {
                continue;
            }
            if (profilesD2 == null) {
                Log.error("The entity profiles of Dataset 2 are missing!");
                continue;
            }
            for (TIntIterator iterator = eqc.getEntityIdsD2().iterator(); iterator.hasNext(); ) {

                printWriter.println();

                printWriter.println("<rdf:Description rdf:about=\"" + counter + "\">");

                printWriter.print("<obj:" + "cluster_id" + ">");
                printWriter.print(counter + "");
                printWriter.println("</obj:" + "cluster_id>");

                printWriter.print("<obj:" + "dataset" + ">");
                printWriter.print("2");
                printWriter.println("</obj:" + "dataset>");

                printWriter.print("<obj:" + "entity_url" + ">");
                printWriter.print(profilesD2.get(iterator.next()).getEntityUrl().replace("&", "") + "");
                printWriter.println("</obj:" + "entity_url" + ">");

                printWriter.println("</rdf:Description>");

            }

        }
        printWriter.println("</rdf:RDF>");

        printWriter.close();
    }

    public void printToJSONrdf(String filename) throws FileNotFoundException {
        final PrintWriter printWriter = new PrintWriter(new File(filename));

        printWriter.println("{\"triples\":\n");
        printWriter.println("[");

        String subString = "{Subject: \"";
        String predString = "\", Predicate: \"";
        String objString = "\", Object: \"";

        int counter = 0;
        boolean isfirst = true;
        for (EquivalenceCluster eqc : entityClusters) {
            if (eqc.getEntityIdsD1().isEmpty()) {
                continue;
            }
            counter++;
            for (TIntIterator iterator = eqc.getEntityIdsD1().iterator(); iterator.hasNext(); ) {
                if (!isfirst) {
                    printWriter.println(",");

                } else {
                    isfirst = false;
                }
                printWriter.print(subString + counter);
                printWriter.print(predString + "cluster_id");
                printWriter.print(objString);
                printWriter.print(counter + "");
                printWriter.println("\"},");

                printWriter.print(subString + counter);
                printWriter.print(predString + "dataset");
                printWriter.print(objString);
                printWriter.print(1 + "");
                printWriter.println("\"},");

                printWriter.print(subString + counter);
                printWriter.print(predString + "entity_url");
                printWriter.print(objString);
                printWriter.print(profilesD1.get(iterator.next()).getEntityUrl().replace("&", "") + "");
                printWriter.println("\"}");

            }
            if (eqc.getEntityIdsD2().isEmpty()) {
                continue;
            }
            if (profilesD2 == null) {
                Log.error("The entity profiles of Dataset 2 are missing!");
                continue;
            }
            for (TIntIterator iterator = eqc.getEntityIdsD2().iterator(); iterator.hasNext(); ) {
                if (!isfirst) {
                    printWriter.println(",");

                } else {
                    isfirst = false;
                }
                printWriter.print(subString + counter);
                printWriter.print(predString + "cluster_id");
                printWriter.print(objString);
                printWriter.print(counter + "");
                printWriter.println("\"},");

                printWriter.print(subString + counter);
                printWriter.print(predString + "dataset");
                printWriter.print(objString);
                printWriter.print(2 + "");
                printWriter.println("\"},");

                printWriter.print(subString + counter);
                printWriter.print(predString + "entity_url");
                printWriter.print(objString);
                printWriter.print(profilesD2.get(iterator.next()).getEntityUrl().replace("&", "") + "");
                printWriter.println("\"}");

            }

        }

        printWriter.println("]");
        printWriter.println("}");
        printWriter.close();

    }

    public void printToRDFNT(String filename) throws FileNotFoundException {
        final PrintWriter printWriter = new PrintWriter(new File(filename));

        String xmlnsrdf = "http://www.w3.org/1999/02/22/";
        String xmlnsobj = "https://www.w3schools.com/rdf/";

        int counter = 0;
        for (EquivalenceCluster eqc : entityClusters) {
            if (eqc.getEntityIdsD1().isEmpty()) {
                continue;
            }
            counter++;
            for (TIntIterator iterator = eqc.getEntityIdsD1().iterator(); iterator.hasNext(); ) {

                printWriter.print("<" + xmlnsrdf + counter + "> ");
                printWriter.print("<" + xmlnsobj + "cluster_id" + "> \"");
                printWriter.print(counter + "");
                printWriter.println("\"^^<http://www.w3.org/2001/XMLSchema#integer>");

                printWriter.print("<" + xmlnsrdf + counter + "> ");
                printWriter.print("<" + xmlnsobj + "dataset" + "> \"");
                printWriter.print("1");
                printWriter.println("\"^^<http://www.w3.org/2001/XMLSchema#integer>");

                printWriter.print("<" + xmlnsrdf + counter + "> ");
                printWriter.print("<" + xmlnsobj + "entity_url" + "> \"");
                printWriter.print(profilesD1.get(iterator.next()).getEntityUrl().replace("&", "") + "");
                printWriter.println("\"^^<http://www.w3.org/2001/XMLSchema#string>");

            }
            if (eqc.getEntityIdsD2().isEmpty()) {
                continue;
            }
            if (profilesD2 == null) {
                Log.error("The entity profiles of Dataset 2 are missing!");
                continue;
            }
            for (TIntIterator iterator = eqc.getEntityIdsD2().iterator(); iterator.hasNext(); ) {

                printWriter.print("<" + xmlnsrdf + counter + "> ");
                printWriter.print("<" + xmlnsobj + "cluster_id" + "> \"");
                printWriter.print(counter + "");
                printWriter.println("\"^^<http://www.w3.org/2001/XMLSchema#integer>");

                printWriter.print("<" + xmlnsrdf + counter + "> ");
                printWriter.print("<" + xmlnsobj + "dataset" + "> \"");
                printWriter.print("2");
                printWriter.println("\"^^<http://www.w3.org/2001/XMLSchema#integer>");

                printWriter.print("<" + xmlnsrdf + counter + "> ");
                printWriter.print("<" + xmlnsobj + "entity_url" + "> \"");
                printWriter.print(profilesD2.get(iterator.next()).getEntityUrl().replace("&", "") + "");
                printWriter.println("\"^^<http://www.w3.org/2001/XMLSchema#string>");

            }

        }

    }

    public void printToHDTRDF(String outputFile) throws IOException, ParserException {

        this.printToRDFNT(outputFile + "help.nt");
        String baseURI = "http://www.w3.org/";
        //String rdfInput = "data//minExample.nt";
        String rdfInput = outputFile + "help.nt";
        String inputType = "ntriples";
        String hdtOutput = outputFile;

        // Create HDT from RDF file
        HDT hdt = HDTManager.generateHDT(
                rdfInput, // Input RDF File
                baseURI, // Base URI
                RDFNotation.parse(inputType), // Input Type
                new HDTSpecification(), // HDT Options
                null // Progress Listener
        );

        // OPTIONAL: Add additional domain-specific properties to the header:
        //Header header = hdt.getHeader();
        //header.insert("myResource1", "property" , "value");
        // Save generated HDT to a file
        hdt.saveToHDT(hdtOutput, null);

        // IMPORTANT: Close hdt when no longer needed
        hdt.close();
    }

    public void printToXML(String filename) throws FileNotFoundException {
        final PrintWriter printWriter = new PrintWriter(new File(filename));

        printWriter.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        printWriter.println();

        printWriter.println("<general>");

        int counter = 0;
        for (EquivalenceCluster eqc : entityClusters) {
            if (eqc.getEntityIdsD1().isEmpty()) {
                continue;
            }
            counter++;
            for (TIntIterator iterator = eqc.getEntityIdsD1().iterator(); iterator.hasNext(); ) {
                printWriter.println();
                printWriter.println("<entity" + counter + ">");

                printWriter.print("<cluster_id" + ">");
                printWriter.print(counter + "");
                printWriter.println("</cluster_id>");

                printWriter.print("<dataset" + ">");
                printWriter.print("1");
                printWriter.println("</dataset>");

                printWriter.print("<entity_url" + ">");
                printWriter.print(profilesD1.get(iterator.next()).getEntityUrl().replace("&", "") + "");
                printWriter.println("</entity_url" + ">");

                printWriter.println("</entity" + counter + ">");

            }
            if (eqc.getEntityIdsD2().isEmpty()) {
                continue;
            }
            if (profilesD2 == null) {
                Log.error("The entity profiles of Dataset 2 are missing!");
                continue;
            }
            for (TIntIterator iterator = eqc.getEntityIdsD2().iterator(); iterator.hasNext(); ) {

                printWriter.println();

                printWriter.println("<entity" + counter + ">");

                printWriter.print("<cluster_id" + ">");
                printWriter.print(counter + "");
                printWriter.println("</cluster_id>");

                printWriter.print("<dataset" + ">");
                printWriter.print("2");
                printWriter.println("</dataset>");

                printWriter.print("<entity_url" + ">");
                printWriter.print(profilesD2.get(iterator.next()).getEntityUrl().replace("&", "") + "");
                printWriter.println("</entity_url" + ">");

                printWriter.println("</entity" + counter + ">");

            }

        }
        printWriter.println("</general>");

        printWriter.close();
    }

    public void printToSPARQL(String endpointURL, String GraphName) {
        final StringBuilder sb = new StringBuilder();

        String sparqlQueryString1 = "INSERT DATA { "
                + "GRAPH " + GraphName + " { ";
        sb.append(sparqlQueryString1);

        int counter = 0;
        for (EquivalenceCluster eqc : entityClusters) {
            if (eqc.getEntityIdsD1().isEmpty()) {
                continue;
            }
            counter++;
            for (TIntIterator iterator = eqc.getEntityIdsD1().iterator(); iterator.hasNext(); ) {

                sb.append("<obj/" + "record/").append(counter).append("> ");
                sb.append("<cluster_id> ");
                sb.append("\"").append(counter).append("\".\n");

                sb.append("<obj/" + "record/").append(counter).append("> ");
                sb.append("<dataset> ");
                sb.append("\"1\".\n");

                sb.append("<obj/" + "record/").append(counter).append("> ");
                sb.append("<entity_url> ");
                sb.append("\"").append(profilesD1.get(iterator.next()).getEntityUrl().replace("&", "")).append("\".\n");

            }
            if (eqc.getEntityIdsD2().isEmpty()) {
                continue;
            }
            if (profilesD2 == null) {
                Log.error("The entity profiles of Dataset 2 are missing!");
                continue;
            }
            for (TIntIterator iterator = eqc.getEntityIdsD2().iterator(); iterator.hasNext(); ) {

                sb.append("<obj/" + "record/").append(counter).append("> ");
                sb.append("<cluster_id> ");
                sb.append("\"").append(counter).append("\".\n");

                sb.append("<obj/" + "record/").append(counter).append("> ");
                sb.append("<dataset> ");
                sb.append("\"2\".\n");

                sb.append("<obj/" + "record/").append(counter).append("> ");
                sb.append("<entity_url> ");
                sb.append("\"").append(profilesD2.get(iterator.next()).getEntityUrl().replace("&", "")).append("\".\n");

                //execute query every 1000 steps
                if (counter % 1000 == 0) {
                    sb.append("}\n }");
                    String sparqlQueryString = sb.toString();

                    //System.out.println(sparqlQueryString);
                    UpdateRequest update = UpdateFactory.create(sparqlQueryString);
                    UpdateProcessor qexec = UpdateExecutionFactory.createRemote(update, endpointURL);
                    qexec.execute();
                    sb.setLength(0);
                    sb.append(sparqlQueryString1);
                }
            }
        }

        if (counter % 1000 != 0) {
            sb.append("}\n }");
            String sparqlQueryString = sb.toString();

            //System.out.println(sparqlQueryString);
            UpdateRequest update = UpdateFactory.create(sparqlQueryString);
            UpdateProcessor qexec = UpdateExecutionFactory.createRemote(update, endpointURL);
            qexec.execute();
        }
    }

    public void printToDB(String dbURL) {
        final StringBuilder sb = new StringBuilder();

        String dbquery1 = "INSERT INTO " + dbtable + " (cluster_id, dataset, entity_url) VALUES ";
        sb.append(dbquery1);

        int counter = 0;
        for (EquivalenceCluster eqc : entityClusters) {
            if (eqc.getEntityIdsD1().isEmpty()) {
                continue;
            }
            counter++;
            for (TIntIterator iterator = eqc.getEntityIdsD1().iterator(); iterator.hasNext(); ) {

                sb.append("('").append(counter).append("', ");
                sb.append("'" + "1" + "', ");
                sb.append("'").append(profilesD1.get(iterator.next()).getEntityUrl().replace("&", "")).append("'), ");

            }
            if (eqc.getEntityIdsD2().isEmpty()) {
                continue;
            }
            if (profilesD2 == null) {
                Log.error("The entity profiles of Dataset 2 are missing!");
                continue;
            }
            for (TIntIterator iterator = eqc.getEntityIdsD2().iterator(); iterator.hasNext(); ) {

                sb.append("('").append(counter).append("', ");
                sb.append("'" + "2" + "', ");
                sb.append("'").append(profilesD2.get(iterator.next()).getEntityUrl().replace("&", "")).append("'), ");
            }
        }

        sb.setLength(sb.length() - 2);//remove last ","
        sb.append(";");
        String dbquery = sb.toString();

        if (dbuser == null) {
            Log.error("Database user has not been set!");
        }
        if (dbpassword == null) {
            Log.error("Database password has not been set!");
        }
        if (dbtable == null) {
            Log.error("Database table has not been set!");
        }
  
        try (Connection conn = DBUtils.getDBConnection(dbURL, dbuser, dbpassword, ssl);
            Statement stmt = conn.createStatement();) {
          
            stmt.execute(dbquery);//retrieve the appropriate table
        } catch (SQLException ex) {
            Log.error("Error in db writing!", ex);
        }
    }

}
