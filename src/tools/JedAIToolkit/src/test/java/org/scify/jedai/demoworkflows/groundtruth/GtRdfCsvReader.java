/*
 * Copyright [2016-2020] [George Papadakis (gpapadis@yahoo.gr)]
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
package org.scify.jedai.demoworkflows.groundtruth;

import org.scify.jedai.datareader.groundtruthreader.AbstractGtReader;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.IdDuplicates;

import com.esotericsoftware.minlog.Log;
import java.io.BufferedReader;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.graph.DefaultEdge;

/**
 *
 * @author G.A.P. II
 */
public class GtRdfCsvReader extends AbstractGtReader {

    private boolean ignoreFirstRow;
    private String separator;

    public GtRdfCsvReader(String filePath) {
        super(filePath);
        separator = ",";
        ignoreFirstRow = false;
    }

    @Override
    public String getMethodConfiguration() {
        return getParameterName(0) + "=" + inputFilePath + "\t"
                + getParameterName(1) + "=" + ignoreFirstRow + "\t"
                + getParameterName(2) + "=" + separator;
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it converts a CSV file into a set of pairs of duplicate entity profiles.";
    }

    @Override
    public String getMethodName() {
        return "CSV Ground-truth Reader";
    }

    @Override
    public String getMethodParameters() {
        return getMethodName() + " involves three parameters:\n"
                + "1)" + getParameterDescription(0) + ".\n"
                + "2)" + getParameterDescription(1) + ".\n"
                + "3)" + getParameterDescription(2) + ".";
    }

    @Override
    public JsonArray getParameterConfiguration() {
        final JsonObject obj1 = new JsonObject();
        obj1.put("class", "java.lang.String");
        obj1.put("name", getParameterName(0));
        obj1.put("defaultValue", "-");
        obj1.put("minValue", "-");
        obj1.put("maxValue", "-");
        obj1.put("stepValue", "-");
        obj1.put("description", getParameterDescription(0));

        final JsonObject obj2 = new JsonObject();
        obj2.put("class", "java.lang.Boolean");
        obj2.put("name", getParameterName(1));
        obj2.put("defaultValue", "false");
        obj2.put("minValue", "-");
        obj2.put("maxValue", "-");
        obj2.put("stepValue", "-");
        obj2.put("description", getParameterDescription(1));

        final JsonObject obj3 = new JsonObject();
        obj3.put("class", "java.lang.Character");
        obj3.put("name", getParameterName(2));
        obj3.put("defaultValue", ",");
        obj3.put("minValue", "-");
        obj3.put("maxValue", "-");
        obj3.put("stepValue", "-");
        obj3.put("description", getParameterDescription(2));

        final JsonArray array = new JsonArray();
        array.add(obj1);
        array.add(obj2);
        array.add(obj3);
        return array;
    }

    @Override
    public String getParameterDescription(int parameterId) {
        switch (parameterId) {
            case 0:
                return "The " + getParameterName(0) + " determines the absolute path to the CSV file that will be read into main memory.";
            case 1:
                return "The " + getParameterName(1) + " determines whether the first line of the CSV file contains attribute names and, thus, should be ignored (true), or it contains a pair of matching entity profiles (false).";
            case 2:
                return "The " + getParameterName(2) + " determines the character used to break every line into a pair of entity ids.";
            default:
                return "invalid parameter id";
        }
    }

    @Override
    public String getParameterName(int parameterId) {
        switch (parameterId) {
            case 0:
                return "File Path";
            case 1:
                return "Ignore First Row";
            case 2:
                return "Separator";
            default:
                return "invalid parameter id";
        }
    }

    protected void getBilateralConnectedComponents(List<Set<Integer>> connectedComponents) {
        for (Set<Integer> cluster : connectedComponents) {
            if (cluster.size() != 2) {
                Log.warn("Connected component that does not involve just a couple of entities!\t" + cluster.toString());
                continue;
            }

            // add a new pair of IdDuplicates for every pair of entities in the cluster
            Iterator<Integer> idIterator = cluster.iterator();
            int id1 = idIterator.next();
            int id2 = idIterator.next();
            if (id1 < id2) {
                id2 = id2 - datasetLimit; // normalize id to [0, profilesD2.size()]
                if (id2 < 0) {
                    Log.warn("Entity id not corresponding to dataset 2!\t" + id2);
                    continue;
                }
                idDuplicates.add(new IdDuplicates(id1, id2));
            } else {
                id1 = id1 - datasetLimit; // normalize id to [0, profilesD2.size()]
                if (id1 < 0) {
                    Log.warn("Entity id not corresponding to dataset 2!\t" + id1);
                    continue;
                }
                idDuplicates.add(new IdDuplicates(id2, id1));
            }
        }
    }

    @Override
    public Set<IdDuplicates> getDuplicatePairs(List<EntityProfile> profilesD1,
            List<EntityProfile> profilesD2) {
        if (!idDuplicates.isEmpty()) {
            return idDuplicates;
        }

        if (profilesD1 == null) {
            Log.error("First list of entity profiles is null! "
                    + "The first argument should always contain entities.");
            return null;
        }

        initializeDataStructures(profilesD1, profilesD2);
        try (BufferedReader br = new BufferedReader(new FileReader(inputFilePath));) {
            String line;
            if (ignoreFirstRow) {
                line = br.readLine();
            }

            String[] nextLine;
            while ((line = br.readLine()) != null) {
                nextLine = line.split(separator);
                
                if (nextLine.length < 2) {
                    Log.warn("Line with inadequate information " + nextLine);
                    continue;
                }

                // add a new edge for every pair of duplicate entities
                int entityId1 = urlToEntityId1.get(nextLine[0]);
                int entityId2 = urlToEntityId2.get(nextLine[1]);
                duplicatesGraph.addEdge(entityId1, entityId2);
            }
        } catch (FileNotFoundException ex) {
            Log.error("Missing file", ex);
        } catch (IOException ex) {
            Log.error("Reading error", ex);
        }
        Log.info("Total edges in duplicates graph\t:\t" + duplicatesGraph.edgeSet().size());

        // get connected components
        final ConnectivityInspector<Integer, DefaultEdge> ci = new ConnectivityInspector<>(duplicatesGraph);
        final List<Set<Integer>> connectedComponents = ci.connectedSets();
        Log.info("Total connected components in duplicate graph\t:\t" + connectedComponents.size());

        // transform connected components into pairs of duplicates
        if (profilesD2 != null) { // Clean-Clean ER
            getBilateralConnectedComponents(connectedComponents);
        } else { // Dirty ER
            getUnilateralConnectedComponents(connectedComponents);
        }
        Log.info("Total pair of duplicats\t:\t" + idDuplicates.size());

        return idDuplicates;
    }

    protected void getUnilateralConnectedComponents(List<Set<Integer>> connectedComponents) {
        for (Set<Integer> cluster : connectedComponents) {
            if (cluster.size() < 2) {
                Log.warn("Connected component with a single element! " + cluster.toString());
                continue;
            }

            // add a new pair of IdDuplicates for every pair of entities in the cluster
            int clusterSize = cluster.size();
            Integer[] clusterEntities = cluster.toArray(new Integer[clusterSize]);
            for (int i = 0; i < clusterSize; i++) {
                for (int j = i + 1; j < clusterSize; j++) {
                    idDuplicates.add(new IdDuplicates(clusterEntities[i], clusterEntities[j]));
                }
            }
        }
    }

    protected void initializeDataStructures(List<EntityProfile> profilesD1,
            List<EntityProfile> profilesD2) {
        // count total entities
        noOfEntities = profilesD1.size();
        datasetLimit = 0;
        if (profilesD2 != null) {
            noOfEntities += profilesD2.size();
            datasetLimit = profilesD1.size(); //specifies where the first dataset ends and the second one starts
        }

        // build inverted index from URL to entity id
        int counter = 0;
        for (EntityProfile profile : profilesD1) {
            int suffixIndex = profile.getEntityUrl().indexOf("conf");
            if (suffixIndex == -1) {
                suffixIndex = profile.getEntityUrl().indexOf("journal");
            }
            String normalizedUrl = profile.getEntityUrl().substring(suffixIndex);
            urlToEntityId1.put(normalizedUrl, counter++);
        }
        if (profilesD2 != null) {
            for (EntityProfile profile : profilesD2) {
                int suffixIndex = profile.getEntityUrl().lastIndexOf("/")+1;
                String normalizedUrl = profile.getEntityUrl().substring(suffixIndex);
                urlToEntityId2.put(normalizedUrl, counter++);
            }
        }

        // add a node for every input entity 
        for (int i = 0; i < noOfEntities; i++) {
            duplicatesGraph.addVertex(i);
        }
        Log.info("Total nodes in duplicate graph\t:\t" + duplicatesGraph.vertexSet().size());
    }

    public void setIgnoreFirstRow(boolean ignoreFirstRow) {
        this.ignoreFirstRow = ignoreFirstRow;
    }

    public void setSeparator(String separator) {
        this.separator = separator;
    }
}
