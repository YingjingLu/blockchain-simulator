package com.blockchain.simulator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.lang.IllegalArgumentException;
import org.json.simple.parser.ParseException;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Collections;

/**
 * Class with methods to convert messages and player states from json to class object and from class objects to json
 */
public class DolevStrongJsonifier extends Jsonifer {
    private final DolevStrongRoundSimulator roundSimulator;

    public DolevStrongJsonifier(final DolevStrongRoundSimulator roundSimulator, final String traceRootPath) {
        super(traceRootPath);
        this.roundSimulator = roundSimulator;
    }

    /**
     * Parse configuration into config object
     */
    public DolevStrongConfig getConfig() throws IOException, ParseException, IllegalArgumentException {
        final String configPath = getConfigPath();
        JSONObject jsonObj = fileToJSONObject(configPath);
        JSONObject dolevStrongSJON = (JSONObject) jsonObj.get("dolev_strong_config");
        if (!dolevStrongSJON.containsKey("round")) {
            throw new IllegalArgumentException("Dolev Strong protocol's round argument should be specified");
        }
        if (!dolevStrongSJON.containsKey("num_corrupt_player")) {
            throw new IllegalArgumentException("Dolev Strong protocol's num_corrupt_player should be specified");
        }
        if (!dolevStrongSJON.containsKey("num_total_player")) {
            throw new IllegalArgumentException("Dolev Strong protocol's num_total_player should be specified");
        }
        if (!dolevStrongSJON.containsKey("max_delay")) {
            throw new IllegalArgumentException("Dolev Strong protocol's max_delay should be specified");
        }
        if (!dolevStrongSJON.containsKey("use_trace")) {
            throw new IllegalArgumentException("Dolev Strong protocol's use_trace should be specified");
        }
        final DolevStrongMessage initialBitMessage;
        if (!dolevStrongSJON.containsKey("inputs")) {
            initialBitMessage = null;
        } else {
            JSONArray level1Array = (JSONArray) dolevStrongSJON.get("inputs");
            if (level1Array.size() == 0) {
                initialBitMessage = null;
            } else {
                if (level1Array.size() > 1) {
                    throw new IllegalArgumentException("Dolev Strong protocol should only have one input, check input format");
                }
                JSONArray level2Array = (JSONArray) level1Array.get(0);
                if (level2Array.size() > 1) {
                    throw new IllegalArgumentException("Dolev Strong protocol should only have one input, check input format");
                } else {
                    if (level2Array.size() == 0) {
                        initialBitMessage = null;
                    } else {
                        initialBitMessage = jsonObjectToMessage((JSONObject) level2Array.get(0));
                    }
                }
            }
        }

        return new DolevStrongConfig(
                Integer.parseInt(dolevStrongSJON.get("round").toString()),
                Integer.parseInt(dolevStrongSJON.get("num_corrupt_player").toString()),
                Integer.parseInt(dolevStrongSJON.get("num_total_player").toString()),
                Integer.parseInt(dolevStrongSJON.get("max_delay").toString()),
                parseBool(dolevStrongSJON, "use_trace"),
                initialBitMessage);
    }

    /**
     * Write message traces for current round to json file
     * @param round
     * @param taskList
     * @throws IOException
     */
    public void writeMessageTrace(final int round, final List<Task> taskList) throws IOException {

        final JSONArray taskArray = new JSONArray();
        for (Task t : taskList) {
            taskArray.add(taskToJSONObject(t));
        }
        final String path = getMessageTracePath(round);
        jsonArrayToFile(taskArray, path);
    }

    public DolevStrongMessageTrace getRoundMessageTrace(final int round)
            throws IOException, ParseException, IllegalArgumentException {
        final String path = getMessageTracePath(round);
        if (!hasMessageTrace(round)) {
            return null;
        }
        JSONArray jsonArray = fileToJSONArray(path);
        List<Task> taskList = new LinkedList<>();
        for (Object obj : jsonArray) {
            JSONObject jsonObject = (JSONObject) obj;
            taskList.add(jsonObjectToTask(jsonObject));
        }
        return new DolevStrongMessageTrace(taskList);
    }

    /**
     * Write player state to a given round
     * @param round
     * @throws IOException
     */
    public void writeStateTracePath (final int round) throws IOException {
        final String path = getStateTracePath(round);
        final JSONObject stateObject = new JSONObject();
        final JSONArray honestPlayerStateArray = new JSONArray();
        final JSONArray corruptPlayerStateArray = new JSONArray();
        for(Map.Entry<Integer, Player> entry : roundSimulator.corruptPlayerMap.entrySet()) {
            corruptPlayerStateArray.add(playerToJSONObject((DolevStrongPlayer) entry.getValue()));
        }
        for (Map.Entry<Integer, Player> entry : roundSimulator.honestPlayerMap.entrySet()) {
            honestPlayerStateArray.add(playerToJSONObject((DolevStrongPlayer) entry.getValue()));
        }
        stateObject.put("honest", honestPlayerStateArray);
        stateObject.put("corrupt", corruptPlayerStateArray);
        jsonObjectToFile(stateObject, path);
    }

    /**
     * Json object to task object
     * @param jsonObject
     * @return
     * @throws IllegalArgumentException
     */
    public Task jsonObjectToTask(JSONObject jsonObject) throws IllegalArgumentException {
        final int delay, targetPlayerId;
        final DolevStrongMessage message;
        final DolevStrongPlayer targetPlayer;

        if (jsonObject.containsKey("target_player")) {
            targetPlayerId = Integer.parseInt(jsonObject.get("target_player").toString());
            if (roundSimulator.honestPlayerMap.containsKey(targetPlayerId)) {
                targetPlayer = (DolevStrongPlayer) roundSimulator.honestPlayerMap.get(targetPlayerId);
            } else {
                assert roundSimulator.corruptPlayerMap.containsKey(targetPlayerId) : "target player should exist for trace";
                targetPlayer = (DolevStrongPlayer) roundSimulator.corruptPlayerMap.get(targetPlayerId);
            }
        } else {
            throw new IllegalArgumentException("Task in Message Trace file should contain target_player");
        }

        if (jsonObject.containsKey("delay")) {
            delay = Integer.parseInt(jsonObject.get("delay").toString());
        } else {
            throw new IllegalArgumentException("Task in Message Trace file should contain delay");
        }

        if (jsonObject.containsKey("message")) {
            message = jsonObjectToMessage((JSONObject) jsonObject.get("message"));
        } else {
            throw new IllegalArgumentException("Task in Message Trace file should contain message");
        }

        return new Task(targetPlayer, message, delay);
    }

    /**
     * JSOn object to message object
     * @param jsonObject
     * @return
     */
    public DolevStrongMessage jsonObjectToMessage(JSONObject jsonObject) {
        final List<String> signatures;
        final int round;
        final List<Bit> message;
        final int fromPlayerId, toPlayerId;

        if (jsonObject.containsKey("round")) {
            round = Integer.parseInt(jsonObject.get("round").toString());
        } else {
            throw new IllegalArgumentException("Message Trace file should contain round");
        }

        if (jsonObject.containsKey("signatures")) {
            JSONArray signatureList = (JSONArray) (jsonObject.get("signatures"));
            signatures = new LinkedList<String>();
            for (Object obj : signatureList) {
                signatures.add((String)obj);
            }

        } else {
            throw new IllegalArgumentException("Message Trace file should contain signatures");
        }

        if (jsonObject.containsKey("message")) {
            JSONArray messageList = (JSONArray) (jsonObject.get("message"));
            message = new LinkedList<Bit>();
            for (Object obj : messageList) {
                message.add(Bit.stringToBit((String)obj));
            }

        } else {
            throw new IllegalArgumentException("Message Trace file should contain message");
        }

        if (jsonObject.containsKey("from_player_id")) {
            fromPlayerId = Integer.parseInt(jsonObject.get("from_player_id").toString());
        } else {
            throw new IllegalArgumentException("Message Trace file should contain from_player_id");
        }

        if (jsonObject.containsKey("to_player_id")) {
            toPlayerId = Integer.parseInt(jsonObject.get("to_player_id").toString());
        } else {
            throw new IllegalArgumentException("Message Trace file should contain to_player_id");
        }
        DolevStrongMessage newMessage = new DolevStrongMessage(
                round,
                message,
                fromPlayerId,
                toPlayerId
        );
        newMessage.addSignatures(signatures);
        return newMessage;
    }

    /**
     * message object to json object
     * @param message
     * @return
     */
    public JSONObject messageToJSONObject(DolevStrongMessage message) {
        JSONObject messageObject = new JSONObject();
        messageObject.put("round", message.getRound());
        messageObject.put("from_player_id", message.getFromPlayerId());
        messageObject.put("to_player_id", message.getToPlayerId());

        JSONArray signatureArray = new JSONArray();
        for (String str : message.getSignatures()) {
            signatureArray.add(str);
        }
        messageObject.put("signatures", signatureArray);

        JSONArray messageArray = new JSONArray();
        for (Bit b : message.getMessage()) {
            messageArray.add(b.toString());
        }
        messageObject.put("message", messageArray);
        return messageObject;
    }

    /**
     *Task object to json object
     * @param task
     * @return
     */
    public JSONObject taskToJSONObject(Task task) {
        final JSONObject messageObject = messageToJSONObject((DolevStrongMessage) task.getMessage());
        final JSONObject taskObject = new JSONObject();
        taskObject.put("target_player", task.getTargetPlayer().getId());
        taskObject.put("message", messageObject);
        taskObject.put("delay", task.getDelay());
        return taskObject;
    }

    /**
     * Player into json object
     * @param player
     * @return
     */
    public JSONObject playerToJSONObject(DolevStrongPlayer player) {
        JSONObject playerObject = new JSONObject();
        DolevStrongPlayerState playerState = new DolevStrongPlayerState(player);
        playerObject.put("player_id", playerState.playerId);

        JSONArray extractSet = new JSONArray();
        for(Bit b : playerState.extractedSet) {
            extractSet.add(b.toString());
        }
        playerObject.put("extracted_set", extractSet);
        return playerObject;
    }

    /**
     * Write output object
     * @throws IOException
     */
    public void writeOutput() throws IOException {
        final String outputPath = getOutputPath();
        JSONObject outputObject = new JSONObject();
        for (Map.Entry<Integer, Player> entry : roundSimulator.honestPlayerMap.entrySet()) {
            final Bit bit = ((DolevStrongPlayer)entry.getValue()).getOutputBit();
            outputObject.put(entry.getKey(), bit.toString());
        }
        jsonObjectToFile(outputObject, outputPath);
    }
}
