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

package org.scify.jedai.textmodels;

import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;
import gr.demokritos.iit.jinsect.documentModel.representations.DocumentWordGraph;

/**
 *
 * @author gap2
 */

public class TokenNGramGraphs extends GraphModel {
    
    public TokenNGramGraphs (int dId, int n, RepresentationModel model, SimilarityMetric simMetric, String iName) {
        super(dId, n, model, simMetric, iName);

        NO_OF_DOCUMENTS[datasetId]++;
        graphModel = new DocumentWordGraph(nSize, nSize, nSize);
    }

    @Override
    public void updateModel(String text) {
        final DocumentWordGraph tempGraph = new DocumentWordGraph(nSize, nSize, nSize);
        tempGraph.setDataString(text.toLowerCase());
        
        getGraphModel().merge(tempGraph, 1 - (NO_OF_DOCUMENTS[datasetId]-1)/NO_OF_DOCUMENTS[datasetId]);
    }
}