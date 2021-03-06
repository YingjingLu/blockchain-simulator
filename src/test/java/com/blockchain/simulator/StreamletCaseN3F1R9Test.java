package com.blockchain.simulator;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.Before;
import org.junit.After;

import org.json.simple.JSONObject;
import org.json.simple.JSONArray;

import java.io.IOException;
import java.lang.IllegalArgumentException;
import org.json.simple.parser.ParseException;

public class StreamletCaseN3F1R9Test {
    public static final String CASE_NAME = "streamlet_n_3_f_1_r_9";

    private final String caseFolder;

    public StreamletCaseN3F1R9Test() {
        caseFolder = TestIO.getTargetCaseFolder(CASE_NAME);
    }

    @Before
    public void setUp() throws IOException, IllegalArgumentException, ParseException {
        // copy the case folder to the target directory
        TestIO.copyCaseIntoTestFolder(CASE_NAME);
    }

    @After
    public void tearDown() throws IOException{
        TestIO.deleteFolder(TestIO.getTargetCaseFolder(CASE_NAME));
    }

    @Test
    public void runStreamletCaseN3F1R9Test() throws IOException, IllegalArgumentException, ParseException {
        StreamletRoundSimulator simulator = new StreamletRoundSimulator(caseFolder);
        StreamletPlayerController playerController = simulator.playerController;
        StreamletJsonifier jsonifier = simulator.jsonifier;
        NetworkSimulator networkSimulator = simulator.networkSimulator;
        simulator.run();
    }
}
