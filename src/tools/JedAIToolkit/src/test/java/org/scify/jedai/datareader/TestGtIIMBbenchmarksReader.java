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
import org.scify.jedai.datamodel.IdDuplicates;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.groundtruthreader.GtIIMBbenchmarksReader;

import java.util.List;
import java.util.Set;

/**
 *
 * @author G.A.P. II
 */

public class TestGtIIMBbenchmarksReader {
    public static void main(String[] args) {
    	String entityFilePath1 = "/home/ethanos/Downloads/JEDAIconfirmedBenchmarks/iimb2009/001/abox.owlprof";
    	String entityFilePath2 = "/home/ethanos/Downloads/JEDAIconfirmedBenchmarks/iimb2009/002/abox.owlprof";
    	String gtFilePath = "/home/ethanos/Downloads/JEDAIconfirmedBenchmarks/iimb2009/002/rdfalign.rdf";
        String baseFilePath = "/home/ethanos/Downloads/JEDAIconfirmedBenchmarks/iimb2009/001/rdfalign.rdf";
        EntitySerializationReader esr1 = new EntitySerializationReader(entityFilePath1);
        EntitySerializationReader esr2 = new EntitySerializationReader(entityFilePath2);
        GtIIMBbenchmarksReader gtOAEIbenchmarksReader = new GtIIMBbenchmarksReader(gtFilePath, baseFilePath);

        List<EntityProfile> profiles1 = esr1.getEntityProfiles();
        List<EntityProfile> profiles2 = esr2.getEntityProfiles();
//        for (EntityProfile profile : profiles1) {
//            System.out.println("\n\n" + profile.getEntityUrl());
//            for (Attribute attribute : profile.getAttributes()) {
//                System.out.print(attribute.toString());
//                System.out.println();
//            }
//        }
        Set<IdDuplicates> duplicates = gtOAEIbenchmarksReader.getDuplicatePairs(profiles1, profiles2);
        for (IdDuplicates duplicate : duplicates) {
        	int id1 = duplicate.getEntityId1();
            System.out.println(id1 + " " + duplicate.getEntityId2());

        }
        //gtOAEIbenchmarksReader.storeSerializedObject(duplicates, "C:\\Users\\G.A.P. II\\Downloads\\cddbDuplicates");
    }
}