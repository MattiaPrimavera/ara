
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

/**
 * @author mokuhazushi
 */
public class ElectionProtocolImpl implements ElectionProtocol {
    
    private static final String PAR_DELTA = "delta";
    private static final String PAR_DELTAPRIM = "deltaPrim";
    private static final String PAR_EMITTERPID = "emitter";
    
    private static final String PROBE_MSG = "probe";
    private static final String PROBE_CYCLE_MSG = "cycleProbe";
    
    private final int election_pid;
    private final int emitter_pid;
    private final List<Long> neighbors;
    private final HashMap<Long, Long> neighborsDelay;
    private final int delta_counter = 0;
    private final int delta;
    private final int deltaPrim;
    
    public ElectionProtocolImpl(String prefix) {
        String tmp[]=prefix.split("\\.");
        this.election_pid=Configuration.lookupPid(tmp[tmp.length-1]);
        this.emitter_pid = Configuration.getPid(prefix+"."+PAR_EMITTERPID);
        this.delta = Configuration.getInt(prefix+"."+PAR_DELTA);
        this.deltaPrim = Configuration.getInt(prefix+"."+PAR_DELTAPRIM);
        this.neighbors = new ArrayList<>();
        this.neighborsDelay = new HashMap<>();
    }

    public ElectionProtocolImpl(int election_pid, int emitter_pid, int delta, int deltaPrim) {
        this.election_pid = election_pid;
        this.emitter_pid = emitter_pid;
        this.delta = delta;
        this.deltaPrim = deltaPrim;
        this.neighbors = new ArrayList<>();
        this.neighborsDelay = new HashMap<>();
    }

    @Override
    public boolean isInElection() {
        return false;
    }

    @Override
    public long getIDLeader() {
        return 0;
    }

    @Override
    public int getMyValue() {
        return 0;
    }

    @Override
    public List<Long> getNeighbors() {
        /*Node node = CommonState.getNode();
        Message msg = new Message(node.getID(), -1, PROBE_MSG, null, CommonState.getPid());
        Emitter emitter = (Emitter) node.getProtocol(emitter_pid);
        emitter.emit(node, msg);

        for (int i=0; i<neighbors.size(); i++) {
            long neighbor = neighbors.get(i);
            if (!neighborsDelay.containsKey(neighbor)) continue;
            if (neighborsDelay.containsKey(neighbor)) {
                if (neighborsDelay.get(neighbor)+deltaPrim > CommonState.getTime()) {
                    neighborsDelay.remove(neighbor);
                    neighbors.remove(neighbor);
                }
            }
        }
         */      
        return neighbors;
    }

    @Override
    public void processEvent(Node node, int pid, Object event) {
        Node myself = CommonState.getNode();
        Message msg = (Message)event;
        String tag = msg.getTag();
        
        if (tag.equals(PROBE_CYCLE_MSG)) {
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
                }
            }
        }
        
        if (tag.equals(PROBE_MSG)) {
            long sender = msg.getIdSrc();
            if (!neighbors.contains(sender)) {
                //Ajouter un voisin
                neighbors.add(sender);
            }
            neighborsDelay.put(sender, CommonState.getTime());        
        }
    }
    
    private void sendProbeMsg() {
        Node myself = CommonState.getNode();
        Message msg = new Message(myself.getID(), -1, PROBE_MSG, null, emitter_pid);
        Emitter emitter = (Emitter) myself.getProtocol(emitter_pid);
        emitter.emit(myself, msg);       
    }

    @Override
    public Object clone() {
        return new ElectionProtocolImpl(election_pid, emitter_pid, delta, deltaPrim);
    }
    

}
