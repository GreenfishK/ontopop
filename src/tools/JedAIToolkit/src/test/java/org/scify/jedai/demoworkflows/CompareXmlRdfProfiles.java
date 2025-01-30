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
package org.scify.jedai.demoworkflows;

import org.apache.log4j.BasicConfigurator;
import org.scify.jedai.datamodel.Attribute;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datareader.entityreader.EntityCSVReader;
import org.scify.jedai.datareader.entityreader.EntityRDFReader;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author GAP2
 */
public class CompareXmlRdfProfiles {

    private final static String prefix = "cleanCleanErDatasets/";

    public static void main(String[] args) {
        BasicConfigurator.configure();

        String mainDirectory = "data" + File.separator + "cleanCleanErDatasets" + File.separator + "DBLP-ACM" + File.separator;

//        EntityRDFReader rdfEntityReader = new EntityRDFReader("C:\\Users\\GAP2\\Documents\\NetBeansProjects\\jedai-core\\data\\cleanCleanErDatasets\\DBLP2toRdf.xml");
        EntityRDFReader rdfEntityReader = new EntityRDFReader("C:\\Users\\GAP2\\Documents\\NetBeansProjects\\jedai-core\\data\\cleanCleanErDatasets\\ACMtoRdf.xml");
        List<EntityProfile> rdfDBLP = rdfEntityReader.getEntityProfiles();
        System.out.println("RDF DBLP Entity Profiles\t:\t" + rdfDBLP.size());

//        EntityCSVReader csvEntityReader = new EntityCSVReader(mainDirectory + "DBLP2.csv");
        EntityCSVReader csvEntityReader = new EntityCSVReader(mainDirectory + "ACM.csv");
        csvEntityReader.setAttributeNamesInFirstRow(true);
        csvEntityReader.setIdIndex(0);
        csvEntityReader.setSeparator(',');
        List<EntityProfile> csvDBLP = csvEntityReader.getEntityProfiles();
        System.out.println("CSV DBLP Entity Profiles\t:\t" + csvDBLP.size());

        Map<String, EntityProfile> profiles = new HashMap<>();
        for (EntityProfile rdfEP : rdfDBLP) {
            int index = rdfEP.getEntityUrl().indexOf(prefix);
            String key = rdfEP.getEntityUrl().substring(index + prefix.length());
            profiles.put(key, rdfEP);
        }

        for (EntityProfile csvEP : csvDBLP) {
            EntityProfile rdfProfile = profiles.get(csvEP.getEntityUrl());
            if (rdfProfile.getAttributes().size() != csvEP.getAttributes().size()) {
                System.out.println("\n\nNew pair");
                System.out.println(rdfProfile.getEntityUrl() + "\t\t" + csvEP.getEntityUrl());
                for (Attribute a : rdfProfile.getAttributes()) {
                    System.out.println(a.getName() + "\t" + a.getValue());
                }
                for (Attribute a : csvEP.getAttributes()) {
                    System.out.println(a.getName() + "\t" + a.getValue());
                }
            }
        }
    }
}
