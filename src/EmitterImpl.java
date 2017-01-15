
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

/**
 * @author mokuhazushi
 */
public class EmitterImpl implements Emitter {
    private static final String PAR_SCOPE = "scope";
    private static final String PAR_LATENCY = "latency";
    private static final String PAR_POSITIONPID = "positionprotocol";
    private static final String PAR_ELECTIONPID = "electionprotocol";
    
    private int position_pid;
    private int election_pid;
    private int scope;
    private int latency;

    public EmitterImpl(String prefix) {
        this.position_pid = Configuration.getPid(prefix+"."+PAR_POSITIONPID);
        election_pid = Configuration.getPid(prefix+"."+PAR_ELECTIONPID);
        this.scope = Configuration.getInt(prefix+"."+PAR_SCOPE);
        this.latency = Configuration.getInt(prefix+"."+PAR_LATENCY);
    }

    public EmitterImpl(int scope, int latency) {
        this.scope = scope;
        this.latency = latency;
    }

    @Override
    public void emit(Node host, Message msg) {
        Node myself;
        for (int i=0; i<Network.size(); i++) {
            Node n = Network.get(i);
            if (n.getID() == host.getID()) continue;
            
            PositionProtocol hostPos = (PositionProtocol) host.getProtocol(position_pid);
            PositionProtocol nPos = (PositionProtocol) n.getProtocol(position_pid);
            if (isInRadius(hostPos, nPos)) {
                //EMIT MESSAGE
                EDSimulator.add(0, msg, n, election_pid);
            }
        }
    }
    
    private boolean isInRadius(PositionProtocol host, PositionProtocol n) {
        double hostX = host.getX();
        double hostY = host.getY();
        double nX = host.getX();
        double nY = host.getY();
        
        return Math.pow(hostX-nX, 2) + Math.pow(hostY-nY, 2) < Math.pow(scope, 2);
    }

    @Override
    public int getLatency() {
        return this.latency;
    }

    @Override
    public int getScope() {
        return this.scope;
    }
    
    @Override
    public Object clone() {
        return new EmitterImpl(scope, latency);
    }

    
}
