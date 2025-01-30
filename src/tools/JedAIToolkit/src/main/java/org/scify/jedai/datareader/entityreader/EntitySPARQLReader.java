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

import org.scify.jedai.datamodel.EntityProfile;

import com.esotericsoftware.minlog.Log;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.atlas.json.JsonArray;
import org.apache.jena.atlas.json.JsonObject;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;

public class EntitySPARQLReader extends AbstractEntityReader {

    private String password;
    private String user;

    private final Set<String> attributesToExclude;
    private final Map<String, EntityProfile> urlToEntity;

    public EntitySPARQLReader(String endpointUrl) {
        super(endpointUrl);

        password = null;
        user = null;

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
            readEndpoint(inputFilePath);
        } catch (Exception ex) {
            Log.error("Error in data reading", ex);
            return null;
        }

        return entityProfiles;
    }

    @Override
    public String getMethodConfiguration() {
        final StringBuilder sb = new StringBuilder();
        sb.append("{");
        attributesToExclude.forEach((attributeName) -> {
            sb.append(attributeName).append(",");
        });
        sb.append("}");

        return getParameterName(0) + "=" + inputFilePath + "\t"
                + getParameterName(1) + "=" + user + "\t"
                + getParameterName(2) + "=" + password + "\t"
                + getParameterName(3) + "=" + sb.toString();
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it converts a SPARQL endpoint into a set of entity profiles.";
    }

    @Override
    public String getMethodName() {
        return "SPARQL Endpoint Reader";
    }

    @Override
    public String getMethodParameters() {
        return getMethodName() + " involves four parameters:\n"
                + "1)" + getParameterDescription(0) + ".\n"
                + "2)" + getParameterDescription(1) + ".\n"
                + "3)" + getParameterDescription(2) + ".\n"
                + "4)" + getParameterDescription(3) + ".";
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
        obj2.put("class", "java.lang.String");
        obj2.put("name", getParameterName(1));
        obj2.put("defaultValue", "-");
        obj2.put("minValue", "-");
        obj2.put("maxValue", "-");
        obj2.put("stepValue", "-");
        obj2.put("description", getParameterDescription(1));

        final JsonObject obj3 = new JsonObject();
        obj3.put("class", "java.lang.String");
        obj3.put("name", getParameterName(2));
        obj3.put("defaultValue", "-");
        obj3.put("minValue", "-");
        obj3.put("maxValue", "-");
        obj3.put("stepValue", "-");
        obj3.put("description", getParameterDescription(2));

        final JsonObject obj4 = new JsonObject();
        obj4.put("class", "java.util.Set<String>");
        obj4.put("name", getParameterName(3));
        obj4.put("defaultValue", "-");
        obj4.put("minValue", "-");
        obj4.put("maxValue", "-");
        obj4.put("stepValue", "-");
        obj4.put("description", getParameterDescription(3));

        final JsonArray array = new JsonArray();
        array.add(obj1);
        array.add(obj2);
        array.add(obj3);
        array.add(obj4);
        return array;
    }

    @Override
    public String getParameterDescription(int parameterId) {
        switch (parameterId) {
            case 0:
                return "The " + getParameterName(0) + " determines the URL the endpoint, from which the entity profiles will be retrieved.";
            case 1:
                return "The " + getParameterName(1) + " determines the username that corresponds to an endpoint account with the necessary access rights.";
            case 3:
                return "The " + getParameterName(2) + " determines the password that corresponds to the username with the necessary access rights.";
            case 4:
                return "The " + getParameterName(3) + " determines the predicates that will be ignored during the creation of entity profiles.";
            default:
                return "invalid parameter id";
        }
    }

    @Override
    public String getParameterName(int parameterId) {
        switch (parameterId) {
            case 0:
                return "SPARQL Endpoint URL";
            case 1:
                return "Username";
            case 2:
                return "Password";
            case 3:
                return "Predicates To Exclude";
            default:
                return "invalid parameter id";
        }
    }

    private void readEndpoint(String endpointUrl) {
        //read each ntriples
        //get spo, create a separate profile for each separate subject,
        //with Attribute=predicate and Value=object
        final String sparqlQueryString1 = "select ?a ?b ?c where {?a ?b ?c}";

        final Query query = QueryFactory.create(sparqlQueryString1);
        try (QueryExecution qexec = QueryExecutionFactory.sparqlService(endpointUrl, query)) {
            final ResultSet results = qexec.execSelect();
            //ResultSetFormatter.out(System.out, results, query);
            //results = qexec.execSelect();
            
            while (results.hasNext()) {
                final QuerySolution qs = results.next();
                
                final String sub = qs.get("a").toString();
                final String pred = qs.get("b").toString();
                final String obj = qs.get("c").toString();
                if (attributesToExclude.contains(pred)) {
                    continue;
                }
                
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
        }
        //ResultSetFormatter.out(System.out, results, query);
        //results = qexec.execSelect();
    }

    public void setAttributesToExclude(String[] attributesNamesToExclude) {
        attributesToExclude.addAll(Arrays.asList(attributesNamesToExclude));
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUser(String user) {
        this.user = user;
    }
}
