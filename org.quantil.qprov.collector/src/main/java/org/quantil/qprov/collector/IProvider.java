/*******************************************************************************
 * Copyright (c) 2020 the QProv contributors.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package org.quantil.qprov.collector;

import java.util.List;

import org.quantil.qprov.core.entities.QPU;

public interface IProvider {

    String getProviderId();

    /**
     * check if we need to authenticate before the first query
     *
     * @return true or false
     */
    boolean preAuthenticationNeeded();

    /**
     * authenticate to provider api
     *
     * @return boolean result of authentication attempt
     */
    boolean authenticate(String token);

    /**
     * fetch all data from provider
     *
     * @return boolean result of fetch attempt
     */
    boolean collect();

    /**
     * fetch qpu data from provider
     *
     * @return list of collected qpus
     */
    List<QPU> collectQPUs();
}
