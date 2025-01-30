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

package org.scify.jedai.utilities.enumerations;

import org.scify.jedai.entityclustering.CenterClustering;
import org.scify.jedai.entityclustering.ConnectedComponentsClustering;
import org.scify.jedai.entityclustering.CorrelationClustering;
import org.scify.jedai.entityclustering.CutClustering;
import org.scify.jedai.entityclustering.IEntityClustering;
import org.scify.jedai.entityclustering.MarkovClustering;
import org.scify.jedai.entityclustering.MergeCenterClustering;
import org.scify.jedai.entityclustering.RicochetSRClusteringCCER;

/**
 *
 * @author GAP2
 */
public enum EntityClusteringDerMethod {
    CENTER_CLUSTERING,
    CONNECTED_COMPONENTS_CLUSTERING,
    CUT_CLUSTERING,
    MARKOV_CLUSTERING,
    MERGE_CENTER_CLUSTERING,
    RICOCHET_SR_CLUSTERING,
    CORRELATION_CLUSTERING;

    public static IEntityClustering getDefaultConfiguration(EntityClusteringDerMethod ecMethod) {
        switch (ecMethod) {
            case CENTER_CLUSTERING:
                return new CenterClustering();
            case CONNECTED_COMPONENTS_CLUSTERING:
                return new ConnectedComponentsClustering();
            case CUT_CLUSTERING:
                return new CutClustering();
            case MARKOV_CLUSTERING:
                return new MarkovClustering();
            case MERGE_CENTER_CLUSTERING:
                return new MergeCenterClustering();
            case RICOCHET_SR_CLUSTERING:
                return new RicochetSRClusteringCCER();
            case CORRELATION_CLUSTERING:
                return new CorrelationClustering();
            default:
                return new ConnectedComponentsClustering();
        }
    }
}
