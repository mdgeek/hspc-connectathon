/*
 * Copyright 2017 Cognitive Medical Systems, Inc (http://www.cognitivemedicine.com).
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @author Jeff Chung
 */

package com.cogmedicine.flowsheet.service;

import ca.uhn.fhir.model.dstu2.resource.Subscription;
import ca.uhn.fhir.model.dstu2.valueset.SubscriptionChannelTypeEnum;
import ca.uhn.fhir.model.dstu2.valueset.SubscriptionStatusEnum;
import ca.uhn.fhir.rest.api.MethodOutcome;

/**
 * Created by Jeff on 4/3/2017.
 */
public class SubscriptionService {

    /**
     * Returns the subscriptionId
     *
     * @param subscription
     * @return
     */
    public String createSubscription(Subscription subscription) {
        MethodOutcome methodOutcome = FhirServiceDstu2.getClient().create().resource(subscription).execute();
        return methodOutcome.getId().getIdPart();
    }

    /**
     * Returns the subscriptionId
     *
     * @param subscription
     * @return
     */
    public void updateSubscription(Subscription subscription) {
        FhirServiceDstu2.getClient().update().resource(subscription).execute();
    }

    /**
     * Deletes a subscription by subscription id
     *
     * @param subscriptionId
     */
    public void deleteSubscription(String subscriptionId) {
        FhirServiceDstu2.getClient().delete().resourceById(Subscription.class.getSimpleName(), subscriptionId).execute();
    }

    /**
     * Creates a vital subscription by patient id
     *
     * @param patientId
     * @return
     */
    public String createVitalSubscription(String patientId, String tminusTime) {
        Subscription subscription = new Subscription();
        subscription.getChannel().setType(SubscriptionChannelTypeEnum.REST_HOOK);
        subscription.setCriteria("Observation?subject=Patient/" + patientId + "&effectiveDate=Tminus" + tminusTime);
        subscription.setStatus(SubscriptionStatusEnum.REQUESTED);

        return createSubscription(subscription);
    }

    /**
     * Update a vital subscription by patient id
     *
     * @param patientId
     * @return
     */
    public void updateVitalSubscription(String subscriptionId, String patientId) {
        Subscription subscription = FhirServiceDstu2.searchResource(Subscription.class, subscriptionId);
        subscription.setCriteria("Observation?subject=Patient/" + patientId);
        updateSubscription(subscription);
    }
}
