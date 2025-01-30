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

import java.util.List;
import org.scify.jedai.datamodel.IdDuplicates;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.groundtruthreader.GtCSVReader;
import java.util.Set;
import org.apache.log4j.BasicConfigurator;
import org.scify.jedai.datamodel.EntityProfile;

/**
 *
 * @author G.A.P. II
 */

public class TestGtCSVReader {
    public static void main(String[] args) {
        BasicConfigurator.configure();
        
        String entityFilePath = "C:\\Users\\GAP2\\Downloads\\cddbProfiles";
        String gtFilePath = "C:\\Users\\GAP2\\Downloads\\cd_gold.csv";
        EntitySerializationReader esr = new EntitySerializationReader(entityFilePath);
        List<EntityProfile> profiles = esr.getEntityProfiles();
        System.out.println("Loaded profiles\t:\t" + profiles.size());
        
        GtCSVReader csvReader = new GtCSVReader(gtFilePath);
        csvReader.setIgnoreFirstRow(true);
        csvReader.setSeparator(";");
        Set<IdDuplicates> duplicates = csvReader.getDuplicatePairs(profiles);
        System.out.println("No of duplicates\t:\t" + duplicates.size());
//        for (EntityProfile profile : profiles) {
//            System.out.println("\n\n" + profile.getEntityUrl());
//            for (Attribute attribute : profile.getAttributes()) {
//                System.out.println(attribute.toString());
//            }
//        }
        csvReader.storeSerializedObject(duplicates, "E:\\Users\\GAP2\\Downloads\\cddbDuplicates");
    }
}