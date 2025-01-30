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

import org.apache.log4j.BasicConfigurator;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datareader.entityreader.EntityCSVReader;

import java.util.List;
import java.util.Set;
import org.scify.jedai.datamodel.IdDuplicates;
import org.scify.jedai.datareader.groundtruthreader.GtCSVReader;

/**
 *
 * @author G.A.P. II
 */

public class TestEntityCSVReader {
    public static void main(String[] args) {
        BasicConfigurator.configure();
        
        String mainDirectory = "/home/gap2/Downloads/DBLP-ACM/";
        EntityCSVReader csvReader = new EntityCSVReader(mainDirectory + "DBLP2.csv");
        csvReader.setAttributeNamesInFirstRow(true);
        csvReader.setSeparator(',');
//        csvReader.setAttributesToExclude(new int[]{1});
        csvReader.setIdIndex(0);
        List<EntityProfile> profilesD1 = csvReader.getEntityProfiles();
        System.out.println("Entities from Dataset 1\t:\t" + profilesD1.size());

        csvReader = new EntityCSVReader(mainDirectory + "ACM.csv");
        csvReader.setAttributeNamesInFirstRow(true);
        csvReader.setSeparator(',');
//        csvReader.setAttributesToExclude(new int[]{1});
        csvReader.setIdIndex(0);
        List<EntityProfile> profilesD2 = csvReader.getEntityProfiles();
        System.out.println("Entities from Dataset 2\t:\t" + profilesD2.size());
        
        GtCSVReader gtCsvReader = new GtCSVReader(mainDirectory + "DBLP-ACM_perfectMapping.csv");
        gtCsvReader.setIgnoreFirstRow(true);
        gtCsvReader.setSeparator(",");
        Set<IdDuplicates> duplicates = gtCsvReader.getDuplicatePairs(profilesD1, profilesD2);
        
        System.out.println("Duplicates\t:\t" + duplicates.size());
    }
}
