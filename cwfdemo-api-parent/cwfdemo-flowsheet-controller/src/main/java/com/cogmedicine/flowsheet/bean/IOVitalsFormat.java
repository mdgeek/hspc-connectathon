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

package com.cogmedicine.flowsheet.bean;

import java.text.SimpleDateFormat;
import java.util.*;

public class IOVitalsFormat {

    public final static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    private IOGridModel ioGridModel;
    private Map<String, List> data;

    public IOVitalsFormat(IOGridModel ioGridModel){
        this.ioGridModel = ioGridModel;

        convertToFlowsheetGridModel();
    }

    private void convertToFlowsheetGridModel(){
        List<Date> headers = ioGridModel.getHeaderNames();
        List<String> rowNames = ioGridModel.getRowNames();
        List<List<String>> rows = ioGridModel.getRows();

        List<Map> rowList = new ArrayList<Map>();
        for(int i = 0; i < rowNames.size(); i++){
            List<Map> cellList = new ArrayList<Map>();
            List<String> row = rows.get(i);
            for(int j = 0; j < row.size(); j++){
                if(row.get(j) != null){
                    Map<String, String> cellEntry = new HashMap<String, String>();
                    cellEntry.put("value", row.get(j));
                    cellEntry.put("timestamp", dateFormat.format(headers.get(j)));

                    cellList.add(cellEntry);
                }
            }

            String rowName = rowNames.get(i);
            Map<String, List> rowEntry = new HashMap<String, List>();
            rowEntry.put(rowName, cellList);

            rowList.add(rowEntry);
        }

        data = new HashMap<String, List>();
        data.put("data", rowList);
    }

    public Map<String, List> getFlowsheetGridModel(){
        return data;
    }
}
