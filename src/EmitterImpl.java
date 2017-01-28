
import peersim.config.Configuration;
import peersim.core.Network;
import peersim.core.Node;
import peersim.edsim.EDSimulator;

public class EmitterImpl implements Emitter {
    private static final String PAR_SCOPE = "scope";
    private static final String PAR_LATENCY = "latency";
    private static final String PAR_POSITIONPID = "positionprotocol";
    private static final String PAR_ELECTIONPID = "electionprotocol";
    
    private int emitter_pid;
    private int position_pid;
    private int election_pid;
    private int scope;
    private int latency;

    public EmitterImpl(String prefix) {
        String tmp[]=prefix.split("\\.");
        this.emitter_pid=Configuration.lookupPid(tmp[tmp.length-1]);
        this.position_pid = Configuration.getPid(prefix+"."+PAR_POSITIONPID);
        this.election_pid = Configuration.getPid(prefix+"."+PAR_ELECTIONPID);
        this.scope = Configuration.getInt(prefix+"."+PAR_SCOPE);
        this.latency = Configuration.getInt(prefix+"."+PAR_LATENCY);
    }

    public EmitterImpl(int scope, int latency, int emitter_pid, int position_pid, int election_pid) {
        this.scope = scope;
        this.latency = latency;
        this.emitter_pid = emitter_pid;
        this.position_pid = position_pid;
        this.election_pid = election_pid;
    }

    @Override
    public void emit(Node host, Message msg) {
        for (int i=0; i<Network.size(); i++) {
            Node n = Network.get(i);
            if (n.getID() == host.getID()) continue;
            
            PositionProtocol hostPos = (PositionProtocol) host.getProtocol(position_pid);
            PositionProtocol nPos = (PositionProtocol) n.getProtocol(position_pid);
            if (isInRadius(hostPos, nPos)) {
                //EMIT MESSAGE
                EDSimulator.add(latency, msg, n, emitter_pid);
            }
        }
    }

    @Override
    public void processEvent(Node node, int pid, Object event) {
        Message msg = (Message)event;
        Node src = Network.get((int)msg.getIdSrc());
         PositionProtocol hostPos = (PositionProtocol) node.getProtocol(position_pid);
            PositionProtocol nPos = (PositionProtocol) src.getProtocol(position_pid);
            if (isInRadius(hostPos, nPos)) {
                //EMIT MESSAGE
                ElectionProtocol electionProt = (ElectionProtocol) node.getProtocol(election_pid);
                electionProt.receiveMsg(msg);
            }
    }
    
    
    public boolean isInRadius(PositionProtocol host, PositionProtocol n) {
        double hostX = host.getX();
        double hostY = host.getY();
        double nX = n.getX();
        double nY = n.getY();
        
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
        return new EmitterImpl(scope, latency, emitter_pid, position_pid, election_pid);
    }

    
}
