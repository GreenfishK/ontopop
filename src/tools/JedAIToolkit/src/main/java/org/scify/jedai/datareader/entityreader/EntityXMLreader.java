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
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.scify.jedai.datamodel.EntityProfile;

import java.io.IOException;
import java.util.*;

/**
 *
 * @author G.A.P. II
 */
public class EntityXMLreader extends AbstractEntityReader {

    private final Set<String> attributesToExclude;
    private final Map<String, EntityProfile> urlToEntity;

    public EntityXMLreader(String filePath) {
        super(filePath);
        attributesToExclude = new HashSet<>();
        urlToEntity = new HashMap<>();
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

        final SAXBuilder saxBuilder = new SAXBuilder();
        try {
            final Document document = saxBuilder.build(inputFilePath);
            readXMLdoc(document);
        } catch (JDOMException | IOException e) {
            Log.error("Error in entities reading!", e);
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
        return getMethodName() + ": it converts an XML file into a set of entity profiles.";
    }

    @Override
    public String getMethodName() {
        return "XML Reader";
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
                return "The " + getParameterName(0) + " determines the absolute path to the XML file that will be read into main memory.";
            case 1:
                return "The " + getParameterName(1) + " specifies the attributes that will be ignored during the creation of entity profiles.";
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
                return "Attributes To Exclude";
            default:
                return "invalid parameter id";
        }
    }

    private void readXMLdoc(Document document) {
        final Element classElement = document.getRootElement();

        final List<Element> dblpRoot = classElement.getChildren();
        for (final Element profile : dblpRoot) {
            final String profName = profile.getName();
            EntityProfile entityProfile = urlToEntity.get(profName);
            if (entityProfile == null) {
                entityProfile = new EntityProfile(profName);
                urlToEntity.put(profName, entityProfile);
                entityProfiles.add(entityProfile);
            }

            final List<Element> profAttributes = profile.getChildren();
            for (final Element attr : profAttributes) {
                String attName = attr.getName();
                if (attributesToExclude.contains(attName)) {
                    continue;
                }
                final String attValue = attr.getValue();
                entityProfile.addAttribute(attName, attValue);
            }
        }
    }

    public void setAttributesToExclude(String[] attributesNamesToExclude) {
        attributesToExclude.addAll(Arrays.asList(attributesNamesToExclude));
    }
}
