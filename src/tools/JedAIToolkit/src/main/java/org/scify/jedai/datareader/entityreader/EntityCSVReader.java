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
package org.scify.jedai.datareader.entityreader;

import com.esotericsoftware.minlog.Log;
import com.opencsv.CSVReader;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.scify.jedai.datamodel.EntityProfile;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author G.A.P. II
 */
public class EntityCSVReader extends AbstractEntityReader {

    private boolean attributeNamesInFirstRow;
    private int idIndex;
    private char separator;
    private String[] attributeNames;
    private final TIntSet attributesToExclude;

    public EntityCSVReader(String filePath) {
        super(filePath);
        attributeNamesInFirstRow = false;
        attributeNames = null;
        idIndex = -1;
        separator = ',';
        attributesToExclude = new TIntHashSet();
    }

    @Override
    public List<EntityProfile> getEntityProfiles() {
        if (!entityProfiles.isEmpty()) {
            return entityProfiles;
        }

        if (inputFilePath == null) {
            Log.error("Input file path has not been set!");
            return null;
        }

        try (final BufferedReader br = new BufferedReader(new FileReader(inputFilePath));
             final CSVReader csvReader = new CSVReader(br, separator)) {

            // Get first line
            final String[] firstRecord = csvReader.readNext();

            if (firstRecord == null) {
                Log.error("Empty file given as input.");
                return null;
            }

            int noOfAttributes = firstRecord.length;
            if (noOfAttributes - 1 < idIndex) {
                Log.error("Id index does not correspond to a valid column index! Counting starts from 0.");
                return null;
            }

            // Setting attribute names
            int entityCounter = 0;
            if (attributeNamesInFirstRow) {
                attributeNames = Arrays.copyOf(firstRecord, noOfAttributes);
            } else { // no attribute names in csv file
                attributeNames = new String[noOfAttributes];
                for (int i = 0; i < noOfAttributes; i++) {
                    attributeNames[i] = "attribute" + (i + 1);
                }

                entityCounter++; //first line corresponds to entity
                readEntity(entityCounter, firstRecord);
            }

            //read entity profiles
            String[] nextRecord;
            while ((nextRecord = csvReader.readNext()) != null) {
                entityCounter++;

                if (nextRecord.length < attributeNames.length - 1) {
                    Log.warn("Line with missing attribute names : " + Arrays.toString(nextRecord));
                    continue;
                }
                if (attributeNames.length < nextRecord.length) {
                    Log.warn("Line with missing more attributes : " + Arrays.toString(nextRecord));
                    continue;
                }

                readEntity(entityCounter, nextRecord);
            }

            return entityProfiles;

        } catch (IOException e) {
            Log.error("Error in entities reading!", e);
            return null;
        }

    }

    @Override
    public String getMethodConfiguration() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        final TIntIterator iterator = attributesToExclude.iterator();
        while (iterator.hasNext()) {
            int attributeId = iterator.next();
            sb.append(attributeId).append(",");
        }
        sb.append("}");

        return getParameterName(0) + "=" + inputFilePath + "\t"
                + getParameterName(1) + "=" + attributeNamesInFirstRow + "\t"
                + getParameterName(2) + "=" + separator + "\t"
                + getParameterName(3) + "=" + idIndex + "\t"
                + getParameterName(4) + "=" + sb.toString();
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it converts a CSV file into a set of entity profiles.";
    }

    @Override
    public String getMethodName() {
        return "CSV Reader";
    }

    @Override
    public String getMethodParameters() {
        return getMethodName() + " involves five parameters:\n"
                + "1)" + getParameterDescription(0) + ".\n"
                + "2)" + getParameterDescription(1) + ".\n"
                + "3)" + getParameterDescription(2) + ".\n"
                + "4)" + getParameterDescription(3) + ".\n"
                + "5)" + getParameterDescription(4) + ".";
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
        obj2.put("defaultValue", "true");
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

        final JsonObject obj4 = new JsonObject();
        obj4.put("class", "java.lang.Integer");
        obj4.put("name", getParameterName(3));
        obj4.put("defaultValue", "0");
        obj4.put("minValue", "0");
        obj4.put("maxValue", "-");
        obj4.put("stepValue", "-");
        obj4.put("description", getParameterDescription(3));

        final JsonObject obj5 = new JsonObject();
        obj5.put("class", "gnu.trove.set.TIntSet");
        obj5.put("name", getParameterName(4));
        obj5.put("defaultValue", "-");
        obj5.put("minValue", "-");
        obj5.put("maxValue", "-");
        obj5.put("stepValue", "-");
        obj5.put("description", getParameterDescription(4));

        final JsonArray array = new JsonArray();
        array.add(obj1);
        array.add(obj2);
        array.add(obj3);
        array.add(obj4);
        array.add(obj5);
        return array;
    }

    @Override
    public String getParameterDescription(int parameterId) {
        switch (parameterId) {
            case 0:
                return "The " + getParameterName(0) + " determines the absolute path to the CSV file that will be read into main memory.";
            case 1:
                return "The " + getParameterName(1) + " determines whether the first line of the CSV file contains the attribute names (true), or it contains an entity profile (false).";
            case 2:
                return "The " + getParameterName(2) + " determines the character used to tokenize every line into attribute values.";
            case 3:
                return "The " + getParameterName(3) + " determines the number of column/attribute (starting from 0) that contains the entity ids. "
                        + "If the given id is larger than the number of columns, an exception is thrown. "
                        + "If id<0, an auto-incremented integer is assigned as id to every entity.";
            case 4:
                return "The " + getParameterName(4) + " specifies the column ids (in the form of comma-separated integers) that will be ignored during the creation of entity profiles.";
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
                return "Attribute Names In First Row";
            case 2:
                return "Separator";
            case 3:
                return "Id index";
            case 4:
                return "Attributes To Exclude";
            default:
                return "invalid parameter id";
        }
    }

    private void readEntity(int index, String[] currentLine) {
        String entityId;
        if (idIndex < 0) {
            entityId = "id" + index;
        } else {
            entityId = currentLine[idIndex];
        }

        final EntityProfile newProfile = new EntityProfile(entityId);
        for (int i = 0; i < attributeNames.length; i++) {
            if (attributesToExclude.contains(i)) {
                continue;
            }
            if (!currentLine[i].trim().isEmpty()) {
                newProfile.addAttribute(attributeNames[i], currentLine[i]);
            }
        }
        entityProfiles.add(newProfile);
    }

    public void setAttributesToExclude(int[] attributesIndicesToExclude) {
        for (int attributeIndex : attributesIndicesToExclude) {
            attributesToExclude.add(attributeIndex);
        }
    }

    public void setAttributeNamesInFirstRow(boolean attributeNamesInFirstRow) {
        this.attributeNamesInFirstRow = attributeNamesInFirstRow;
    }

    public void setIdIndex(int idIndex) {
        this.idIndex = idIndex;
        attributesToExclude.add(idIndex);
    }

    public void setSeparator(char separator) {
        this.separator = separator;
    }
}
