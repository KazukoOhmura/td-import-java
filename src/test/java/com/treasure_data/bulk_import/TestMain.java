package com.treasure_data.bulk_import;

import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;

import com.treasure_data.bulk_import.Config;

public class TestMain {

    @Test @Ignore
    public void testPrepareParts01() throws Exception {
        Properties props = System.getProperties();
        props.load(this.getClass().getClassLoader()
                .getResourceAsStream("treasure-data.properties"));

        //props.setProperty(Config.BI_PREPARE_PARTS_TIMECOLUMN, "date_code");
        props.setProperty(Config.BI_PREPARE_PARTS_TIMEVALUE, "1370941200");
        props.setProperty(Config.BI_PREPARE_PARTS_PARALLEL, "5");
        final String[] args = new String[] {
                "prepare_parts",
                "./in/from_SQLServer_to_csv_10_v01.csv",
                "./in/from_SQLServer_to_csv_10_v02.csv",
                "./in/from_SQLServer_to_csv_10_v03.csv",
                "./in/from_SQLServer_to_csv_10_v04.csv",
                "./in/from_SQLServer_to_csv_10_v05.csv",
                "./in/from_SQLServer_to_csv_10_v06.csv",
                "./in/from_SQLServer_to_csv_10_v07.csv",
                "./in/from_SQLServer_to_csv_10_v08.csv",
                "./in/from_SQLServer_to_csv_10_v09.csv",
        };

        Main.prepareParts(args, props);
    }

}