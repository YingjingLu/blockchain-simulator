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

public class DolevStrongCaseN3F1R1Test {

    public static final String CASE_NAME = "dolevstrong_n_3_f_1_r_1";

    private final String caseFolder;

    public DolevStrongCaseN3F1R1Test() {
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
    public void runDolevStrongcaseN3F1R1() throws IOException, IllegalArgumentException, ParseException {
        DolevStrongRoundSimulator simulator = new DolevStrongRoundSimulator(caseFolder);
        DolevStrongPlayerController playerController = simulator.playerController;
        DolevStrongJsonifier jsonifier = simulator.jsonifier;
        NetworkSimulator networkSimulator = simulator.networkSimulator;
        simulator.run();

        // check for outputs
        JSONObject outputObject = jsonifier.fileToJSONObject(jsonifier.getOutputPath());
        assertEquals(outputObject.get("0").toString(), "0");
        assertEquals(outputObject.get("1").toString(), "1");

        // check for message round 0
        JSONArray r0Array = jsonifier.fileToJSONArray(jsonifier.getMessageTracePath(0));
        assertEquals(r0Array.size(), 3);

        // check for message round 1
        JSONArray r1Array = jsonifier.fileToJSONArray(jsonifier.getMessageTracePath(1));
        assertEquals(r1Array.size(), 9);

    }

}
