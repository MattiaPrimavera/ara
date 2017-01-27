
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Control;
import peersim.core.Fallible;
import peersim.core.Network;
import peersim.core.Node;

/**
 * @author mokuhazushi
 */
public class EndControler implements Control{
    
    private static final String PAR_ELECTIONPID = "electionprotocol";
    private static final String PAR_POSITIONPID = "positionprotocol";
    private static final String PAR_TIMEUNIT = "time_unit";
    
    private final int election_pid;
    private final int position_pid;
    
    public EndControler(String prefix) {
        election_pid = Configuration.getPid(prefix+"."+PAR_ELECTIONPID);
        position_pid = Configuration.getPid(prefix+"."+PAR_POSITIONPID);
    }
	
	@Override
	public boolean execute() {
            int networkSize = Network.size();
            long averageTimeNoLeader = 0;
            double ecartTypeTimeNoLeader = 0;
            double averageElectionRate = 0;
            double ecartTypeElectionRate = 0;
            long averageElectionTime = 0;
            double ecartTypeElectionTime = 0;
            long averageMsgOverhead = 0;
            double ecartTypeMsgOverhead = 0;
            double averageNbLeader = 0;
            double ecartTypeNbLeader = 0;
            
            int max_speed = ((PositionProtocol)(Network.get(0).getProtocol(position_pid))).getMaxSpeed();
            double field_size = ((PositionProtocol)(Network.get(0).getProtocol(position_pid))).getMaxX() 
                    * ((PositionProtocol)(Network.get(0).getProtocol(position_pid))).getMaxY();
            int time_unit = ((PositionProtocol)(Network.get(0).getProtocol(position_pid))).getTimePause();
            
            System.out.println("End of simulation");
            //Processing all average
            for(int i=0; i<networkSize; i++){
                Node n = Network.get(i);
                ElectionProtocol elec = (ElectionProtocol) n.getProtocol(election_pid);
                ElectionProtocolImpl elecImpl = (ElectionProtocolImpl)elec;
                
                averageTimeNoLeader += elecImpl.getTimeNoLeader();
                averageElectionRate += elecImpl.getElectionRate();
                averageElectionTime += elecImpl.getElectionTime();
                averageMsgOverhead += elecImpl.getMsgOverhead();
                averageNbLeader += elecImpl.getNbTimesLeader();
            }
            
            averageTimeNoLeader /= networkSize;
            averageElectionRate /= networkSize;
            averageElectionTime /= networkSize;
            averageMsgOverhead /= networkSize;
            averageNbLeader /= networkSize;
            
            //Calcul de l'écart-type
            for(int i=0; i<networkSize; i++) {
                Node n = Network.get(i);
                ElectionProtocol elec = (ElectionProtocol) n.getProtocol(election_pid);
                ElectionProtocolImpl elecImpl = (ElectionProtocolImpl)elec;
                
                ecartTypeTimeNoLeader += Math.pow((elecImpl.getTimeNoLeader() - averageTimeNoLeader), 2);
                ecartTypeElectionRate += Math.pow((elecImpl.getElectionRate() - averageElectionRate), 2);
                ecartTypeElectionTime += Math.pow((elecImpl.getElectionTime() - averageElectionTime), 2);
                ecartTypeMsgOverhead += Math.pow((elecImpl.getMsgOverhead() - averageMsgOverhead), 2);
                ecartTypeNbLeader += Math.pow((elecImpl.getNbTimesLeader() - averageNbLeader), 2);
            }
            
            ecartTypeTimeNoLeader = (long)Math.sqrt(ecartTypeTimeNoLeader/networkSize);
            ecartTypeElectionRate = (long)Math.sqrt(ecartTypeElectionRate/networkSize);
            ecartTypeElectionTime = (long)Math.sqrt(ecartTypeElectionTime/networkSize);
            ecartTypeMsgOverhead = (long)Math.sqrt(ecartTypeMsgOverhead/networkSize);
            ecartTypeNbLeader = (long)Math.sqrt(ecartTypeNbLeader/networkSize);
            
            //Processing electionRate
            averageElectionRate = (averageElectionRate / CommonState.getEndTime()) * time_unit;
            ecartTypeElectionRate = (ecartTypeElectionRate / CommonState.getEndTime()) * time_unit;
            
            //Processing nbLeader
            averageNbLeader = (averageNbLeader / CommonState.getEndTime()) * time_unit;
            ecartTypeNbLeader = (ecartTypeNbLeader / CommonState.getEndTime()) * time_unit;
            
            //Printing
            System.out.println("Number of nodes = "+networkSize
                + "\nNode density ="+networkSize/(field_size)
                + "\nMax speed ="+max_speed
            
                + "\nAverage time without leader = "+averageTimeNoLeader
                + "\nEcart-type without leader = "+ecartTypeTimeNoLeader
                    
                + "\nAverage election rate = "+averageElectionRate
                + "\nEcart-type election rate = "+ecartTypeElectionRate
                    
                + "\nAverage election time = "+averageElectionTime
                + "\nEcart-type election time = "+ecartTypeElectionTime
                    
                + "\nAverage message overhead = "+averageMsgOverhead
                + "\nEcart-type message overhead = "+ecartTypeMsgOverhead
            
                + "\nAverage number of leader per unit of time = "+averageNbLeader
                + "\nEcart-type number of leader = "+ecartTypeNbLeader);
            
            System.exit(0);
            return false;
	}

}
