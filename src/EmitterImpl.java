
import peersim.core.Node;

/**
 * @author mokuhazushi
 */
public class EmitterImpl implements Emitter {

    public EmitterImpl(String index) {
    }

    @Override
    public void emit(Node host, Message msg) {
        return;
    }

    @Override
    public int getLatency() {
        return 0;
    }

    @Override
    public int getScope() {
        return 0;
    }
    
    @Override
    public Object clone() {
        return new EmitterImpl("");
    }

    
}
