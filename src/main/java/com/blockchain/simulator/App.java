package com.blockchain.simulator;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import java.io.FileReader;
import java.io.IOException;
import java.lang.IllegalArgumentException;
import org.json.simple.parser.ParseException;

/**
 * The main app for parsing config and initiating call
 *
 */
public class App
{
    public static void main( String[] args )
            throws IOException, ParseException, IllegalArgumentException
    {
        System.out.println("Hello\n");
        JSONParser parser = new JSONParser();
        FileReader fileReader = new FileReader(Jsonifer.getConfigPathForApp(args[0]));
        JSONObject jsonObj = (JSONObject) parser.parse(fileReader);
        if (!jsonObj.containsKey("protocol")) {
            throw new IllegalArgumentException("Should specify protocol in config");
        }
        final String protocol = jsonObj.get("protocol").toString();
        if (protocol.equals("dolev_strong")) {
            DolevStrongRoundSimulator simulator = new DolevStrongRoundSimulator(args[0]);
            simulator.run();
        } else if (protocol.equals("streamlet")) {
            StreamletRoundSimulator simulator = new StreamletRoundSimulator(args[0]);
            simulator.run();
        } else {
            throw new IllegalArgumentException("Protocol in Config is not implemented");
        }
    }
}
