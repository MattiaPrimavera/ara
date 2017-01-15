
import java.util.List;
import peersim.core.Node;

/**
 * @author mokuhazushi
 */
public class ElectionProtocolImpl implements ElectionProtocol {

    @Override
    public boolean isInElection() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getIDLeader() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getMyValue() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public List<Long> getNeighbors() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void processEvent(Node node, int pid, Object event) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object clone() {
        return new ElectionProtocolImpl();
    }
    
    

}
