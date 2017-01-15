
import peersim.core.Node;

/**
 * @author mokuhazushi
 */
public class EmitterImpl implements Emitter {

    @Override
    public void emit(Node host, Message msg) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getLatency() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int getScope() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public Object clone() {
        return new EmitterImpl();
    }

    
}
