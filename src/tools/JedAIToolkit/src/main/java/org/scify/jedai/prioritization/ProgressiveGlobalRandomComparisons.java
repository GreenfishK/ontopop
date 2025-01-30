/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.scify.jedai.prioritization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.scify.jedai.datamodel.AbstractBlock;
import org.scify.jedai.datamodel.Comparison;
import org.scify.jedai.datamodel.ComparisonIterator;
import org.scify.jedai.utilities.enumerations.WeightingScheme;

/**
 *
 * @author gap2
 */
public class ProgressiveGlobalRandomComparisons extends ProgressiveGlobalTopComparisons {

    public ProgressiveGlobalRandomComparisons(int budget) {
        super(budget, WeightingScheme.CBS);
    }

    @Override
    protected Iterator<Comparison> processDecomposedBlocks(List<AbstractBlock> blocks) {
        final Random r = new Random();
        final List<Comparison> allComparisons = new ArrayList<>();
        for (AbstractBlock block : blocks) {
            final ComparisonIterator cIterator = block.getComparisonIterator();
            while (cIterator.hasNext()) {
                final Comparison currentComparison = cIterator.next();
                allComparisons.add(currentComparison);
                if (comparisonsBudget < allComparisons.size()) {
                    allComparisons.remove(r.nextInt(allComparisons.size()));
                }
            }
        }

        Collections.shuffle(allComparisons);
        return allComparisons.iterator();
    }
}
