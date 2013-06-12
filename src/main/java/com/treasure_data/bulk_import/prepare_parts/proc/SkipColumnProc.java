//
// Treasure Data Bulk-Import Tool in Java
//
// Copyright (C) 2012 - 2013 Muga Nishizawa
//
//    Licensed under the Apache License, Version 2.0 (the "License");
//    you may not use this file except in compliance with the License.
//    You may obtain a copy of the License at
//
//        http://www.apache.org/licenses/LICENSE-2.0
//
//    Unless required by applicable law or agreed to in writing, software
//    distributed under the License is distributed on an "AS IS" BASIS,
//    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//    See the License for the specific language governing permissions and
//    limitations under the License.
//
package com.treasure_data.bulk_import.prepare_parts.proc;

import java.util.logging.Logger;

import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.util.CsvContext;

public class SkipColumnProc implements ColumnProc, CellProcessor {

    private static final Logger LOG = Logger.getLogger(
            SkipColumnProc.class.getName());

    protected ColumnProc next;

    public SkipColumnProc(ColumnProc next) {
        this.next = next;
    }

    public int getIndex() {
        return next.getIndex();
    }

    public String getColumnName() {
        return next.getColumnName();
    }

    public Object execute(final Object value) {
        return execute(value, null);
    }

    public Object execute(final Object value, final CsvContext context) {
        return value;
    }
}
