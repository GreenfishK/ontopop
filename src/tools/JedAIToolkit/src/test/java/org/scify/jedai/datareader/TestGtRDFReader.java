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
import org.scify.jedai.datareader.entityreader.EntityRDFReader;
import org.scify.jedai.datareader.groundtruthreader.GtRDFReader;

import java.util.List;
import java.util.Set;

/**
 *
 * @author G.A.P. II
 */

public class TestGtRDFReader {
    public static void main(String[] args) {
        String FilePath = "/home/ethanos/Downloads/dbpedia_2015-10.nt";
        EntityRDFReader rdfReader = new EntityRDFReader(FilePath);
        GtRDFReader gtrdfReader = new GtRDFReader(FilePath);
        rdfReader.setAttributesToExclude(new String[]{"http://www.w3.org/2002/07/owl#sameAs"});
        List<EntityProfile> profiles = rdfReader.getEntityProfiles();
        Set<IdDuplicates> duplicates = gtrdfReader.getDuplicatePairs(profiles);
        for (IdDuplicates duplicate : duplicates) {
        	int id1 = duplicate.getEntityId1();
            System.out.println(id1 + " " + duplicate.getEntityId2());

        }
    }
}