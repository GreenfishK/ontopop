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

import org.scify.jedai.similarityjoins.tokenbased.AbstractTokenBasedJoin;
import org.scify.jedai.similarityjoins.tokenbased.AllPairs;
import org.scify.jedai.similarityjoins.tokenbased.PPJoin;
import org.scify.jedai.similarityjoins.tokenbased.PartEnumJoin;

/**
 *
 * @author Georgios
 */
public enum TokenBasedSimilarityJoinMethod {
    ALL_PAIRS,
    PART_ENUM_JOIN,
    PP_JOIN;

    public static AbstractTokenBasedJoin getTokenBasedSimilarityJoin(TokenBasedSimilarityJoinMethod joinMethod) {
        switch (joinMethod) {
            case ALL_PAIRS:
                return new AllPairs(0.8f);
            case PP_JOIN:
                return new PPJoin(0.8f);
            case PART_ENUM_JOIN:
                return new PartEnumJoin(0.8f);
            default:
                return new PPJoin(0.8f);
        }
    }
}
