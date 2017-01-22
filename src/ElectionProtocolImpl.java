
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

/**
 * @author mokuhazushi
 */
public class ElectionProtocolImpl implements ElectionProtocol {
    
    private static final int MAX_VALUE = 200;
    
    
    private static final String PAR_DELTA = "delta";
    private static final String PAR_DELTAPRIM = "deltaPrim";
    private static final String PAR_EMITTERPID = "emitter";
    
    private static final String PROBE_MSG = "probe";
    private static final String PROBE_CYCLE_MSG = "cycleProbe";
    private static final String ELECTION_MSG = "election";
    private static final String ACK_MSG = "ack";
    private static final String LEADER_MSG = "leader";
    private static final int BROADCAST_MSG = -1;
    
    private final int election_pid;
    private final int emitter_pid;
    private final List<Long> neighbors;
    private final HashMap<Long, Long> neighborsDelay;
    private final int delta;
    private final int deltaPrim;
    
    //Protocol variables
    private ComputationIndex computationIndex = new ComputationIndex(0, 0);
    private boolean isInElection = false;
    private long parent = -1;
    private boolean sentAckToParent = false;
    private List<Long> waitingAcks = new ArrayList<>();
    private long leader = -1;
    private boolean inElection = false;
    private int nodeValue;
    private Map<Long, Integer> neighborsValue = new HashMap<>();
    private List<Long> waitingForLeader = new ArrayList<>();
    
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
        long myId = myself.getID();
        
        //application layer messages
        if (tag.equals(PROBE_MSG)) { //Probe message from neighbor
            long sender = msg.getIdSrc();
            if (!neighbors.contains(sender)) { //Is this neighbor in list ?
                //Ajouter un voisin
                neighbors.add(sender);
            }
            neighborsDelay.put(sender, CommonState.getTime());        
        }
        
        if (tag.equals(ELECTION_MSG)) { //Election message
            if (msg.getIdDest() == BROADCAST_MSG || msg.getIdDest() == myId) { //Process election message only if broadcast or idDest is me
                long srcId = msg.getIdSrc();
                ComputationIndex destComputationIndex = (ComputationIndex)msg.getContent();
                
                if (destComputationIndex.compare(computationIndex)) { //Priority election
                    computationIndex = new ComputationIndex(destComputationIndex);
                    parent = srcId;
                    waitingAcks = new ArrayList<>(neighbors);
                    waitingForLeader = new ArrayList<>();
                    neighborsValue = new HashMap<>();
                    for (long neighbor : neighbors) { //Propagate election message to everyone except my parent
                        if (neighbor == parent) continue;
                        sendElectionMsg(neighbor);
                    }
                }
                
                else if (msg.getIdDest() == myId && msg.getIdDest() != parent && parent != -1) { //Send ack message when received election message from node that is not my parent
                    sendAckMsg(msg.getIdDest());
                }
            }
        }
        
        if (tag.equals(ACK_MSG)) { //Ack message
            long srcId = msg.getIdSrc();
            if (parent ==  -1) { //Root of spanning tree 
                neighborsValue.put(srcId, (int)msg.getContent());
            }
              
            if (waitingAcks.contains(srcId)) {
                waitingAcks.remove(srcId);
                waitingForLeader.add(srcId);
            }
            
            if (waitingAcks.isEmpty() && parent != -1) { //Node in spanning tree
                sendAckMsg(parent);
            }
            
            if (waitingAcks.isEmpty() && parent == -1) { //Root of spanning tree
                if (neighborsValue.isEmpty()) { //Alone node
                    leader = myself.getID();
                }
                else {
                    long currentLeader = -1;
                    int maxValue = -1;
                    for (long id : neighborsValue.keySet()) {
                        int value = neighborsValue.get(id);
                        if (value > maxValue) {
                            maxValue = value;
                            currentLeader = id;
                        }
                    }
                    leader = currentLeader;
                }
                
                for (Long id : waitingForLeader) {
                    sendLeaderMsg(id);
                }
            }
        }
        
        if (tag.equals(LEADER_MSG)) { //Leader message
            leader = (int)msg.getContent();
            for (Long id : waitingForLeader) {
                sendLeaderMsg(id);
            }
        }
    }
    
    private void startNewElection() {
        Node myself = CommonState.getNode();
        computationIndex.setId(myself.getID());
        computationIndex.setIndex(computationIndex.getIndex()+1);
        waitingAcks = new ArrayList<>(neighbors);
        waitingForLeader = new ArrayList<>();
        neighborsValue = new HashMap<>();
        isInElection = true;
        
        sendElectionMsg(BROADCAST_MSG);
    }

    @Override
    public void processEvent(Node node, int pid, Object event) {
        Node myself = CommonState.getNode();
        Message msg = (Message)event;
        String tag = msg.getTag();
        
        if (tag.equals(PROBE_CYCLE_MSG)) { //Emit probe message periodically
            sendProbeMsg();
            Message recall = new Message(-1, -1, PROBE_CYCLE_MSG, null, -1);
            EDSimulator.add(delta, recall, myself, election_pid);
            
            //CHECK DISCONNECTED NEIGHBORS
            for (int i=0; i<neighbors.size(); i++) {
                long neighbor = neighbors.get(i);
                if (!neighborsDelay.containsKey(neighbor)) {
                    System.out.println("WARNING - Neighbor "+i+" not found in delay hashmap");
                    continue;
                }
                if (neighborsDelay.get(neighbor) + deltaPrim < CommonState.getTime()) {
                    //Neighbor i has disconnected
                    neighborsDelay.remove(neighbor);
                    
                    neighbors.remove(i);
                    if (neighbor == leader) { //Leader has disconnected
                        leader = -1;
                    }
                    if (waitingAcks.contains(neighbor)) { //Do not wait ack from neighbor that has disconnected
                        waitingAcks.remove(neighbor);
                    }
                    if (neighborsValue.containsKey(neighbor)) { //Do not consider value of neighbor that has disconnected
                        neighborsValue.remove(neighbor);
                    }
                }
            }
            
            //CHECK IF NEW ELECTION IS NEEDED
            if (!inElection && leader == -1) {
                startNewElection();
            }
        }
    }
    
    private void sendProbeMsg() {
        Node myself = CommonState.getNode();
        Message msg = new Message(myself.getID(), -1, PROBE_MSG, null, emitter_pid);
        Emitter emitter = (Emitter) myself.getProtocol(emitter_pid);
        emitter.emit(myself, msg);       
    }
    
    private void sendElectionMsg(long dest) {
        Node myself = CommonState.getNode();
        Message msg = new Message(myself.getID(), dest, ELECTION_MSG, computationIndex, emitter_pid);
        Emitter emitter = (Emitter) myself.getProtocol(emitter_pid);
        emitter.emit(myself, msg);  
    }
    
    private void sendAckMsg(long dest) {
        Node myself = CommonState.getNode();
        Message msg = new Message(myself.getID(), dest, ACK_MSG, nodeValue, emitter_pid);
        Emitter emitter = (Emitter) myself.getProtocol(emitter_pid);
        emitter.emit(myself, msg);  
        
    }
    
    private void sendLeaderMsg(long dest) {
        Node myself = CommonState.getNode();
        Message msg = new Message(myself.getID(), dest, LEADER_MSG, leader, emitter_pid);
        Emitter emitter = (Emitter) myself.getProtocol(emitter_pid);
        emitter.emit(myself, msg);  
    }

    @Override
    public Object clone() {
        return new ElectionProtocolImpl(election_pid, emitter_pid, delta, deltaPrim);
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
}
