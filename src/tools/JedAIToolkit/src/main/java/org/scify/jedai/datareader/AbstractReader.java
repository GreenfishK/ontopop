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
package org.scify.jedai.datareader;

import org.scify.jedai.datamodel.EntityProfile;
import com.esotericsoftware.minlog.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;

/**
 *
 * @author G.A.P. II
 */
public abstract class AbstractReader implements IDataReader {

    protected String inputFilePath;

    public AbstractReader(String filePath) {
        inputFilePath = filePath;
    }

    public static Object loadSerializedObject(String fileName) {
        Object object;
        try {
            final InputStream file = new FileInputStream(fileName);
            final InputStream buffer = new BufferedInputStream(file);
            try (ObjectInput input = new ObjectInputStream(buffer)) {
                object = input.readObject();
            }
        } catch (ClassNotFoundException cnfEx) {
            Log.error("Missing class", cnfEx);
            return null;
        } catch (IOException ioex) {
            Log.error("Error in data reading", ioex);
            return null;
        }

        return object;
    }

    public void convertToRDFfile(List<EntityProfile> profiles, String outputPath, String basicURI) {
        try {
            FileWriter fileWriter = new FileWriter(outputPath);
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.println("<?xml version=\"1.0\"?>");
            printWriter.println();
            printWriter.println("<rdf:RDF");
            printWriter.println("xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"");
            printWriter.println("xmlns:obj=\"https://www.w3schools.com/rdf/\">");
            profiles.stream().map((profile) -> {
                printWriter.println("<rdf:Description rdf:about=\"" + basicURI + profile.getEntityUrl().replace("&", "") + "\">");
                return profile;
            }).map((profile) -> {
                profile.getAttributes().stream().map((attribute) -> {
                    printWriter.print("<obj:" + attribute.getName().replace("&", "") + ">");
                    return attribute;
                }).map((attribute) -> {
                    printWriter.print(attribute.getValue().replace("&", ""));
                    return attribute;
                }).forEachOrdered((attribute) -> {
                    printWriter.println("</obj:" + attribute.getName().replace("&", "") + ">");
                });
                return profile;
            }).forEachOrdered((_item) -> {
                printWriter.println("</rdf:Description>");
            });
            printWriter.println("</rdf:RDF>");
            printWriter.close();
        } catch (IOException ioex) {
            Log.error("Error in converting to RDF", ioex);
        }
    }

    @Override
    public void storeSerializedObject(Object object, String outputPath) {
        try {
            final OutputStream file = new FileOutputStream(outputPath);
            final OutputStream buffer = new BufferedOutputStream(file);
            try (ObjectOutput output = new ObjectOutputStream(buffer)) {
                output.writeObject(object);
            }
        } catch (IOException ioex) {
            Log.error("Error in storing serialized object", ioex);
        }
    }
}
