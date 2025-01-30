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
import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.rdfhdt.hdt.exceptions.NotFoundException;
import org.rdfhdt.hdt.hdt.HDT;
import org.rdfhdt.hdt.hdt.HDTManager;
import org.rdfhdt.hdt.triples.IteratorTripleString;
import org.rdfhdt.hdt.triples.TripleString;
import org.scify.jedai.datamodel.EntityProfile;

import java.io.IOException;
import java.util.*;

/**
 *
 * @author Manos Thanos
 */
public class EntityHDTRDFReader extends AbstractEntityReader {

    private String prefix;
    private final Set<String> attributesToExclude;
    private final Map<String, EntityProfile> urlToEntity;

    public EntityHDTRDFReader(String filePath) {
        super(filePath);

        prefix = "";
        urlToEntity = new HashMap<>();
        attributesToExclude = new HashSet<>();
        attributesToExclude.add("owl:sameAs");
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

        //load the rdf model from the input file
        try {
            readModel(inputFilePath);
        } catch (IOException ex) {
            Log.error("Error in entities reading!", ex);
            return null;
        } catch (NotFoundException e) {
            Log.error(e.getMessage());
        }

        return entityProfiles;
    }

    @Override
    public String getMethodConfiguration() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        attributesToExclude.forEach((attributeName) -> sb.append(attributeName).append(","));
        sb.append("}");

        return getParameterName(0) + "=" + inputFilePath + "\t"
                + getParameterName(1) + "=" + sb.toString();
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it converts an RDF file of any format into a set of entity profiles.";
    }

    @Override
    public String getMethodName() {
        return "HDT_RDF Reader";
    }

    @Override
    public String getMethodParameters() {
        return getMethodName() + " involves two parameters:\n"
                + "1)" + getParameterDescription(0) + ".\n"
                + "2)" + getParameterDescription(1) + ".";
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
        obj2.put("class", "java.util.Set<String>");
        obj2.put("name", getParameterName(1));
        obj2.put("defaultValue", "-");
        obj2.put("minValue", "-");
        obj2.put("maxValue", "-");
        obj2.put("stepValue", "-");
        obj2.put("description", getParameterDescription(1));

        final JsonArray array = new JsonArray();
        array.add(obj1);
        array.add(obj2);
        return array;
    }

    @Override
    public String getParameterDescription(int parameterId) {
        switch (parameterId) {
            case 0:
                return "The " + getParameterName(0) + " determines the absolute path to the RDF file that will be read into main memory.";
            case 1:
                return "The " + getParameterName(1) + " specifies the predicates that will be ignored during the creation of entity profiles.";
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
                return "Predicates To Exclude";
            default:
                return "invalid parameter id";
        }
    }

    private void readModel(String inpFIle) throws IOException, NotFoundException {
        // Search pattern: Empty string means "any"
        try ( //read each ntriples
                //get spo, create a separate profile for each separate subject,
                //with Attribute=predicate and Value=object
                HDT hdt = HDTManager.loadHDT(inpFIle, null)) {
            // Search pattern: Empty string means "any"
            final IteratorTripleString iter = hdt.search("", "", "");
            //final StmtIterator iter = ts.m.listStatements();
            while (iter.hasNext()) {
                TripleString stmt = iter.next();

                final CharSequence predicate = stmt.getPredicate();
                final String pred = predicate.toString();
                if (attributesToExclude.contains(pred)) {
                    continue;
                }

                final CharSequence subject = stmt.getSubject();
                String sub = subject.toString();
                if (!prefix.equals("")) {
                    sub = sub.replace(prefix, "");
                }

                final CharSequence object = stmt.getObject();
                final String obj = object.toString();

                //if already exists a profile for the subject, simply add po as <Att>-<Value>
                EntityProfile entityProfile = urlToEntity.get(sub);
                if (entityProfile == null) {
                    entityProfile = new EntityProfile(sub);
                    entityProfiles.add(entityProfile);
                    urlToEntity.put(sub, entityProfile);
                }

                if (!obj.isEmpty()) {
                    entityProfile.addAttribute(pred, obj);
                }
            }
            // IMPORTANT: Close hdt when no longer needed
        }
    }

    public void setAttributesToExclude(String[] attributesNamesToExclude) {
        attributesToExclude.addAll(Arrays.asList(attributesNamesToExclude));
    }

    public void setPrefixOmission(String prefix) {
        this.prefix = prefix;
    }
}
