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
package org.scify.jedai.datamodel;

import java.util.ArrayList;
import java.util.List;
import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;

/**
 *
 * @author GAP2
 */

public class RepModelSimMetricCombo {

    private final RepresentationModel repModel;
    private final SimilarityMetric simMetric;
    
    public RepModelSimMetricCombo(RepresentationModel rModel, SimilarityMetric sMetric) {
        repModel = rModel;
        simMetric = sMetric;
    }

    public RepresentationModel getRepModel() {
        return repModel;
    }

    public SimilarityMetric getSimMetric() {
        return simMetric;
    }
    
    public static List<RepModelSimMetricCombo> getAllValidCombos() {
        final List<RepModelSimMetricCombo> validCombos = new ArrayList<>();
        for (RepresentationModel rModel : RepresentationModel.values()) {
            final List<SimilarityMetric> metrics = SimilarityMetric.getModelCompatibleSimMetrics(rModel);
            metrics.forEach((sMetric) -> {
                validCombos.add(new RepModelSimMetricCombo(rModel, sMetric));
            });
        }
        return validCombos;
    }
}
