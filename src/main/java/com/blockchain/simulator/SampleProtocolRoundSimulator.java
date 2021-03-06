package com.blockchain.simulator;
import java.util.List;
import org.json.simple.parser.ParseException;
import java.lang.IllegalArgumentException;
import java.io.IOException;
import java.util.Map;

/**
 * Sample player RoundSimulator responsible for running protocol's procedure for each round
 */
public class SampleProtocolRoundSimulator extends RoundSimulator {

    public final SampleProtocolJsonifier jsonifier;
    private final SampleProtocolConfig config;

    SampleProtocolPlayerController playerController;

    /**
     * Constructor to initialize all player mapping and required network, playerController
     * @param traceRootPath
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws ParseException
     */
    public SampleProtocolRoundSimulator( final String traceRootPath)
            throws IOException, IllegalArgumentException, ParseException {
        super();
        jsonifier = new SampleProtocolJsonifier(this, traceRootPath);
        this.config = jsonifier.getConfig();
        final int totalPlayer = this.config.numTotalPlayer;
        final int corruptPlayer = this.config.numCorruptPlayer;

        final int startCorrupt = totalPlayer - corruptPlayer;
        for (int i = 0; i < totalPlayer; i++) {
            if (i >= startCorrupt) {
                corruptPlayerMap.put(i, new DolevStrongPlayer(i, playerController));
            } else {
                honestPlayerMap.put(i, new DolevStrongPlayer(i, playerController));
            }
        }
        for (Map.Entry<Integer, Player> entry : honestPlayerMap.entrySet()) {
            playerMap.put(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<Integer, Player> entry : corruptPlayerMap.entrySet()) {
            playerMap.put(entry.getKey(), entry.getValue());
        }
        playerController = new SampleProtocolPlayerController(
                networkSimulator,
                authenticator,
                honestPlayerMap,
                corruptPlayerMap,
                playerMap
        );
    }

    /**
     * @custom.protocol_dependent: Major protocol execution procedure
     *
     * TODO: fill in more execution procedure in case the protocol requires
     *
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws ParseException
     */
    public void run() throws IOException, IllegalArgumentException, ParseException {
        for (int round = 0; round < this.config.round; round ++) {
            playerController.beginRound(round);
            networkSimulator.beginRound((round));
            // flush the pending nessage get delayed from previous rounds to this round
            networkSimulator.sendMessagesToPlayers(round);

            // process initial input for each player of that round
            List<SampleProtocolMessage> inputList = jsonifier.getInput(round);
            List<Task> inputTaskList = playerController.receiveInputForRound(inputList, round);
            // flush the input to players
            playerController.sendMessageListViaNetwork(round, inputTaskList);
            // If the network has the upper bound delay, then we should bound it
            networkSimulator.boundMessageDelayForSynchronousNetwork(config.maxDelay, inputTaskList);
            networkSimulator.sendMessagesToPlayers(round);

            // Inter player communication for inner round
            List<Task> messageTaskList;
            if (this.config.useTrace) {
                messageTaskList = jsonifier.getMessageTrace(round);
            } else {
                messageTaskList = playerController.generateMessageTasksAmongPlayers(round);
            }

            // flush the message to players that are supposed to be delivered for the current round
            playerController.sendMessageListViaNetwork(round, messageTaskList);
            // If the network has the upper bound delay, then we should bound it
            networkSimulator.boundMessageDelayForSynchronousNetwork(config.maxDelay, messageTaskList);
            networkSimulator.sendMessagesToPlayers(round);

            playerController.endRoundForPlayers(round);

            // dump the current round messages and player states traces
            jsonifier.writeMessageTrace(round, messageTaskList);
            jsonifier.writeStateTracePath(round);
        }

        // the end of last round
        // every player reach an output
        networkSimulator.sendMessagesToPlayers(this.config.round);
        playerController.endRoundForPlayers(this.config.round);
        playerController.createOutputForEveryPlayer();
        jsonifier.writeStateTracePath(this.config.round);
        playerController.printOutput();
    }

}
