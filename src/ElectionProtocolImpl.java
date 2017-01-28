
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

public class ElectionProtocolImpl implements ElectionProtocol {
    
    private static final int MAX_VALUE = 200;
    private static final int NO_LEADER = -1;
    private static final int SPAN_TREE_ROOT = -1;
    
    private static final String PAR_DELTA = "delta";
    private static final String PAR_DELTAPRIM = "deltaPrim";
    private static final String PAR_EMITTERPID = "emitter";
    
    private static final String PROBE_MSG = "probe";
    private static final String PROBE_CYCLE_MSG = "cycleProbe";
    private static final String ELECTION_MSG = "election";
    private static final String ACK_MSG = "ack";
    private static final String LEADER_MSG = "leader";
    private static final String LEADER_MERGE_MSG = "leaderrefresh"; //To merge components 
    private static final String LEADER_ALIVE_MSG = "leaderalive"; //To check when starting new election
    private static final int BROADCAST_MSG = -1;
    
    private final int election_pid;
    private final int emitter_pid;
    private final List<Long> neighbors;
    private final HashMap<Long, Long> neighborsDelay;
    private final int delta;
    private final int deltaPrim;
    
    //Protocol variables
    private int nodeValue;
    private int leaderValue;
    private Map<Long, LeaderObject> neighborsValue = new HashMap<>();
    private boolean inElection = false;
    private long parent = -1;
    private boolean sentAckToParent = false;
    private long leader = -1;
    private List<Long> electionNeighbors = new ArrayList<>();
    private List<Long> waitingAcks = new ArrayList<>();
    private ComputationIndex computationIndex = new ComputationIndex(0, 0);
    
    //Other variables
    private long leaderDelay = -1; //Delay before considering leader has crashed/disconnected
    private int leaderAliveRound = -1; //Round number of leader alive broadcast message
    
    //Monitoring variables
    private long timeNoLeader = 0;
    private long startNoLeader = 0;
    private boolean alreadyStartNoLeader = false;
    private long electionRate = 0;
    private long startElection = 0;
    private long electionTime = 0;
    private long msgOverhead = 0;
    private long nbTimesLeader = 0;
    
    
    public ElectionProtocolImpl(String prefix) {
        String tmp[]=prefix.split("\\.");
        this.election_pid=Configuration.lookupPid(tmp[tmp.length-1]);
        this.emitter_pid = Configuration.getPid(prefix+"."+PAR_EMITTERPID);
        this.delta = Configuration.getInt(prefix+"."+PAR_DELTA);
        this.deltaPrim = Configuration.getInt(prefix+"."+PAR_DELTAPRIM);
        this.neighbors = new ArrayList<>();
        this.neighborsDelay = new HashMap<>();
        nodeValue = (int)(Math.random()*MAX_VALUE);
    }

    public ElectionProtocolImpl(int election_pid, int emitter_pid, int delta, int deltaPrim) {
        this.election_pid = election_pid;
        this.emitter_pid = emitter_pid;
        this.delta = delta;
        this.deltaPrim = deltaPrim;
        this.neighbors = new ArrayList<>();
        this.neighborsDelay = new HashMap<>();
        nodeValue = (int)(Math.random()*MAX_VALUE);
    }

    @Override
    public boolean isInElection() {
        return inElection;
    }

    @Override
    public long getIDLeader() {
        return leader;
    }

    @Override
    public int getMyValue() {
        return nodeValue;
    }

    @Override
    public List<Long> getNeighbors() {    
        return neighbors;
    }

    @Override
    public void receiveMsg(Message msg) {
        Node myself = CommonState.getNode();
        String tag = msg.getTag();
        long sender = msg.getIdSrc();
        
        //application layer messages
        if (tag.equals(PROBE_MSG)) { //Probe message from neighbor 
            if (!neighbors.contains(sender)) { //Is this neighbor in list ?
                neighbors.add(sender); //Add neighbor
            }
            neighborsDelay.put(sender, CommonState.getTime());        
        }
        
        if (tag.equals(LEADER_MERGE_MSG)) {
            if (inElection) return; //Wait end of election before refreshing leader
            LeaderObject newLeader = (LeaderObject)msg.getContent();
            if (newLeader.compare(new LeaderObject(leader, leaderValue))) { //New leader is better
                leader = newLeader.getLeaderId();
                leaderValue = newLeader.getLeaderValue();
            }
        }
        
        if (tag.equals(LEADER_ALIVE_MSG)) {
            if (inElection) return; //Do nothing if in election
            LeaderObject lo = (LeaderObject)msg.getContent();
            
            if (lo.getLeaderId() != leader) return; //This node doesnt share this leader
            if (lo.getLeaderValue() == leaderAliveRound) return; //Message from same round number, ignore
            leaderAliveRound = lo.getLeaderValue();
            leaderDelay = CommonState.getTime();
            for (long neighbor : neighbors) {
                if (neighbor == sender) continue;
                sendMsg(neighbor, LEADER_ALIVE_MSG);
            }
        }
        
        if (tag.equals(ELECTION_MSG)) { //Election message
            ComputationIndex neighborCI = (ComputationIndex)msg.getContent();
            
            if (!inElection) {
                initElectionVariables();
            }
            
            if (parent != sender) { //Immediately send ack
                sendMsg(sender, ACK_MSG);
            }
            
            if (neighborCI.compare(computationIndex)) { //Received computation index is higher
                computationIndex = new ComputationIndex(neighborCI); //The received computation index is higher
                parent = sender;
                waitingAcks = new ArrayList<>();
                for (long neighbor : electionNeighbors) {
                    if (neighbor == parent) continue;
                    sendMsg(neighbor, ELECTION_MSG);
                    waitingAcks.add(neighbor);
                }
                if (waitingAcks.isEmpty()) { //I had no other neighbor from start
                    sendMsg(parent, ACK_MSG);
                    sentAckToParent = true;
                }
            }
        }
        
        if (!inElection) return; //Ignore other election protocol messages if not in election
        
        if (tag.equals(ACK_MSG)) { //Ack message
            if (waitingAcks.remove(sender)) {
                LeaderObject lo = (LeaderObject)msg.getContent();
                neighborsValue.put(sender, lo);
            }
            
            if (waitingAcks.isEmpty() && !sentAckToParent) { //send ack to parent
                if (parent == SPAN_TREE_ROOT) {
                    LeaderObject lo = chooseLeader();
                    leader = lo.leaderId;
                    leaderValue = lo.getLeaderValue();
                    for (long neighbor : electionNeighbors) {
                        sendMsg(neighbor, LEADER_MSG);
                    }
                    endElection();
                }
                else {
                    sendMsg(parent, ACK_MSG);
                }
                sentAckToParent = true;
            }
        }
        
        if (tag.equals(LEADER_MSG)) { //Leader message
            LeaderObject newLeader = (LeaderObject)msg.getContent();
            leader = newLeader.getLeaderId();
            leaderValue = newLeader.getLeaderValue();
            
            for (long neighbor : electionNeighbors) {
                if (neighbor == parent) continue;
                sendMsg(neighbor, LEADER_MSG);
            }
            endElection();
        }
    }

    @Override
    public void processEvent(Node node, int pid, Object event) {
        Node myself = CommonState.getNode();
        Message msg = (Message)event;
        String tag = msg.getTag();
        
        if (tag.equals(PROBE_CYCLE_MSG)) { //Emit probe message periodically
            sendMsg(BROADCAST_MSG, PROBE_MSG);
            Message recall = new Message(-1, -1, PROBE_CYCLE_MSG, null, -1);
            EDSimulator.add(delta, recall, myself, election_pid);
            
            //CHECK DISCONNECTED NEIGHBORS
            for (int i=0; i<neighbors.size(); i++) {
                long neighbor = neighbors.get(i);
                if (!neighborsDelay.containsKey(neighbor)) {
                    continue;
                }
                if (neighborsDelay.get(neighbor) + deltaPrim < CommonState.getTime()) {
                    //Neighbor i has disconnected
                    neighborsDelay.remove(neighbor);
                    neighbors.remove(i);
                    
                    if (inElection && !sentAckToParent) {
                        waitingAcks.remove(neighbor); //Do not wait ack from disconnected neighbor
                        neighborsValue.remove(neighbor);
                        doNotWaitAcks();
                    }
                }
            }
            
            //CHECK IF NEW ELECTION IS NEEDED
            if (leader == NO_LEADER && !inElection) {
                startNewElection();
            }
            
            //USE OF PERIODICITY TO SEND LEADER REFRESH MESSAGE IF NECESSARY
            if (!inElection) {
                for (long neighbor : neighbors) { 
                    /* We use neighbors instead of electionNeighbors because we want 
                    joining nodes to receive this msg */
                    if (neighbor == parent) continue;
                    sendMsg(neighbor, LEADER_MERGE_MSG);
                }
            }
            //LEADER SEND ALIVE MESSAGE PERIODICALLY
            if (!inElection && leader == myself.getID()) {
                leaderAliveRound++; //Update round number first
                for (long neighbor : neighbors) {
                    /* Because of merging components we cannot use electionNeighbors 
                    but nodes who do not share this leader will ignore this message */
                    sendMsg(neighbor, LEADER_ALIVE_MSG);
                }
            }
            
            //OTHER NODE THAT HAS END ELECTION CHECK IF LEADER IS STILL ALIVE
            if (!inElection && leader != myself.getID()) {
                if (leaderDelay + deltaPrim < CommonState.getTime()) {
                    leader = NO_LEADER;
                    
                    //MONITORING - time without leader
                    if (!alreadyStartNoLeader) {
                        startNoLeader = CommonState.getTime();
                        alreadyStartNoLeader = true;
                    }
                }
            }
        }
    }
    
    private void startNewElection() {
        
        initElectionVariables();
        if (electionNeighbors.isEmpty()) { //No neighbor, i am the leader
            leader = CommonState.getNode().getID();
            leaderValue = nodeValue;
            endElection();
            return;
        }
        for (long neighbor : electionNeighbors) {
            sendMsg(neighbor, ELECTION_MSG);
            waitingAcks.add(neighbor); //I wait an ack to all neighbors i sent an election message
        }
    }
    
    private void endElection() {
        inElection = false;
        leaderDelay = CommonState.getTime();
        leaderAliveRound = 0;
        
        //MONITORING - time without leader
        timeNoLeader += CommonState.getTime() - startNoLeader;
        alreadyStartNoLeader = false;
        
        //MONITORING - election time
        electionTime += CommonState.getTime() - startElection;
        
        //MONITORING - nb time leader
        if (leader == CommonState.getNode().getID()) {
            nbTimesLeader++;
        }
    }
    
    private void initElectionVariables() {
        Node myself = CommonState.getNode();
        inElection = true;
        parent = SPAN_TREE_ROOT;
        sentAckToParent = false;
        leader = NO_LEADER;
        electionNeighbors = new ArrayList<>(neighbors);
        waitingAcks = new ArrayList<>();
        computationIndex.setId(myself.getID());
        computationIndex.setIndex(computationIndex.getIndex()+1);
        neighborsValue = new HashMap<>();
        leaderValue = -1;
        
        //MONITORING - time without leader
        if (!alreadyStartNoLeader) {
            startNoLeader = CommonState.getTime();
            alreadyStartNoLeader = true;
        }
        
        //MONITORING - Election rate
        electionRate++;
        
        //MONITORING - Election time
        startElection = CommonState.getTime();
    }
    
    private void sendMsg(long dest, String msgType){
        Node myself = CommonState.getNode();
        Message msg = null;
        switch(msgType){
            case PROBE_MSG:
                msg = new Message(myself.getID(), BROADCAST_MSG, PROBE_MSG, null, emitter_pid);
                break;
            case ELECTION_MSG:
                msg = new Message(myself.getID(), dest, ELECTION_MSG, computationIndex, emitter_pid);
                break;
            case ACK_MSG:
                msg = new Message(myself.getID(), dest, ACK_MSG, chooseLeader(), emitter_pid);
                break;
            case LEADER_MSG:
                msg = new Message(myself.getID(), dest, LEADER_MSG, new LeaderObject(leader, leaderValue), emitter_pid);
                break;
            case LEADER_MERGE_MSG:
                msg = new Message(myself.getID(), dest, LEADER_MERGE_MSG, new LeaderObject(leader, leaderValue), emitter_pid);
                break;
            case LEADER_ALIVE_MSG:
                msg = new Message(myself.getID(), dest, LEADER_ALIVE_MSG, new LeaderObject(leader, leaderAliveRound), emitter_pid);
                break;
            default:
                System.out.println("WARNING UNKNOWN MESSAGE TYPE AT sendMsg type="+msgType);
        }
        Emitter emitter = (Emitter) myself.getProtocol(emitter_pid);
        emitter.emit(myself, msg); 
        
        //MONITORING - message overhead
        msgOverhead++;
    }
    
    private LeaderObject chooseLeader() {
        Node myself = CommonState.getNode();
        if (neighborsValue.isEmpty()) { //Alone node
            return new LeaderObject(myself.getID(), nodeValue);
        }
        else {
            long currentLeader = -1;
            int maxValue = -1;
            for (long id : neighborsValue.keySet()) {
                LeaderObject lo = neighborsValue.get(id);
                if (lo.getLeaderValue() > maxValue) {
                    maxValue = lo.getLeaderValue();
                    currentLeader = lo.getLeaderId();
                }
            }
            return new LeaderObject(currentLeader, maxValue);
        }
    }
    
    private void doNotWaitAcks() {
        if (waitingAcks.isEmpty() && !sentAckToParent) { //send ack to parent
            if (parent == SPAN_TREE_ROOT) {
                chooseLeader();
                for (long neighbor : electionNeighbors) {
                    sendMsg(neighbor, LEADER_MSG);
                }
            }
            else {
                sendMsg(parent, ACK_MSG);
            }
            sentAckToParent = true;
        }
    }

    @Override
    public Object clone() {
        return new ElectionProtocolImpl(election_pid, emitter_pid, delta, deltaPrim);
    }

    public long getTimeNoLeader() {
        return timeNoLeader;
    }

    public long getElectionRate() {
        return electionRate;
    }

    public long getElectionTime() {
        return electionTime;
    }

    public long getMsgOverhead() {
        return msgOverhead;
    }
    
    public long getNbTimesLeader() {
        return nbTimesLeader;
    }
    

    private class ComputationIndex {
        long id;
        int index;

        public ComputationIndex(long id, int index) {
            this.id = id;
            this.index = index;
        }
        
        public ComputationIndex(ComputationIndex cpuIndex) {
            this.id = cpuIndex.id;
            this.index = cpuIndex.index;
        }

        public long getId() {
            return id;
        }

        public int getIndex() {
            return index;
        }

        public void setId(long id) {
            this.id = id;
        }

        public void setIndex(int index) {
            this.index = index;
        }
        
        /* Return true if this > n */
        public boolean compare(ComputationIndex n) {
            return index > n.index || (index == n.index && id > n.id);
        }
        
    }
    
    private class LeaderObject {
        private long leaderId;
        private int leaderValue;

        public LeaderObject(long leaderId, int leaderValue) {
            this.leaderId = leaderId;
            this.leaderValue = leaderValue;
        }

        public long getLeaderId() {
            return leaderId;
        }

        public int getLeaderValue() {
            return leaderValue;
        }

        public void setLeaderId(long leaderId) {
            this.leaderId = leaderId;
        }

        public void setLeaderValue(int leaderValue) {
            this.leaderValue = leaderValue;
        }
        
        /* Return true if this.value > l.value */
        public boolean compare(LeaderObject l) {
            return leaderValue > l.leaderValue;
        }
        
        public boolean equals(LeaderObject l) {
            return leaderValue == l.leaderValue && leaderId == l.leaderId;
        }
    }
}
