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

package org.scify.jedai.blockprocessing.comparisoncleaning;

import java.util.List;
import java.util.Set;
import org.apache.log4j.BasicConfigurator;
import org.scify.jedai.blockprocessing.blockcleaning.BlockFiltering;
import org.scify.jedai.blockprocessing.blockcleaning.SizeBasedBlockPurging;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.IdDuplicates;
import org.scify.jedai.datareader.entityreader.EntitySerializationReader;
import org.scify.jedai.datareader.groundtruthreader.GtSerializationReader;
import org.scify.jedai.datareader.groundtruthreader.IGroundTruthReader;
import org.scify.jedai.utilities.BlocksPerformance;
import org.scify.jedai.utilities.datastructures.AbstractDuplicatePropagation;
import org.scify.jedai.utilities.datastructures.BilateralDuplicatePropagation;

/**
 *
 * @author gap2
 */
public class TestBlast {

    public static void main(String[] args) {
        BasicConfigurator.configure();

        String fileName = "/home/gap2/data/JedAIdata/blocks/dblpAcm/sb";
        float bpRatio = 0.004f;
        float bfRatio = 0.5000000000000003f;

        final EntitySerializationReader inReader = new EntitySerializationReader(null);
        final List<AbstractBlock> blocks = (List<AbstractBlock>) inReader.loadSerializedObject(fileName);

        final SizeBasedBlockPurging sbbp = new SizeBasedBlockPurging(bpRatio);
        final List<AbstractBlock> purgedBlocks = sbbp.refineBlocks(blocks);

        final BlockFiltering bf = new BlockFiltering(bfRatio);
        final List<AbstractBlock> filteredBlocks = bf.refineBlocks(purgedBlocks);

        final IGroundTruthReader gtReader = new GtSerializationReader("/home/gap2/data/JedAIdata/datasets/cleanCleanErDatasets/dblpAcmIdDuplicates");
        final Set<IdDuplicates> duplicatePairs = gtReader.getDuplicatePairs(null);
        final AbstractDuplicatePropagation adp = new BilateralDuplicatePropagation(duplicatePairs);

        BlocksPerformance blockStats = new BlocksPerformance(filteredBlocks, adp);
        blockStats.setStatistics();
        blockStats.printStatistics(0, "", "");
        final float originalPC = blockStats.getPc();
        System.out.println("Original PC\t:\t" + originalPC);

//        final BLAST blast = new BLAST();
        final WeightedNodePruning blast = new WeightedNodePruning();
        final List<AbstractBlock> newBlocks = blast.refineBlocks(filteredBlocks);
        blockStats = new BlocksPerformance(newBlocks, adp);
        blockStats.setStatistics();
        blockStats.printStatistics(0, "", "");
        final float newPC = blockStats.getPc();
        System.out.println("New PC\t:\t" + newPC);
    }
}
