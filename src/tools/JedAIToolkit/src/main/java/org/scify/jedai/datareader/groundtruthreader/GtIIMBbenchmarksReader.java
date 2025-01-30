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
package org.scify.jedai.datareader.groundtruthreader;

import com.esotericsoftware.minlog.Log;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author G.A.P. II
 */
public class GtIIMBbenchmarksReader extends GtRDFReader {

    private final String baseGTfile;

    public GtIIMBbenchmarksReader(String filePath, String baseFile) {
        super(filePath);
        baseGTfile = baseFile;
    }

    @Override
    public String getMethodInfo() {
        return getMethodName() + ": it converts two ground-truth files of an IIMB Benchmark dataset into a set of pairs of duplicate entity profiles.";
    }

    @Override
    public String getMethodName() {
        return "RDF IIMB Benchmark Ground-truth Reader";
    }

    // we parse both gt files and keep as duplicates the entity-instances
    // being at the same corresponding order, since they are linked to the 
    // same entity1-instance in both files
    @Override
    protected void performReading() {
        try {
            final DocumentBuilder dBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            
            final Document doc1 = dBuilder.parse(baseGTfile);
            doc1.getDocumentElement().normalize();
            
            final Document doc2 = dBuilder.parse(inputFilePath);
            
            final NodeList nList1 = doc1.getElementsByTagName("Cell");
            final NodeList nList2 = doc2.getElementsByTagName("Cell");
            for (int temp = 0; temp < nList1.getLength(); temp++) {
                final Node nNode1 = nList1.item(temp);
                final Node nNode2 = nList2.item(temp);
                int entityId1;
                int entityId2;
                if (nNode1.getNodeType() == Node.ELEMENT_NODE) {
                    Element eElement = (Element) nNode1;
                    final Element eElement1 = (Element) eElement.getElementsByTagName("entity2").item(0);
                    entityId1 = urlToEntityId1.get(eElement1.getAttribute("rdf:resource"));
                    if (nNode1.getNodeType() == Node.ELEMENT_NODE) {
                        eElement = (Element) nNode2;
                        
                        final Element eElement2 = (Element) eElement.getElementsByTagName("entity2").item(0);
                        entityId2 = urlToEntityId2.get(eElement2.getAttribute("rdf:resource"));
                        duplicatesGraph.addEdge(entityId1, entityId2);
                    }

                }
            }
        } catch (ParserConfigurationException | SAXException | IOException ex) {
            Log.error("Error in duplicates reading!", ex);
        }
    }
}
