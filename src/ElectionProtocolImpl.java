
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import peersim.cdsim.CDProtocol;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Node;

/**
 * @author mokuhazushi
 */
public class ElectionProtocolImpl implements ElectionProtocol {
    
    private static final String PAR_DELTA = "delta";
    private static final String PAR_DELTAPRIM = "deltaPrim";
    private static final String PAR_EMITTERPID = "emitter";
    
    private static final String PROBE_MSG = "probe";
    
    private int emitter_pid;
    private List<Long> neighbors;
    private HashMap<Long, Long> neighborsDelay;
    private int delta;
    private int deltaPrim;
    private Thread probeThread;
    private boolean threadStart = false;
    
    public ElectionProtocolImpl(String prefix) {
        this.emitter_pid = Configuration.getPid(prefix+"."+PAR_EMITTERPID);
        this.delta = Configuration.getInt(prefix+"."+PAR_DELTA);
        this.deltaPrim = Configuration.getInt(prefix+"."+PAR_DELTAPRIM);
        this.neighbors = new ArrayList<>();
        this.neighborsDelay = new HashMap<>();
    }

    public ElectionProtocolImpl(int emitter_pid, int delta, int deltaPrim) {
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
        Node node = CommonState.getNode();
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
               
        return neighbors;
    }

    @Override
    public void processEvent(Node node, int pid, Object event) {
        Message msg = (Message)event;
        String tag = msg.getTag();
        
        if (tag.equals(PROBE_MSG)) {
            long sender = msg.getIdSrc();
            if (!neighbors.contains(sender)) {
                //Ajouter un voisin
                neighbors.add(sender);
            }
            neighborsDelay.put(sender, CommonState.getTime());
        }
        return;
    }

    @Override
    public Object clone() {
        return new ElectionProtocolImpl(emitter_pid, delta, deltaPrim);
    }
    

}
