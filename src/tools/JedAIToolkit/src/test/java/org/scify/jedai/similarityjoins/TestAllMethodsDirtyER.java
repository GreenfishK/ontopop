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
package org.scify.jedai.similarityjoins;

import org.apache.log4j.BasicConfigurator;
import org.scify.jedai.datamodel.EntityProfile;
import org.scify.jedai.datamodel.SimilarityPairs;
import org.scify.jedai.datareader.entityreader.EntityCSVReader;
import org.scify.jedai.similarityjoins.tokenbased.PPJoin;
//import org.scify.jedai.similarityjoins.characterbased.AllPairs;
import java.util.List;

/**
 *
 * @author G.A.P. II
 */
public class TestAllMethodsDirtyER {

    public static void main(String[] args) {
        BasicConfigurator.configure();

        //String mainDirectory = "data" + File.separator + "cleanCleanErDatasets" + File.separator + "DBLP-ACM" + File.separator;
        String mainDirectory = "C:\\cygwin64\\Similarity-Search-and-Join\\Zinp\\";

        EntityCSVReader testJedaiEntityReader;// = new EntityCSVReader(mainDirectory + "DBLP2.csv");


        testJedaiEntityReader = new EntityCSVReader(mainDirectory + "testJedai.txt");
        testJedaiEntityReader.setAttributeNamesInFirstRow(false);
        //csvEntityReader.setIdIndex(0);
        testJedaiEntityReader.setSeparator(';');
        List<EntityProfile> csvACM = testJedaiEntityReader.getEntityProfiles();

        //System.out.println("CSV ACM Entity Profiles\t:\t" + csvACM.size());

        /*for (EntityProfile ep : csvACM)
        {
            for (Attribute at : ep.getAttributes())
            {
                System.out.println(at.getValue());
                //System.out.println("i "+at.getName());

            }
        }*/
        System.out.println("Input Entity Profiles\t:\t" + csvACM.size());
        AbstractSimilarityJoin CBJ;
        //CBJ = new FastSS(5);
        //CBJ = new AllPairs(0.6);
        //CBJ = new PassJoin(5);
        CBJ = new PPJoin(0.6f);
        //CBJ = new AllPairs(0.6, "attribute1", csvACM);

        SimilarityPairs simPairs = CBJ.executeFiltering("attribute1", csvACM);

        //System.out.println(CBJ.getRes_num());
        /*System.out.println("sim size="+simPairs.getSimilarities().length);
        IEntityClustering entityClusttering = new ConnectedComponentsClustering();
        entityClusttering.setSimilarityThreshold(0.0);
        EquivalenceCluster[] entityClusters = entityClusttering.getDuplicates(simPairs);
        for (EquivalenceCluster ec : entityClusters)
        {
        	int size=ec.getEntityIdsD1().size();
        	int size2=ec.getEntityIdsD2().size();
            System.out.println("size1="+(size));
            System.out.println("size2="+(size2));
            System.out.println("size detailed?="+(size*(size-1)/2));

        }
        System.out.println("size="+entityClusters.length);*/


    }
}
