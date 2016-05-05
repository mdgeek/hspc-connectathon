/*
 * #%L
 * Medication Administration Record
 * %%
 * Copyright (C) 2014 - 2016 Healthcare Services Platform Consortium
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package org.hspconsortium.cwfdemo.ui.mar;

import java.util.HashMap;
import java.util.Map;

import org.carewebframework.ui.zk.PopupDialog;
import org.carewebframework.ui.zk.ZKUtil;

import org.hl7.fhir.dstu3.model.MedicationOrder;

public class MedicationActionUtil {
    
    
    public static final String MED_ORDER_ZUL = "singleDoseMedicationOrderEntryForm.zul";
    
    public static final String MED_ADMIN_ZUL = "singleDoseMedicationAdministrationEntryForm.zul";
    
    public static final String MED_ORDER_KEY = "prescription";
    
    public static void show(boolean isOrder) {
        show(isOrder, null);
    }
    
    /**
     * Loads the appropriate template for either a medication order or a medication administration.
     * 
     * @param isOrder The type of template to load. True for orders, false for medication
     *            administrations
     * @param prescription The prescription to associate with an administration for the prefilling
     *            of the medication administration template
     */
    public static void show(boolean isOrder, MedicationOrder prescription) {
        String zul = null;
        if (isOrder) {
            zul = MED_ORDER_ZUL;
        } else {
            zul = MED_ADMIN_ZUL;
        }
        Map<Object, Object> args = new HashMap<>();
        args.put(MED_ORDER_KEY, prescription);
        PopupDialog.popup(ZKUtil.getResourcePath(MedicationActionUtil.class) + zul, args, true, true, true);
    }
}
