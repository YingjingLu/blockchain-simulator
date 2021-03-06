# Config Trace & State File

In our simulator design, the simulator inputs and outputs for one protocol configuration we call it a ```case``` is stored in a folder. This folder will contains the configurations of protocol input, the messages being generated during protocol execution period and the player states, and outputs. 

Te folder structure is shown as below:

## Config/trace folder structure:
**If you are uploading a case zip to UI, remember to name of the zip and the name od the root folder is the same, Otherwise the server will crash**
```
.
+-- _config.json
+-- _output.json (DolevStrong output ONLY, No need in configuration)
+-- _message_trace
|   +-- 0.json
|   +-- 1.json
+-- _player_state_trace
|   +-- init.json
|   +-- 0.json
|   +-- 1.json
+-- _proposal_trace (Streamlet ONLY)
|   +-- 0.json
```

The `config.json` in the root directory of this case folder contains the configurations such as player number, and inputs to players for every round. This is required to start running the simulator.

The `/message_trace` folder contains messages being generated for every round during the protocol execution. For example `0.json` stores the messages being generated at round 0 among players. **In our simulator, every protocol's round index starts from 0**.

The `/player_state_trace` folder contains each player's state at the end of every round. `init.json` records all the player states during protocol initialization. `0.json` records states of each players at the end of round 0.

## config.json
* The `config.json` defines parameters this protocol takes in. Below is the json structure for `config.json`
* top level key `protocol` specifies the protocol this config is for for the simulator to do initialization. It can be `"dolev_strong"` or `"streamlet"` or `"new_protocol"` that you further define. If you specify `"dolev_strong"` then further `"dolev_strong_config"` must be present, if you specify `"streamlet"`, `"streamlet_config"` must be present and so on.
* `"dolev_strong_config"`, `"streamlet_config"` and other protocol dependent configurations must contains keys: `round, num_corrupt_player, num_total_player, use_trace, max_delay`, `inputs` is optional if the protocol does not receive any inputs at any round.
* `round`: for Dolev strong, this round is not including the zeroth round. So if `dolev_strong_config` with round of 3 that means there will be round `0,1,2,3` where round `0` is the round sender receives input and sends to other players. At the end of round `3` is the round every player generates output. However, for `streamlet_config` round of 3 will include round `0,1,2` when executed.
* Player IDs generated will indexed from 0 and corrupt players will always be at later index. For example if you have 5 players and 3 of them are corrupt that will be player `0, 1` being honest and player with id `2, 3, 4` will be corrupt.
* `use_trace`:  true if we use messages defined in the message_trace folder for communication among players each round, if false, protocol do not use any message traces but use protocol's definition to generate and communicate messages.
* `max_delay`: max number of round -1 for partially synchronous, no bound for max delay. If this is set to a non-negative number This is equivalent to the delta value in synchronous model. Note that since the round based protocols delivers the messages at the beginning of next round during good network conditions, `max_delay` should be set to 1 for DolevStrong and at least 1 for Streamlet to avoid undefined behaviors. For Dolev Strong, `max_delay=1` is aligned with the protocol's definition. In Streamlet, if `max_delay` is `-1`, then every epoch consist of `2` rounds by default, and if `max_delay` is an integer `>= 1`, then the epoch length is `2*max_delay` per the Streamlet's definition that epoch length should set to `2` times `delta`.
* `inputs`: The 2D array of `message` objects for each protocol. First dimension is the round and second dimension is the message. So `inputs[0]` stands for all the messages for players in round 0, and so on. For streamlet, there will be only one message, so only `inputs[0][0]` will be cunted. If `inputs` is missing, the sender and the bit will be randomly generated. In Streamlet, the inputs will be transactions. In our simulator, each transaction is a unique integer. Transactions can be inputed to players through the messages. In the example blow, in streamlet config, we deliver to player 6 transactions 0 and 1 in round 0. If you do not specify transaction inputs, the streamlet will only run block proposal and block voting and will not record any transactions in players' chains.

```
{
    "protocol": "dolev_strong" or "streamlet" or "new_protocol",
    // if protocol is dolev_strong, then this field must be defined
    "dolev_strong_config": {
        "round": "3",
        "num_corrupt_player": "3",
        "num_total_player": "10",
        "use_trace": true,  
        "max_delay": 1 // max number of round -1 for no limit,
        // if this field is not given, then it is gonna have random sender with a random bit
        "inputs": [ 
            // 2D array of input for each round
            // each round is a separate array
            [ // this is the 0th array so for round zero
                {
                    "round": "0",
                    "message": ["0"], // "1" or "0",
                    "signatures": [],
                    "from_player_id": "-1", // 
                    "to_player_id": "0" // for Dolev strong this is the designated sender
                }
            ]
        ]
    },
    // if protocol is streamlet, then this field must be defined
    "streamlet_config": {
        "round": "10",
        "num_corrupt_player": "3",
        "num_total_player": "10",
        "use_trace": true,
        "max_delay": 9,
        "inputs": [ // if not given then player by default has a dummy input message, do not need to specify here
            [
                {
                    "is_vote": false,
                    "approved": "0",
                    "proposer_id": "0",
                    "signatures": [],
                    "round": "0",
                    "message": ["0", "1"],
                    "from_player_id": "-1",
                    "to_player_id": "6" // player which receives input
                }
            ]
        ]
    },
    // customize this for your own protocol
    "new_protocol_config": {
        // required arguments 
        "round": "10",
        "num_corrupt_player": "3",
        "num_total_player": "10",
        "use_trace": true,
        "max_delay": 2,
        "inputs": [
            [
                new_message_message_object    
            ]    
        ]
    }
}
```

## Message Trace json structure
Below defines the message trace json structure for one round (for example `/message_trace/0.json`), This can differ depending on protocol's need. These message traces can be either specified before simulation run or not. If you want to let simulator automatically generate message, leave no file there and the simulator will fill out messages as it runs, or you can choose to use the messages you write and pass that in to the simulator with `config` `use_trace=true`. You can also specify part of the keys for example in streamlet you can specify `proposal_task` and leave `vote_task` blank, the simulator will use the proposal task you defined and automatically generate the vote task messages.

### DolevStrongMessageTrace
Each of tasks in this array includes a message one player sends to another
```
[
    Task 1 Object,
    Task 2 object,
    ....
]
```

### StreamletMessageTrace
* `proposal_task` is the array of tasks, each of which includes a proposal message that epoch leader sends to the other players. Each message also represents its inplicit echoing.
* `vote_task` is an array of tasks, each of which includes a vote message that one player sends to another, each message also represents the inplicit echoing of this message among players.
* `input_echo` is the list of tasks each includes the inplicit echoing of the message when a player receives an input to broadcast it to other players
* `message_echo` is the list of tasks each includes an echo for a player who echos the message it receives that has not seen before and want to send it to other players

```
{
    "proposal_task": [
        task 1 json,
        task 2 json,
        ...
    ],
    "vote_task": [
        task 1 json,
        task 2 json,
        ...
    ],
    "input_echo": [
        task 1 json,
        ...
    ],
    "message_echo": [
        task 1 json,
        ....
    ]
}
```

Note that for both `input_echo` and `message_echo`, when one player echos a message to another player, it does not modify the content of the message object, including `message.from_player_id` and `message.to_player_id`, it just changes the `target_player` field in the task object. We implement it this way to allow messages be uniquely identifiable to avoid players double counting echo messages. 

The blow example shows a message originally sent from player 6 to player 2 being echoed to player 1. 
```
{
    "target_player": "1",
    "message": 
        {
            "is_vote": true,
            "approved": "0" or "1" or "2",
            "proposer_id": "0",
            "signatures": [
                "signature1",
                "signature2",
                ...
            ],
            "round": "3",
            "message": ["0", "1"],
            "from_player_id": "2",
            "to_player_id": "6"
        },
    "delay": "1"
}
```

### StreamletProposalTrace
The trace of a block proposal. It is the same as the StreamletBlock
```
{
    "round": "5",
    "proposer_id": "3",
    "message": ["0", "1"],
    "prev": "4",
    "notarized": false,
    "finalized": "false",
    "level": "34"
}
```

## Player State Trace json structure
Below is the state trace for the two protocols that records every player's key states for output by the end of each round.
### DolevStrongPlayerState
```
[
    {
        "player_id": 1,
        "extracted_set": ["0", "1"]
    },
    ....
]
```
### StreamletPlayerState
* Players are divided into honest and corrupt two groups depending on the config of the case.
* `chain` key contains the array of blocks that player have at the end of an epoch
* the chain is a 2D array. The first dimension is the block level, second dimension is each block. So `chain[0]` represents all blocks a player have at level 0, `chain[1]` is the array of all blocks of level 1 and so on.
```
{
    "honest": [
        {
            "player_id": "5",
            "chains": [ // each key represent the level
                [
                    streamlet block object 1,
                    streamlet block object 2,
                    ...
                ],
                [
                    streamlet block object 1,
                    streamlet block object 2,
                    ...
                ],
                ...
            ]
        },
        ...
    ],
    "corrupt": [
        {
            "player_id": "5",
            "chains": [ // each key represent the level
                [
                    streamlet block object 1,
                    streamlet block object 2,
                    ...
                ],
                [
                    streamlet block object 1,
                    streamlet block object 2,
                    ...
                ],
                ...
            ]
        },
        ...
    ]
}
```

## Supporting objects that message_trace and state_trace includes inside their structures

### Task Object json
* THe task object that above message trace and player traces used. This is the json serialized version of task object in the simulator. that represents the packet passed to `NetworkSimulator` to be processed in the network queue.

* when the delay is `-1` it means the message will be delayed infinitely, and the network simulator will simply drop it. 

```
{
    "target_player": "1",
    "message": message object json,
    "delay": "1"
}
```

### DolevStrongMessage
```
{
    "round": "3",
    "message": ["0"],
    "signatures": ["string 1", "string 2", ...],
    "from_player_id": "3",
    "to_player_id": "5"
}
```

### StreamletBlock
```
{
    "round": "5",
    "proposer_id": "3",
    "message": ["0", "1"],
    "prev": "4",
    "notarized": false,
    "finalized": "false",
    "level": "34"
}
```

### StreamletMessage
```
{
    "is_vote": true,
    "approved": "0" or "1" or "2",
    "proposer_id": "0",
    "signatures": [
        "signature1",
        "signature2",
        ...
    ],
    "round": "3",
    "message": ["0", "1"],
    "from_player_id": "2",
    "to_player_id": "6"
}
```


