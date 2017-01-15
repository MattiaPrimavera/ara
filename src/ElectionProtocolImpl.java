
import java.util.ArrayList;
import java.util.List;
import peersim.config.Configuration;
import peersim.core.Node;

/**
 * @author mokuhazushi
 */
public class ElectionProtocolImpl implements ElectionProtocol {
    
    private static final String PAR_DELTA = "delta";
    private static final String PAR_DELTAPRIM = "deltaPrim";
    private static final String PAR_EMITTERPID = "emitter";
    
    private int emitter_pid;
    private List<Long> neighbors;
    private int delta;
    private int deltaPrim;
    
    public ElectionProtocolImpl(String prefix) {
        this.emitter_pid = Configuration.getPid(prefix+"."+PAR_EMITTERPID);
        this.delta = Configuration.getInt(prefix+"."+PAR_DELTA);
        this.deltaPrim = Configuration.getInt(prefix+"."+PAR_DELTAPRIM);
        this.neighbors = new ArrayList<>();
    }

    public ElectionProtocolImpl(int emitter_pid, int delta, int deltaPrim) {
        this.emitter_pid = emitter_pid;
        this.delta = delta;
        this.deltaPrim = deltaPrim;
        this.neighbors = new ArrayList<>();
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
        return neighbors;
    }

    @Override
    public void processEvent(Node node, int pid, Object event) {
        System.out.println("election ici");
        return;
    }

    @Override
    public Object clone() {
        return new ElectionProtocolImpl(emitter_pid, delta, deltaPrim);
    }
    
    

}
