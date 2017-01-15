
import java.util.ArrayList;
import java.util.List;
import peersim.core.Node;

/**
 * @author mokuhazushi
 */
public class ElectionProtocolImpl implements ElectionProtocol {
    
    public ElectionProtocolImpl(String index) {
        
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
        return new ArrayList();
    }

    @Override
    public void processEvent(Node node, int pid, Object event) {
        System.out.println("election ici");
        return;
    }

    @Override
    public Object clone() {
        return new ElectionProtocolImpl("");
    }
    
    

}
